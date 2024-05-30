FROM openjdk:17-jdk-slim
WORKDIR /app
#VOLUME /app/backend-data
ADD gateway-http-backend-1.0-SNAPSHOT.jar gateway-http-backend.jar
ENTRYPOINT ["java", "-jar", "/app/gateway-http-backend.jar"]
EXPOSE 8083 
