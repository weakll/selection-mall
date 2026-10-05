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

## 演示数据导入

`init.sql` 只建表结构，不含商品数据。新装环境需要按顺序导入以下两个脚本：

```powershell
# 1) 商品目录：8 商品 / 20 SKU / 1 个自营品牌（自营严选定位）
Get-Content -Raw -Encoding UTF8 .\deploy\mysql\catalog.sql |
  docker compose --env-file .env -f .\deploy\docker-compose.yml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'

# 2) 演示优惠券
Get-Content -Raw -Encoding UTF8 .\deploy\mysql\demo-data.sql |
  docker compose --env-file .env -f .\deploy\docker-compose.yml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'
```

`catalog.sql` 由 `scripts/gen_catalog.py` 生成，可重复执行（幂等），
并会同步历史订单明细里反规范化存储的商品名与图片。商品配图的生成方式见
[演示数据与商品配图](../docs/demo-assets.md)。

## 生产化部署

生产 Compose 会构建 H5、网关和业务服务镜像；MySQL、Redis、Nacos 作为外部基础设施运行。先启动基础设施并完成后端打包，再在仓库根目录执行：

```powershell
Copy-Item deploy/.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml build
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml ps
```

访问 `http://服务器地址/` 使用 H5，`/api/` 请求由 Nginx 转发到网关。生产环境必须替换 `.env` 中的密码和 `FRONTEND_URL`，并在云安全组仅开放 `80` 或 `443`。应用服务通过 Docker 网络访问 `mysql`、`redis`、`nacos` 服务名。
