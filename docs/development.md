# 开发说明

## 环境要求

| 工具 | 建议版本 |
|:---|:---|
| JDK | 17 |
| Maven | 3.9 或更高 |
| Node.js | 20 LTS 或更高 |
| pnpm | 9 或更高 |
| Docker Desktop | 当前稳定版 |
| MySQL | 8.x |
| Redis | 7.x |
| Nacos | 2.x |

## 本地配置

1. 复制 `.env.example` 为 `.env`。
2. 修改本地数据库、Redis 和 Nacos 配置。
3. 确认 `.env` 被 Git 忽略。
4. 执行 `docker compose --env-file .env -f deploy/docker-compose.yml up -d` 启动基础设施。
5. 启动后端服务。
6. 启动 H5 前端。

具体命令将在对应源码迁入后补充，当前不提供无法验证的启动命令。

## 配置规则

- 真实密码、Token、支付宝密钥和模型 API Key 不进入 Git。
- 公共服务配置与本地开发配置分离。
- 所有可替换地址优先使用环境变量。
- 示例配置必须可以直接说明每个变量的用途。

## Git 规范

提交信息使用以下前缀：

```text
chore:
docs:
refactor:
feat:
fix:
test:
ci:
```

一次提交只处理一个主题。源码迁移、缓存改造、库存改造和支付改造必须分开提交。

## 完成标准

- 后端可以执行 Maven 编译和测试。
- 前端可以安装依赖并完成构建。
- 核心接口具备明确的成功与失败行为。
- 缓存、库存和支付链路具备测试或可重复验证步骤。
- README 中的启动说明与实际环境一致。
