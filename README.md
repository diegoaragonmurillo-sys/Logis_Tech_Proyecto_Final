# LogisTech

LogisTech es una aplicación móvil de logística para Android que automatiza el registro de cajas mediante reconocimiento de texto (OCR) y lectura de códigos QR. La app integra un backend en la nube y un módulo de inteligencia artificial que genera reportes automáticos, reduciendo el tiempo de registro y los errores humanos en procesos logísticos.

## Stack tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Arquitectura:** MVVM (ViewModel)
- **Cámara:** CameraX
- **Reconocimiento (ML):** Google ML Kit
  - Barcode Scanning (lectura de códigos QR)
  - Text Recognition / OCR (lectura de texto en imágenes)
- **Inteligencia Artificial:** Gemini Flash (generación de reportes)
- **Networking:** Retrofit + Gson Converter
- **Tiempo real:** WebSocket
- **Navegación:** Navigation Compose
- **Backend:** FastAPI (API REST)
- **Hosting:** VPS de Elastika

## Componente de IA

LogisTech combina dos niveles de IA:

1. **Google ML Kit (captura de datos):** Barcode Scanning y Text Recognition procesan en tiempo real lo que captura la cámara (`CameraX`), reconociendo códigos QR y texto de etiquetas. Corren on-device, sin necesidad de conexión a internet para esta parte.
2. **Gemini Flash (módulo de reportes):** a partir de los datos ya registrados y almacenados en el backend, Gemini Flash genera reportes automáticos en la pantalla "Reporte IA", interpretando la información logística almacenada y devolviendo un análisis en tiempo real (no es una respuesta fija ni hardcodeada).

## Servicio externo: Elastika

El backend (FastAPI) está alojado en un **VPS de Elastika**, lo que permite mantener el sistema disponible de forma permanente y accesible de forma remota para la app móvil.

## Capturas de pantalla

> Agregar aquí mínimo 3 capturas (login, escaneo QR/OCR, dashboard, historial o reporte IA).

![Login](ruta/a/captura1.png)
![Escaneo](ruta/a/captura2.png)
![Dashboard](ruta/a/captura3.png)

## Video demostrativo

[Ver video en YouTube](https://youtu.be/RV9x3drUul8) https://youtu.be/RV9x3drUul8

## Integrantes

- Katherine Melany Quispe Romero
- Alexandra Fedra Robles Uscamayta
- Victor Delgado

## Configuración del proyecto

LogisTech no requiere API keys dentro del código Android. La URL del backend está definida en `RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "http://38.250.116.214:8080/"
```

La clave de Gemini Flash se configura del lado del backend (FastAPI), no en la app móvil.

### Pasos para ejecutar el proyecto

1. Clonar el repositorio:
   ```
   git clone https://github.com/diegoaragonmurillo-sys/Logis_Tech_Proyecto_Final.git
   ```
2. Abrir el proyecto en Android Studio (versión reciente, compileSdk 35).
3. Esperar la sincronización de Gradle.
4. Conectar un dispositivo o emulador con Android 8.0 (API 26) o superior.
5. Otorgar permiso de cámara cuando la app lo solicite (necesario para el escáner QR/OCR).
6. Ejecutar la app (Run ▶).

### Requisitos

- Android Studio actualizado
- JDK 11
- Conexión a internet (para sincronizar con el backend y generar reportes con IA)
