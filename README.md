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

## Publicar y desplegar la API en EC2

El workflow `.github/workflows/publish-ecr.yml` ejecuta `./mvnw -B test`, construye y publica la
imagen de la API en ECR con el SHA del commit como etiqueta inmutable y, al hacer push a `main`,
actualiza el contenedor `pet-api` de EC2 mediante AWS Systems Manager (SSM). Si el mismo SHA ya está
publicado, reutiliza la imagen. Antes de terminar, consulta `GET /pets` y revierte al contenedor
anterior si la API o Oracle no responden.

Prepara primero la infraestructura EC2 con la plantilla
[`ec2/pet-app-ec2-stack.yml` del repositorio frontend](https://github.com/cvalverd/pet-app-angular21/blob/main/ec2/pet-app-ec2-stack.yml).
La instancia necesita un perfil IAM con Systems Manager, lectura de ECR, permiso de lectura del
secreto Oracle y `s3:GetObject` para el ZIP privado del wallet. En AWS Academy se usa el perfil
existente; en una cuenta propia, la plantilla puede crear un rol mínimo. La base Oracle, el secreto y
el objeto del wallet son recursos externos y deben existir antes del primer despliegue.

En **Settings → Secrets and variables → Actions** de este repositorio configura estos **Secrets**:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`

Y estas **Variables**:

- `AWS_REGION` (opcional; predeterminado `us-east-1`)
- `EC2_INSTANCE_ID`
- `ORACLE_CREDENTIALS_SECRET_ARN`
- `ORACLE_WALLET_S3_BUCKET`
- `ORACLE_WALLET_S3_KEY`

Los tres secretos AWS son independientes de los del repositorio frontend; las credenciales de AWS
Academy expiran y deben renovarse. Los valores de base de datos no se configuran en GitHub: el script
de despliegue los obtiene de Secrets Manager dentro de EC2 y los inyecta al contenedor. El wallet
permanece en S3 y no se agrega a la imagen.

Las credenciales de GitHub Actions necesitan acceso a ECR, `ssm:SendCommand`,
`ssm:GetCommandInvocation` y `ec2:DescribeInstances`. Para probar el despliegue inicial, ejecuta
primero el workflow del backend en `main` para iniciar la API; luego ejecuta el workflow frontend.
`workflow_dispatch` desde otra rama permite publicar una imagen, pero el despliegue a EC2 solo corre
desde `main`.

El workflow **Deploy Pet API to ECS** del repositorio frontend es una ruta legacy separada y no
actualiza EC2. No la ejecutes para esta demo.

El endpoint EC2 es HTTP público, sin autenticación ni cifrado en tránsito; úsalo solo para la demo,
nunca con datos reales o sensibles.
