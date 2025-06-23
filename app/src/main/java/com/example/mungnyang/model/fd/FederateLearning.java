package com.example.mungnyang.model.fd;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.mungnyang.api.ApiClient;
import com.example.mungnyang.api.UploadAPI;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FederateLearning {
    public static final int IMAGE_SIZE = 224;
    public static final int TOTAL_PARAMS = 4263751;
    private static final String[] CLASS_NAMES = {
            "1_구진 플라크", "2_비듬/각질/상피성잔고리", "3_태선화/과다색소침착",
            "4_농포/여드름", "5_미란/궤양", "6_결절/종괴", "7_무증상"
    };

    public static void runTraining(Bitmap bmp, Interpreter tflite, Context context) {
        ByteBuffer xBuf = preprocessImage(bmp);

        float[][] probsBefore = new float[1][7];
        Map<String, Object> inferIn = new HashMap<>();
        Map<String, Object> inferOut = new HashMap<>();
        inferIn.put("input", xBuf);
        inferOut.put("output", probsBefore);
        tflite.runSignature(inferIn, inferOut, "infer");

        int idxBefore = argmax(probsBefore[0]);
        float confBefore = probsBefore[0][idxBefore];

        final int MAX_EPOCHS = 100;
        final float THRESHOLD = 1e-6f;
        float[] delta = new float[TOTAL_PARAMS];
        float[] prevDelta = null;

        for (int epoch = 1; epoch <= MAX_EPOCHS; epoch++) {
            ByteBuffer yBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
            yBuf.putInt(idxBefore).rewind();

            Map<String, Object> trainIn = new HashMap<>();
            Map<String, Object> trainOut = new HashMap<>();
            trainIn.put("x", xBuf);
            trainIn.put("y", yBuf);
            trainOut.put("delta", delta);
            trainOut.put("loss", new float[1]);

            tflite.runSignature(trainIn, trainOut, "train_and_delta");

            if (prevDelta != null) {
                float maxDiff = 0f;
                for (int i = 0; i < TOTAL_PARAMS; i++) {
                    float diff = Math.abs(delta[i] - prevDelta[i]);
                    if (diff > maxDiff) maxDiff = diff;
                }
                Log.d("CONVERGENCE", "epoch=" + epoch + " maxDeltaDiff=" + maxDiff);
                if (maxDiff < THRESHOLD) break;
            }

            if (prevDelta == null) prevDelta = new float[TOTAL_PARAMS];
            System.arraycopy(delta, 0, prevDelta, 0, TOTAL_PARAMS);
        }

        float[][] probsAfter = new float[1][7];
        Map<String, Object> inferIn2 = new HashMap<>();
        Map<String, Object> inferOut2 = new HashMap<>();
        inferIn2.put("input", xBuf);
        inferOut2.put("output", probsAfter);
        tflite.runSignature(inferIn2, inferOut2, "infer");

        int idxAfter = argmax(probsAfter[0]);
        float confAfter = probsAfter[0][idxAfter];

        Log.d("FED_LEARN", "\uD83D\uDD0E Before: " + CLASS_NAMES[idxBefore] + String.format(" (%.2f%%)", confBefore * 100));
        Log.d("FED_LEARN", "\uD83D\uDD0E After : " + CLASS_NAMES[idxAfter] + String.format(" (%.2f%%)", confAfter * 100));
        Log.d("FED_LEARN", "Params  : " + delta.length);

        if (confAfter < confBefore) {
            Log.w("DELTA", "학습 결과 악화 → 학습 전 label로 재학습 시작");
            float[] deltaRetry = new float[TOTAL_PARAMS];
            float[] prevDeltaRetry = null;
            for (int epoch = 1; epoch <= MAX_EPOCHS; epoch++) {
                ByteBuffer yBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
                yBuf.putInt(idxBefore).rewind();
                Map<String, Object> trainIn = new HashMap<>();
                Map<String, Object> trainOut = new HashMap<>();
                trainIn.put("x", xBuf);
                trainIn.put("y", yBuf);
                trainOut.put("delta", deltaRetry);
                trainOut.put("loss", new float[1]);
                tflite.runSignature(trainIn, trainOut, "train_and_delta");

                if (prevDeltaRetry != null) {
                    float maxDiff = 0f;
                    for (int i = 0; i < TOTAL_PARAMS; i++) {
                        float diff = Math.abs(deltaRetry[i] - prevDeltaRetry[i]);
                        if (diff > maxDiff) maxDiff = diff;
                    }
                    Log.d("DELTA_RETRY", "epoch=" + epoch + ", maxDeltaDiff=" + maxDiff);
                    if (maxDiff < THRESHOLD) break;
                }

                if (prevDeltaRetry == null) prevDeltaRetry = new float[TOTAL_PARAMS];
                System.arraycopy(deltaRetry, 0, prevDeltaRetry, 0, TOTAL_PARAMS);
            }
            delta = deltaRetry;
            Log.d("DELTA", "재학습된 델타로 저장 진행");
        }

        saveAndSendDelta(delta, context);
    }

    private static ByteBuffer preprocessImage(Bitmap bmp) {
        Bitmap safeBitmap = bmp.getConfig() == Bitmap.Config.HARDWARE
                ? bmp.copy(Bitmap.Config.ARGB_8888, true)
                : bmp;


        Bitmap resized = Bitmap.createScaledBitmap(safeBitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        ByteBuffer buf = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4).order(ByteOrder.nativeOrder());
        int[] px = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(px, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
        for (int p : px) {
            buf.putFloat(((p >> 16) & 0xFF) / 255f);
            buf.putFloat(((p >> 8) & 0xFF) / 255f);
            buf.putFloat((p & 0xFF) / 255f);
        }
        buf.rewind();

        if (resized != bmp) resized.recycle();
        if (safeBitmap != bmp) safeBitmap.recycle();

        return buf;
    }

    private static void saveAndSendDelta(float[] delta, Context context) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(delta.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (float v : delta) buffer.putFloat(v);
            byte[] bytes = buffer.array();

            String fileName = "converged_delta_" + System.currentTimeMillis() + ".bin";
            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            maybeUploadDeltaBatch(context);
        } catch (Exception e) {
            Log.e("DeltaTrainer", "Save failed", e);
        }
    }

    private static void maybeUploadDeltaBatch(Context context) {
        File dir = context.getFilesDir();
        File[] deltaFiles = dir.listFiles((file) ->
                file.getName().startsWith("converged_delta_") && file.getName().endsWith(".bin")
        );

        if (deltaFiles == null || deltaFiles.length < 5) {
            Log.d("BATCH", "현재 누적 델타 수: " + (deltaFiles == null ? 0 : deltaFiles.length));
            return;
        }

        Arrays.sort(deltaFiles, Comparator.comparingLong(File::lastModified));
        File[] batch = Arrays.copyOfRange(deltaFiles, 0, 5);
        uploadDeltaBatch(batch);
    }

    private static void uploadDeltaBatch(File[] deltaFiles) {
        MultipartBody.Part[] parts = new MultipartBody.Part[deltaFiles.length];

        for (int i = 0; i < deltaFiles.length; i++) {
            File file = deltaFiles[i];
            RequestBody body = RequestBody.create(file, MediaType.parse("application/octet-stream"));
            parts[i] = MultipartBody.Part.createFormData("delta", file.getName(), body);
        }

        UploadAPI api = ApiClient.getClient().create(UploadAPI.class);
        api.uploadDeltaBatch(parts).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("UPLOAD", "배치 업로드 완료 → " + response.code());
                for (File file : deltaFiles) {
                    if (file.delete()) {
                        Log.d("UPLOAD", "삭제 완료: " + file.getName());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UPLOAD", "배치 업로드 실패", t);
            }
        });
    }

    private static int argmax(float[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) maxIdx = i;
        }
        return maxIdx;
    }
}