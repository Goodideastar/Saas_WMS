# AI Gateway - Python FastAPI 服务
FROM python:3.11-slim

WORKDIR /app

# 添加标签
LABEL maintainer="wms-team"
LABEL version="1.0.0"
LABEL description="WMS AI Gateway Service"

# 设置时区（slim 镜像需安装 tzdata；apt 换阿里云源，与 Maven/npm/pip 镜像策略一致）
ENV TZ=Asia/Shanghai
RUN sed -i 's|deb.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list.d/debian.sources 2>/dev/null; \
    sed -i 's|deb.debian.org|mirrors.aliyun.com|g' /etc/apt/sources.list 2>/dev/null; \
    apt-get update \
    && apt-get install -y --no-install-recommends tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖清单，使用国内 pip 镜像加速（与 Maven/npm 镜像策略一致）
COPY requirements.txt .
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt

# 复制源代码（.dockerignore 已排除 .env / __pycache__ 等）
COPY . .

# 创建非 root 用户运行（与 backend/frontend 保持一致）
RUN useradd -m -u 1000 appuser \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8090

# 健康检查（slim 镜像无 wget/curl，使用 Python 标准库）
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8090/health', timeout=5)" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8090"]
