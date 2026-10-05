package io.github.weakll.mall.ai.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.github.weakll.mall.ai.config.MallAiProperties;
import io.github.weakll.mall.ai.model.ChatChunk;
import io.github.weakll.mall.ai.model.ChatClient;
import io.github.weakll.mall.ai.model.ChatRequest;
import io.github.weakll.mall.ai.tool.FaqTools;
import io.github.weakll.mall.ai.tool.OrderTools;
import io.github.weakll.mall.ai.tool.ProductTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 带工具调用能力的对话客户端。
 *
 * <p>与直接调用模型的原实现的关键差别：这里把 {@link AgentToolSet} 注册进
 * AgentScope 的 {@link Toolkit}，由 {@link ReActAgent} 驱动"思考 → 调工具 → 依据结果作答"
 * 的循环。没有这一步，模型只能凭空作答——实测中它会回答"我来帮您查询一下订单，请稍等"
 * 却并不真的去查，因为它根本看不到工具。
 *
 * <p>每次请求新建 Agent 实例：{@code ReActAgent} 持有内部状态，
 * 复用同一实例会让不同用户的会话互相污染。当前阶段不引入服务端记忆，
 * 上下文由请求携带的历史决定，因此新建实例的代价是可接受的。
 */
public class ReActChatClient implements ChatClient {

    private static final Logger log = LoggerFactory.getLogger(ReActChatClient.class);

    /** 工具开始执行时推给用户的提示文案，避免等待期界面无反馈。 */
    private static final Map<String, String> TOOL_PROGRESS_HINTS = Map.of(
            "queryProduct", "（正在查询商品…）",
            "queryMyOrders", "（正在查询您的订单…）",
            "searchFaq", "（正在查询售后政策…）");

    private final MallAiProperties properties;

    private final Model model;

    private final ProductTools productTools;

    private final OrderTools orderTools;

    private final FaqTools faqTools;

    public ReActChatClient(MallAiProperties properties, Model model,
                           ProductTools productTools, OrderTools orderTools, FaqTools faqTools) {
        this.properties = properties;
        this.model = model;
        this.productTools = productTools;
        this.orderTools = orderTools;
        this.faqTools = faqTools;
    }

    @Override
    public Flux<ChatChunk> stream(ChatRequest request, String token) {
        if (model == null) {
            return Flux.just(ChatChunk.error(
                    "AI 客服未配置模型密钥，请联系管理员设置 DEEPSEEK_API_KEY"));
        }

        String systemPrompt = request.systemPrompt() != null && !request.systemPrompt().isBlank()
                ? request.systemPrompt()
                : properties.getAgent().getSystemPrompt();

        ReActAgent agent;
        try {
            // 每次请求现场构造工具集：AgentToolSet 持有本次会话的登录凭据，
            // 做成单例会让并发请求互相覆盖，做成作用域代理又有干扰 @Tool
            // 注解扫描的风险（工具靠反射注册）。现场构造最简单也最可靠。
            AgentToolSet toolSet = new AgentToolSet(productTools, orderTools, faqTools, token);

            Toolkit toolkit = new Toolkit();
            // registerTool 会扫描对象上的 @Tool 方法并生成 schema
            toolkit.registerTool(toolSet);

            agent = ReActAgent.builder()
                    .name("mall-customer-service")
                    .description("精选商城在线客服")
                    .sysPrompt(systemPrompt)
                    .model(model)
                    .toolkit(toolkit)
                    .maxIters(properties.getAgent().getMaxIters())
                    .build();
        } catch (Exception ex) {
            log.error("构建 Agent 失败: {}", ex.getMessage());
            return Flux.just(ChatChunk.error("AI 服务初始化失败，请稍后重试"));
        }

        List<Msg> messages = toMessages(request);

        return agent.streamEvents(messages)
                .concatMap(event -> {
                    List<ChatChunk> chunks = toChunks(event);
                    return chunks.isEmpty() ? Flux.empty() : Flux.fromIterable(chunks);
                })
                .concatWith(Flux.just(ChatChunk.done("stop")))
                .onErrorResume(ex -> {
                    log.warn("Agent 执行失败: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
                    return Flux.just(ChatChunk.error("AI 服务暂时不可用，请稍后重试"));
                });
    }

    /**
     * 事件到分片的映射。
     *
     * <p>只透出两类信息：正文增量（给用户看答案）与工具调用提示（让用户知道在查什么）。
     * 思考内容、工具入参与原始返回值都不外泄——工具返回值可能含内部字段。
     */
    private static List<ChatChunk> toChunks(AgentEvent event) {
        if (event instanceof TextBlockDeltaEvent delta) {
            String text = delta.getDelta();
            return text == null || text.isEmpty() ? List.of() : List.of(ChatChunk.delta(text));
        }
        if (event instanceof ToolCallStartEvent start) {
            String hint = TOOL_PROGRESS_HINTS.get(start.getToolCallName());
            // 未知工具不提示，避免把内部工具名暴露给用户
            return hint == null ? List.of() : List.of(ChatChunk.delta("\n" + hint + "\n"));
        }
        return List.of();
    }

    private static List<Msg> toMessages(ChatRequest request) {
        List<Msg> messages = new ArrayList<>();
        for (ChatRequest.Message message : request.messages()) {
            MsgRole role = message.role() == ChatRequest.Role.USER ? MsgRole.USER : MsgRole.ASSISTANT;
            messages.add(Msg.builder().role(role).textContent(message.content()).build());
        }
        return messages;
    }

    @Override
    public String describe() {
        return "AgentScope/ReActAgent provider=" + properties.getModel().getProvider()
                + " model=" + properties.getModel().getModel()
                + " tools=" + TOOL_PROGRESS_HINTS.keySet()
                + " maxIters=" + properties.getAgent().getMaxIters()
                + " apiKeyConfigured=" + properties.getModel().hasApiKey();
    }
}
