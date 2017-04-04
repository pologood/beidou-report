package com.baidu.beidou.report.vo.plan;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.vo.StatInfo;


public class PlanSumData extends StatInfo {
	
	/** 有效推广计划总预算 */
	protected int sumBudget;
	/** 列表中总计划数 */
	protected int planCount;
	/** 有效计划数 */
	protected int validPlanCount;
	/** 是否需要前端排序，前端规则为返回条数<=1W */
	protected int needFrontSort = ReportConstants.Boolean.FALSE;
	
	public int getSumBudget() {
		return sumBudget;
	}
	public void setSumBudget(int sumBudget) {
		this.sumBudget = sumBudget;
	}
	public int getPlanCount() {
		return planCount;
	}
	public void setPlanCount(int planCount) {
		this.planCount = planCount;
	}
	public int getValidPlanCount() {
		return validPlanCount;
	}
	public void setValidPlanCount(int validPlanCount) {
		this.validPlanCount = validPlanCount;
	}
	public int getNeedFrontSort() {
		return needFrontSort;
	}
	public void setNeedFrontSort(int needFrontSort) {
		this.needFrontSort = needFrontSort;
	}

}
