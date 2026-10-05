# AI 客服服务（service-ai）

商城 v2 的 AI 智能客服。用户可在 H5「客服中心」页用自然语言询问商品、自己的订单和售后政策，
回答以 SSE 逐字流式返回。

## 1. 它是怎么接入的

AI 客服是**独立服务**，不并入商城既有进程。

| | selection-mall | service-ai |
|---|---|---|
| Spring Boot | 3.0.5 | 3.0.5（同栈） |
| Spring Cloud Alibaba | 2022.0.0.0-RC2 | 同版本 |
| 端口 | — | 8516 |
| 数据库 | MySQL 8 | **不访问**（排除数据源自动装配） |
| 业务数据来源 | 自己的表 | **Feign 调 product / order 服务** |

### 为什么不合进商城进程

不是偏好问题，是版本边界。商城运行在 Spring Boot 3.0.5 + Spring Cloud Alibaba
2022.0.0.0-RC2；而 AI 侧参考的开源实现（ragent）要求 Spring Boot 4.x 及其专属 starter
（`mybatis-plus-spring-boot4-starter`、`sa-token-spring-boot4-starter`）。
Spring Cloud Alibaba 在 2022.0.0.0-RC2 之后没有 Boot 4 对应版本，
把商城升到 Boot 4 会让 Gateway / Nacos / Sentinel / OpenFeign 全部失去支撑。

因此 v2 采取「独立服务 + 网关路由 + Feign 取数」，这也与 `docs/roadmap.md` 里
「v2 默认通过独立服务接入」一致。

## 2. 依赖与已验证的兼容性

