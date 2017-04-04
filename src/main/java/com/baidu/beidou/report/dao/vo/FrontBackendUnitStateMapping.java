package com.baidu.beidou.report.dao.vo;

import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.report.constant.QueryParameterConstant;

/**
 * <p>ClassName:FrontBackendUnitStateMapping
 * <p>Function: 推广创意前后端状态映射，由前端列表中的状态映射成后端DB中的实际状态
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * 
 * @see QueryParameterConstant.FrontViewState
 * @see CproUnitConstant#UNIT_STATE_*
 * @created  2011-3-17
 */
public class FrontBackendUnitStateMapping extends FrontBackendGroupStateMapping {

	/** 
	 * 对应的推广组状态列表
	 * */
	protected int[] unitState;
	
	/** 是包括还是排除，true表示包括，false表示排除 */
	protected boolean includeUnitState;
	
	public FrontBackendUnitStateMapping(int[] planStates, boolean hasBudgetOver, boolean includePlanState,
			int[] groupState, boolean includeGroupState,
			int[] unitState, boolean includeUnitState) {
		super(planStates, hasBudgetOver, includePlanState, groupState, includeGroupState);
		this.unitState = unitState;
		this.includeUnitState = includeUnitState;
		
	}

	public int[] getUnitState() {
		return unitState;
	}

	public void setUnitState(int[] unitState) {
		this.unitState = unitState;
	}

	public boolean isIncludeUnitState() {
		return includeUnitState;
	}

	public void setIncludeUnitState(boolean includeUnitState) {
		this.includeUnitState = includeUnitState;
	}

	@Override
	public boolean needFilterByState() {
		return super.needFilterByState() || !ArrayUtils.isEmpty(unitState);
	}
}
