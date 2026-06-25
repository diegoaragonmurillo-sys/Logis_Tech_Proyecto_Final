package com.example.logist_tech

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.logist_tech.auth.LoginScreen
import com.example.logist_tech.auth.SessionManager
import com.example.logist_tech.scanner.ScannerResultHolder
import com.example.logist_tech.scanner.ScannerScreen
import com.example.logist_tech.ui.screens.HomeScreen
import com.example.logist_tech.ui.screens.events.GestionCajaScreen
import com.example.logist_tech.ui.screens.events.RegistroCajaScreen
import com.example.logist_tech.ui.theme.Logist_TechTheme
import com.example.logist_tech.ui.viewmodels.LogistViewModel
import com.example.logist_tech.ocr.OcrResultScreen
import com.example.logist_tech.ui.screens.DashboardScreen
import com.example.logist_tech.ui.screens.GlobalHistoryScreen
import com.example.logist_tech.ui.screens.ReporteScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Logist_TechTheme {
                val viewModel: LogistViewModel = viewModel()
                val context = LocalContext.current

                LaunchedEffect(SessionManager.estaLogueado()) {
                    if (SessionManager.estaLogueado()) {
                        viewModel.listenToNotifications()
                    }
                }

                LaunchedEffect(viewModel.ultimaNotificacion) {
                    viewModel.ultimaNotificacion?.let {
                        Toast.makeText(context, "NOTIFICACIÓN: $it", Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (SessionManager.estaLogueado()) "home" else "login"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onNavigateScanner = { navController.navigate("scanner") },
                                onNavigateHistory = { navController.navigate("history") },
                                onNavigateDashboard = { navController.navigate("dashboard") },
                                onNavigateReporte = { navController.navigate("reporte") },
                                onLogout = {
                                    SessionManager.logout()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(onNavigateBack = { navController.popBackStack() }, viewModel = viewModel)
                        }

                        composable("history") {
                            GlobalHistoryScreen(onNavigateBack = { navController.popBackStack() }, viewModel = viewModel)
                        }

                        composable("reporte") {
                            ReporteScreen(onNavigateBack = { navController.popBackStack() }, viewModel = viewModel)
                        }

                        composable("scanner") {
                            ScannerScreen(
                                onNavigarResultado = {
                                    val qr = ScannerResultHolder.textoQr
                                    val ocr = ScannerResultHolder.textoOcr

                                    when {
                                        qr.isNotBlank() -> {
                                            val qrData = com.example.logist_tech.ocr.OcrProcessor.parsearQr(qr)
                                            val idCaja = qrData?.idCaja?.ifBlank { qr } ?: qr
                                            val encodedQr = Uri.encode(idCaja)

                                            val destino = if (SessionManager.rol == SessionManager.Rol.RECEPTOR)
                                                "registro_caja/$encodedQr"
                                            else
                                                "gestion_caja/$encodedQr"
                                            navController.navigate(destino)
                                        }

                                        ocr.isNotBlank() -> {
                                            val qrData = com.example.logist_tech.ocr.OcrProcessor.parsearQr(ocr)
                                            val idCaja = qrData?.idCaja ?: ""

                                            if (idCaja.isNotBlank()) {
                                                val encodedQr = Uri.encode(idCaja)
                                                val destino = if (SessionManager.rol == SessionManager.Rol.RECEPTOR)
                                                    "registro_caja/$encodedQr"
                                                else
                                                    "gestion_caja/$encodedQr"
                                                navController.navigate(destino)
                                            } else {
                                                navController.navigate("ocr_result")
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        composable("registro_caja/{qr}") { backStackEntry ->
                            val encodedQr = backStackEntry.arguments?.getString("qr") ?: ""
                            val qr = Uri.decode(encodedQr)
                            RegistroCajaScreen(
                                codigoQr = qr,
                                onSuccess = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                onBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }

                        composable("gestion_caja/{qr}") { backStackEntry ->
                            val encodedQr = backStackEntry.arguments?.getString("qr") ?: ""
                            val qr = Uri.decode(encodedQr)
                            GestionCajaScreen(
                                codigoQr = qr,
                                onSuccess = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                onBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }

                        composable("ocr_result") {
                            OcrResultScreen(
                                textoOcr = ScannerResultHolder.textoOcr,
                                textoQr = ScannerResultHolder.textoQr,
                                imagenCapturada = ScannerResultHolder.imagenBitmap,
                                onRegistrarEnInventario = {
                                    navController.navigate("registro_caja/${Uri.encode(" ")}") {
                                        popUpTo("ocr_result") { inclusive = true }
                                    }
                                },
                                onVolver = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}