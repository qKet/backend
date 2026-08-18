# Gradle 빌드는 이제 여기서 안 함 — CI(GitHub Actions)가 `./gradlew clean test bootJar`로
# 미리 jar를 만들어두고, 이 Dockerfile은 그 결과물을 런타임 이미지에 담기만 함
# (캐싱 안 되는 Docker 안 빌드보다 CI에서 Gradle 캐시 쓰는 게 훨씬 빠름).
#
# 로컬에서 그냥 `docker build .`만 하면 안 됨 — 먼저 `./gradlew clean bootJar`로
# build/libs/*.jar를 만들어둬야 함.
#
# 태그를 -jammy(Ubuntu 22.04)로 고정 — OS 버전 안 박은 `21-jre`가 최근 Ubuntu Noble(24.04)
# 베이스로 바뀌면서, 그 이미지가 기본으로 갖고 있는 `ubuntu` 유저가 이미 UID 1000을 써서
# 아래 `useradd --uid 1000`이 "UID 1000 is not unique"(exit code 4)로 빌드 실패했음
# (2026-08-18 실제로 겪음). -jammy는 이 기본 유저가 없어서 UID 1000을 그대로 쓸 수 있고,
# 앞으로 베이스 이미지가 또 바뀌어도 이 Dockerfile은 영향 안 받음.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar

# EXPOSE는 application.yml의 server.port(8080)와 반드시 맞춰야 함 — 실제 트래픽 차단 효과는
# 없지만(문서용 메타데이터) K8s Service/probe 포트(CD 레포 backend-deployment.yaml)와
# 일치시켜야 로컬 docker run -p 매핑이나 문서 볼 때 헷갈리지 않음.
EXPOSE 8080

# 컨테이너를 root로 띄우지 않음 — JVM 프로세스가 컴프로마이즈돼도 컨테이너 안에서 root 권한을
# 못 갖게 하는 최소한의 방어. UID 고정(1000)은 K8s SecurityContext에서 runAsNonRoot 검증할 때도 씀.
RUN useradd --system --uid 1000 --shell /usr/sbin/nologin appuser
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
