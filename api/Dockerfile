# The NGO edition's image, built from source. This edition builds against the core checked out next to
# it (settings.gradle: includeBuild '../../orudesk/api'), so the build context is the folder that holds BOTH
# repositories:
#   cd ..   # the folder with orudesk/ and orudesk-ngo/ side by side
#   docker build -f orudesk-ngo/api/Dockerfile -t orudesk-ngo .

# ---- Stage 1: build the jar with the JDK ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY orudesk/api orudesk/api
COPY orudesk-ngo/api orudesk-ngo/api
WORKDIR /src/orudesk-ngo/api
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :apps:orudesk-ngo:bootJar -x test \
 && find apps/orudesk-ngo/build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /src/app.jar \; \
 && java -Djarmode=tools -jar /src/app.jar extract --layers --launcher --destination /src/extracted

# ---- Stage 2: run it with only the JRE (same as the core's image) ----
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home orudesk
WORKDIR /app
COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./
USER orudesk
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health/readiness HTTP/1.0\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
