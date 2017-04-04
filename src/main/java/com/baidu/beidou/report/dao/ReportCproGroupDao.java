package com.baidu.beidou.report.dao;


import java.util.List;

import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.olap.vo.GroupViewItem;

/**
 * <p>ClassName:ReportCproPlanDao
 * <p>Function: 查询REPORT相关的推广组信息DAO
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 */
public interface ReportCproGroupDao {	
	
	/**
	 * <p>findCproGroupReportInfo:根据多个查询条件查询
	 *
	 * @param queryParam
	 * @return 组报告列表     
	 * @since 北斗星(2.0)
	*/	
	public List<GroupViewItem> findCproGroupReportInfo(GroupQueryParameter queryParam) ;
	
	/**
	 * 根据plan获取group数量
	 * @param planId
	 * @return
	 */
	public long countGroupByPlan(Integer planId);
	
	/**
	 * 根据user获取group数量
	 * @param userId
	 * @return
	 */
	public long countGroupByUser(Integer userId);
	

	/**
	 * <p>findAllCproGroupIds:根据多个查询条件获取所有推广组的ids
	 *
	 * @param queryParam
	 * @return 组ids列表     
	 * @since 北斗星(2.0)
	*/	
	public List<Integer> findAllCproGroupIds(GroupQueryParameter queryParam) ;
	
	/**
	 * countCproGroupReportInfo:满足DB过滤条件的总条数。一般在判断是否分页的时候会用到该方法
	 *
	 * @param queryParam 查询条件
	 * @return  符合查询条件的总记录条数    
	*/
	public int countCproGroupReportInfo(GroupQueryParameter queryParam) ;
	
	/**
	 * 此查找给投放网络使用
	 * @param queryParam
	 * @return
	 */
	public List<ExtGroupViewItem> findExtCproGroupReportInfo(GroupQueryParameter queryParam);
	
}
