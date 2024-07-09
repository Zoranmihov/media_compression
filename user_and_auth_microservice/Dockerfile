# Use an official OpenJDK 21 runtime as a parent image
FROM openjdk:21-jdk-slim

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src

# Give executable permissions to mvnw
RUN chmod +x mvnw

# Package the application
RUN ./mvnw clean package -DskipTests

# Expose the port the application runs on
EXPOSE 8080

# Run the application
CMD ["./mvnw", "spring-boot:run"]
