# Cómo funciona la implementación: modos JPA y REST

Este documento explica, paso a paso y con ejemplos del código real, cómo se consigue que la
aplicación use la base de datos directamente **o** llame a un servicio remoto, según un único
valor de configuración.

---

## 1. El problema que resuelve esto

Antes de la migración, `UserService` dependía directamente de `UserRepository`:

```java
// ANTES
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;   // ← atado a JPA

    public List<UserDTO> getUsers() {
        return userRepository.findAllWithAllergies(); // ← query SQL directa
    }
}
```

Si quisiéramos cambiar el origen de los datos (llamar a otro servicio en vez de la BBDD),
tendríamos que modificar `UserService`. Eso rompe el principio de que la lógica de negocio
no debería saber ni importarle de dónde vienen los datos.

---

## 2. La solución: interfaces (Puertos)

Se crea una **interfaz** que describe *qué operaciones* necesita el servicio, sin decir *cómo*
se implementan:

```java
// application/port/UserDataPort.java
public interface UserDataPort {

    List<User> findAllWithAllergies();
    List<User> findAllByNameContaining(String name);
    List<User> findAll();
    User save(User user);
    void updateById(Long id, String name);
    long count();
}
```

Ahora `UserService` depende de la interfaz, no de ninguna implementación concreta:

```java
// DESPUÉS
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDataPort userDataPort;   // ← sólo conoce la interfaz

    public List<UserDTO> getUsers() {
        return userDataPort.findAllWithAllergies(); // ← no sabe si es JPA o REST
    }
}
```

`UserService` no sabe, ni le importa, si detrás hay una BBDD o una llamada HTTP.
Eso lo decide la configuración.

---

## 3. Las dos implementaciones (Adaptadores)

### Adaptador JPA

Implementa la interfaz usando Spring Data JPA (acceso directo a PostgreSQL):

```java
// infrastructure/persistence/jpa/UserJpaAdapter.java

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class UserJpaAdapter implements UserDataPort {

    private final UserRepository userRepository;  // ← repositorio JPA real

    @Override
    public List<User> findAllWithAllergies() {
        return userRepository.findAllWithAllergies();  // ← ejecuta SQL en PostgreSQL
    }

    @Override
    public void updateById(Long id, String name) {
        userRepository.updateById(id, name);  // ← UPDATE en BBDD
    }

    // ... resto de métodos ...
}
```

### Adaptador REST

Implementa la misma interfaz pero haciendo llamadas HTTP a otro servicio:

```java
// infrastructure/persistence/rest/UserRestAdapter.java

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
@RequiredArgsConstructor
public class UserRestAdapter implements UserDataPort {

    private final RestTemplate restTemplate;
    
    @Value("${app.data-service.url}")
    private String baseUrl;

    @Override
    public List<User> findAllWithAllergies() {
        // ← hace GET http://otro-servicio/api/users
        UserResponse[] responses = restTemplate.getForObject(
            baseUrl + "/api/users", UserResponse[].class);
        return Arrays.stream(responses).map(this::toUser).toList();
    }

    @Override
    public void updateById(Long id, String name) {
        // ← hace PUT http://otro-servicio/api/users/{id}
        restTemplate.exchange(
            baseUrl + "/api/users/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("name", name)),
            Void.class,
            id);
    }

    // ... resto de métodos ...
}
```

Ambas clases implementan **exactamente la misma interfaz** `UserDataPort`.
Spring sólo registrará **una de las dos** como bean, dependiendo de la configuración.

---

## 4. `@ConditionalOnProperty`: cómo funciona exactamente

Esta anotación le dice a Spring: *"registra este bean SÓLO SI la propiedad tiene ese valor"*.

```java
@ConditionalOnProperty(
    name         = "app.datasource.mode",  // nombre de la propiedad a leer
    havingValue  = "jpa",                  // valor que debe tener
    matchIfMissing = true                  // si la propiedad NO existe, también se activa
)
```

### Tabla de decisión

| Valor en `application.yaml` | ¿Qué bean se crea? |
|------------------------------|---------------------|
| `app.datasource.mode: jpa` | `UserJpaAdapter` ✅ — `UserRestAdapter` ❌ |
| `app.datasource.mode: rest` | `UserJpaAdapter` ❌ — `UserRestAdapter` ✅ |
| *(propiedad no existe)* | `UserJpaAdapter` ✅ — `UserRestAdapter` ❌ |

