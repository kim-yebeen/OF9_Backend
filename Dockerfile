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

# Java 최적화
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.awt.headless=true"
ENV DISPLAY=:99

# 🔧 JWT 설정 수정 (Spring Boot property 네이밍 규칙에 맞게)
ENV JWT_SECRET=c29tZXZlcnlzZWN1cmVhbmRsb25nYmFzZTY0ZW5jb2RlZHNlY3JldGtleWZvcmhzNTEyIQ==
ENV JWT_ACCESS_EXPIRATION=86400000
ENV JWT_REFRESH_EXPIRATION=604800000

# 🔧 RDS PostgreSQL 설정 (MySQL이 아님!)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://of9-db.cvs68wqogubb.ap-northeast-2.rds.amazonaws.com:5432/postgres
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=Capstone34~!
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# JPA 및 Hibernate 설정
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=validate
ENV SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
ENV SPRING_JPA_SHOW_SQL=false

# 크롤링 활성화
ENV INITIAL_CRAWL_ENABLED=true

# Spring Boot 프로파일
ENV SPRING_PROFILES_ACTIVE=default

# 로깅 및 성능 최적화
ENV LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
ENV LOGGING_LEVEL_ORG_HIBERNATE_SQL=WARN
ENV LOGGING_LEVEL_ORG_OPENQA_SELENIUM=WARN
ENV LOGGING_LEVEL_IO_GITHUB_BONIGARCIA=WARN

# Tomcat 최적화
ENV SERVER_TOMCAT_THREADS_MIN_SPARE=2
ENV SERVER_TOMCAT_THREADS_MAX=8
ENV SERVER_TOMCAT_MAX_CONNECTIONS=30

# HikariCP 최적화
ENV SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3
ENV SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
ENV SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=300000
ENV SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=180000

ENTRYPOINT ["sh", "-c", "Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 & java $JAVA_OPTS -jar app.jar"]
EXPOSE 8080
