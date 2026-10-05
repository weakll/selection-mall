-- 售后政策知识库表（AI 客服检索用）
--
-- 为什么放在数据库而不是继续用 YAML：
--   检索评测需要一个可被 SQL 过滤与打分的索引载体。
--   用 MySQL 原生全文索引（ngram 分词器）做候选过滤，再在应用侧算精确 BM25。
--
-- 中文全文检索要点：
--   InnoDB 默认分词器按空格切词，中文整句会被当成一个词，检索不到。
--   必须显式指定 WITH PARSER ngram，它按 n 元组切分（ngram_token_size 默认 2）。
--
-- source_text 字段说明：
--   检索用的合并文本（标准问法 + 答复 + 关键词）。关键词是人工标注的口语同义词，
--   对召回价值最高，因此在应用侧建索引时会重复加权，这里只做原文留存。

CREATE TABLE IF NOT EXISTS `faq_knowledge` (
    `id` VARCHAR(64) NOT NULL COMMENT '稳定标识，评测标注集通过它标注正确答案',
    `category` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '类别，便于按类别统计召回',
    `question` VARCHAR(512) NOT NULL COMMENT '标准问法',
    `answer` TEXT NOT NULL COMMENT '标准答复',
    `keywords` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '口语化命中词，逗号分隔',
    `source_text` TEXT NOT NULL COMMENT '检索用合并文本（问法+答复+关键词）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FULLTEXT KEY `ft_faq_ngram` (`source_text`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='售后政策知识库，由 service-ai 启动时从 faq-knowledge.yml 同步';
