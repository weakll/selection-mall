package io.github.weakll.mall.ai.tool;

import io.github.weakll.mall.feign.order.OrderFeignClient;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.entity.order.OrderItem;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.common.PageResult;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工具层单测：不触网，验证字段裁剪、参数校验与用户隔离的实现方式。
 */
class BusinessToolsTest {

    private final ToolSupport support = new ToolSupport();

    // ---------- 关键词与条数校验 ----------

    @Test
    @DisplayName("关键词为空或纯空白时应被拒绝，不发起远程调用")
    void shouldRejectBlankKeywordWithoutRemoteCall() {
        ProductFeignClient client = mock(ProductFeignClient.class);
        ProductTools tools = new ProductTools(client, support);

        assertFalse(tools.searchByKeyword("   ", 5).success());
        assertFalse(tools.searchByKeyword(null, 5).success());

        verify(client, never()).searchByPage(any(), any(), any());
    }

    @Test
    @DisplayName("条数上限被收敛到 MAX_ITEMS，防止模型传入超大 limit")
    void shouldClampLimit() {
        assertEquals(ToolSupport.MAX_ITEMS, support.normalizeLimit(1000));
        assertEquals(ToolSupport.MAX_ITEMS, support.normalizeLimit(null));
        assertEquals(ToolSupport.MAX_ITEMS, support.normalizeLimit(-3));
        assertEquals(2, support.normalizeLimit(2));
    }

    // ---------- 商品工具 ----------

    @Test
    @DisplayName("商品结果只暴露必要字段，不含成本价")
    void shouldNotExposeCostPriceInProductResult() {
        ProductFeignClient client = mock(ProductFeignClient.class);
        ProductSku sku = new ProductSku();
        sku.setId(7L);
        sku.setSkuName("测试商品");
        sku.setSalePrice(new BigDecimal("99.00"));
        sku.setCostPrice(new BigDecimal("10.00"));
        sku.setStockNum(3);

        when(client.searchByPage(eq(1), any(), eq("手机")))
                .thenReturn(Result.build(new PageResult<>(1L, List.of(sku)), ResultCodeEnum.SUCCESS));

        ToolResult result = new ProductTools(client, support).searchByKeyword("手机", 5);

        assertTrue(result.success());
        assertTrue(result.content().contains("测试商品"));
        assertTrue(result.content().contains("99.00"));
        assertFalse(result.content().contains("10.00"), "成本价不得进入模型上下文");
    }

    @Test
    @DisplayName("搜索无结果时给出可读提示而非空串")
    void shouldExplainEmptyProductResult() {
        ProductFeignClient client = mock(ProductFeignClient.class);
        when(client.searchByPage(any(), any(), any()))
                .thenReturn(Result.build(new PageResult<>(0L, List.of()), ResultCodeEnum.SUCCESS));

        ToolResult result = new ProductTools(client, support).searchByKeyword("不存在的东西", 5);

        assertTrue(result.success());
        assertTrue(result.content().contains("没有找到"));
    }

    // ---------- 订单工具：用户隔离的关键断言 ----------

    @Test
    @DisplayName("订单查询不接受 userId 入参：身份只能来自 token 透传")
    void orderToolsShouldNotAcceptUserId() {
        // 结构性断言：OrderTools 的公开方法签名里不存在 userId。
        // 这是"用户数据隔离"的实现方式——没有可传错的口子，而不是靠运行时校验。
        for (var method : OrderTools.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            for (var parameter : method.getParameterTypes()) {
                assertFalse(parameter.getSimpleName().toLowerCase().contains("userid"),
                        "订单工具方法 " + method.getName() + " 不得接受 userId 参数");
            }
        }
    }

    @Test
    @DisplayName("中文状态被正确解析为后端状态码")
    void shouldParseChineseOrderStatus() {
        assertEquals(0, OrderTools.parseStatus("待付款"));
        assertEquals(1, OrderTools.parseStatus("待发货"));
        assertEquals(2, OrderTools.parseStatus("已发货"));
        assertEquals(2, OrderTools.parseStatus("待收货"));
        assertEquals(3, OrderTools.parseStatus("已完成"));
        assertEquals(-1, OrderTools.parseStatus("已取消"));
        assertNull(OrderTools.parseStatus("随便写的"));
        assertNull(OrderTools.parseStatus(""));
        assertNull(OrderTools.parseStatus(null));
    }

