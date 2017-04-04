package com.baidu.beidou.report.dao.vo;

import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.report.constant.QueryParameterConstant;

/**
 * <p>ClassName:FrontBackendGroupStateMapping
 * <p>Function: 推广组前后端状态映射，由前端列表中的状态映射成后端DB中的实际状态
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * 
 * @see QueryParameterConstant.FrontViewState
 * @see CproGroupConstant#GROUP_STATE_*
 * @created  2011-3-17
 */
public class FrontBackendGroupStateMapping extends FrontBackendPlanStateMapping {

	/** 
	 * 对应的推广组状态列表
	 * */
	protected int[] groupState;
	
	/** 是包括还是排除，true表示包括，false表示排除 */
	protected boolean includeGroupState;
	
	public FrontBackendGroupStateMapping(int[] planStates, boolean hasBudgetOver, boolean includePlanState,
			int[] groupState, boolean includeGroupState) {
		super(planStates, hasBudgetOver, includePlanState);
		this.groupState = groupState;
		this.includeGroupState = includeGroupState;
		
	}

	public int[] getGroupState() {
		return groupState;
	}

	public void setGroupState(int[] groupState) {
		this.groupState = groupState;
	}

	public boolean isIncludeGroupState() {
		return includeGroupState;
	}

	public void setIncludeGroupState(boolean includeGroupState) {
		this.includeGroupState = includeGroupState;
	}

	@Override
	public boolean needFilterByState() {
		return super.needFilterByState() || !ArrayUtils.isEmpty(groupState);
	}

}
