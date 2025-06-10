package com.example.mungnyang.model.fd;

/*
public class federateLearning {
    public static final int IMAGE_SIZE = 224;
    public static final int TOTAL_PARAMS = 4263751;

    public static void runTraining(Bitmap bmp, Interpreter tflite, Context context) {
        // 전처리
        ByteBuffer input = preprocessImage(bmp);

        // 예측 (inference 전)
        float[][] probsBefore = new float[1][7];
        Map<String, Object> inferIn = new HashMap<>();
        Map<String, Object> inferOut = new HashMap<>();
        inferIn.put("input", input);
        inferOut.put("output", probsBefore);
        tflite.runSignature(inferIn, inferOut, "infer");

        int predictedIdx = argmax(probsBefore[0]);

        // 학습
        ByteBuffer yBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        yBuf.putInt(predictedIdx).rewind();

        float[] delta = new float[TOTAL_PARAMS];
        float[] loss = new float[1];

        Map<String, Object> trainIn = new HashMap<>();
        Map<String, Object> trainOut = new HashMap<>();
        trainIn.put("x", input);
        trainIn.put("y", yBuf);
        trainOut.put("delta", delta);
        trainOut.put("loss", loss);

        tflite.runSignature(trainIn, trainOut, "train_and_delta");

        saveAndSendDelta(delta, context);
    }

    private static ByteBuffer preprocessImage(Bitmap bmp) {
        Bitmap resized = Bitmap.createScaledBitmap(bmp, IMAGE_SIZE, IMAGE_SIZE, true);
        ByteBuffer buf = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4).order(ByteOrder.nativeOrder());
        int[] px = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(px, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
        for (int p : px) {
            buf.putFloat(((p >> 16) & 0xFF) / 255f);
            buf.putFloat(((p >> 8) & 0xFF) / 255f);
            buf.putFloat((p & 0xFF) / 255f);
        }
        buf.rewind();
        return buf;
    }

    private static void saveAndSendDelta(float[] delta, Context context) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(delta.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (float v : delta) buffer.putFloat(v);
            byte[] bytes = buffer.array();

            String fileName = "delta_" + System.currentTimeMillis() + ".bin";
            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            sendToServer(file);
        } catch (Exception e) {
            Log.e("DeltaTrainer", "Save failed", e);
        }
    }

    private static void sendToServer(File file) {
        RequestBody body = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("delta", file.getName(), body);

        UploadAPI api = ApiClient.getClient().create(UploadAPI.class);
        api.uploadDeltaBatch(new MultipartBody.Part[]{part}).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("UPLOAD", "Upload success " + response.code());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UPLOAD", "Upload failed", t);
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
*/

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
            "구진 플라크", "비듬/각질/상피성잔고리", "태선화/과다색소침착",
            "농포/여드름", "미란/궤양", "결절/종괴", "무증상"
    };

    public static void runTraining(Bitmap bmp, Interpreter tflite, Context context) {
        ByteBuffer xBuf = preprocessImage(bmp);

        // 1. 학습 전 추론
        float[][] probsBefore = new float[1][7];
        Map<String, Object> inferIn = new HashMap<>();
        Map<String, Object> inferOut = new HashMap<>();
        inferIn.put("input", xBuf);
        inferOut.put("output", probsBefore);
        tflite.runSignature(inferIn, inferOut, "infer");

        int idxBefore = argmax(probsBefore[0]);
        float confBefore = probsBefore[0][idxBefore];

        // 2. 학습 실행
        ByteBuffer yBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        yBuf.putInt(idxBefore).rewind();

        float[] delta = new float[TOTAL_PARAMS];
        float[] loss = new float[1];
        Map<String, Object> trainIn = new HashMap<>();
        Map<String, Object> trainOut = new HashMap<>();
        trainIn.put("x", xBuf);
        trainIn.put("y", yBuf);
        trainOut.put("delta", delta);
        trainOut.put("loss", loss);
        tflite.runSignature(trainIn, trainOut, "train_and_delta");

        // 3. 학습 후 추론
        float[][] probsAfter = new float[1][7];
        Map<String, Object> inferIn2 = new HashMap<>();
        Map<String, Object> inferOut2 = new HashMap<>();
        inferIn2.put("input", xBuf);
        inferOut2.put("output", probsAfter);
        tflite.runSignature(inferIn2, inferOut2, "infer");

        int idxAfter = argmax(probsAfter[0]);
        float confAfter = probsAfter[0][idxAfter];

        Log.d("FED_LEARN", "🔎 Before: " + CLASS_NAMES[idxBefore] + String.format(" (%.2f%%)", confBefore * 100));
        Log.d("FED_LEARN", "🔎 After : " + CLASS_NAMES[idxAfter] + String.format(" (%.2f%%)", confAfter * 100));
        Log.d("FED_LEARN", "Params  : " + delta.length);

        // 4. 신뢰도 낮으면 재학습
        if (confAfter < 0.7f) {
            Log.w("DELTA_FILTER", "⚠️ 학습 결과 신뢰도 낮음. 원래 정답(label=" + idxBefore + ") 기준으로 재학습");
            final int MAX_EPOCHS_RETRY = 100;
            final float THRESHOLD_RETRY = 1e-6f;

            float[] deltaRetry = new float[TOTAL_PARAMS];
            float[] prevDelta = null;

            for (int epoch = 1; epoch <= MAX_EPOCHS_RETRY; epoch++) {
                ByteBuffer yBuf2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
                yBuf2.putInt(idxBefore).rewind();

                Map<String, Object> trainIn2 = new HashMap<>();
                Map<String, Object> trainOut2 = new HashMap<>();
                trainIn2.put("x", xBuf);
                trainIn2.put("y", yBuf2);
                trainOut2.put("delta", deltaRetry);
                trainOut2.put("loss", new float[1]);

                tflite.runSignature(trainIn2, trainOut2, "train_and_delta");

                if (prevDelta != null) {
                    float maxDiff = 0f;
                    for (int i = 0; i < TOTAL_PARAMS; i++) {
                        float diff = Math.abs(deltaRetry[i] - prevDelta[i]);
                        if (diff > maxDiff) maxDiff = diff;
                    }
                    Log.d("DELTA_RETRY", "epoch=" + epoch + ", maxDeltaDiff=" + maxDiff);
                    if (maxDiff < THRESHOLD_RETRY) {
                        Log.d("CONVERGENCE", "✅ 수렴 완료 (epoch=" + epoch + ")");
                        break;
                    }
                }

                if (prevDelta == null) prevDelta = new float[TOTAL_PARAMS];
                System.arraycopy(deltaRetry, 0, prevDelta, 0, TOTAL_PARAMS);
            }

            delta = deltaRetry;
        }

        saveAndSendDelta(delta, context);
    }

    private static ByteBuffer preprocessImage(Bitmap bmp) {
        Bitmap resized = Bitmap.createScaledBitmap(bmp, IMAGE_SIZE, IMAGE_SIZE, true);
        ByteBuffer buf = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4).order(ByteOrder.nativeOrder());
        int[] px = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(px, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
        for (int p : px) {
            buf.putFloat(((p >> 16) & 0xFF) / 255f);
            buf.putFloat(((p >> 8) & 0xFF) / 255f);
            buf.putFloat((p & 0xFF) / 255f);
        }
        buf.rewind();
        return buf;
    }

    private static void saveAndSendDelta(float[] delta, Context context) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(delta.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (float v : delta) buffer.putFloat(v);
            byte[] bytes = buffer.array();

            String fileName = "delta_" + System.currentTimeMillis() + ".bin";
            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            sendToServer(file);
        } catch (Exception e) {
            Log.e("DeltaTrainer", "Save failed", e);
        }
    }

    private static void sendToServer(File file) {
        RequestBody body = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("delta", file.getName(), body);

        UploadAPI api = ApiClient.getClient().create(UploadAPI.class);
        api.uploadDeltaBatch(new MultipartBody.Part[]{part}).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("UPLOAD", "Upload success " + response.code());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UPLOAD", "Upload failed", t);
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
