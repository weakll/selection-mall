# 精选商城

精选商城（Selection Mall）是一个基于 `Spring Cloud Alibaba` 和 `Vue 3` 的前后端分离微服务商城项目。

## 项目状态

v1 正在开发中。后端模块和 H5 前端均已完成第一阶段迁移，并分别通过 Maven 和 Vite 生产构建。

## v1 目标

- 网关统一路由与登录校验
- 用户、商品、购物车、订单和支付服务拆分
- Nacos 服务注册发现
- OpenFeign 服务调用
- Redis 商品缓存与缓存一致性
- 库存原子扣减与防超卖
- 订单提交幂等
- 支付回调幂等
- Vue 3 H5 商城
- Docker Compose 本地基础设施
- GitHub Actions 构建验证

## 技术栈

| 层级 | 技术 |
|:---|:---|
| 后端 | Java 17、Spring Boot、Spring Cloud Alibaba、Nacos、OpenFeign |
| 数据 | MySQL、MyBatis、Redis |
| 网关 | Spring Cloud Gateway |
| 前端 | Vue 3、Vite、Vant、Pinia、Vue Router、Axios |
| 工程化 | Maven、Docker Compose、GitHub Actions |

## 仓库结构

```text
selection-mall/
├─ backend/          # Java 微服务后端
├─ mall-h5/          # Vue 3 H5 商城
├─ deploy/           # 本地部署与基础设施配置
├─ docs/             # 架构、开发和路线图
└─ .github/          # CI 与仓库协作配置
```

## 文档

- [架构设计](docs/architecture.md)
- [开发说明](docs/development.md)
- [开发路线图](docs/roadmap.md)
- [后端迁移说明](backend/README.md)
- [H5 迁移说明](mall-h5/README.md)
- [本地部署说明](deploy/README.md)

## Roadmap

v1 先完成微服务商城、缓存一致性、库存并发控制和支付幂等。v2 计划增加 AI 智能客服能力，通过独立服务接入，不提前耦合到 v1 主链路。
