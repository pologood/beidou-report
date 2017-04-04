package com.baidu.beidou.report.service;


import java.util.Date;
import java.util.List;
import java.util.Set;

import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.olap.vo.GroupViewItem;


/**
 * <p>ClassName:ReportCproGroupMgr
 * <p>Function: 报告部分访问group的MGR
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 */
public interface ReportCproGroupMgr {

	static final int GROUP_ALLSITE = 1;
	static final int GROUP_NOSITE = 2;
	static final int GROUP_NORMAL = 0;
	
	/**
	 * <p>findCproGroupViewItem:根据查询参数获取对应的显示VO。
	 *
	 * @param qp 查询参数
	 * @param shouldPagable 是否应该分页，通常在分页的时候不会按统计字段和state排序，不会按统计字段过滤
	 * @return      
	*/
	public List<GroupViewItem> findCproGroupViewItem(QueryParameter qp, boolean shouldPagable);	
	
	/**
	 * 根据推广组ID获取推广组选站状态
	 * 返回1：全网投放
	 * 返回2：无可投放网站
	 * 返回0：正常
	 * @param groupId
	 * @return
	 */
	public int getGroupSiteStatus(Integer groupId);
	
	
	/**
	 * countCproGroupViewItem: 计算满足条件的记录条数
	 *
	 * @param qp
	 * @return 满足查询条件的所有记录条数     
	*/
	public int countCproGroupViewItem(QueryParameter qp);	
	
	public int countCproGroupByUserId(Integer userId);
	
	public int countCproGroupByPlanId(Integer planId);
	

	/**
	 * <p>findAllCproGroupIdsByQuery:根据查询参数获取满足条件的所有ids。
	 *
	 * @param qp 查询参数
	 * @param shouldPagable 是否应该分页，通常在分页的时候不会按统计字段和state排序，不会按统计字段过滤
	 * @return 满足查询条件的groupids     
	*/
	public List<Integer> findAllCproGroupIdsByQuery(QueryParameter qp, boolean shouldPagable);	
	
	/**
	 * 根据层级ID查询全部QT推广组ID
	 * @param userId
	 * @param planId
	 * @param groupId
	 * @return
	 */
	public Set<Integer> findAllQTGroupIdsByLevel(Integer userId, Integer planId, Integer groupId);
	
	/**
	 * 为投放网络使用
	 * @param queryParam
	 * @return
	 */
	public List<ExtGroupViewItem> findExtCproGroupReportInfo(GroupQueryParameter queryParam);
	
	/**
	 * 根据层级ID查询全部KT推广组ID
	 * @param userId
	 * @param planId
	 * @param groupId
	 * @return
	 */
	public Set<Integer> findAllKTGroupIdsByLevel(Integer userId, Integer planId, Integer groupId);
	
	/**
	 * 查询推广组分日报告数据  add by zhuxiaoling since cpweb-550
	 * @param userId
	 * @param qp
	 * @param from
	 * @param to
	 * @return
	 */
	public List<ReportDayViewItem> getGroupReportDayViewItems(Integer userId, QueryParameter qp, 
			Date from, Date to);

}
