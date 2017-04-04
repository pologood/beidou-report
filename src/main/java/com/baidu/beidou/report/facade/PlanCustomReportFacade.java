//package com.baidu.beidou.report.facade;
//
//import java.util.Date;
//import java.util.List;
//
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.plan.PlanCustomReportVo;
//import com.baidu.beidou.report.vo.plan.PlanReportSumData;
//import com.baidu.beidou.olap.vo.PlanViewItem;
//import com.baidu.beidou.user.bo.User;
//import com.opensymphony.xwork2.TextProvider;
//
//public interface PlanCustomReportFacade {
//
//	/**
//	 * 定制报告--推广计划效果报告--列表查询
//	 * 
//	 * @param queryParameter
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public List<PlanViewItem> findPlanStatData(QueryParameter queryParameter, Date from, Date to, List<String> infoForReport);
//
//	/**
//	 * 定制报告-- 推广计划效果报告--列表汇总
//	 * 
//	 * @param userViewItemList
//	 * @return
//	 */
//	public PlanReportSumData sumPlanStatData(List<PlanViewItem> planViewItemList);
//
//	/**
//	 * 定制报告--账户效果报告--获取下载VO
//	 * 
//	 * @param user
//	 * @param userViewItemList
//	 * @param sumUserAccountItem
//	 * @param textProvider
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public PlanCustomReportVo getPlanCustomReportVo(User user, List<PlanViewItem> planViewItemList,
//			PlanReportSumData planReportSumData, List<String> infoForReport, TextProvider textProvider, String from, String to,
//			String level);
//}
