package io.github.weakll.mall.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.entity.order.OrderItem;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具层共用的支撑逻辑。
 *
 * <p>抽出来的原因：商品与订单两个工具都要做"限流条数 + 裁剪字段 + 序列化"，
 * 不抽的话三处重复且容易在某一处忘记裁剪导致 token 膨胀。
 */
@Component
public class ToolSupport {

    /** 单次工具返回给模型的最大条目数。超过这个量既浪费 token，也超出模型可用性。 */
    public static final int MAX_ITEMS = 5;

    private static final Logger log = LoggerFactory.getLogger(ToolSupport.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 校验并归一化关键词。
     *
     * @return 归一化后的关键词；不合法时返回 null
     */
    public String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        // 过短的关键词会产生大量无关命中，直接拒绝比让模型自行判断更省成本
        return trimmed.length() < 1 ? null : trimmed;
    }

    /** 归一化条数上限，防止模型传入超大 limit。 */
    public int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return MAX_ITEMS;
        }
        return Math.min(limit, MAX_ITEMS);
    }

    /**
     * 拆开 {@link Result} 包装。
     *
     * @return 业务数据；调用失败或返回空时返回 null，由调用方转成 {@link ToolResult#fail}
     */
    public <T> T unwrap(Result<T> result, String what) {
        if (result == null) {
            log.warn("{} 返回空结果对象", what);
            return null;
        }
        if (result.getCode() == null || result.getCode() != 200) {
            log.warn("{} 返回非成功码 code={} message={}", what, result.getCode(), result.getMessage());
            return null;
        }
        return result.getData();
    }

    /**
     * 商品摘要：只保留回答用户问题所需的字段。
     *
     * <p>刻意排除 costPrice（成本价）等内部字段——工具结果会进入模型上下文，
     * 内部成本数据不应外泄。
     */
    public Map<String, Object> toProductSummary(ProductSku sku) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("skuId", sku.getId());
        map.put("name", sku.getSkuName());
        map.put("price", sku.getSalePrice());
        map.put("stock", sku.getStockNum());
        map.put("spec", sku.getSkuSpec());
        map.put("sales", sku.getSaleNum());
        return map;
    }

    /** 订单摘要：含商品明细，排除收货人手机号等敏感字段。 */
    public Map<String, Object> toOrderSummary(OrderInfo order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", order.getOrderNo());
        map.put("status", statusText(order.getOrderStatus()));
        map.put("totalAmount", order.getTotalAmount());
        map.put("createdAt", order.getCreateTime());
        map.put("paymentTime", order.getPaymentTime());
        map.put("deliveryTime", order.getDeliveryTime());

        List<Map<String, Object>> items = new ArrayList<>();
        if (order.getOrderItemList() != null) {
            for (OrderItem item : order.getOrderItemList()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", item.getSkuName());
                entry.put("price", item.getSkuPrice());
                entry.put("count", item.getSkuNum());
                items.add(entry);
            }
        }
        map.put("items", items);
        return map;
    }

    /**
     * 订单状态中文描述。
     *
     * <p>取值与后端 {@code OrderInfo.orderStatus} 的 Schema 注释一致
     * （0 待付款 / 1 待发货 / 2 已发货 / 3 已完成 / -1 已取消）。
     */
    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case -1 -> "已取消";
            default -> "未知(" + status + ")";
        };
    }

    /** 序列化为 JSON；失败时退回 toString，保证工具不会因序列化问题整体失败。 */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("工具结果序列化失败，退回字符串形式: {}", ex.getMessage());
            return String.valueOf(value);
        }
    }
}
