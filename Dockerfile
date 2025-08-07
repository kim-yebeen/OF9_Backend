FROM eclipse-temurin:17-jre

# Chrome 설치 (가장 간단한 방법)
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    && wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y ./google-chrome-stable_current_amd64.deb \
    && rm google-chrome-stable_current_amd64.deb \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

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
