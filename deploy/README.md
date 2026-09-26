# Deploy

本目录用于维护 Selection Mall 的本地基础设施和部署配置。

## 计划内容

```text
deploy/
├─ docker-compose.yml
├─ mysql/
│  └─ init.sql
├─ nacos/
└─ scripts/
```

## 原则

- 本地基础设施优先使用 Docker Compose。
- 密码通过 `.env` 注入，仓库只保留 `.env.example`。
- 数据库初始化脚本只包含结构和演示数据，不包含个人数据。
- 部署配置应在后端和前端源码迁入后逐步补齐。
