# Use an official OpenJDK runtime as a parent image
FROM amazoncorretto:17.0.7-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file to the container
COPY build/libs/IoTServer-all.jar /app/IoTServer-all.jar
# Copy the secrets.json file to the container
COPY app/secrets.json /app/secrets.json

# Run the JAR file
CMD ["java", "-jar", "/app/IoTServer-all.jar"]
