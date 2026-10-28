FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system --gid 1001 novelcopilot \
    && useradd --system --uid 1001 --gid novelcopilot --home-dir /app --shell /usr/sbin/nologin novelcopilot

COPY --from=build --chown=novelcopilot:novelcopilot /app/target/*.jar app.jar

USER novelcopilot

# A plataforma de hospedagem pode sobrescrever esta configuração após a medição real do container.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=30.0 -XX:InitialRAMPercentage=10.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
