# Configuración Android - Linogo API

## Setup de la Base URL

La URL del backend no está hardcodeada en el repositorio por seguridad. Sigue estos pasos para configurarla:

## Opción 1: BuildConfig (Recomendada)

En tu `build.gradle.kts` (app level):

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"https://tu-app.railway.app\"")
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://tu-app.railway.app\"")
        }
    }
}
```

Luego en tu código Kotlin:

```kotlin
object ApiClient {
    private val BASE_URL = BuildConfig.BASE_URL
    // ...
}
```

## Opción 2: local.properties

1. Crea/edita `local.properties` en la raíz de tu proyecto Android:

```properties
api.base.url.debug=http://10.0.2.2:8080
api.base.url.release=https://tu-app.railway.app
```

2. Agrega `local.properties` a tu `.gitignore`:

```gitignore
local.properties
```

3. En tu `build.gradle.kts`:

```kotlin
val apiBaseUrlDebug = project.findProperty("api.base.url.debug") as? String ?: "http://10.0.2.2:8080"
val apiBaseUrlRelease = project.findProperty("api.base.url.release") as? String ?: "https://tu-app.railway.app"

android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrlDebug\"")
    }
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrlDebug\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrlRelease\"")
        }
    }
}
```

## Permisos Requeridos

En `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Network Security Config

Crea `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Solo para desarrollo con HTTP -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

Referencia en el `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
</application>
```

## Dependencias Mínimas

```kotlin
dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

## Verificación

Para verificar que la configuración es correcta:

```kotlin
// En tu Application o MainActivity
init {
    println("BASE_URL: ${BuildConfig.BASE_URL}")
    println("DEBUG: ${BuildConfig.DEBUG}")
}
```

Deberías ver:
- **Debug:** `http://10.0.2.2:8080`
- **Release:** `https://tu-app.railway.app`
