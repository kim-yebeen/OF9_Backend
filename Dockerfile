# 경량화된 Chrome 환경
FROM eclipse-temurin:17-jre

# Chrome 설치 (필수 패키지만)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg2 \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libgtk-3-0 \
    libnss3 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xvfb \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# 환경변수로 기존 설정 덮어쓰기 (코드 변경 없이 최적화)
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.awt.headless=true"
ENV DISPLAY=:99

# Spring Boot 환경변수로 기존 application.properties 값들 덮어쓰기
ENV SPRING_JPA_SHOW_SQL=false
ENV SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3
ENV SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
ENV SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=300000
ENV SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=180000
ENV SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD=60000
ENV SPRING_SQL_INIT_MODE=always
ENV INITIAL_CRAWL_ENABLED=true

# JWT 설정 추가 (없어서 에러 발생)
ENV JWT_SECRET=baseballdiaryverylongsecretkeyforsignature2025
ENV JWT_EXPIRATION=86400000

# DB 설정도 기본값으로 추가 (혹시 모르니까)
ENV SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/baseball?useSSL=false&serverTimezone=UTC
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=password
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver

# 로깅 레벨 최적화
ENV LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
ENV LOGGING_LEVEL_ORG_HIBERNATE_SQL=WARN
ENV LOGGING_LEVEL_ORG_OPENQA_SELENIUM=WARN
ENV LOGGING_LEVEL_IO_GITHUB_BONIGARCIA=WARN

# 톰캣 최적화
ENV SERVER_TOMCAT_THREADS_MIN_SPARE=2
ENV SERVER_TOMCAT_THREADS_MAX=8
ENV SERVER_TOMCAT_MAX_CONNECTIONS=30

ENTRYPOINT ["sh", "-c", "Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 & java $JAVA_OPTS -jar app.jar"]
EXPOSE 8080