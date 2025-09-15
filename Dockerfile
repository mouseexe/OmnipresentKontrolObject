FROM gradle:7.6-jdk11 AS builder
WORKDIR /home/gradle/src
COPY . .
RUN gradle shadowJar --no-daemon
FROM registry.access.redhat.com/ubi8/openjdk-11
COPY --from=builder /home/gradle/src/build/libs/*.jar /deployments/app.jar
CMD ["java", "-jar", "/deployments/app.jar"]