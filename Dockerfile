# Gradle 빌드는 이제 여기서 안 함 — CI(GitHub Actions)가 `./gradlew clean test bootJar`로
# 미리 jar를 만들어두고, 이 Dockerfile은 그 결과물을 런타임 이미지에 담기만 함
# (캐싱 안 되는 Docker 안 빌드보다 CI에서 Gradle 캐시 쓰는 게 훨씬 빠름).
#
# 로컬에서 그냥 `docker build .`만 하면 안 됨 — 먼저 `./gradlew clean bootJar`로
# build/libs/*.jar를 만들어둬야 함.
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
