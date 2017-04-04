package com.baidu.beidou.report.dao;


import java.util.List;

import com.baidu.beidou.report.dao.vo.PlanQueryParameter;
import com.baidu.beidou.report.vo.plan.PlanOffTimeVo;
import com.baidu.beidou.olap.vo.PlanViewItem;

/**
 * <p>ClassName:ReportCproPlanDao
 * <p>Function: 查询REPORT相关的推广计划信息DAO
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 */
public interface ReportCproPlanDao {	
	
	/**
	 * <p>findCproPlanReportInfo:根据多个查询条件查询
	 *
	 * @param queryParam
	 * @return 计划报告列表     
	 * @since 北斗星(2.0)
	*/
	public List<PlanViewItem> findCproPlanReportInfo(PlanQueryParameter queryParam);
	

	/**
	 * findCproPlanOfftime:获取计划下线时间
	 *
	 * @param planIds 计划列表
	 * @return  计划下线时间信息    
	*/
	public List<PlanOffTimeVo> findCproPlanOfftime(List<Integer> planIds);
	
	/**
	 * findInSchedualPlanIds: 查询处于投放中的计划ID
	 *
	 * @param userId 用户名的userId
	 * @param planIds 待查询计划IDS
	 * @return 处于投放中的计划IDS列表     
	*/
	public List<Integer> findInSchedualPlanIds(int userId, List<Integer> planIds);

	/**
	 * findPauseInSchedualPlanIds: 查询可以投放中但由于未到投放时间的计划ID
	 *
	 * @param userId 用户名的userId
	 * @param planIds 待查询计划IDS
	 * @return 处于暂停投放中的计划IDS列表     
	*/ 
	public List<Integer> findPauseInSchedualPlanIds(int userId,	List<Integer> planIds);
}
