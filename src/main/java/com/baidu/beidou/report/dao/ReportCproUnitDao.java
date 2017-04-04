package com.baidu.beidou.report.dao;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.report.dao.vo.UnitQueryParameter;
import com.baidu.beidou.olap.vo.UnitViewItem;

/**
 * <p>ClassName:ReportCproUnitDao
 * <p>Function: 查询REPORT相关的创意信息DAO
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 */
public interface ReportCproUnitDao {	
	
	/**
	 * <p>findCproUnitReportInfo:根据多个查询条件查询
	 *
	 * @param queryParam
	 * @return 创意报告列表     
	 * @since 北斗星(2.0)
	*/	
	public List<UnitViewItem> findCproUnitReportInfo(UnitQueryParameter queryParam) ;
	
	/**
	 * countCproUnitReportInfo: 满足条件的总条数。一般在分页的时候会用到该方法
	 *
	 * @param queryParam 符合查询条件的总记录条数
	 * @return      
	*/
	public int countCproUnitReportInfo(UnitQueryParameter queryParam) ;


	/**
	 * <p>findAllCproUnitIds:根据多个查询条件获取所有推广创意的ids
	 *
	 * @param queryParam
	 * @return 组ids列表     
	 * @since 北斗星(2.0)
	*/	
	public List<Long> findAllCproUnitIds(UnitQueryParameter queryParam) ;
	
	/**
	 * findCproUnitAuditInfo: 根据ids查询拒绝理由
	 *
	 * @param unitIds 待查询ids集
	 * @param userId userId 用于分表
	 * @return key为adid，value为拒绝理由      
	*/
	public Map<Long, String> findCproUnitAuditInfo(Collection<Long> unitIds, int userId);
}