> **Nunca se crean los dos a la vez.**
> Spring lanza error si encuentra dos beans del mismo tipo sin saber cuál inyectar.
> El `@ConditionalOnProperty` garantiza que exactamente uno de los dos existe en el contexto.

---

## 5. El flujo completo, desde la config hasta la ejecución

Imaginemos que la config tiene `mode: jpa` (el valor por defecto):

```
application.yaml
────────────────
app:
  datasource:
    mode: jpa
```

### Paso 1 — Spring Boot arranca y escanea las clases

Encuentra `UserJpaAdapter` con:
```java
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
```
Lee `app.datasource.mode` del yaml → vale `"jpa"` → condición cumplida → **REGISTRA** el bean.

Encuentra `UserRestAdapter` con:
```java
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
```
Lee `app.datasource.mode` → vale `"jpa"` → condición NO cumplida → **NO REGISTRA** el bean.

### Paso 2 — Spring inyecta la dependencia en UserService

`UserService` pide un bean de tipo `UserDataPort`:

```java
private final UserDataPort userDataPort;  // ← Spring busca quién implementa esto
```

En el contexto sólo existe `UserJpaAdapter` (el REST no se registró).
Spring lo inyecta automáticamente.

### Paso 3 — Se hace una petición HTTP

```
GET /users
  → UserController.getUsers()
    → UserService.getUsers()
      → userDataPort.findAllWithAllergies()
        → [userDataPort es UserJpaAdapter]
          → userRepository.findAllWithAllergies()
            → SELECT * FROM users JOIN user_allergy ... (SQL en PostgreSQL)
```

---

## 6. El mismo flujo con `mode: rest`

```
application.yaml
────────────────
app:
  datasource:
    mode: rest
  data-service:
    url: http://localhost:8081
```

Spring **no registra** `UserJpaAdapter` y **sí registra** `UserRestAdapter`.

```
GET /users
  → UserController.getUsers()
    → UserService.getUsers()
      → userDataPort.findAllWithAllergies()
        → [userDataPort es UserRestAdapter]
          → restTemplate.getForObject("http://localhost:8081/api/users", ...)
            → HTTP GET al otro servicio → recibe JSON → lo convierte a User
```

`UserService` ejecuta exactamente el mismo código en los dos casos.
La diferencia está únicamente en qué objeto concreto hay detrás de `userDataPort`.

---

## 7. Lo mismo aplica para los 4 puertos

El patrón se repite idéntico para todas las entidades:

| Interfaz (Puerto) | Adaptador JPA | Adaptador REST |
|-------------------|---------------|----------------|
| `UserDataPort` | `UserJpaAdapter` | `UserRestAdapter` |
| `ProductDataPort` | `ProductJpaAdapter` | `ProductRestAdapter` |
| `AllergyDataPort` | `AllergyJpaAdapter` | `AllergyRestAdapter` |
| `ProductAuditPort` | `ProductAuditJpaAdapter` | `ProductAuditRestAdapter` |

Cada par tiene su `@ConditionalOnProperty` correspondiente.
Cambiar una línea en el yaml activa los 4 adaptadores REST y desactiva los 4 JPA (o viceversa).

---

## 8. El RestTemplate: por qué también es condicional

`RestTemplate` es el cliente HTTP que usan los adaptadores REST.
No tiene sentido crearlo cuando estamos en modo JPA, así que también es condicional:

