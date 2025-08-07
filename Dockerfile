FROM openjdk:17-jdk-slim

# Chrome 설치를 위한 기본 설정
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg \
    software-properties-common \
    apt-transport-https \
    ca-certificates

# Google Chrome 저장소 추가 및 설치
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable

# Chrome 의존성 패키지들 설치
RUN apt-get install -y \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxkbcommon0 \
    libxss1 \
    libgtk-3-0 \
    libxrandr2 \
    libgconf-2-4 \
    libxdamage1 \
    libxcomposite1 \
    libxi6 \
    libxtst6 \
    libnss3 \
    libcups2 \
    libxrandr2 \
    libasound2 \
    libpangocairo-1.0-0 \
    libatk1.0-0 \
    libcairo-gobject2 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Chrome 심볼릭 링크 생성 (google-chrome 명령어 사용 가능하게)
RUN ln -s /usr/bin/google-chrome-stable /usr/bin/google-chrome

WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"

# JWT 설정
ENV JWT_SECRET=c29tZXZlcnlzZWN1cmVhbmRsb25nYmFzZTY0ZW5jb2RlZHNlY3JldGtleWZvcmhzNTEyIQ==
ENV JWT_ACCESS_EXPIRATION=86400000
ENV JWT_REFRESH_EXPIRATION=604800000

# PostgreSQL 설정
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://of9-db.cvs68wqogubb.ap-northeast-2.rds.amazonaws.com:5432/postgres
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=Capstone34~!
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# JPA 설정
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=validate
ENV SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# 크롤링 활성화
ENV INITIAL_CRAWL_ENABLED=true
ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080