FROM eclipse-temurin:17-jre

# Chrome 설치 (가장 간단한 방법)
FROM openjdk:17-jdk-slim

# Chrome과 필요한 의존성들을 한번에 설치
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# 나머지 환경변수들은 그대로...
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"
ENV JWT_SECRET=c29tZXZlcnlzZWN1cmVhbmRsb25nYmFzZTY0ZW5jb2RlZHNlY3JldGtleWZvcmhzNTEyIQ==
ENV JWT_ACCESS_EXPIRATION=86400000
ENV JWT_REFRESH_EXPIRATION=604800000
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://of9-db.cvs68wqogubb.ap-northeast-2.rds.amazonaws.com:5432/postgres
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=Capstone34~!
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=validate
ENV SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
ENV INITIAL_CRAWL_ENABLED=true
ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080