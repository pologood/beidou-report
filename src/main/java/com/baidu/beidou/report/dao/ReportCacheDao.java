package com.baidu.beidou.report.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.report.vo.ExtendCostBean;

public interface ReportCacheDao extends ReportDao{

	/**
	 * 获取统计数据缓存最后更新时间
	 * @return
	 */
	public Date queryStatCacheLastModifiedDate();
	
	/**
	 * 根据若干ID返回今日消费金额，消费同比，环比等信息
	 * @param userIdList
	 * @return
	 */
	List<ExtendCostBean> getExtendCostBeanByUserIds (List<Integer> userIdList);
	
	/**
	 * 根据userids查询已消费金额
	 * @param userIdList
	 * @return
	 */
	Map<Integer, Long> getAllCostByUserIds(List<Integer> userIdList);
	
}
