# 多阶段构建 - 前端
# 阶段 1: 构建阶段
FROM node:20-alpine AS builder
WORKDIR /app

# 安装所有依赖（包括 devDependencies，因为 vite 是开发依赖）
COPY package.json package-lock.json* ./
# 使用国内 npm 镜像加速
RUN npm config set registry https://registry.npmmirror.com && npm ci && npm cache clean --force

# 复制源代码
COPY . .

# 构建参数（Vite API 基础 URL）
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# 构建生产版本
RUN npm run build

# 阶段 2: 运行阶段（latest 基于 Debian，无 wget，健康检查用 bash /dev/tcp）
FROM nginx:latest
WORKDIR /app

# 添加标签
LABEL maintainer="wms-team"
LABEL version="1.0.0"
LABEL description="WMS Frontend Service"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建非 root 用户运行 Nginx
RUN chown -R nginx:nginx /usr/share/nginx/html \
    && chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /var/log/nginx \
    && chown -R nginx:nginx /etc/nginx/conf.d

# 健康检查（Debian 版镜像无 wget/curl，用 bash 内置 /dev/tcp 探测）
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/80' || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
