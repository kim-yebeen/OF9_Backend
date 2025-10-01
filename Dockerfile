# 멀티스테이지 빌드 - 빌드 캐싱 최적화
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# Gradle 의존성 캐싱을 위해 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# 런타임 스테이지 - 최소화
FROM eclipse-temurin:17-jre-alpine

# Chrome 및 ChromeDriver 경량 설치 (Alpine 버전)
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    curl \
    && rm -rf /var/cache/apk/*

# Chromium 심볼릭 링크 생성 (google-chrome 명령어로 실행 가능하게)
RUN ln -s /usr/bin/chromium-browser /usr/bin/google-chrome \
    && ln -s /usr/bin/chromedriver /usr/local/bin/chromedriver

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
# 메모리 최적화 (t3.micro용)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Djava.awt.headless=true"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]