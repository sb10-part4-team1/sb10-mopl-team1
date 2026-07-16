# ========== 1. Build stage ==========
FROM amazoncorretto:17 AS builder
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Jar 레이어 분리 추출 스테이지 추가 (Jar 다이어트 핵심)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# ========== 2. Runtime stage ==========
FROM amazoncorretto:17-alpine3.21
WORKDIR /app

RUN addgroup -S app \
    && adduser -S app -G app -h /app -s /sbin/nologin \
    && mkdir -p /app/.logs \
    && chown -R app:app /app

EXPOSE 8080

ENV JVM_OPTS=""

# 추출한 레이어들을 분리하여 복사 (변경이 거의 없는 라이브러리를 아래에 두고 캐시 극대화)
COPY --from=builder --chown=app:app /app/dependencies/ ./
COPY --from=builder --chown=app:app /app/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /app/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /app/application/ ./

USER app

ENTRYPOINT ["sh", "-c", "exec java ${JVM_OPTS} org.springframework.boot.loader.launch.JarLauncher"]
