# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

COPY src src
# The cache mount keeps downloaded Maven dependencies between builds (much faster rebuilds).
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package \
 && mv target/*.jar target/application.jar \
 && java -Djarmode=tools -jar target/application.jar extract --layers --destination target/extracted \
 && mkdir -p target/extracted/spring-boot-loader target/extracted/snapshot-dependencies

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app \
 && useradd --system --gid app --no-create-home app

WORKDIR /application
# Layers, least-changing first: Docker only re-uploads the layer that changed.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "application.jar"]