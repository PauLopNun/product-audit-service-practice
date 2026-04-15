# Postman - llamadas completas (audit-service + data-service)

## 1) Importar en Postman
1. Abre Postman.
2. `Import` -> `File`.
3. Selecciona `POSTMAN_COLLECTION_ALL_SERVICES.json`.

## 2) Variables de la coleccion
La coleccion incluye variables editables:
- `auditBaseUrl` (default: `http://localhost:8080`)
- `dataBaseUrl` (default: `http://localhost:8081`)
- `userId`, `productId`, `revision`, `fromRev`, `toRev`, `nameLike`, `newUserName`, `updatedUserName`

## 3) Carpetas incluidas
- **Audit Service (8080) - Proyecto actual**
  - Endpoints de `UserController` y `ProductController`
  - Swagger (`/swagger-ui.html`) y OpenAPI (`/v3/api-docs`)
- **Data Service (8081) - Proyecto vinculado**
  - Endpoints ` /api/users`, `/api/allergies`, `/api/products` y auditoria consumidos por los adapters REST

## 4) Nota importante
Esta coleccion esta basada en los endpoints del proyecto actual y en las rutas consumidas por los adapters REST hacia el servicio vinculado. Si en el proyecto vinculado hay endpoints extra, se pueden anadir en una segunda pasada.

