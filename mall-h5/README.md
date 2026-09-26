# Mall H5

H5 商城使用 Vue 3、Vite、Vant、Pinia、Vue Router 和 Axios。

## 当前状态

源码已经迁入，依赖管理统一为 pnpm，并已通过生产构建验证。

## 主要页面

- 首页
- 商品分类与商品列表
- 商品详情
- 购物车
- 订单确认与订单列表
- 用户中心
- 收货地址
- 登录与注册

## 本地运行

```bash
pnpm install
pnpm dev
pnpm build
```

开发环境默认通过 Vite 代理将 `/api` 转发到 `http://127.0.0.1:8500`。生产环境如需直连其他网关，可配置 `VITE_API_BASE_URL`。
