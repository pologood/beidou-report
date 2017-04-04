package com.baidu.beidou.report.vo.product;

import com.baidu.beidou.report.vo.StatInfo;

public class ProductViewItemSum extends StatInfo {

	private static final long serialVersionUID = -293301590337637843L;
	
	private Integer productCount;

	public Integer getProductCount() {
		return productCount;
	}

	public void setProductCount(Integer productCount) {
		this.productCount = productCount;
	}

	public ProductViewItemSum(){
	}

	public ProductViewItemSum(Integer productCount){
		this.productCount = productCount;
	}

}
