package com.baidu.beidou.report.dao.impl;

import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.dao.BaseDaoSupport;

public class BaseReportDaoImpl extends BaseDaoSupport {

	
	/**
	 * makePlanStateFilter:生成plan的状态过滤条件
	 *
	 * @param sb
	 * @param planStates 状态集合（不含已下线）
	 * @param hasBudgetOver 是否需要包括已下线
	 * @param include      采用in | not in 方式查询
	*/
	protected void makePlanStateFilter(StringBuilder sb, 
			int[] planStates, boolean hasBudgetOver, boolean include) {

		if ( !ArrayUtils.isEmpty( planStates) || hasBudgetOver) {//生成plan状态的过滤sql
			
			if (include) {//采用in
				//and ( (p.planstate in ()) or (p.planstate=0 and p.budgetover=1) )
				sb.append(" AND ( ");
				if (!ArrayUtils.isEmpty( planStates)) {
					if (planStates.length == 1) {
						sb.append(" p.planstate=");
						sb.append(planStates[0]);
						if (CproPlanConstant.PLAN_STATE_NORMAL == planStates[0]) {
							sb.append(" AND p.budgetover=" + CproPlanConstant.PLAN_NOT_BUDGETOVER);
						}
						sb.append(" ");
					} else {
						
						//此处有个潜在问题，如果state列表中有“有效”这个状态的话则会把已下线选出来，但在当前配置条件不出有问题。
						sb.append(" p.planstate IN (");
						sb.append(StringUtils.makeStrFromArray(planStates, ","));
						sb.append(") ");
						if (hasBudgetOver) {
							sb.append(" OR (p.planstate=" + CproPlanConstant.PLAN_STATE_NORMAL + " and p.budgetover=" 
									+ CproPlanConstant.PLAN_BUDGETOVER +")");
						}
					}
				} else {
					sb.append(" p.planstate=" + CproPlanConstant.PLAN_STATE_NORMAL + " and p.budgetover=" 
							+ CproPlanConstant.PLAN_BUDGETOVER +"");
				}
				
				sb.append(") ");
			} else {//采用not in
				//and ( (p.planstate not in ()) and not (p.planstate=0 and p.budgetover=1) )
				
				sb.append(" AND ( ");
				if (!ArrayUtils.isEmpty( planStates)) {
					sb.append(" p.planstate NOT IN (");
					sb.append(StringUtils.makeStrFromArray(planStates, ","));
					sb.append(") ");
					
					if (hasBudgetOver) {
						sb.append(" AND NOT (p.planstate=" + CproPlanConstant.PLAN_STATE_NORMAL + " and p.budgetover=" 
								+ CproPlanConstant.PLAN_BUDGETOVER + ")");
					}
				} else {
					sb.append(" NOT (p.planstate=" + CproPlanConstant.PLAN_STATE_NORMAL + " and p.budgetover=" 
							+ CproPlanConstant.PLAN_BUDGETOVER + ")");
				}
				
				sb.append(") ");
			}
		}
	}

	protected void makeGroupStateFilter(StringBuilder sb, int[] groupState, boolean include) {

		if ( !ArrayUtils.isEmpty( groupState) ) {//生成group状态的过滤sql
			if (groupState.length == 1) {

				sb.append(" AND g.groupstate" ); 
				sb.append(include ? "" : "!");
				sb.append("=");
				sb.append(groupState[0]);
				sb.append(" ");
			} else {
				sb.append(" AND g.groupstate " ); 
				sb.append(include ? "" : " NOT ");
				sb.append(" IN (");
				sb.append(StringUtils.makeStrFromArray(groupState, ","));
				sb.append(") ");
			}
		}
	}

	protected void makeUnitStateFilter(StringBuilder sb, int[] unitState, boolean include) {

		if ( !ArrayUtils.isEmpty( unitState) ) {//生成group状态的过滤sql

			if (unitState.length == 1) {

				sb.append(" AND us.state" ); 
				sb.append(include ? "" : "!");
				sb.append("=");
				sb.append(unitState[0]);
				sb.append(" ");
			} else {
				sb.append(" AND us.state " ); 
				sb.append(include ? "" : " NOT ");
				sb.append(" IN (");
				sb.append(StringUtils.makeStrFromArray(unitState, ","));
				sb.append(") ");
			}
		}
	}

}
