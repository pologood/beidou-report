package com.baidu.beidou.report.dao.vo;


/**
 * <p>ClassName:QueryParameter
 * <p>Function: Group的SQL查询参数
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-2-26
 * @since    星计划(beidou 2.0)
 */
public class GroupQueryParameter extends PlanQueryParameter {
	/** 展现类型：1固定，2悬浮，4贴片 */
	protected Integer[] groupType;
	
	/** 指定的推广计划 */
	protected Integer planId;
	/** 推广计划状态 */
	protected int[] planState ;
	protected boolean includePlanState ;
	
	/** 定向方式：0－KT，1－RT，2－QT，3—PT，4-VT*/
	protected Integer[] targetType;

	public Integer[] getGroupType() {
		return groupType;
	}

	public void setGroupType(Integer[] groupType) {
		this.groupType = groupType;
	}

	public Integer getPlanId() {
		return planId;
	}

	public void setPlanId(Integer planId) {
		this.planId = planId;
	}

	public int[] getPlanState() {
		return planState;
	}

	public void setPlanState(int[] planState) {
		this.planState = planState;
	}

	public boolean isIncludePlanState() {
		return includePlanState;
	}

	public void setIncludePlanState(boolean includePlanState) {
		this.includePlanState = includePlanState;
	}

	public Integer[] getTargetType() {
		return targetType;
	}

	public void setTargetType(Integer[] targetType) {
		this.targetType = targetType;
	}

}
