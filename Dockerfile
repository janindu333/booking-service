# Use an official Java runtime as a parent image
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the application JAR file into the container
COPY target/booking-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application will run on
EXPOSE 8084

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
