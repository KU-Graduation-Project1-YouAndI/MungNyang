package com.example.mungnyang.uicomponents.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.mungnyang.R
import com.example.mungnyang.model.Routes
import com.example.mungnyang.uicomponents.LocalNavGraphViewModelStoreOwner
import com.example.mungnyang.viewmodel.NavViewModel

@Composable
fun LoginScreen(onNavigateMain: ()->Unit) {
    val navViewModel: NavViewModel =
        viewModel(viewModelStoreOwner = LocalNavGraphViewModelStoreOwner.current)
//    val navViewModel: NavViewModel = viewModel()

    var userId by remember { mutableStateOf("") }
    var userPw by remember { mutableStateOf("") }

    val loginResult = navViewModel.checkInfo(userId, userPw)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBF9270)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFBF9270)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_big_bright),
                contentDescription = "",
                modifier = Modifier.size(160.dp)
            )
        }
        Spacer(modifier = Modifier.height(60.dp))

        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(400.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "회원가입",
                        color = Color(0xFFC8C7CC),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center

                    )
                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "로그인",
                            color = Color(0xFF000000),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center

                        )
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(3.dp)
                                .background(Color(0xFFFFF2C2))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEFEFF4))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        placeholder = {
                            Text(
                                text = "name@example.com",
                                color = Color(0xFFC8C7CC)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userPw,
                        onValueChange = { userPw = it },
                        placeholder = {
                            Text(
                                text = "••••••••••••",
                                color = Color(0xFFC8C7CC)
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            navViewModel.loginStatus.value = true
                            navViewModel.setUserInfo(userId, userPw)
                            if (loginResult) { onNavigateMain() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFF2C2),
                            contentColor = Color(0xFFBF9270)
                        )
                    ) {
                        Text(
                            text = "로그인",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Prev() {
    val navController = rememberNavController()
    LoginScreen(onNavigateMain = { navController.navigate(Routes.Main.route) })
}