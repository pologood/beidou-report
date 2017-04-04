package com.baidu.beidou.report.service;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.olap.vo.UnitViewItem;


/**
 * <p>ClassName:ReportCproUnitMgr
 * <p>Function: 报告部分访问unit的MGR
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 */
public interface ReportCproUnitMgr {

	
	/**
	 * <p>findCproUnitViewItem:根据查询参数获取对应的显示VO。
	 *
	 * @param qp 查询参数
	 * @param shouldPagable 是否应该分页，通常在分页的时候不会按统计字段和state排序，不会按统计字段过滤
	 * @return      
	*/
	public List<UnitViewItem> findCproUnitViewItem(QueryParameter qp, boolean shouldPagable);	
	
	/**
	 * countCproUnitViewItem: 计算满足条件的记录条数
	 *
	 * @param qp
	 * @return 满足查询条件的所有记录条数     
	*/
	public int countCproUnitViewItem(QueryParameter qp);	

	/**
	 * findCproUnitAuditInfo: 根据ids查询拒绝理由
	 *
	 * @param unitIds 待查询ids集
	 * @param userId userId 用于分表
	 * @return key为adid，value为拒绝理由      
	*/
	public Map<Long, String> findCproUnitAuditInfo(Collection<Long> unitIds, int userId);

	/**
	 * <p>findAllCproUnitIdsByQuery:根据查询参数获取满足条件的所有ids。
	 *
	 * @param qp 查询参数
	 * @param shouldPagable 是否应该分页，通常在分页的时候不会按统计字段和state排序，不会按统计字段过滤
	 * @return 满足查询条件的Unitids     
	*/
	public List<Long> findAllCproUnitIdsByQuery(QueryParameter qp, boolean shouldPagable);	
	
}
