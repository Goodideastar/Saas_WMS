# 多阶段构建 - 后端
# 阶段 1: 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 优化 Docker 缓存层
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 设置 JVM 参数并打包
ARG MAVEN_OPTS="-Xmx512m"
ENV MAVEN_OPTS=$MAVEN_OPTS
RUN mvn package -DskipTests -B -q

# 阶段 2: 运行阶段
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 添加标签
LABEL maintainer="wms-team"
LABEL version="1.0.0"
LABEL description="WMS Backend Service"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/cache/apk/*

# 创建非 root 用户
RUN addgroup -g 1000 -S appgroup \
    && adduser -u 1000 -S appuser -G appgroup

# 复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 修改文件所有权
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM 参数优化
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs/heapdump.hprof", \
  "-Xms256m", \
  "-Xmx512m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
