# Guía de lo que he implementado

Explicación de las decisiones que tomé y por qué, para que sea fácil de seguir.

---

## 1. La base de datos — PostgreSQL con Docker

En vez de instalar PostgreSQL en la máquina, uso Docker. El archivo `docker-compose.yml` define el contenedor:

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16
    container_name: demo-postgres
    environment:
      POSTGRES_DB: demo
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"   # puerto 5433 en mi máquina → 5432 dentro del contenedor
    volumes:
      - demo-postgres-data:/var/lib/postgresql/data  # los datos persisten aunque pares el contenedor
```

Y en `application.yaml` le digo a Spring cómo conectarse:

```yaml
datasource:
  url: jdbc:postgresql://127.0.0.1:5433/demo
  username: postgres
  password: postgres
```

Lo bueno es que Spring Boot detecta el `docker-compose.yml` automáticamente y levanta el contenedor solo al arrancar la app. No tengo que hacer `docker-compose up` manualmente.

---

## 2. Migraciones con Liquibase — por qué no uso ddl-auto: create

Hibernate tiene una opción `ddl-auto: create` que crea las tablas automáticamente a partir de las entidades Java. Lo he puesto en `validate` a propósito:

```yaml
# application.yaml
jpa:
  hibernate:
    ddl-auto: validate  # solo comprueba que el schema coincide, no lo modifica
```

Esto es porque en proyectos reales nunca dejas que Hibernate toque la base de datos directamente. En su lugar uso **Liquibase**, que gestiona los cambios de schema con archivos versionados llamados `changeSets`.

### Cómo funciona Liquibase aquí

Hay un archivo maestro que actúa como índice:

```
db/changelog/db.changelog-master.yaml  ← punto de entrada
```

```yaml
# db.changelog-master.yaml
databaseChangeLog:
  - include:
      file: db/changelog/db.changelog-001-init.yaml
  - include:
      file: db/changelog/db.changelog-002-practice.yaml
  - include:
      file: db/changelog/db.changelog-003-products.yaml
```

Cada archivo tiene `changeSets`. Un changeSet es un bloque de cambios con un `id` único. Liquibase guarda en una tabla interna (`DATABASECHANGELOG`) cuáles ha aplicado ya, así que nunca los repite.

**Regla importante:** una vez que un changeSet está en producción, no se toca. Si quieres cambiar algo, creas uno nuevo.

---

## 3. Los archivos de changelog uno a uno

### `db.changelog-001-init.yaml` — las tablas base

Crea las tres tablas iniciales del proyecto:

```
users         → id, name
allergy       → id, name, severity
user_allergy  → user_id, allergy_id  (tabla pivote de la relación ManyToMany)
```

La tabla `user_allergy` es la que hace funcionar la relación ManyToMany entre `User` y `Allergy`. Tiene claves foráneas a las dos tablas con `CASCADE DELETE`, lo que significa que si borras un usuario sus filas en `user_allergy` se borran solas.

### `db.changelog-002-practice.yaml` — ampliaciones

Añade el campo `email` a `users` y crea una tabla de prueba. Lo hago en un archivo separado porque es un cambio posterior, no de la creación inicial.

### `db.changelog-003-products.yaml` — la tabla de productos

Este lo creé yo. Tiene dos `changeSets`:

**changeSet 006** — crea la tabla `products`:

```yaml
columns:
  - id       BIGINT autoIncrement PK
  - name     VARCHAR(255) NOT NULL
  - category VARCHAR(100) NOT NULL
  - price    DECIMAL(10,2) NOT NULL
  - stock    INTEGER NOT NULL
  - active   BOOLEAN NOT NULL default true
```

También crea índices en `name`, `category` y `active` porque son los campos por los que voy a buscar y filtrar. Sin índice, cada consulta haría un full scan de toda la tabla.

**changeSet 007** — carga los datos desde el CSV:

```yaml
- loadData:
    tableName: products
    file: db/changelog/products.csv
    columns:
      - column: {name: id, type: SKIP}   # ← importante, ver nota abajo
      - column: {name: name, type: STRING}
      - column: {name: category, type: STRING}
      - column: {name: price, type: NUMERIC}
      - column: {name: stock, type: NUMERIC}
      - column: {name: active, type: BOOLEAN}
