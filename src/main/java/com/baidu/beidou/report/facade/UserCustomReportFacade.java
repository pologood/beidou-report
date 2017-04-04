//package com.baidu.beidou.report.facade;
//
//import java.util.Date;
//import java.util.List;
//
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.plan.UserAccountCustomReportVo;
//import com.baidu.beidou.olap.vo.UserAccountViewItem;
//import com.baidu.beidou.user.bo.User;
//import com.opensymphony.xwork2.TextProvider;
//
///**
// * 定制报告业务接口
// * 
// * @author hujunhai 2013-1-6
// */
//public interface UserCustomReportFacade {
//
//	/**
//	 * 定制报告--账户效果报告--列表查询
//	 * @param queryParameter
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public List<UserAccountViewItem> findUserAccountStatData(QueryParameter queryParameter, Date from, Date to);
//
//	/**
//	 * 定制报告-- 账户效果报告--列表汇总
//	 * @param userViewItemList
//	 * @return
//	 */
//	public UserAccountViewItem sumUserAccountStatData(List<UserAccountViewItem> userViewItemList);
//
//	/**
//	 * 定制报告--账户效果报告--获取下载VO
//	 * @param user
//	 * @param userViewItemList
//	 * @param sumUserAccountItem
//	 * @param textProvider
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	public UserAccountCustomReportVo getUserAccountCustomReportVo(User user, List<UserAccountViewItem> userViewItemList,
//			UserAccountViewItem sumUserAccountItem, TextProvider textProvider, String from, String to);
//	
//}
