package com.baidu.beidou.report.dao.vo;

/**
 * <p>ClassName:GroupQtExistenceQueryParameters
 * <p>Function: 查询当前层级是否有设置QT的sql查询参数封装VO
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-6-10
 */
public class GroupQtExistenceQueryParameters extends SqlQueryParameter {

	protected Integer planId;
	protected Integer groupId;
	public Integer getPlanId() {
		return planId;
	}
	public void setPlanId(Integer planId) {
		this.planId = planId;
	}
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	
	
}
