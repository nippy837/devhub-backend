# 第一阶段：使用 Maven 和 Java 21 编译后端
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制构建配置和源代码
COPY pom.xml ./
COPY src ./src

# 打包成可运行的 JAR
# 首次部署先跳过测试，避免依赖数据库的测试阻塞打包
RUN mvn -B -DskipTests package

# 第二阶段：只保留运行后端需要的 Java 21 环境
FROM eclipse-temurin:21-jre
WORKDIR /app

# 从第一阶段取出 JAR，并统一命名为 app.jar
COPY --from=build /app/target/*.jar app.jar

# 声明后端服务端口
EXPOSE 8080

# 容器启动时运行 Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]