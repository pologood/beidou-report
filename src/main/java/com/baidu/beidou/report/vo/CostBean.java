package com.baidu.beidou.report.vo;

/**
 * <p>ClassName:CostBean
 * <p>Function: 返回用户的消费信息
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-6
 */
public class CostBean {
	private int userId; // 用户ID
	private long lastCost; // 昨日消费金额，单位是分
	private long allCost; // 已消费金额，单位是分
	
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public long getLastCost() {
		return lastCost;
	}
	public void setLastCost(long lastCost) {
		this.lastCost = lastCost;
	}
	public long getAllCost() {
		return allCost;
	}
	public void setAllCost(long allCost) {
		this.allCost = allCost;
	}
}
