//package com.baidu.beidou.report.facade;
//
//import java.util.Date;
//import java.util.List;
//
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.ReportSumData;
//import com.baidu.beidou.report.vo.group.GroupCustomReportVo;
//import com.baidu.beidou.olap.vo.GroupViewItem;
//import com.baidu.beidou.report.vo.site.SiteCustomReportVo;
//import com.baidu.beidou.olap.vo.SiteViewItem;
//import com.baidu.beidou.user.bo.User;
//import com.opensymphony.xwork2.TextProvider;
//
//public interface GroupCustomReportFacade {
//
//	/**
//	 * 定制报告--推广組效果报告--不分网站--列表查询
//	 * 
//	 * @param queryParameter
//	 * @param from
//	 * @param to
//	 * @param infoForReport
//	 *            列表查询的时候为null
//	 * @return
//	 */
//	public List<GroupViewItem> findGroupStatData(QueryParameter queryParameter, Date from, Date to, List<String> infoForReport);
//
//	/**
//	 * 定制报告--推广組效果报告--分网站--列表查询
//	 * 
//	 * @param queryParameter
//	 * @param from
//	 * @param to
//	 * @param infoForReport
//	 *            列表查询的时候为null
//	 * @return
//	 */
//	public List<SiteViewItem> findSiteStatData(QueryParameter queryParameter, Date from, Date to, List<String> infoForReport);
//
//	/**
//	 * 定制报告--推广組效果报告--不分网站--获取汇总数据
//	 * 
//	 * @param viewItemList
//	 * @return
//	 */
//	public ReportSumData sumGroupStatData(List<GroupViewItem> viewItemList);
//
//	/**
//	 * 定制报告--推广組效果报告--不分网站--获取汇总数据
//	 * 
//	 * @param viewItemList
//	 * @return
//	 */
//	public ReportSumData sumSiteStatData(List<SiteViewItem> viewItemList);
//
//	/**
//	 * 定制报告--推广组效果--不分网站--获取下载报告VO
//	 * 
//	 * @param user
//	 * @param viewItemList
//	 * @param reportSumData
//	 * @param infoForReport
//	 * @param textProvider
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public GroupCustomReportVo getGroupCustomReportVo(User user, List<GroupViewItem> viewItemList, ReportSumData reportSumData,
//			List<String> infoForReport, TextProvider textProvider, String from, String to, int isBySite);
//
//	/**
//	 * 定制报告--推广组效果--分网站--获取下载报告VO
//	 * 
//	 * @param user
//	 * @param viewItemList
//	 * @param reportSumData
//	 * @param infoForReport
//	 * @param textProvider
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public SiteCustomReportVo getSiteCustomReportVo(User user, List<SiteViewItem> viewItemList, ReportSumData reportSumData,
//			List<String> infoForReport, TextProvider textProvider, String from, String to, int isBySite);
//
//}
