package com.baidu.beidou.report.service;

import java.util.List;
import java.util.Map;

import com.baidu.beidou.report.vo.CostBean;
import com.baidu.beidou.report.vo.ExtendCostBean;

/**
 * <p>ClassName:ReportRpcService
 * <p>Function: 报表查询RPC服务
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-6
 */
public interface ReportRpcService {
	
	/**
	 * 根据若干ID返回已消费金额
	 * @param userIdList
	 * @param params 远程调用参数
	 * @return
	 */
	Map<Integer, Long> getAllCostByUserIds (List<Integer> userIdList);
	
	/**
	 * 根据若干ID返回今日消费金额，消费同比，环比等信息
	 * @param userIdList
	 * @return
	 */
	Map<Integer, ExtendCostBean> getExtendCostBeanByUserIds (List<Integer> userIdList);
	
	/**
	 * 根据若干ID返回昨日消费金额和已消费金额（已按照其他列排序并分页，需要凑齐数据）
	 * @param userIdList
	 * @return
	 */
	Map<Integer, CostBean> getCostBeanByUserIds (List<Integer> userIdList);
	
	/**
	 * 根据若干ID，返回根据昨日消费排序后的分页数据（已按照搜索词搜索，需要对搜索出来的userId按照已消费金额排序并分页）
	 * 
	 * @param userIdList
	 * @param isAsc
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	List<CostBean> getCostBeanByUserIdsOrderLastCost (List<Integer> userIdList, boolean isAsc, int currPage, int pageSize);
	
	/**
	 * 根据若干ID，返回根据已消费金额排序后的分页数据（已按照搜索词搜索，需要对搜索出来的userId按照已消费金额排序并分页）
	 * 
	 * @param userIdList
	 * @param isAsc
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	List<CostBean> getCostBeanByUserIdsOrderAllCost (List<Integer> userIdList, boolean isAsc, int currPage, int pageSize);
}
