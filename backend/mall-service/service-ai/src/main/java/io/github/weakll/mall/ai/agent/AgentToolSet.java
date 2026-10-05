package io.github.weakll.mall.ai.agent;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.github.weakll.mall.ai.auth.ToolAuthContext;
import io.github.weakll.mall.ai.tool.FaqTools;
import io.github.weakll.mall.ai.tool.OrderTools;
import io.github.weakll.mall.ai.tool.ProductTools;
import io.github.weakll.mall.ai.tool.ToolResult;

/**
 * 暴露给模型的工具集合。
 *
 * <p>这一层只做"业务工具 → 模型可调用工具"的适配：方法签名与文档即模型看到的工具契约。
 * 业务逻辑仍在 {@link ProductTools} / {@link OrderTools} / {@link FaqTools} 中，
 * 因此这些工具可脱离 Agent 框架单独单测。
 *
 * <p>顺序参数一律用包装类型并标注 required=false：模型可能省略可选参数，
 * 用 int 会因反序列化 null 失败。
 *
 * <p>刻意不加 {@code @Component}：本类持有会话凭据，由
 * {@link ReActChatClient} 按请求现场构造，避免单例化导致凭据串号。
 */
public class AgentToolSet {

    private final ProductTools productTools;

    private final OrderTools orderTools;

    private final FaqTools faqTools;

    /**
     * 本次会话的登录凭据。
     *
     * <p>必须由调用方从 servlet 线程传入并在此前设置：工具执行发生在 Reactor
     * 调度器线程上，那里没有请求上下文，任何"执行时再去解析"的做法都会得到 null。
     *
     * <p>因此本对象<b>不是并发安全的</b>：一次请求对应一个实例。
     * 若将来要支持并发会话，须改为每次调用传入，而不是持有为字段。
     */
    private final String token;

    public AgentToolSet(ProductTools productTools, OrderTools orderTools, FaqTools faqTools,
                        String token) {
        this.productTools = productTools;
        this.orderTools = orderTools;
        this.faqTools = faqTools;
        this.token = token;
    }

    @Tool(name = "queryProduct",
            description = "搜索商城在售商品，返回商品名称、价格、库存与规格。"
                    + "回答价格、库存、有没有某类商品等问题时调用。")
    public String queryProduct(
            @ToolParam(name = "keyword", description = "商品名称关键词，例如「手机」「连衣裙」", required = true)
            String keyword,
            @ToolParam(name = "limit", description = "返回条数，最多 5 条", required = false)
            Integer limit) {
        return render(productTools.searchByKeyword(keyword, limit));
    }

    @Tool(name = "queryMyOrders",
            description = "查询当前登录用户自己的订单，返回订单号、状态、金额与商品明细。"
                    + "只能查到该用户本人的订单，无法查询他人的订单。")
    public String queryMyOrders(
            @ToolParam(name = "status", description = "订单状态筛选：待付款、待发货、已发货、已完成、已取消；不填表示全部",
                    required = false)
            String status,
            @ToolParam(name = "limit", description = "返回条数，最多 5 条", required = false)
            Integer limit) {
        // 订单查询需要带登录凭据调下游服务；工具执行在 Reactor 调度器线程上，
        // 没有 servlet 请求上下文，因此必须在此显式绑定 token 供 Feign 拦截器读取。
        return ToolAuthContext.callWith(
                token, () -> render(orderTools.listMyOrders(status, limit)));
    }

    @Tool(name = "searchFaq",
            description = "查询售后政策知识库，涵盖退换货、发货时效、优惠券、发票、支付方式、订单取消等。"
                    + "回答政策类问题必须依据本工具返回的内容，不得编造时限或金额。")
    public String searchFaq(
            @ToolParam(name = "query", description = "用户的问题原文", required = true)
            String query) {
        return render(faqTools.search(query));
    }

    /**
     * 统一工具返回格式。
     *
     * <p>失败也返回正常文本而非抛异常：抛异常会让 ReAct 循环中断，
     * 而"服务暂时不可用"这类信息让模型转述给用户，体验更好。
     */
    private static String render(ToolResult result) {
        if (result.success()) {
            return result.content();
        }
        return "工具调用失败：" + result.content();
    }
}
