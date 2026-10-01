# Zweistufiger Build: bauen mit Maven, ausliefern nur mit der JRE.
# Das haelt das Ergebnis klein und bringt keinen Compiler ins laufende System.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Erst nur die pom.xml kopieren, damit die Abhaengigkeiten in einer eigenen
# Docker-Schicht landen und bei Codeaenderungen nicht neu geladen werden.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
