//package com.baidu.beidou.report.facade;
///**
// * 创意定制报告facade层封装
// * @author wangchongjie
// * @since 2013-01-09
// */
//
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.olap.vo.UnitViewItem;
//
//public interface UnitCustomReportFacade {
//
//	//排序merge后的结果
//	public void sortData(List<Map<String, Object>> data, QueryParameter qp);
//	
//	//获取doris统计数据
//	public List<Map<String, Object>> fetchStatData(QueryParameter qp, Date from, Date to);
//	
//	//merge db中的信息
//	public void mergeInfoData(List<Map<String, Object>> data, QueryParameter qp);
//	
//	//转会为前端显示的vo
//	public List<UnitViewItem> translateData(List<Map<String, Object>> data, QueryParameter qp);
//	
//	//定制报告-填充用户自定义的推广计划、推广组
//	public void fillFilterContent(QueryParameter qp, List<String[]> accountInfoList);
//	
//}
