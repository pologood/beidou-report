package com.baidu.beidou.report.vo.plan;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>ClassName:PlanOffTimeVo
 * <p>Function: 用于表示Plan的下线时间
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-5
 */
public class PlanOffTimeVo implements Serializable {

	/** 计划Id */
	protected Integer planId;
	
	protected Date offtime;

	public Integer getPlanId() {
		return planId;
	}

	public void setPlanId(Integer planId) {
		this.planId = planId;
	}

	public Date getOfftime() {
		return offtime;
	}

	public void setOfftime(Date offtime) {
		this.offtime = offtime;
	} 
	
}