核心依赖是 [AgentScope](https://github.com/agentscope-ai/agentscope) `2.0.2`——框架无关的
Agent 运行时，提供 ReAct 循环、工具注册与流式事件。

它与商城技术栈的兼容性做过字节码级验证，不是"跑起来没报错"：

| 检查项 | 结论 |
|---|---|
| 字节码版本 | 434/434 个 class 为 **Java 17**（major 61），无需 Java 21 |
| 编译 | Boot 3.0.5 下 `mvn test-compile` 通过 |
| 运行时类加载 | 关键类型全部可解析，无 `NoClassDefFoundError` |
| Jackson 降级 2.21.1 → 2.14.2 | 所用 `JsonMapper` 在 2.14.2 中存在；未引用 2.15+ 专属 API |
| Reactor 降级 3.8.2 → 3.5.4 | 只用 Flux/Mono/Sinks 核心类型；实跑订阅成功（单次回答 115 个增量） |

降级是 Maven 按 Boot BOM 收敛的结果——Boot 3.0.5 的依赖管理生效，
不会把 AgentScope 声明的高版本带进来。

## 3. 代码结构

```
service-ai/src/main/java/io/github/weakll/mall/ai/
├── AiApplication.java              启动类（排除数据源自动装配）
├── config/
│   ├── MallAiConfiguration.java    装配：模型 bean + ChatClient bean
│   └── MallAiProperties.java       mall.ai.* 配置，密钥只从环境变量读
├── model/
│   ├── ChatClient.java             自有窄接口：上层不依赖 AgentScope
│   ├── ChatChunk.java              流式分片（delta / done / error）
│   └── ChatRequest.java            对话请求与消息
├── agent/
│   ├── ReActChatClient.java        Agent 装配、事件→分片映射
│   └── AgentToolSet.java           @Tool 方法，暴露给模型的工具契约
├── tool/
│   ├── ProductTools.java           商品：关键词搜索、详情
│   ├── OrderTools.java             订单：列表、详情（用户隔离）
│   ├── FaqTools.java               售后政策知识库
│   ├── ToolSupport.java            条数收敛、字段裁剪、序列化
│   └── ToolResult.java             工具结果
├── auth/
│   ├── ToolAuthContext.java        工具执行期的凭据上下文
│   └── ToolTokenFeignInterceptor.java  把凭据注入 Feign 请求
├── controller/ChatController.java  SSE 端点 + 健康检查
└── service/ChatService.java        消息组装（请求作用域，捕获凭据）

src/main/resources/
├── application.yml / application-dev.yml
└── ai/faq-knowledge.yml            售后政策知识库（7 条）
```

## 4. 接口

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/ai/auth/chat/stream?message=...` | 需登录 | SSE 流式对话 |
| GET | `/api/ai/health` | 公开 | 健康检查（不返回密钥） |
| POST | `/api/ai/echo` | 公开 | 诊断入参绑定 |

`auth` 段由网关的 `AuthGlobalFilter` 强制校验（它匹配 `/api/**/auth/**`），
因此对话接口天然要求登录。

### SSE 事件格式

```
event:delta
data:{"content":"您"}

event:done
data:{"finishReason":"stop"}
```

失败时发 `event:error`，`data` 为 `{"message":"..."}`，流随即终止。

## 5. 三个工具与数据来源

| 工具 | 数据来源 | 说明 |
|---|---|---|
| `queryProduct` | `GET /api/product/{page}/{limit}?keyword=` | 复用既有接口，其查询对象本就支持 keyword 过滤，**未改动商品服务** |
| `queryMyOrders` | `GET /api/order/orderInfo/auth/{page}/{limit}` | **不接受 userId 参数** |
| `searchFaq` | `resources/ai/faq-knowledge.yml` | 启动时载入内存，关键词命中 |

### 用户数据隔离是怎么保证的

`queryMyOrders` **没有 userId 参数**。身份链路是：

```
前端 token 头 → 网关校验 → service-ai 捕获
  → 工具执行时绑定 ToolAuthContext → Feign 拦截器写入 token 头
  → 订单服务按当前登录用户过滤
```

即：**模型无法通过构造参数越权查询他人订单**，因为不存在可传错的口子。
这比"传入 userId 再校验"更安全。测试 `BusinessToolsTest` 里有一条结构性断言，
用反射检查订单工具的公开方法签名中不含 userId。

> 注意：这条链路依赖显式传参而非 ThreadLocal。原因见第 7 节。

## 6. 配置

全部配置项在 `application-dev.yml`，密钥经环境变量注入：

```yaml
mall:
  ai:
    enabled: true
    model:
      provider: deepseek
      base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
      api-key: ${DEEPSEEK_API_KEY:}          # 只从环境变量读
      model: ${DEEPSEEK_MODEL:deepseek-chat}
    agent:
      max-iters: 6                            # ReAct 循环上限
      system-prompt: |                        # 客服人设与工具使用约束
        ...
```

本地把密钥写入 `deploy/.env`（已被 `.gitignore` 忽略）。
**不要写进 `deploy/.env.example`**——那是被跟踪的模板，值会直接提交到仓库。

未配置密钥时服务仍可正常启动，对话接口返回可读提示而非启动失败，
便于在只排查基础设施时隔离 AI 故障。

## 7. 两个必须知道的实现约束

### 7.1 凭据必须显式跨线程传递

商城通用的 `UserTokenFeignInterceptor` 从 `RequestContextHolder` 取 token 透传。
但 Agent 的**工具执行发生在 Reactor 的 `boundedElastic` 调度器线程上**，
那里没有 servlet 请求上下文，取到的是 null——更糟的是它还会把 null
写进 `token` 头，覆盖掉正确值。

所以本服务：

1. **不加** `@EnableUserTokenFeignInterceptor`
2. `ChatService` 声明为请求作用域，在 servlet 线程上捕获 token
3. token 沿 `chat(request, token)` 显式传参到工具执行点
4. `AgentToolSet` 用 `ToolAuthContext.callWith` 在执行线程上绑定，供 Feign 拦截器读取

**推论**：`AgentToolSet` 持有凭据，**不是并发安全的**，一次请求一个实例
（由 `ReActChatClient` 现场构造）。若将来要支持并发会话，须改为每次调用传参。

### 7.2 分页响应的反序列化

商城列表接口返回 `Result<PageInfo<T>>`，`PageInfo` 来自 pagehelper，序列化后
`data` 是 `{"total":43,"list":[...]}` **对象**。若 Feign 客户端把它声明成
`Result<List<T>>`，Jackson 会反序列化失败，异常在调用侧抛出、表现成"下游 500"，
极易误判为下游服务故障。

因此 `mall-model` 增加了 `PageResult<T>`（只含 `total` + `list`，
`@JsonIgnoreProperties(ignoreUnknown = true)`），Feign 客户端统一使用它。
好处是不引入 pagehelper 依赖，也不受其字段变化影响。

## 8. 运行

前置：MySQL / Redis / Nacos 已启动（`deploy/docker-compose.yml`），
product / order / gateway 服务已启动。

```bash
cd backend
# 构建（首次或改动依赖后）
mvnw -pl mall-service/service-ai -am -DskipTests install

# 启动（注意：不要加 -am，聚合模块没有 main class 会让 spring-boot:run 失败）
mvnw -f mall-service/service-ai/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

环境变量（或让 `.env` 生效）：

```
DEEPSEEK_API_KEY=...        REDIS_HOST=127.0.0.1
REDIS_PORT=6379             REDIS_PASSWORD=...
NACOS_SERVER_ADDR=127.0.0.1:8848
```

前端：`cd mall-h5 && pnpm dev`（3000 端口，`/api` 代理到网关 8500），
访问 `http://127.0.0.1:3000/#/service`。

### 自测

```bash
# 单测（不触网）
mvnw -f mall-service/service-ai/pom.xml test

# 含真实模型调用（需设 DEEPSEEK_API_KEY，会产生少量调用费用）
DEEPSEEK_API_KEY=... mvnw -f mall-service/service-ai/pom.xml test
```

未设 `DEEPSEEK_API_KEY` 时，`ReActChatClientLiveTest` 自动跳过。

## 9. 已知边界

- **无服务端会话持久化**：上下文由前端携带最近若干条（上限 10 条）。
  刷新页面即丢失对话。ragent 内部有完整的会话/摘要记忆系统，
  若需要应作为独立一期实现，而不是零散补丁。
- **知识库在资源文件里**：7 条政策。条目增长到需要后台维护时迁移到 MySQL 表，
  `faq-knowledge.yml` 可作为初始种子数据。
- **无向量检索**：客服场景的知识库条目少、变更低频，关键词命中足够且更可解释。
  不建议为"简历好看"引入 Milvus/ES——会换来"这个场景为什么需要向量库"这类答不好的问题。
- **单实例多请求未验证并发**：见 7.1 的凭据约束。
- **`Bash` 工具**：未提供，Agent 只有三个业务工具，无文件与网络访问能力。

## 10. 归属

本服务的**模型适配层与 Agent 集成思路**参考了开源项目
[ragent](https://github.com/nageoffer/ragent)（Apache-2.0）的公开设计，
未复制其代码；Agent 运行时直接使用其同样采用的 AgentScope 库。
详见仓库根目录 `NOTICE`。
