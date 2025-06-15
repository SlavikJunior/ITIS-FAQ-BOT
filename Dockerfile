FROM gradle:8.7-jdk21 AS build

WORKDIR /app
COPY src ./src
COPY build.gradle .
COPY settings.gradle .
COPY gradle ./gradle
COPY build/libs/*.jar ./app.jar
COPY build/dependencies/ ./lib/

RUN gradle build --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar ./app.jar

CMD ["java", "-cp", "app.jar:lib/*", "bot.BotsRunner"]