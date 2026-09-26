# Backend

后端采用 Maven 多模块结构，统一使用以下命名：

```text
groupId: io.github.weakll.mall
Java package: io.github.weakll.mall
```

## 计划模块

```text
backend/
├─ mall-common/
├─ mall-model/
├─ mall-gateway/
├─ mall-service/
│  ├─ service-user/
│  ├─ service-product/
│  ├─ service-cart/
│  ├─ service-order/
│  └─ service-pay/
└─ mall-service-client/
```

## 当前状态

后端源码已经迁入，模块坐标统一为 `io.github.weakll.mall`，并已通过以下构建验证：

```bash
mvn -DskipTests clean package
```

## 后续改造规则

- 不复制构建产物、日志、IDE 配置和本地密钥。
- 所有模块统一使用 `mall-` 或 `service-` 命名。
- Java 包名不得保留旧项目标识。
- 配置通过环境变量注入，不在仓库保存真实密码和密钥。
- 先保证单体模块可构建，再迁入服务拆分代码。
- 缓存、库存和支付改造分别提交，避免一次提交混合多个主题。