```

> **Por qué `id: SKIP`:** el CSV tiene una columna `id` con valores del 1 al 40. Si los inserto directamente, la secuencia interna de PostgreSQL (el mecanismo que genera IDs automáticos) no se entera y se queda en 1. Cuando JPA intente insertar un producto nuevo, generaría `id = 1`, que ya existe, y rompería con un error de clave duplicada. Con `SKIP`, Liquibase ignora esa columna y PostgreSQL asigna los IDs usando la secuencia, que después de 40 inserciones queda en 41.

---

## 4. La entidad Product

```java
// domain/Product.java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    private BigDecimal price;  // para dinero siempre BigDecimal, nunca double
    private Integer stock;
    private Boolean active;
}
```

`@Entity` le dice a JPA que esta clase representa una tabla. `@Table(name = "products")` especifica el nombre exacto porque si no, Hibernate intentaría buscar una tabla llamada `product` (sin s) y fallaría la validación.

`BigDecimal` en vez de `double` para el precio porque los doubles tienen errores de precisión con decimales. Por ejemplo, `0.1 + 0.2` en double no da exactamente `0.3`.

---

## 5. El repositorio — ProductRepository

Spring Data JPA genera las consultas SQL automáticamente a partir del nombre del método. No tengo que escribir SQL a mano para la mayoría de casos.

```java
// repository/ProductRepository.java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. findById → SELECT * FROM products WHERE id = ?
    Optional<Product> findById(Long id);

    // 2. findByName → SELECT * FROM products WHERE name = ?
    Optional<Product> findByName(String name);

    // 3. existsByName → SELECT COUNT(*) > 0 FROM products WHERE name = ?
    //    más eficiente que findByName porque no carga la entidad entera
    boolean existsByName(String name);

    // 4. countByActiveTrue → SELECT COUNT(*) FROM products WHERE active = true
    long countByActiveTrue();

    // 5. deleteByCategory → necesita @Query porque el nombre derivado haría
    //    primero un SELECT y luego N deletes uno a uno (muy ineficiente)
    @Transactional
    @Modifying
    @Query("DELETE FROM Product p WHERE p.category = :category")
    void deleteByCategory(@Param("category") String category);

    // 6. el nombre largo lo genera Spring Data automáticamente:
    //    WHERE category = ? AND price < ? AND stock > ? AND active = true
    List<Product> findByCategoryAndPriceLessThanAndStockGreaterThanAndActiveTrue(
            String category,
            BigDecimal price,
            Integer stock
    );
}
```

### Por qué el método 5 usa @Query en vez de nombre derivado

Spring Data puede generar `deleteByCategory` automáticamente, pero lo hace de forma ineficiente: primero hace un `SELECT` para cargar todas las entidades, y luego lanza un `DELETE` por cada una. Con `@Query` hago un solo `DELETE` en la base de datos.

`@Modifying` le dice a Spring que esta query modifica datos (no es solo un SELECT).
`@Transactional` es obligatorio en métodos `@Modifying` — sin él Spring lanza `TransactionRequiredException` al ejecutarlo.

---

## 6. Cómo encaja todo — el flujo completo

```
Arranque de la app
       │
       ▼
Spring Boot detecta docker-compose.yml
       │
       ▼
Levanta el contenedor demo-postgres en el puerto 5433
       │
       ▼
Liquibase lee db.changelog-master.yaml
       │
       ▼
Aplica los changeSets que aún no están en DATABASECHANGELOG
  001 → crea users, allergy, user_allergy
  002 → añade email a users
  003 → crea products, carga los 40 productos del CSV
       │
       ▼
Hibernate valida que las entidades Java coinciden con el schema
  (ddl-auto: validate — si no coincide, la app no arranca)
       │
       ▼
Servidor listo en http://localhost:8080
```

---

## 7. Los endpoints de User (lo que ya existía)

```
GET    /users                      → todos los usuarios con sus alergias (JOIN FETCH)
GET    /users/name-like/{name}     → búsqueda parcial por nombre
GET    /users/allergies            → todos los usuarios (carga alergias por separado)
POST   /users/{name}               → crear usuario (el nombre va en la URL)
PUT    /users/update-name/{id}/{name} → actualizar nombre por id
```

La capa está organizada en tres niveles:
- **Controller** → recibe la petición HTTP, llama al Service
- **Service** → lógica, transacciones, mapea entidades a DTOs
- **Repository** → solo acceso a datos, sin lógica

Los DTOs (`UserDTO`, `AllergyDTO`) son clases separadas de las entidades para no exponer directamente el modelo de base de datos en la API.
