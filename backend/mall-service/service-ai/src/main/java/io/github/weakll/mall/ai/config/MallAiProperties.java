package io.github.weakll.mall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 客服配置。
 *
 * <p>对应 {@code mall.ai.*}。api-key 只从环境变量注入，不落配置文件，
 * 也不参与日志输出（见 {@link #toString()} 的刻意省略）。
 */
@Data
@ConfigurationProperties(prefix = "mall.ai")
public class MallAiProperties {

    /** 总开关。关掉时服务仍可启动，便于排查基础设施问题。 */
    private boolean enabled = true;

    private Model model = new Model();

    private Agent agent = new Agent();

    @Data
    public static class Model {

        /** 供应商标识，当前仅接 OpenAI 兼容端点的 DeepSeek。 */
        private String provider = "deepseek";

        /** OpenAI 兼容基址，DeepSeek 为 https://api.deepseek.com。 */
        private String baseUrl = "https://api.deepseek.com";

        /** 密钥，只从环境变量读。 */
        private String apiKey = "";

        /** 模型名，deepseek-chat 对应对话模型。 */
        private String model = "deepseek-chat";

        private int connectTimeoutMs = 10_000;

        private int readTimeoutMs = 120_000;

        /** 是否已配置可用密钥。缺 key 时不发起调用，直接给出可读错误。 */
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    @Data
    public static class Agent {

        /** ReAct 循环上限，超出后强制收尾，避免工具调用死循环。 */
        private int maxIters = 6;

        private String systemPrompt = "";
    }
}
