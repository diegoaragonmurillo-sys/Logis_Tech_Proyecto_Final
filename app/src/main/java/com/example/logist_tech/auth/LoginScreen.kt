package com.example.logist_tech.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logist_tech.R
import com.example.logist_tech.network.LoginRequest
import com.example.logist_tech.network.RetrofitClient
import kotlinx.coroutines.launch

private val AzulPrimario = Color(0xFF2980B9)
private val AzulSuave = Color(0xFFEAF3FB)
private val GrisTexto = Color(0xFF555555)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}
) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun intentarLogin() {
        when {
            usuario.isBlank()  -> { errorMsg = "Ingresa tu usuario"; return }
            password.isBlank() -> { errorMsg = "Ingresa tu contraseña"; return }
        }
        errorMsg = null
        cargando = true
        scope.launch {
            try {
                val resp = RetrofitClient.api.login(
                    LoginRequest(username = usuario.trim(), password = password)
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    val rolParsed = runCatching {
                        SessionManager.Rol.valueOf(body.rol.uppercase())
                    }.getOrNull()

                    if (body.token.isNullOrBlank() || rolParsed == null) {
                        errorMsg = "Respuesta inválida del servidor"
                    } else {
                        SessionManager.login(
                            token = body.token!!,
                            username = body.username ?: usuario.trim(),
                            rol = rolParsed
                        )
                        onLoginSuccess()
                    }
                } else if (resp.code() == 401 || resp.code() == 422) {
                    errorMsg = "Usuario o contraseña incorrectos"
                } else {
                    errorMsg = "Error del servidor (${resp.code()})"
                }
            } catch (e: Exception) {
                errorMsg = "No se pudo conectar. Revisa tu red."
            } finally {
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp).clip(CircleShape).background(AzulSuave)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_logistech),
                contentDescription = "LogisTech Logo",
                modifier = Modifier.size(215.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("LogisTech System", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AzulPrimario)
        Spacer(Modifier.height(4.dp))
        Text("Acceso para personal de operación", fontSize = 14.sp, color = GrisTexto)

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it; errorMsg = null },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !cargando,
            isError = errorMsg != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = campoColors()
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMsg = null },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !cargando,
            isError = errorMsg != null,
            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { passVisible = !passVisible }) {
                    Icon(
                        if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passVisible) "Ocultar" else "Mostrar",
                        tint = GrisTexto
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = campoColors()
        )

        if (errorMsg != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { intentarLogin() },
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario)
        ) {
            if (cargando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp))
            } else {
                Text("INGRESAR AL SISTEMA", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor     = Color(0xFF111111),
    unfocusedTextColor   = Color(0xFF111111),
    focusedBorderColor   = AzulPrimario,
    unfocusedBorderColor = Color(0xFFBBBBBB),
    focusedLabelColor    = AzulPrimario,
    cursorColor          = AzulPrimario
)