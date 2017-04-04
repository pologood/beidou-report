package com.baidu.beidou.report.vo.product;

import com.baidu.beidou.report.vo.StatInfo;


public class ProductViewItem extends StatInfo {	
	
	private static final long serialVersionUID = 5413007885446491700L;

	/** 产品名*/
	private String productName;
	
	/**产品ID*/
	private Long productId;

	/** 计划ID */
	private Integer planId;
	
	/** 计划名 */
	private String planName;
	
	/**组ID*/
	private Integer groupId;
	
	/** 组名 */
	private String groupName;	
	

	/**---------------getters and setters----------------*/
	public Integer getPlanId() {
		return planId;
	}

	public void setPlanId(Integer planId) {
		this.planId = planId;
	}

	public String getPlanName() {
		return planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

}
