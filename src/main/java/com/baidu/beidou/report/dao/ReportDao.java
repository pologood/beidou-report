/**
 * ??2009 Baidu
 */
package com.baidu.beidou.report.dao;

import java.util.List;
import com.baidu.beidou.report.vo.CostBean;

public interface ReportDao {

	/** 按照昨天消费排序 */
	public static final String ORDERBY_LAST_COST = "lastCost";
	/** 按照所有消费排序 */
	public static final String ORDERBY_ALL_COST = "allCost";

	/**
	 * 根据若干ID，返回根据昨日消费排序后的分页数据（已按照搜索词搜索，需要对搜索出来的userId按照已消费(昨天消费)金额排序并分页）
	 * 
	 * @param userIdList
	 * @param OrderBy 排序字段，为空则不排序
	 * @param order 排序顺序， desc | asc
	 * @param currPage >=0 ,小于0则不分密度
	 * @param pageSize >0，小于1则不分页
	 * @return
	 */
	List<CostBean> getCostBeanByUserIdsOrderByCost (List<Integer> userIdList, 
			String orderBy, String order, int currPage, int pageSize);
	

}
