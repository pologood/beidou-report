package com.baidu.beidou.report.dao.vo;


/**
 * <p>ClassName:QueryParameter
 * <p>Function: Plan的SQL查询参数
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-2-26
 * @since    星计划(beidou 2.0)
 */
public class PlanQueryParameter extends SqlQueryParameter {
	/** 是否含有“已下线”状态 */
	protected boolean budgetOver;
	/**
	 * 计划属性，0-所有功能，1-仅无线
	 */
	protected Integer promotionType;

	public Integer getPromotionType() {
		return promotionType;
	}

	public void setPromotionType(Integer promotionType) {
		this.promotionType = promotionType;
	}

	public boolean isBudgetOver() {
		return budgetOver;
	}

	public void setBudgetOver(boolean budgetOver) {
		this.budgetOver = budgetOver;
	}
}