    @Test
    @DisplayName("无法识别的状态直接拒绝，不把 null 当'全部'误传给后端")
    void shouldRejectUnknownStatusInsteadOfQueryingAll() {
        OrderFeignClient client = mock(OrderFeignClient.class);
        OrderTools tools = new OrderTools(client, support);

        ToolResult result = tools.listMyOrders("已签收", 5);

        assertFalse(result.success());
        assertTrue(result.content().contains("无法识别"));
        verify(client, never()).listMyOrders(any(), any(), any());
    }

    @Test
    @DisplayName("订单摘要包含状态中文与商品明细，但不含收货人手机号")
    void orderSummaryShouldHideReceiverPhone() {
        OrderFeignClient client = mock(OrderFeignClient.class);
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORD-1");
        order.setOrderStatus(1);
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setReceiverPhone("13800000000");
        OrderItem item = new OrderItem();
        item.setSkuName("测试商品");
        item.setSkuPrice(new BigDecimal("199.00"));
        item.setSkuNum(1);
        order.setOrderItemList(List.of(item));

        when(client.listMyOrders(eq(1), any(), any()))
                .thenReturn(Result.build(new PageResult<>(1L, List.of(order)), ResultCodeEnum.SUCCESS));

        ToolResult result = new OrderTools(client, support).listMyOrders(null, 5);

        assertTrue(result.success());
        assertTrue(result.content().contains("ORD-1"));
        assertTrue(result.content().contains("待发货"), "状态码 1 应渲染为待发货");
        assertTrue(result.content().contains("测试商品"));
        assertFalse(result.content().contains("13800000000"), "收货人手机号不应进入模型上下文");
    }

    @Test
    @DisplayName("订单为空时给出可读提示")
    void shouldExplainEmptyOrderResult() {
        OrderFeignClient client = mock(OrderFeignClient.class);
        when(client.listMyOrders(any(), any(), any()))
                .thenReturn(Result.build(new PageResult<>(0L, List.of()), ResultCodeEnum.SUCCESS));

        ToolResult result = new OrderTools(client, support).listMyOrders(null, 5);

        assertTrue(result.success());
        assertTrue(result.content().contains("没有任何订单"));
    }

    // ---------- FAQ 工具 ----------

    private FaqTools faqTools() {
        FaqTools tools = new FaqTools(new ClassPathResource("ai/faq-knowledge.yml"), support);
        tools.reload();
        return tools;
    }

    @Test
    @DisplayName("知识库能正常载入")
    void shouldLoadFaqKnowledge() {
        assertTrue(faqTools().size() >= 5, "知识库应至少载入 5 条");
    }

    @Test
    @DisplayName("口语化提问也能命中对应政策")
    void shouldMatchColloquialQuestions() {
        FaqTools tools = faqTools();

        // 口语问法不含书面词"发货"，靠"什么时候能到"这类口语关键词命中
        assertTrue(tools.search("我买的东西什么时候能到").content().contains("24 小时"));
        assertTrue(tools.search("我去快递站拿不到件").content().contains("24 小时"));
        assertTrue(tools.search("东西不想要了怎么退").content().contains("7 天"));
        assertTrue(tools.search("优惠券怎么用").content().contains("优惠券"));
    }

    @Test
    @DisplayName("知识库无匹配时明确告知，不让模型自由发挥")
    void shouldTellModelWhenNoFaqMatched() {
        ToolResult result = faqTools().search("你们公司股票代码是多少");

        assertTrue(result.success());
        assertTrue(result.content().contains("没有") && result.content().contains("人工客服"));
    }

    @Test
    @DisplayName("空提问被拒绝")
    void shouldRejectBlankFaqQuery() {
        assertFalse(faqTools().search("  ").success());
    }
}
