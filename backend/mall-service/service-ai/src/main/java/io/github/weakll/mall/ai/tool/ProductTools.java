package io.github.weakll.mall.ai.tool;

import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.common.PageResult;
import io.github.weakll.mall.model.vo.h5.ProductItemVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品查询工具。
 *
 * <p>供 AI 客服回答"有没有 XX 商品""XX 多少钱""还有货吗"。
 * 价格与库存一律来自商品服务实时数据，模型不得编造。
 */
@Component
public class ProductTools {

    private static final Logger log = LoggerFactory.getLogger(ProductTools.class);

    private final ProductFeignClient productFeignClient;

    private final ToolSupport support;

    public ProductTools(ProductFeignClient productFeignClient, ToolSupport support) {
        this.productFeignClient = productFeignClient;
        this.support = support;
    }

    /**
     * 按关键词搜索在售商品。
     *
     * @param keyword 商品名称关键词，如"手机""连衣裙"
     * @param limit   返回条数上限，实际会被收敛到 {@link ToolSupport#MAX_ITEMS}
     */
    public ToolResult searchByKeyword(String keyword, Integer limit) {
        String normalized = support.normalizeKeyword(keyword);
        if (normalized == null) {
            return ToolResult.fail("请提供有效的商品关键词");
        }

        int size = support.normalizeLimit(limit);
        try {
            PageResult<ProductSku> page = support.unwrap(
                    productFeignClient.searchByPage(1, size, normalized), "商品搜索");
            List<ProductSku> products = page == null ? null : page.listOrEmpty();

            if (products == null || products.isEmpty()) {
                return ToolResult.ok("没有找到与「" + normalized + "」相关的在售商品");
            }

            List<Map<String, Object>> summaries = new ArrayList<>(products.size());
            for (ProductSku sku : products) {
                summaries.add(support.toProductSummary(sku));
            }
            return ToolResult.ok(support.toJson(summaries));
        } catch (Exception ex) {
            log.warn("商品搜索失败 keyword={}: {}", normalized, ex.getMessage());
            return ToolResult.fail("商品服务暂时不可用，请稍后重试");
        }
    }

    /** 按 SKU ID 查询商品详情。 */
    public ToolResult getProductDetail(Long skuId) {
        if (skuId == null || skuId <= 0) {
            return ToolResult.fail("请提供有效的商品 ID");
        }
        try {
            ProductItemVo item = support.unwrap(productFeignClient.getItem(skuId), "商品详情");
            if (item == null) {
                return ToolResult.fail("未找到该商品（ID " + skuId + "）");
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            if (item.getProductSku() != null) {
                detail.putAll(support.toProductSummary(item.getProductSku()));
            }
            if (item.getProduct() != null) {
                // 用可读名称而非 ID：模型直接转述给用户，不必再做一次映射
                detail.put("productName", item.getProduct().getName());
                detail.put("brand", item.getProduct().getBrandName());
                detail.put("unit", item.getProduct().getUnitName());
            }
            // 规格值只给可选集合，不把整张属性表塞进上下文
            if (item.getSkuSpecValueMap() != null && !item.getSkuSpecValueMap().isEmpty()) {
                detail.put("availableSpecs", item.getSkuSpecValueMap().keySet());
            }
            return ToolResult.ok(support.toJson(detail));
        } catch (Exception ex) {
            log.warn("商品详情查询失败 skuId={}: {}", skuId, ex.getMessage());
            return ToolResult.fail("商品服务暂时不可用，请稍后重试");
        }
    }

    /** 供提示词描述使用：说明该工具能做什么。 */
    public static String description() {
        return "查询商城在售商品的价格、库存与规格。回答商品相关问题前必须先调用本工具获取真实数据。";
    }
}
