# 런타임 전용 Dockerfile (JAR 파일 직접 사용)
FROM eclipse-temurin:17-jre-alpine

# Chrome 및 ChromeDriver 경량 설치 (Alpine 버전)
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    curl \
    && rm -rf /var/cache/apk/*

# Chromium 심볼릭 링크 생성
RUN ln -s /usr/bin/chromium-browser /usr/bin/google-chrome \
    && ln -s /usr/bin/chromedriver /usr/local/bin/chromedriver

WORKDIR /app

# 빌드된 JAR 파일 복사 (GitHub Actions에서 전송됨)
COPY app.jar app.jar

ENV TZ=Asia/Seoul
# 메모리 최적화 (t3.micro용)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Djava.awt.headless=true"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]