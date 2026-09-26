package io.github.weakll.mall.model.entity.product;

import io.github.weakll.mall.model.entity.base.BaseEntity;
import lombok.Data;

@Data
public class ProductDetails extends BaseEntity {

	private Long productId;
	private String imageUrls;

}
