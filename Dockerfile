# Dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar -Dspring.profiles.active=db,jwt app.jar"]

EXPOSE 8080

# 메모리 제한 (선택사항)
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar -Dspring.profiles.active=db,jwt app.jar"]
