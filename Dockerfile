# 기존 Dockerfile을 이렇게 수정
FROM your-dockerhub-username/java-chrome-base:latest

WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"
ENV JWT_SECRET=c29tZXZlcnlzZWN1cmVhbmRsb25nYmFzZTY0ZW5jb2RlZHNlY3JldGkQ==
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