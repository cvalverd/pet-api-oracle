# ---- Etapa 1: compilar con Maven Wrapper ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# mvnw puede venir con saltos de linea de Windows (CRLF) si se edito en Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B clean package -DskipTests

# ---- Etapa 2: imagen liviana solo con el JRE ----
FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home appuser \
    && mkdir -p /app/wallet \
    && chown appuser:appuser /app/wallet
COPY --from=build /app/target/pet-api-0.0.1-SNAPSHOT.jar /app/pet-api.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

ENV TNS_ADMIN=/app/wallet
RUN chmod 0555 /app/docker-entrypoint.sh
USER appuser
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
