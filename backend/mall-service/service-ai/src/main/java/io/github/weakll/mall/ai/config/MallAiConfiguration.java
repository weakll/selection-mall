package io.github.weakll.mall.ai.config;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.compat.deepseek.DeepSeekFormatter;
import io.github.weakll.mall.ai.agent.ReActChatClient;
import io.github.weakll.mall.ai.model.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 客服装配。
 *
 * <p>当 {@code mall.ai.enabled=false} 时整个装配不生效，服务仍可启动，
 * 便于在只排查基础设施时隔离 AI 相关故障。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MallAiProperties.class)
@ConditionalOnProperty(prefix = "mall.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MallAiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MallAiConfiguration.class);

    /**
     * 对话模型。DeepSeek 走 OpenAI 兼容端点，复用 AgentScope 的 OpenAI 实现与 DeepSeek 适配器。
     *
     * <p>密钥缺失时返回 null 而非抛异常：服务照常启动，对话接口给出可读提示。
     * 否则未配密钥的环境连健康检查都调不通，排查成本很高。
     */
    @Bean
    @ConditionalOnMissingBean(Model.class)
    public Model chatModel(MallAiProperties properties) {
        MallAiProperties.Model config = properties.getModel();
        if (!config.hasApiKey()) {
            log.warn("未配置 mall.ai.model.api-key，AI 客服将不可用；请设置环境变量 DEEPSEEK_API_KEY");
            return null;
        }
        return OpenAIChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .baseUrl(config.getBaseUrl())
                .stream(true)
                .formatter(new DeepSeekFormatter())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    public ChatClient chatClient(MallAiProperties properties, Model chatModel,
                                io.github.weakll.mall.ai.tool.ProductTools productTools,
                                io.github.weakll.mall.ai.tool.OrderTools orderTools,
                                io.github.weakll.mall.ai.tool.FaqTools faqTools) {
        return new ReActChatClient(properties, chatModel, productTools, orderTools, faqTools);
    }
}
