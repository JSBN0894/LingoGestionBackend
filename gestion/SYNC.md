# Sincronización para App Móvil

## Descripción

Este módulo proporciona endpoints para que la app móvil Android pueda sincronizar los datos básicos de operación (catálogo de productos, categorías y estados).

## Endpoints

### 1. Obtener versión actual de sincronización

```http
GET /api/sync/version
```

**Respuesta:**
```json
{
  "needsSync": false,
  "currentVersion": 5,
  "description": "Producto creado: Molde de silicona rosa"
}
```

### 2. Validar si necesita sincronizar

La app envía su versión local y el servidor indica si necesita actualizar.

```http
POST /api/sync/validate
Content-Type: application/json

{
  "clientVersion": 3
}
```

**Respuesta:**
```json
{
  "needsSync": true,
  "currentVersion": 5,
  "description": "Producto creado: Molde de silicona rosa"
}
```

### 3. Obtener catálogo completo

Descarga todos los productos y categorías con la versión actual.

```http
GET /api/sync/catalog
```

**Respuesta:**
```json
{
  "version": 5,
  "products": [
    {
      "id": 1,
      "name": "Molde de silicona rosa",
      "pricePerUnit": 15000,
      "stock": 50,
      "imageUrl": "https://example.com/image.jpg",
      "description": "Molde de silicona para repostería",
      "categoryId": 3,
      "categoryName": "Moldes de silicona"
    }
  ],
  "categories": [
    {
      "id": 1,
      "name": "Moldes",
      "parentId": null
    },
    {
      "id": 3,
      "name": "Moldes de silicona",
      "parentId": 1
    }
  ]
}
```

### 4. Obtener estados

Descarga todos los estados de operaciones y envíos.

```http
GET /api/sync/states
```

**Respuesta:**
```json
{
  "version": 5,
  "states": [
    {
      "id": 1,
      "name": "Pendiente",
      "priority": 1
    },
    {
      "id": 2,
      "name": "Confirmado",
      "priority": 2
    }
  ],
  "shipmentStates": [
    {
      "id": 1,
      "name": "Por enviar",
      "priority": 1
    },
    {
      "id": 2,
      "name": "En tránsito",
      "priority": 2
    }
  ]
}
```

### 5. Incrementar versión manualmente

Útil cuando se hacen cambios masivos y se quiere forzar la sincronización.

```http
POST /api/sync/version/increment?description=Actualización masiva de precios
```

**Respuesta:**
```json
{
  "version": 6,
  "description": "Actualización masiva de precios"
}
```

## Flujo recomendado para la app móvil

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Al iniciar la app, consultar versión actual              │
│    GET /api/sync/version                                    │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Comparar con versión local almacenada                    │
│    - Si son iguales → No hacer nada                         │
│    - Si la del servidor es mayor → Sincronizar              │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Si necesita sincronizar:                                 │
│    a) GET /api/sync/catalog → Guardar productos y categorías│
│    b) GET /api/sync/states → Guardar estados                │
│    c) Actualizar versión local en SharedPreferences         │
└─────────────────────────────────────────────────────────────┘
```

## Ejemplo de implementación en Android (Kotlin)

```kotlin
class SyncManager @Inject constructor(
    private val apiService: ApiService,
    private val preferences: SyncPreferences
) {
    suspend fun syncIfNeeded(): SyncResult {
        // 1. Obtener versión del servidor
        val serverVersion = apiService.getSyncVersion()
        val localVersion = preferences.getClientVersion()
        
        // 2. Verificar si necesita sincronizar
        if (serverVersion.currentVersion <= localVersion) {
            return SyncResult.NoUpdateNeeded
        }
        
        // 3. Sincronizar datos
        return try {
            val catalog = apiService.getSyncCatalog()
            val states = apiService.getSyncStates()
            
            // Guardar en base de datos local
            saveCatalog(catalog)
            saveStates(states)
            
            // Actualizar versión local
            preferences.setClientVersion(catalog.version)
            
            SyncResult.Success(catalog.version)
        } catch (e: Exception) {
            SyncResult.Error(e)
        }
    }
}

sealed class SyncResult {
    object NoUpdateNeeded : SyncResult()
    data class Success(val newVersion: Long) : SyncResult()
    data class Error(val exception: Throwable) : SyncResult()
}
```

## Seguridad

Los endpoints de sincronización están configurados para **no requerir autenticación**, permitiendo que la app móvil obtenga los datos públicos (catálogo y estados) sin credenciales.

Si necesitas proteger estos endpoints en el futuro, puedes:
1. Requerir un API Key en los headers
2. Implementar autenticación con JWT
3. Usar certificados de cliente

## Base de datos

La versión de sincronización se almacena en la tabla `sync_version`:

```sql
CREATE TABLE sync_version (
    id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

La versión se incrementa automáticamente cuando:
- Se crea un producto
- Se actualiza un producto
- Se elimina un producto
