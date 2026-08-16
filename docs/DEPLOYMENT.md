# WMS 部署文档

> 全栈 SaaS 仓储管理系统：Spring Boot 3.2 (Java 21) + Vue 3 + MySQL 8.0 + Redis + FastAPI AI Gateway

## 一、架构总览

```
                      ┌─────────────────────────────────────────┐
 浏览器 ──► Nginx:80 ─┤ /          → Vue3 静态资源 (SPA)         │
                      │ /api/      → wms-backend:8080 (Java)    │
                      │ /ai/       → wms-ai-gateway:8090 (SSE)  │
                      └─────────────────────────────────────────┘
                                        │
                        ┌───────────────┼───────────────┐
                   mysql:8.0       redis:latest    LLM API(外部)
```

| 服务 | 容器名 | 镜像/构建 | 端口（开发） | 端口（生产） |
|---|---|---|---|---|
| 前端 | wms-frontend | nginx:latest | 80 | 80 / 443 |
| 后端 | wms-backend | 多阶段 Maven 构建 → JRE21 Alpine | 8080 | 8080 |
| AI 网关 | wms-ai-gateway | python:3.11-slim 构建 | 8090 | 不暴露（走 Nginx） |
| MySQL | wms-mysql | mysql:8.0（固定 tag） | 3306 | 3306 |
| Redis | wms-redis | redis:latest | 6379 | 不暴露 + 密码 |

## 二、部署文件清单

| 文件 | 用途 |
|---|---|
| `docker-compose.yml` | 基础编排（开发/测试默认全量配置） |
| `docker-compose.prod.yml` | 生产覆盖层：强制强密码、Redis 不暴露端口、资源扩容 |
| `.env.example` | 环境变量模板，复制为 `.env` 后填写 |
| `Dockerfile` | 后端镜像（多阶段构建，非 root 运行，G1GC + OOM heapdump） |
| `frontend/Dockerfile` | 前端镜像（Node 构建 → Nginx 运行） |
| `frontend/nginx.conf` | Nginx：SPA 路由、`/api/`、`/ai/`（SSE 禁缓冲）反代、Gzip、安全头 |
| `ai-gateway/Dockerfile` | AI 网关镜像（阿里 pip 源） |
| `src/main/resources/schema.sql` | 数据库初始化（幂等：CREATE TABLE IF NOT EXISTS + INSERT IGNORE） |
| `settings.xml` | Maven 阿里云仓库（容器内构建加速） |
| `Makefile` | 常用部署命令快捷方式 |

## 三、快速部署（开发/测试）

### 1. 准备环境变量

```bash
cp .env.example .env
# 编辑 .env，至少填写 LLM_API_KEY（讯飞/阿里云）
```

### 2. 一键启动

```bash
docker compose up -d          # 或 make up
docker compose ps             # 确认 5 个服务均 healthy
```

- MySQL 首次启动自动执行 `schema.sql` 建库建表并写入初始数据（admin 账号 + 权限）
- 访问 `http://<host>/`，默认账号 `admin / admin123`

### 3. 常用运维命令

```bash
make logs              # 全部日志
make logs-backend      # 后端日志
make build-backend     # 仅重建后端
make restart           # 重启
make down              # 停止（保留数据卷）
make down-v            # 停止并清空数据（危险）
```

### 4. 更新版本

```bash
git pull
docker compose up -d --build backend frontend   # 代码变更后重建
# schema.sql 变更：幂等脚本，MySQL 重启不会重复执行；需手动增量执行新 SQL
```

## 四、生产部署

### 1. 环境变量（生产必须显式提供，缺失则拒绝启动）

```bash
# .env 中必须设置：
MYSQL_ROOT_PASSWORD=<强密码>
REDIS_PASSWORD=<强密码>
JWT_SECRET=<随机长字符串>
LLM_API_KEY=<真实Key>
```

### 2. 启动

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

生产覆盖层与开发版的差异：

| 项 | 开发 | 生产 |
|---|---|---|
| Redis 端口 | 宿主机 6379 | 不暴露，`requirepass` 强制密码 |
| AI 网关端口 | 宿主机 8090 | 不暴露，统一走 Nginx `/ai/` |
| 资源限制 | 后端 1G/1C | 后端 2G/2C，MySQL 1G，均带 reservations |
| 必填校验 | 默认值兜底 | `:?` 强制显式提供，缺失即报错 |
| Spring profile | default | `SPRING_PROFILES_ACTIVE: prod` |

### 3. HTTPS（可选）

`frontend/nginx.conf` 已监听 443（生产 compose 映射 443），挂载证书后补充 `ssl_certificate` 配置即可。

## 五、本地开发（不走 Docker 应用容器）

基础设施用容器，应用本地跑（便于热调试）：

```bash
docker compose up -d mysql redis    # 仅起基础设施

# 后端（8080）
mvn spring-boot:run

# 前端（3000，代理 /api → localhost:8080）
cd frontend && npm install && npm run dev
```

## 六、健康检查与排障

| 服务 | 检查方式 |
|---|---|
| 前端 | `curl http://<host>/health` → 200 OK |
| 后端 | `curl http://<host>:8080/actuator/health` |
| AI 网关 | `curl http://<host>:8090/health`（生产从容器网络内检查） |
| MySQL | `docker exec wms-mysql mysqladmin ping` |
| Redis | `docker exec wms-redis redis-cli ping` |

常见问题：

| 现象 | 原因与处理 |
|---|---|
| 后端构建时 Maven 卡住/报 jar 损坏 | 网络抖动导致依赖损坏，`docker compose build --no-cache backend` 重建 |
| 镜像拉取 500/超时 | `/etc/docker/daemon.json` 配置多个加速器（DaoCloud、1ms.run 等）后 `systemctl restart docker` |
| 登录报“服务器错误” | 检查后端日志中 Mapper 表名与 schema.sql 是否一致（当前统一无前缀：user/warehouse/...） |
| AI 对话无响应 | 确认 `.env` 的 LLM_API_KEY 有效；Nginx `/ai/` 已禁用 proxy_buffering（SSE） |
| 权限更新后菜单不显示 | 权限缓存在 Redis，重新登录刷新 `user:perm:{userId}` |

## 七、安全要点（已内置）

- 后端容器以非 root（appuser）运行，镜像含 HEALTHCHECK 与资源限制
- 验证码单次有效（Redis），JWT 密钥生产强制显式配置
- `schema.sql` 使用 `INSERT IGNORE`，重跑不会重置已有用户密码
- admin 密码在库中为 BCrypt 哈希，更新用户信息不会覆盖密码字段
- `.dockerignore` 排除 `.env` 等敏感文件，密钥只经环境变量注入
- Nginx 输出 X-Frame-Options / nosniff / XSS / Referrer-Policy 安全头
