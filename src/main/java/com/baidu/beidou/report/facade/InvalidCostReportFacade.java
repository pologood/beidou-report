//package com.baidu.beidou.report.facade;
//
//import java.util.Date;
//import java.util.List;
//
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.plan.InvalidCostReportVo;
//import com.baidu.beidou.report.vo.plan.InvalidCostViewItem;
//import com.baidu.beidou.user.bo.User;
//import com.opensymphony.xwork2.TextProvider;
//
//public interface InvalidCostReportFacade {
//
//	/**
//	 * 定制报告--账户效果报告--列表查询
//	 * @param queryParameter
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public List<InvalidCostViewItem> findInvalidCostStatData(QueryParameter queryParameter, Date from, Date to);
//
//	/**
//	 * 定制报告-- 账户效果报告--列表汇总
//	 * @param viewItemList
//	 * @return
//	 */
//	public InvalidCostViewItem sumInvalidCostStatData(List<InvalidCostViewItem> viewItemList);
//
//	/**
//	 * 定制报告--账户效果报告--获取下载VO
//	 * @param user
//	 * @param userViewItemList
//	 * @param sumInvalidCostItem
//	 * @param textProvider
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public InvalidCostReportVo getInvalidCostReportVo(User user, List<InvalidCostViewItem> viewItemList,
//			InvalidCostViewItem sumItem, TextProvider textProvider, String from, String to);
//	
//}
