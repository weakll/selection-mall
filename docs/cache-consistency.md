# 商品缓存一致性

## 缓存范围

| 数据 | Redis 缓存名 | 基础 TTL | 随机范围 |
|:---|:---|---:|---:|
| 商品详情 | `mall:catalog:product-item` | 30 分钟 | ±5 分钟 |
| 分类树 | `mall:catalog:category-tree` | 6 小时 | ±5 分钟 |
| 一级分类 | `mall:catalog:category-one` | 6 小时 | ±5 分钟 |
| 品牌列表 | `mall:catalog:brand-list` | 6 小时 | ±5 分钟 |

## 读取策略

商品服务使用 Cache Aside 模式：

1. 请求先查询 Redis。
2. 缓存命中时直接返回。
3. 缓存未命中时查询 MySQL。
4. 查询结果写入 Redis，包括空结果。

空结果也进入缓存，可以降低不存在商品或分类反复访问数据库的风险。

## 写入与失效

后台管理服务在商品、分类和品牌发生变更后主动清理对应缓存：

| 写操作 | 失效缓存 |
|:---|:---|
| 商品新增、修改、删除、审核、上下架 | `mall:catalog:product-item` |
| 分类导入 | `mall:catalog:category-tree`、`mall:catalog:category-one` |
| 品牌新增、修改、删除 | `mall:catalog:brand-list` |

缓存失效默认发生在事务方法成功返回之后，避免数据库事务回滚后缓存被错误清理。

## TTL 抖动

`JitterRedisCacheWriter` 在每次写入时为基础 TTL 增加随机偏移，降低大量缓存同时过期导致的数据库瞬时压力。

## 故障降级

商品和后台管理服务都配置了 `LoggingCacheErrorHandler`：

- Redis 读取失败时记录告警并回退到数据库。
- Redis 写入或失效失败时记录告警，不阻断商品查询和后台业务。

该策略优先保证商城核心业务可用，同时通过日志暴露缓存异常。

## 当前边界

- 商品缓存采用整体失效，不维护单个商品与 SKU 的反向索引。
- 缓存与数据库采用最终一致，不实现消息队列补偿。
- 当前只缓存商品读模型，库存校验和下单仍然实时读取商品服务。
