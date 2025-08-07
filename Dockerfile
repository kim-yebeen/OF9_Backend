FROM kimyebeen125/java-chrome-base:v2

WORKDIR /app
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"

ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080