# ==============================================================
# 校园图书借阅管理系统 - 多阶段构建 Dockerfile
# 支持跨平台：linux/amd64 (x86) 和 linux/arm64 (Apple Silicon / AWS Graviton)
#
# 构建命令（本地）：
#   docker build -t campus-library .
# 跨平台构建（需要 Docker Buildx）：
#   docker buildx build --platform linux/amd64,linux/arm64 -t campus-library .
# ==============================================================


# ==============================================================
# 阶段一：编译构建（Build Stage）
# ==============================================================
# --platform=$BUILDPLATFORM：在宿主机原生平台执行构建，避免跨架构编译的性能损耗
# eclipse-temurin 是 AdoptOpenJDK 的官方继任者，由 Adoptium 社区维护，
# 提供 linux/amd64 和 linux/arm64 的官方多架构镜像
FROM --platform=$BUILDPLATFORM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 注入阿里云 Maven 镜像配置，让所有依赖（含插件、父pom）都走国内节点
# 必须在下载依赖之前完成，否则仍会访问 Maven Central
COPY settings.xml /root/.m2/settings.xml

# 1. 优先复制 pom.xml 并预下载所有依赖
#    利用 Docker 层缓存：只要 pom.xml 不变，此层被缓存，后续构建无需重新下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# 2. 复制源代码并执行 Maven package 生命周期（跳过测试加快构建）
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# 验证 JAR 包生成成功
RUN ls -lh target/*.jar


# ==============================================================
# 阶段二：运行时（Runtime Stage）
# ==============================================================
# eclipse-temurin:17-jre 官方多架构镜像，同时支持：
#   - linux/amd64（Intel/AMD x86_64 服务器）
#   - linux/arm64（Apple M1/M2、AWS Graviton、树莓派4等 ARM64 设备）
# 使用 JRE（而非 JDK）减小最终镜像体积（约 250MB vs 500MB）
FROM eclipse-temurin:17-jre

# 设置容器时区为上海（影响日期时间的显示和 Java LocalDate/LocalDateTime）
ENV TZ=Asia/Shanghai

WORKDIR /app

# 从构建阶段复制打包好的可执行 JAR
COPY --from=builder /app/target/*.jar app.jar

# 声明服务监听端口（文档作用，不影响实际端口映射）
EXPOSE 8080

# 安装 curl（用于 Docker Compose 健康检查）并清理 apt 缓存减小镜像体积
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 安全加固：创建非 root 用户运行应用，降低容器逃逸风险
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

# 启动命令（使用 exec 格式，支持 SIGTERM 优雅关闭）
# - Xms256m/Xmx512m：限制 JVM 堆内存（容器内避免内存溢出）
# - spring.profiles.active=docker：激活 Docker 配置文件（使用 MySQL）
# - java.security.egd：加速 SecureRandom 初始化（避免启动缓慢）
ENTRYPOINT ["java", \
            "-Xms256m", \
            "-Xmx512m", \
            "-Dspring.profiles.active=docker", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]
