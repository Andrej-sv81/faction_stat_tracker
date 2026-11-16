FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN ./mvnw dependency:go-offline 2>/dev/null || true
COPY src src
RUN apt-get update && apt-get install -y maven && mvn package -DskipTests
FROM eclipse-temurin:21-jre
COPY --from=build /app/target/*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
