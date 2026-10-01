package io.github.weakll.mall.product.service.impl;

import com.alibaba.fastjson.JSON;

import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.common.cache.CacheConstants;
import io.github.weakll.mall.model.dto.h5.ProductSkuDto;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.model.entity.product.Product;
import io.github.weakll.mall.model.entity.product.ProductDetails;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.h5.ProductItemVo;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.product.mapper.ProductDetailsMapper;
import io.github.weakll.mall.product.mapper.ProductMapper;
import io.github.weakll.mall.product.mapper.ProductSkuMapper;
import io.github.weakll.mall.product.service.ProductService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductDetailsMapper productDetailsMapper;
    @Autowired
    private io.github.weakll.mall.product.cache.CatalogCache catalogCache;
    @Override
    public PageInfo<ProductSku> findByPage(Integer page, Integer limit, ProductSkuDto productSkuDto) {
        PageHelper.startPage(page, limit);
        List<ProductSku> productSkuList = productSkuMapper.findByPage(productSkuDto);
        return new PageInfo<>(productSkuList);
    }

    @Override
    public ProductSku getBySkuId(Long skuId) {
        return productSkuMapper.getById(skuId);
    }

    @Transactional
    @Override
    public Boolean updateSkuSaleNum(List<SkuSaleDto> skuSaleDtoList) {
        if(!CollectionUtils.isEmpty(skuSaleDtoList)) {
            for(SkuSaleDto skuSaleDto : skuSaleDtoList) {
                productSkuMapper.updateSale(skuSaleDto.getSkuId(), skuSaleDto.getNum());
            }
        }
        catalogCache.evictAll(CacheConstants.PRODUCT_ITEM);
        return true;
    }

    @Transactional
    @Override
    public Boolean deductStock(List<SkuSaleDto> skuSaleDtoList) {
        if (!CollectionUtils.isEmpty(skuSaleDtoList)) {
            for (SkuSaleDto skuSaleDto : skuSaleDtoList) {
                validateSkuSale(skuSaleDto);
                int affectedRows = productSkuMapper.deductStock(
                        skuSaleDto.getSkuId(),
                        skuSaleDto.getNum()
                );
                if (affectedRows != 1) {
                    throw new MallException(ResultCodeEnum.STOCK_LESS);
                }
            }
        }
        catalogCache.evictAll(CacheConstants.PRODUCT_ITEM);
        return true;
    }

    @Transactional
    @Override
    public Boolean restoreStock(List<SkuSaleDto> skuSaleDtoList) {
        if (!CollectionUtils.isEmpty(skuSaleDtoList)) {
            for (SkuSaleDto skuSaleDto : skuSaleDtoList) {
                validateSkuSale(skuSaleDto);
                int affectedRows = productSkuMapper.restoreStock(
                        skuSaleDto.getSkuId(),
                        skuSaleDto.getNum()
                );
                if (affectedRows != 1) {
                    throw new MallException(ResultCodeEnum.DATA_ERROR);
                }
            }
        }
        catalogCache.evictAll(CacheConstants.PRODUCT_ITEM);
        return true;
    }

    @Override
    public List<ProductSku> findProductSkuSale() {
        return productSkuMapper.findProductSkuSale();
    }

    @Override
    public ProductItemVo item(Long skuId) {
        return catalogCache.getOrLoad(
                CacheConstants.PRODUCT_ITEM,
                String.valueOf(skuId),
                Duration.ofMinutes(30),
                ProductItemVo.class,
                () -> loadItem(skuId)
        );
    }

    private ProductItemVo loadItem(Long skuId) {
        //当前sku信息
        ProductSku productSku = productSkuMapper.getById(skuId);
        if (productSku == null) {
            return null;
        }

        //当前商品信息
        Product product = productMapper.getById(productSku.getProductId());
        if (product == null) {
            return null;
        }

        //同一个商品下面的sku信息列表
        List<ProductSku> productSkuList = productSkuMapper.findByProductId(productSku.getProductId());
        //建立sku规格与skuId对应关系
        Map<String,Object> skuSpecValueMap = new HashMap<>();
        productSkuList.forEach(item -> {
            skuSpecValueMap.put(item.getSkuSpec(), item.getId());
        });

        //商品详情信息
        ProductDetails productDetails = productDetailsMapper.getByProductId(productSku.getProductId());
        if (productDetails == null) {
            return null;
        }

        ProductItemVo productItemVo = new ProductItemVo();
        productItemVo.setProductSku(productSku);
        productItemVo.setProduct(product);
        productItemVo.setDetailsImageUrlList(Arrays.asList(productDetails.getImageUrls().split(",")));
        productItemVo.setSliderUrlList(Arrays.asList(product.getSliderUrls().split(",")));
        productItemVo.setSpecValueList(JSON.parseArray(product.getSpecValue()));
        productItemVo.setSkuSpecValueMap(skuSpecValueMap);
        return productItemVo;
    }

    private void validateSkuSale(SkuSaleDto skuSaleDto) {
        if (skuSaleDto == null
                || skuSaleDto.getSkuId() == null
                || skuSaleDto.getNum() == null
                || skuSaleDto.getNum() <= 0) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }
    }

}
