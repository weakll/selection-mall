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
| MySQL | `3307`，容器内部仍为 `3306` |
| Redis | `6379` |
| Nacos HTTP | `8848` |
| Nacos gRPC | `9848`、`9849` |

## 说明

- MySQL、Redis 和 Nacos 均使用持久化数据卷。
- MySQL 只会在 `mysql-data` 数据卷首次初始化时自动执行 `deploy/mysql/init.sql`。
- 已有数据卷不会因重新启动 Compose 而重新执行初始化脚本。需要应用当前建表脚本时，在仓库根目录运行：

  ```powershell
  Get-Content -Raw -Encoding UTF8 .\deploy\mysql\init.sql |
    docker compose --env-file .env -f .\deploy\docker-compose.yml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'
  ```

- 初始化脚本使用 `CREATE TABLE IF NOT EXISTS`，不会删除或重建现有表；已有表的结构变更需要单独编写迁移。
- 真实环境必须修改 `.env` 中的默认密码。
- Compose 文件基于 Nacos Client `2.2.1`，服务端固定为 `nacos/nacos-server:v2.2.1`。
