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
2. Ejecuta `sql/pet.sql` con tu usuario (SQL Developer Web o SQL Developer).
3. Copia `.env.example` a `.env` y completa `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. El nombre del
   servicio (`<servicio>_tp`) está en `wallet/tnsnames.ora`.

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

19 pruebas (controller con MockMvc, service con Mockito y modelo); no necesitan la base de datos.
JaCoCo deja el reporte en `target/site/jacoco/index.html` (98% de cobertura de instrucciones).

## Publicar la imagen en Amazon ECR

El workflow de GitHub Actions `.github/workflows/publish-ecr.yml` ejecuta las pruebas y, al hacer
push a `main` (o iniciarlo manualmente en esa rama), construye la imagen Docker y la publica en
`622519216316.dkr.ecr.us-east-1.amazonaws.com/pet-api-oracle` con el SHA del commit como etiqueta.
La primera ejecución crea el repositorio ECR si aún no existe.

Configura en **Settings → Secrets and variables → Actions** de este repositorio los secretos
`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`. Los secretos del repositorio del
frontend no se comparten automáticamente. Como las credenciales de AWS Academy expiran, actualiza
los tres secretos cada vez que roten.

El workflow solo publica la imagen: no crea ni inicia tareas ECS. La API requiere el wallet de
Oracle y las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en tiempo de ejecución. ECS debe
inyectar `ORACLE_WALLET_BASE64` desde AWS Secrets Manager; el entrypoint decodifica el ZIP en
`/app/wallet`, valida `tnsnames.ora`, elimina el valor Base64 del entorno y luego inicia Java. Ni la
wallet ni las credenciales se incluyen en la imagen.

Para desplegarla en ECS, publica primero una imagen después de estos cambios y ejecuta el workflow
manual **Deploy Pet API to ECS** en el repositorio del frontend. Este espera los secretos
`pet-api/oracle-db-credentials` (JSON con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`) y
`pet-api/oracle-wallet` (ZIP de wallet codificado en Base64) en Secrets Manager, región `us-east-1`.
La plantilla no incluye valores secretos y el preflight consulta únicamente los metadatos de los
secretos. La API queda pública, sin autenticación, detrás de CloudFront HTTPS; úsala solo para la
demo hasta agregar autenticación y controles de acceso.
