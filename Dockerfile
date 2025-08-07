FROM kimyebeen125/java-chrome-base:latest

WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080