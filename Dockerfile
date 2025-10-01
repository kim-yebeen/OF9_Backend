FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    curl \
    && rm -rf /var/cache/apk/*

RUN ln -s /usr/bin/chromium-browser /usr/bin/google-chrome \
    && ln -s /usr/bin/chromedriver /usr/local/bin/chromedriver

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]