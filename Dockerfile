# 경량화된 Chrome 환경 (EC2 비용 절약)
FROM eclipse-temurin:17-jre

# 필수 패키지만 설치하여 용량 최소화
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg2 \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libgtk-3-0 \
    libgtk-4-1 \
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

# 메모리 사용량 최소화 (EC2 t2.micro 고려)
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV DISPLAY=:99

ENTRYPOINT ["sh", "-c", "Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 & java $JAVA_OPTS -jar app.jar"]
EXPOSE 8080