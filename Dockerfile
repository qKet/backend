# =========================
# 1단계: Gradle 빌드
# =========================
FROM gradle:8.10.2-jdk21 AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew

RUN ./gradlew clean bootJar --no-daemon


# =========================
# 2단계: JDK 21 실행
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]