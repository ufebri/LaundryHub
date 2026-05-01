# Stage 1: Build the application
FROM gradle:8.6-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Build the backend distribution (this includes the shared module)
RUN gradle :backend:installDist --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine

# Expose the default port
EXPOSE 8080

# Create an app directory
RUN mkdir /app

# Copy the built distribution from the build stage
COPY --from=build /home/gradle/src/backend/build/install/backend /app

# Set the working directory
WORKDIR /app/bin

# Set the entrypoint to the generated run script
CMD ["./backend"]
