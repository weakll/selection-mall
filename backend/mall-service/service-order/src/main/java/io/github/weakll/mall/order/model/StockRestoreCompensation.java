package io.github.weakll.mall.order.model;

import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StockRestoreCompensation {
    private Long id;
    private String orderNo;
    private String stockItemsJson;
    private Integer status;
    private Integer retryCount;
    private String errorMessage;
    private Date nextRetryTime;
    private Date createTime;
    private Date updateTime;

    private List<SkuSaleDto> stockItems;
}
