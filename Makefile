.PHONY: help up down build restart logs clean status

help: ## 显示帮助信息
	@echo "WMS 仓储管理系统 - Docker 部署"
	@echo ""
	@echo "用法: make [命令]"
	@echo ""
	@echo "命令:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

up: ## 启动所有服务
	docker-compose up -d

up-build: ## 重新构建并启动所有服务
	docker-compose up --build -d

down: ## 停止所有服务
	docker-compose down

down-v: ## 停止服务并删除数据卷（⚠️ 会清除所有数据）
	docker-compose down -v

restart: ## 重启所有服务
	docker-compose restart

logs: ## 查看日志
	docker-compose logs -f

logs-backend: ## 查看后端日志
	docker-compose logs -f backend

logs-frontend: ## 查看前端日志
	docker-compose logs -f frontend

logs-db: ## 查看数据库日志
	docker-compose logs -f mysql

status: ## 查看服务状态
	docker-compose ps

clean: ## 清理无用镜像
	docker system prune -f

build: ## 构建镜像
	docker-compose build

build-backend: ## 仅构建后端镜像
	docker-compose build backend

build-frontend: ## 仅构建前端镜像
	docker-compose build frontend

start: ## 启动已停止的服务
	docker-compose start

stop: ## 停止运行中的服务
	docker-compose stop

exec-backend: ## 进入后端容器
	docker-compose exec backend sh

exec-frontend: ## 进入前端容器
	docker-compose exec frontend sh

exec-mysql: ## 进入数据库容器
	docker-compose exec mysql sh

exec-redis: ## 进入 Redis 容器
	docker-compose exec redis sh
