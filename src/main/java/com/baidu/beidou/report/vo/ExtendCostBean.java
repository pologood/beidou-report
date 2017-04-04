/*******************************************************************************
 * CopyRight (c) 2000-2012 Baidu Online Network Technology (Beijing) Co., Ltd. All rights reserved.
 * Filename:    ExtendCostBean.java
 * Creator:     <a href="mailto:xuxiaohu@baidu.com">Xu,Xiaohu</a>
 * Create-Date: 2013-2-1 下午2:14:04
 *******************************************************************************/
package com.baidu.beidou.report.vo;

/**
 * 存放用户的今日消费、同比、环比消费增幅等信息
 *
 * @author <a href="mailto:xuxiaohu@baidu.com">Xu,Xiaohu</a>
 * @version 2013-2-1 下午2:14:04
 */
public class ExtendCostBean{
	
	/**
	 * 用户id
	 */
	private int userId;
	/**
	 * 消费环比增幅，计算为:(昨日消费/前日消费-1)*10000 取整
	 */
	private int lastDayGrowth;
	/**
	 * 消费同比增幅，计算为:（昨日消费/上周同日消费-1)*10000 取整
	 */
	private int lastWeekDayGrowth;
	
	/**
	 * 当前消费金额，单位是分
	 */
	private long todayCost;
	
	/**
	 * 昨日消费金额
	 */
	private long yestCost;
	
	/**
	 * 昨日展现量
	 */
	private long yestSrchs;

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getLastDayGrowth() {
		return lastDayGrowth;
	}

	public void setLastDayGrowth(int lastDayGrowth) {
		this.lastDayGrowth = lastDayGrowth;
	}

	public int getLastWeekDayGrowth() {
		return lastWeekDayGrowth;
	}

	public void setLastWeekDayGrowth(int lastWeekDayGrowth) {
		this.lastWeekDayGrowth = lastWeekDayGrowth;
	}

	public long getTodayCost() {
		return todayCost;
	}

	public void setTodayCost(long todayCost) {
		this.todayCost = todayCost;
	}

	public long getYestCost() {
		return yestCost;
	}

	public void setYestCost(long yestCost) {
		this.yestCost = yestCost;
	}

	public long getYestSrchs() {
		return yestSrchs;
	}

	public void setYestSrchs(long yestSrchs) {
		this.yestSrchs = yestSrchs;
	}

}
