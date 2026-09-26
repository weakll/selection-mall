# Deploy

本目录维护精选商城的本地基础设施，包括 MySQL、Redis 和 Nacos。

## 启动

在仓库根目录执行：

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f deploy/docker-compose.yml up -d
```

检查状态：

```powershell
docker compose --env-file .env -f deploy/docker-compose.yml ps
```

停止服务：

```powershell
docker compose --env-file .env -f deploy/docker-compose.yml down
```

## 默认端口

| 服务 | 端口 |
|:---|:---|
| MySQL | `3306` |
| Redis | `6379` |
| Nacos HTTP | `8848` |
| Nacos gRPC | `9848`、`9849` |

## 说明

- MySQL、Redis 和 Nacos 均使用持久化数据卷。
- `deploy/mysql/init.sql` 当前只负责创建数据库，业务表结构将在确认后加入。
- 真实环境必须修改 `.env` 中的默认密码。
- Compose 文件基于 Nacos Client `2.2.1`，服务端固定为 `nacos/nacos-server:v2.2.1`。
