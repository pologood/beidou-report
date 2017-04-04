package com.baidu.beidou.report.dao.vo;

import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.report.constant.QueryParameterConstant;

/**
 * <p>ClassName:FrontBackendStateMapping
 * <p>Function: 推广计划前后端状态映射，由前端列表中的状态映射成后端DB中的实际状态
 * 如“所有未删除”映射成："有效、已下线、暂停、未开始、已结束"，其中已下线为planstate=0 and budget=1
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * 
 * @see QueryParameterConstant.FrontViewState
 * @see CproPlanConstant#PLAN_STATE_*
 * @see CproPlanConstant#PLAN_BUDGETOVER
 * @created  2011-3-17
 */
public class FrontBackendPlanStateMapping extends FrontBackendStateMapping{

	/** 
	 * 对应的推广计划状态列表，不包括“已下线” 
	 * 说明：由于“已下线”不能通过planstate直接表示，所以需要单独出来表示
	 * */
	protected int[] planStateWithoutBudgetOver;
	/** 是否含有“已下线”状态，state + hasBudgetOver为对应的所有实际状态列表 */
	protected boolean hasBudgetOver;
	
	/** 是包括还是排除，true表示包括，false表示排除 */
	protected boolean includePlanState;
	
	
	/**
	 * @param planStates 计划状态
	 * @param hasBudgetOver 是否要在推广计划状态中加上已下线
	 * @param includePlanState 采用in 还是 not in的方式
	 */
	public FrontBackendPlanStateMapping(int[] planStates, boolean hasBudgetOver, boolean includePlanState) {
		this.planStateWithoutBudgetOver = planStates;
		this.hasBudgetOver = hasBudgetOver;
		this.includePlanState = includePlanState;
		
	}
	

	public int[] getPlanStateWithoutBudgetOver() {
		return planStateWithoutBudgetOver;
	}

	public void setPlanStateWithoutBudgetOver(int[] planStateWithoutBudgetOver) {
		this.planStateWithoutBudgetOver = planStateWithoutBudgetOver;
	}

	public boolean isHasBudgetOver() {
		return hasBudgetOver;
	}

	public void setHasBudgetOver(boolean hasBudgetOver) {
		this.hasBudgetOver = hasBudgetOver;
	}

	public boolean isIncludePlanState() {
		return includePlanState;
	}

	public void setIncludePlanState(boolean includePlanState) {
		this.includePlanState = includePlanState;
	}


	@Override
	public boolean needFilterByState() {
		return !ArrayUtils.isEmpty(planStateWithoutBudgetOver) || hasBudgetOver;
	}
	
}
