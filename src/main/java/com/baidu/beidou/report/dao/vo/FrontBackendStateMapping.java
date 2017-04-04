package com.baidu.beidou.report.dao.vo;

import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.report.constant.QueryParameterConstant;

/**
 * <p>ClassName:FrontBackendStateMapping
 * <p>Function: 前端后状态映射
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * 
 * @see QueryParameterConstant.FrontViewState
 * @see CproPlanConstant#PLAN_STATE_*
 * @see CproPlanConstant#PLAN_BUDGETOVER
 * @created  2011-3-17
 */
public abstract class FrontBackendStateMapping {


	/**
	 * needFilterByState: 是否需要按状态进行过滤
	 *
	 * @return true表示需要按状态进行过滤，false表示不需要      
	*/
	public abstract boolean needFilterByState();
	
}
