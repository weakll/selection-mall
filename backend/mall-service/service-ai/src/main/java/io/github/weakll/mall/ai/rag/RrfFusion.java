package io.github.weakll.mall.ai.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（Reciprocal Rank Fusion）多路召回融合。
 *
 * <p>公式：{@code score(d) = Σ_r 1 / (k + rank_r(d))}，{@code rank} 从 1 开始；
 * 未出现在某一路结果里的文档该路不贡献分数。
 *
 * <p><b>为什么用 RRF 而不是加权求和：</b>不同召回通道的分数量纲不可比——
 * 词表匹配命中即 1 分，BM25 的分数取决于词频与文档长度（本次实测跨度 0~100+）。
 * 直接加权求和需要先做分数归一化，而归一化方式本身又是一个要调的超参。
 * RRF 只用**排名**，天然免疫量纲问题，且对单路返回条数不敏感——这是它成为业界默认融合方式的原因。
 *
 * <p>{@code k} 默认 60（原论文取值）。它的作用是压低头部排名的绝对优势：
 * k 越大，各路第一名之间的差距越小，融合越"民主"。本次未做 k 的敏感度实验，
 * 沿用论文默认值，避免在样本量只有 120 条的情况下过拟合。
 */
public final class RrfFusion {

    /** 原论文默认值。 */
    public static final int DEFAULT_K = 60;

    private RrfFusion() {
    }

    /**
     * 融合多路结果。
     *
     * @param rankedLists 各路召回的文档 id，已按各自相关性降序排列
     * @param topK        返回条数上限
     * @return 融合后的文档 id，按融合分数降序
     */
    public static List<String> fuse(List<List<String>> rankedLists, int topK) {
        return fuse(rankedLists, topK, DEFAULT_K);
    }

    /** 可指定 k 的融合，供评测对比。 */
    public static List<String> fuse(List<List<String>> rankedLists, int topK, int k) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<String> list : rankedLists) {
            if (list == null) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                String id = list.get(i);
                if (id == null) {
                    continue;
                }
                // rank 从 1 开始，因此 i+1
                scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            }
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(scores.entrySet());
        // 分数相同时按 id 排序，保证结果稳定可复现（否则同一输入可能给出不同顺序，评测不可重复）
        entries.sort(Comparator
                .comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed()
                .thenComparing(Map.Entry::getKey));

        List<String> result = new ArrayList<>(Math.min(topK, entries.size()));
        for (int i = 0; i < entries.size() && i < topK; i++) {
            result.add(entries.get(i).getKey());
        }
        return result;
    }
}
