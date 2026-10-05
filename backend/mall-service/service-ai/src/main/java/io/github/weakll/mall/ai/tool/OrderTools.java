package io.github.weakll.mall.ai.tool;

import io.github.weakll.mall.feign.order.OrderFeignClient;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.vo.common.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单查询工具。
 *
 * <p><b>用户数据隔离在本层不靠入参保证</b>：不提供 userId 参数，
 * 身份来自 {@code token} 请求头 → {@code UserTokenFeignInterceptor} 透传 →
 * 订单服务按当前登录用户过滤。因此模型无法通过构造参数越权查询他人订单，
 * 这比"传入 userId 再校验"更安全——没有可以传错的口子。
 */
@Component
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    private final OrderFeignClient orderFeignClient;

    private final ToolSupport support;

    public OrderTools(OrderFeignClient orderFeignClient, ToolSupport support) {
        this.orderFeignClient = orderFeignClient;
        this.support = support;
    }

    /**
     * 查询当前用户自己的订单。
     *
     * @param statusText 状态筛选，用中文表述（如"待付款"），为空表示全部
     * @param limit      返回条数上限
     */
    public ToolResult listMyOrders(String statusText, Integer limit) {
        Integer status = parseStatus(statusText);
        if (statusText != null && !statusText.isBlank() && status == null) {
            return ToolResult.fail("无法识别的订单状态「" + statusText
                    + "」，可用值为：待付款、待发货、已发货、已完成、已取消");
        }

        int size = support.normalizeLimit(limit);
        try {
            PageResult<OrderInfo> page = support.unwrap(
                    orderFeignClient.listMyOrders(1, size, status), "订单查询");
            List<OrderInfo> orders = page == null ? null : page.listOrEmpty();

            if (orders == null || orders.isEmpty()) {
                return ToolResult.ok(status == null
                        ? "该用户当前没有任何订单"
                        : "该用户没有「" + statusText + "」状态的订单");
            }

            List<Map<String, Object>> summaries = new ArrayList<>(orders.size());
            for (OrderInfo order : orders) {
                summaries.add(support.toOrderSummary(order));
            }
            return ToolResult.ok(support.toJson(summaries));
        } catch (Exception ex) {
            log.warn("订单查询失败 status={}: {}", statusText, ex.getMessage());
            return ToolResult.fail("订单服务暂时不可用，请稍后重试");
        }
    }

    /** 按订单号查询当前用户的某一笔订单。 */
    public ToolResult getMyOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return ToolResult.fail("请提供订单号");
        }
        try {
            OrderInfo order = support.unwrap(
                    orderFeignClient.getOrderInfoByOrderNo(orderNo.trim()), "订单详情");
            if (order == null) {
                // 服务端按当前用户过滤，查不到既可能是订单不存在，也可能不属于该用户
                return ToolResult.ok("未找到订单「" + orderNo.trim() + "」，请确认订单号是否正确");
            }
            return ToolResult.ok(support.toJson(support.toOrderSummary(order)));
        } catch (Exception ex) {
            log.warn("订单详情查询失败 orderNo={}: {}", orderNo, ex.getMessage());
            return ToolResult.fail("订单服务暂时不可用，请稍后重试");
        }
    }

    /** 把中文状态描述解析成后端状态码；无法识别时返回 null。 */
    static Integer parseStatus(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return null;
        }
        String value = statusText.trim();
        return switch (value) {
            case "待付款", "未付款", "0" -> 0;
            case "待发货", "1" -> 1;
            case "已发货", "待收货", "2" -> 2;
            case "已完成", "3" -> 3;
            case "已取消", "-1" -> -1;
            default -> null;
        };
    }

    /** 供提示词描述使用。 */
    public static String description() {
        return "查询当前登录用户自己的订单状态与明细。只能查到该用户本人的订单，"
                + "涉及其它用户的订单时应直接说明无权查询。";
    }
}
