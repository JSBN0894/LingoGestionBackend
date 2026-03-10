# Seguridad para Producción en Railway

## ⚠️ IMPORTANTE: Antes de desplegar

### 1. Generar JWT Secret Seguro

**NO subas el proyecto sin generar un nuevo secreto JWT.** El secreto por defecto está expuesto en el repositorio.

#### En Linux/Mac:
```bash
openssl rand -base64 64
```

#### En Windows (PowerShell):
```powershell
# Opcion 1: Con OpenSSL (si está instalado)
openssl rand -base64 64

# Opcion 2: Con .NET
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# Opcion 3: Online (solo para desarrollo, no producción)
# https://generate-secret.vercel.app/64
```

### 2. Configurar Variables en Railway

En el Dashboard de Railway:

1. Ve a tu proyecto → **Variables**
2. Agrega las siguientes variables:

| Variable | Valor | Requerido |
|----------|-------|-----------|
| `JWT_SECRET` | El valor generado arriba | ✅ OBLIGATORIO |
| `SPRING_PROFILES_ACTIVE` | `prod` | ✅ OBLIGATORIO |
| `CORS_ALLOWED_ORIGINS` | `https://tu-dominio.com` | ⚠️ Recomendado |
| `PORT` | `8080` | ℹ️ Railway lo asigna automáticamente |

### 3. Base de Datos

Railway automáticamente provee las variables de PostgreSQL cuando agregas un servicio de base de datos:

- `DATABASE_URL` (usada por defecto)
- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`

**No necesitas configurarlas manualmente.**

---

## ✅ Checklist de Seguridad

Antes de desplegar, verifica:

- [ ] `JWT_SECRET` generado y configurado en Railway
- [ ] `SPRING_PROFILES_ACTIVE=prod` configurado
- [ ] `.env` está en `.gitignore`
- [ ] `CORS_ALLOWED_ORIGINS` configurado con tus dominios reales
- [ ] Swagger UI deshabilitado en producción (`springdoc.swagger-ui.enabled=false`)
- [ ] Logging de seguridad en nivel `WARN`
- [ ] No hay secrets hardcoded en el código
- [ ] Contraseñas de base de datos no están en el repositorio

---

## 🔒 Variables de Entorno Requeridas

### Producción (Railway):
```bash
JWT_SECRET=<generado con openssl rand -base64 64>
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://tu-dominio.com,https://app.tu-dominio.com
```

### Desarrollo (local .env):
```bash
JWT_SECRET=RGV2U2VjcmV0S2V5Rm9yRGV2ZWxvcG1lbnRPbmx5TXVzdEJlQ2hhbmdlZEluUHJvZHVjdGlvbkVudmlyb25tZW50
SPRING_PROFILES_ACTIVE=dev
CORS_ALLOWED_ORIGINS=*
```

---

## 🚨 Problemas Comunes

### "JWT_SECRET no está configurado"
La aplicación no iniciará sin un secreto JWT válido. Configura la variable de entorno en Railway.

### "El JWT_SECRET es demasiado corto"
El secreto debe tener al menos 256 bits (32 bytes). Usa `openssl rand -base64 64` para generar uno seguro.

### Error de conexión a base de datos
Verifica que tengas un servicio PostgreSQL agregado en Railway y que las variables `DATABASE_URL` o `PG*` estén disponibles.

### CORS errors en el frontend
Configura `CORS_ALLOWED_ORIGINS` con las URLs exactas de tu frontend (sin trailing slash).

---

## 📋 Comandos Útiles

### Ver logs en Railway:
```bash
railway logs
```

### Variables de entorno en Railway:
```bash
railway variables list
railway variables set JWT_SECRET=tu_secreto
```

### Verificar que la app está corriendo:
```bash
curl https://tu-app.railway.app/api/health
```
