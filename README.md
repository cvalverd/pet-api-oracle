# pet-api

Microservicio de mascotas en **Spring Boot 3.5 (Java 21)** conectado a **Oracle Cloud** (Autonomous
Database con wallet). Es el backend del frontend `pet-app` en Angular 21 (`C:\dev\pet-app-angular21`).
Sigue la misma estructura del ejemplo [cvalverd/bdget](https://github.com/cvalverd/bdget).

## Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/pets` | Lista de mascotas `[{ id, name, breed, age }]` (la usa la lista de Angular) |
| GET | `/pets/{id}` | Una mascota (la usa el detalle de Angular); 404 si no existe |
| POST | `/pets` | Crea una mascota (201); 400 con el detalle si los datos no son válidos |
| PUT | `/pets/{id}` | Actualiza una mascota; 404 si no existe |
| DELETE | `/pets/{id}` | Elimina una mascota (204); 404 si no existe |

Ejemplo de cuerpo para POST/PUT: `{ "name": "Firulais", "breed": "Labrador", "age": 3 }`.

## Estructura (igual que bdget)

```
controller/   PetController (REST /pets) y CorsConfig (permite al frontend Angular)
service/      PetService + PetServiceImpl (reglas: 404 si no existe, el id lo genera Oracle)
repository/   PetRepository (JpaRepository)
model/        Pet (entidad JPA con validaciones)
exception/    PetNotFoundException y GlobalExceptionHandler (errores en JSON)
sql/pet.sql   Tabla PET y datos de prueba
```

## Configurar Oracle Cloud

1. En la consola de Oracle Cloud, en tu Autonomous Database: **Database connection → Download wallet**.
   Descomprímelo en la carpeta `wallet/` de este proyecto.
2. No necesitas crear la tabla manualmente: al iniciar, Flyway aplica las migraciones versionadas de
   `src/main/resources/db/migration`, crea `PET` e inserta las cuatro mascotas de demostracion. Si el
   esquema ya contiene objetos pero no tiene historial de Flyway, se adopta desde la version 2 y la
   migracion V3 asegura la tabla e inserta solo las mascotas de demostracion que falten; no elimina ni
   reemplaza registros existentes. El usuario de la base debe poder crear tablas. `sql/pet.sql` queda
   como alternativa manual; no la ejecutes si la aplicacion ya aplico las migraciones.
3. Copia `.env.example` a `.env` y completa `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. `DB_URL`
   acepta la URL JDBC completa o el alias de servicio (`<servicio>_tp`) que aparece en
   `wallet/tnsnames.ora`; para un alias se usa `TNS_ADMIN` o, si no está definido, `/app/wallet`.

`wallet/` y `.env` están en `.gitignore`: el wallet y la contraseña **no se suben al repositorio**.
En el ejemplo bdget el wallet quedó commiteado y la contraseña escrita en `docker-compose.yml`; aquí
llegan por variables de entorno (o por secrets en GitHub Actions).

## Ejecutar con Oracle Cloud

Local (Java 21, sin instalar Maven), en PowerShell:

```powershell
$env:DB_URL="jdbc:oracle:thin:@<servicio>_tp?TNS_ADMIN=C:/dev/pet-api-oracle/wallet"
$env:DB_USERNAME="<usuario>"
$env:DB_PASSWORD="<contraseña>"
.\mvnw spring-boot:run
```

Con Docker Compose (usa `.env` y monta `wallet/`):

```bash
docker compose up --build
```

O solo con Docker:

```bash
docker build -t pet-api .
docker run -p 8080:8080 --env-file .env -v "$(pwd)/wallet:/app/wallet:ro" pet-api
```

La imagen se compila con el Maven Wrapper y corre con un JRE 21, como usuario sin privilegios.
El wallet y las credenciales nunca quedan dentro de la imagen (están en `.dockerignore`).

La API queda en `http://localhost:8080/pets`. Luego levanta el frontend Angular (`npm start` en
`pet-app-angular21`) y abre `http://localhost:4200`.

## Pruebas

```bash
./mvnw test
```

Las pruebas de controller, service, modelo y extracción segura de la wallet no necesitan una base de
datos Oracle ni acceso a AWS.

## Publicar la imagen en Amazon ECR

El workflow de GitHub Actions `.github/workflows/publish-ecr.yml` ejecuta las pruebas y, al hacer
push a `main` (o iniciarlo manualmente en esa rama), construye la imagen Docker y la publica en
`622519216316.dkr.ecr.us-east-1.amazonaws.com/pet-api-oracle` con el SHA del commit como etiqueta.
La primera ejecución crea el repositorio ECR si aún no existe.

Configura en **Settings → Secrets and variables → Actions** de este repositorio los secretos
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`. Los secretos del repositorio del
frontend no se comparten automáticamente. Como las credenciales de AWS Academy expiran, actualiza
los tres secretos cada vez que roten.

Después de publicar la imagen, el workflow registra una nueva revisión de la tarea y actualiza
automáticamente el servicio ECS `pet-api` cuando se ejecuta en `main`. El stack debe existir antes:
si es el primer despliegue, ejecuta una vez **Deploy Pet API to ECS** en el repositorio del frontend.
La API requiere el wallet de Oracle y las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en
tiempo de ejecución. ECS inyecta las credenciales desde Secrets Manager. Antes de iniciar Spring,
la aplicación descarga el ZIP privado de S3 mediante el AWS SDK para Java y el task role de ECS,
valida que contenga `tnsnames.ora` en la raíz y lo extrae en `/app/wallet`. Spring Boot aplica las
migraciones Flyway antes de inicializar JPA para crear `PET` e insertar los datos de demostracion.
La variable
`ORACLE_WALLET_S3_BUCKET` identifica el bucket y `ORACLE_WALLET_S3_KEY` el objeto; no se usan
credenciales AWS estáticas ni valores Base64. En ejecución local, Docker Compose conserva el montaje
de `./wallet` y no descarga desde S3 si esas variables no están configuradas.

El primer despliegue crea la infraestructura desde el workflow manual **Deploy Pet API to ECS** en el
repositorio del frontend. Este requiere el secreto `pet-api/oracle-db-credentials` (JSON con
`DB_URL`, `DB_USERNAME` y `DB_PASSWORD`) en Secrets Manager, y las variables Actions
`ORACLE_WALLET_S3_BUCKET=pet-api-oracle-wallet-622519216316-us-east-1-an` y
`ORACLE_WALLET_S3_KEY=Wallet_BTJOWHCMGWJBQTNV.zip`. La tarea usa `LabRole` tanto como execution role
como task role porque AWS Academy no permite crear roles. La confianza de `LabRole` incluye ECS Tasks
y la simulación IAM actual permite `s3:GetObject` sobre el objeto configurado; el rol es compartido y
sus permisos no son de mínimo privilegio para producción. La wallet no se almacena en Secrets Manager
ni se incluye en la imagen. La plantilla no incluye valores secretos y el preflight comprueba permisos
sin leer credenciales ni el objeto wallet.

El workflow verifica la existencia del objeto y el permiso antes de desplegar. Después, cada
publicación exitosa desde `main` actualiza el servicio existente y comprueba `GET /pets`. El endpoint
actual es HTTP público, sin autenticación ni cifrado en tránsito; úsalo solo para la demo, nunca con
datos reales o sensibles.