```java
// infrastructure/config/RestClientConfig.java

@Configuration
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

En modo `jpa`: este `@Configuration` entero se ignora → `RestTemplate` no existe como bean.
En modo `rest`: se crea el bean `RestTemplate` → los adaptadores REST lo reciben inyectado.

Si intentaras arrancar con `mode: jpa` pero los adaptadores REST estuviesen activos,
Spring fallaría al arrancar diciendo *"No bean of type RestTemplate found"*.
El `@ConditionalOnProperty` en los adaptadores REST evita que eso ocurra.

---

## 9. El DataSeeder: por qué también es condicional

Al arrancar por primera vez, la app crea usuarios y alergias de prueba:

```java
// infrastructure/seeder/DataSeeder.java

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserDataPort userDataPort;
    private final AllergyDataPort allergyDataPort;

    @Override
    public void run(String... args) {
        if (userDataPort.count() > 0 || allergyDataPort.count() > 0) return;

        for (int j = 1; j <= 5; j++) {
            Allergy allergy = new Allergy();
            allergy.setName("Allergy " + j);
            allergyDataPort.save(allergy);
        }
        // ...
    }
}
```

En modo `jpa` tiene sentido: la BBDD empieza vacía y hay que rellenarla.

En modo `rest` **no tiene sentido**: los datos viven en el otro servicio, que ya tendrá
sus propios datos. Si el `DataSeeder` se ejecutase en modo REST, haría llamadas HTTP
al servicio remoto para "inicializarlo", lo cual no es responsabilidad de esta app.

Por eso `DataSeeder` también lleva `@ConditionalOnProperty(havingValue = "jpa")`.

---

## 10. Resumen visual de qué beans existen según el modo

### Con `mode: jpa` (o sin config):

```
Contexto de Spring
┌─────────────────────────────────────────────────────┐
│  UserService         → inyecta UserJpaAdapter       │
│  ProductAuditService → inyecta ProductJpaAdapter    │
│                        inyecta ProductAuditJpaAdapter│
│  ProductController   → inyecta ProductJpaAdapter    │
│                                                     │
│  UserJpaAdapter        ✅ (registrado)              │
│  ProductJpaAdapter     ✅                           │
│  AllergyJpaAdapter     ✅                           │
│  ProductAuditJpaAdapter✅                           │
│  DataSeeder            ✅                           │
│                                                     │
│  UserRestAdapter       ❌ (no existe)               │
│  ProductRestAdapter    ❌                           │
│  AllergyRestAdapter    ❌                           │
│  ProductAuditRestAdapter❌                          │
│  RestTemplate          ❌                           │
└─────────────────────────────────────────────────────┘
```

### Con `mode: rest`:

```
Contexto de Spring
┌─────────────────────────────────────────────────────┐
│  UserService         → inyecta UserRestAdapter      │
│  ProductAuditService → inyecta ProductRestAdapter   │
│                        inyecta ProductAuditRestAdapter│
│  ProductController   → inyecta ProductRestAdapter   │
│                                                     │
│  UserJpaAdapter        ❌ (no existe)               │
│  ProductJpaAdapter     ❌                           │
│  AllergyJpaAdapter     ❌                           │
│  ProductAuditJpaAdapter❌                           │
│  DataSeeder            ❌                           │
│                                                     │
│  UserRestAdapter       ✅ (registrado)              │
│  ProductRestAdapter    ✅                           │
│  AllergyRestAdapter    ✅                           │
│  ProductAuditRestAdapter✅                          │
│  RestTemplate          ✅                           │
└─────────────────────────────────────────────────────┘
```

---

## 11. Cómo cambiar el modo

Sólo hay que tocar una línea en `application.yaml`:

```yaml
app:
  datasource:
    mode: rest          # ← cambiar esto de "jpa" a "rest"
  data-service:
    url: http://localhost:8081
```

O pasarlo por línea de comandos sin tocar el yaml:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.datasource.mode=rest"
```

O como variable de entorno:

```bash
APP_DATASOURCE_MODE=rest ./mvnw spring-boot:run
```

Spring Boot convierte `APP_DATASOURCE_MODE` a `app.datasource.mode` automáticamente.

---

## 12. Analogía para entenderlo de forma intuitiva

Piensa en un enchufe eléctrico y dos aparatos:

- El **enchufe** es la interfaz `UserDataPort`: define la "forma" de la conexión.
- El **televisor** es `UserJpaAdapter`: se conecta a la BBDD (corriente de 220V).
- El **portátil** es `UserRestAdapter`: se conecta al servicio remoto (USB-C).

`UserService` es la **pared** que tiene el enchufe.
No le importa qué aparato está conectado, sólo que algo implementa la forma del enchufe.

`@ConditionalOnProperty` es el **electricista** que, según la configuración,
conecta el televisor **o** el portátil, pero nunca los dos a la vez.
