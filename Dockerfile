# Dockerfile - 완전한 Chrome 환경 구성
FROM eclipse-temurin:17-jre

# Chrome 및 필요한 모든 패키지 설치
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg2 \
    unzip \
    xvfb \
    libxi6 \
    libgconf-2-4 \
    default-jdk \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV DISPLAY=:99

ENTRYPOINT ["sh", "-c", "Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 & java $JAVA_OPTS -jar app.jar"]

EXPOSE 8080

