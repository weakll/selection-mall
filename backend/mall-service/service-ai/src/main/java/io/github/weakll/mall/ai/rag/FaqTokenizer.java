package io.github.weakll.mall.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检索用分词器。
 *
 * <p>中文没有词边界，且不能引入分词词典依赖（会带来词典体积与版本维护成本），
 * 因此采用 n 元组切分。这里有一段实测得出的取舍：
 *
 * <p><b>中文只切 2-gram，不切 1-gram。</b> 1-gram 会引入大量噪音——
 * "你们公司地址在哪里"里的"在""哪""里"这类高频单字能与任何文档配上，
 * 使得知识库外的越界问题也拿到高分（实测越界最高分 22.41，
 * 甚至高于部分有效查询），导致"查不到"的判断失去依据。
 * 改为只切 2-gram 后，越界最高分降到 11.18，判别力明显改善。
 *
 * <p>英文与数字整体保留（如 {@code app}、{@code 7}），因为它们是完整语义单位，
 * 切开反而破坏含义。
 */
public final class FaqTokenizer {

    /** 中文 n 元组的 n。取 2 是"有搭配语义"与"不过度稀疏"之间的折中。 */
    private static final int GRAM = 2;

    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9]+|[\\u4e00-\\u9fff]+");

    private FaqTokenizer() {
    }

    /**
     * 切分为检索词。
     *
     * @param text 原文，可为 null
     * @return 词列表；保持出现顺序且不去重（词频由调用方统计）
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        Matcher matcher = SEGMENT.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String segment = matcher.group();
            if (Character.isDigit(segment.charAt(0)) || isAscii(segment)) {
                tokens.add(segment);
                continue;
            }
            // 中文字符串：按 GRAM 切滑动窗口
            int length = segment.length();
            if (length < GRAM) {
                tokens.add(segment);
                continue;
            }
            for (int i = 0; i + GRAM <= length; i++) {
                tokens.add(segment.substring(i, i + GRAM));
            }
        }
        return tokens;
    }

    /** 去重后的检索词，用于查询侧（同一查询词重复出现不增加权重）。 */
    public static List<String> distinctTokens(String text) {
        return tokenize(text).stream().distinct().toList();
    }

    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }
}
