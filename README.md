# 精选商城

[![CI](https://github.com/weakll/selection-mall/actions/workflows/ci.yml/badge.svg)](https://github.com/weakll/selection-mall/actions/workflows/ci.yml)

精选商城（Selection Mall）是一个基于 `Spring Cloud Alibaba` 和 `Vue 3` 的前后端分离微服务商城项目，覆盖商品浏览、购物车、下单、库存控制、优惠券和可演示支付链路。

## 项目状态

v1 正在开发中。后端模块和 H5 前端均已完成第一阶段迁移，并分别通过 Maven 和 Vite 生产构建。

## v1 目标

- 网关统一路由与登录校验
- 用户、商品、购物车、订单和支付服务拆分
- Nacos 服务注册发现
- OpenFeign 服务调用
- Redis 商品缓存与缓存一致性
- 库存原子扣减与防超卖
- 订单提交幂等与待付款订单取消
- 支付记录幂等、订单状态流转与库存恢复
- Vue 3 H5 商城
- Docker Compose 本地基础设施与生产化应用编排
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
- [商品缓存一致性](docs/cache-consistency.md)
- [交易一致性](docs/transaction-consistency.md)
- [开发说明](docs/development.md)
- [开发路线图](docs/roadmap.md)
- [后端迁移说明](backend/README.md)
- [H5 迁移说明](mall-h5/README.md)
- [本地部署说明](deploy/README.md)

## 简历项目亮点

- 采用 Spring Cloud Alibaba 微服务拆分用户、商品、购物车、订单、支付和网关，使用 Nacos 完成服务发现，OpenFeign 完成跨服务调用。
- 在订单提交链路中使用 Redis 幂等键、库存原子扣减和失败补偿，避免重复下单、超卖以及订单落库失败造成的库存泄漏。
- 实现优惠券发布查询、主动领取、个人限领、库存校验和事务行锁，并通过 MySQL 联调验证真实数据读写。
- 完成待付款订单取消和库存恢复，支付记录使用条件更新保证重复回调不会重复推进订单状态和商品销量。
- 使用 Docker Compose 编排 MySQL、Redis、Nacos、网关、业务服务和 Nginx H5 静态站点，提供可复现的生产化部署入口。

## 简历截图建议

建议在本地启动服务后截取以下页面，并保存到简历或项目附件中：

1. H5 首页：商品分类、热销推荐和库存展示。
2. 商品详情：规格、价格、库存和立即购买入口。
3. 确认订单：收货地址、商品明细、优惠券和实付款金额。
4. 我的订单：待付款订单的取消、支付按钮和支付完成后的状态变化。
5. Docker Desktop：生产 Compose 服务列表和健康状态。

截图应展示真实运行结果；当前支付模块为本地虚拟支付演示链路，不代表已接入真实资金渠道。

## Roadmap

v1 先完成微服务商城、缓存一致性、库存并发控制和支付幂等。v2 计划增加 AI 智能客服能力，通过独立服务接入，不提前耦合到 v1 主链路。
