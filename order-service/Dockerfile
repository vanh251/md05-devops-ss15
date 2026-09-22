# Stage 1: Build ứng dụng bằng Gradle
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy gradle wrapper và các tệp cấu hình trước để tận dụng Docker layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Phân quyền thực thi cho script gradlew
RUN chmod +x ./gradlew

# Copy mã nguồn dự án
COPY src src

# Build ứng dụng thành tệp JAR (bỏ qua bước test để build nhanh hơn)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime nhẹ chứa ứng dụng đã build
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo non-root user đảm bảo an toàn bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy file jar từ stage build
COPY --from=build /app/build/libs/*.jar app.jar

# Cấu hình cổng và biến môi trường
EXPOSE 8080

# Chạy ứng dụng Java Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
