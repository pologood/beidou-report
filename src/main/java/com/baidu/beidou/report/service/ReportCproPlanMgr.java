package com.baidu.beidou.report.service;


import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.vo.ReportSumData;
import com.baidu.beidou.report.vo.plan.PlanOffTimeVo;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.report.vo.plan.UserAccountReportVo;
import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.user.bo.User;
import com.opensymphony.xwork2.TextProvider;


/**
 * <p>ClassName:ReportCproPlanMgr
 * <p>Function: 报告部分访问plan的MGR
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 */
public interface ReportCproPlanMgr {
	
	/**
	 * <p>getPlanViewItem:根据查询参数获取对应的显示VO，不做分页，取全部数据。
	 *
	 * @param qp 查询参数
	 * @return      
	*/
	public List<PlanViewItem> findPlanViewItemWithoutPagable(QueryParameter qp);	
	
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
	 * 网盟推广首页--查询账户分日报告数据 add by kanghongwei since 星二期第二批
	 * 
	 * @param queryParameter
	 * @param from
	 * @param to
	 * @return
	 */
	public List<UserAccountViewItem> findUserAccountStatData(QueryParameter queryParameter,
			Date from, Date to);

	/**
	 * 汇总账户分日统计报告数据 add by kanghongwei since 星二期第二批
	 * 
	 * @param accountDateList
	 * @return
	 */
	public UserAccountViewItem sumUserAccountStatData(List<UserAccountViewItem> userViewItemList);

	/**
	 * 网盟推广首页--分页账户分日报告数据 add by kanghongewi since 星二期第二批
	 * 
	 * @param accountViewList
	 * @param queryParameter
	 * @return
	 */
	public List<UserAccountViewItem> pageUserAccountData(List<UserAccountViewItem> accountViewList,
			QueryParameter queryParameter);

	/**
	 * 网盟推广首页--获取分日账户报表下载数据 add by kanghongewi since 星二期第二批
	 * 
	 * @param user
	 * @param userViewItemList
	 * @param sumUserAccountItem
	 * @param textProvider
	 * @return
	 */
	public UserAccountReportVo getDownloadAccountReportVo(User user,
			List<UserAccountViewItem> userViewItemList, UserAccountViewItem sumUserAccountItem,
			TextProvider textProvider, String from, String to);
	
	/**
	 * 查询推广计划分日报告数据  add by zhuxiaoling since cpweb-550
	 * @param userId
	 * @param qp
	 * @param from
	 * @param to
	 * @return
	 */
	
	public List<ReportDayViewItem> getPlanReportDayViewItems(Integer userId, QueryParameter qp, 
			Date from, Date to);
	
	/**
	 * 汇总推广计划/组分日统计报告数据 add by zhuxiaoling since cpweb-550
	 * @param itemList
	 * @return
	 */
	public ReportSumData sumReportDayStatData(List<ReportDayViewItem> itemList);

	/**
	 * findPauseInSchedualPlanIds: 查询可以投放中但由于未到投放时间的计划ID
	 *
	 * @param userId 用户名的userId
	 * @param planIds 待查询计划IDS
	 * @return 处于暂停投放中的计划IDS列表     
	*/ 
	public List<Integer> findPauseInSchedualPlanIds(int userId, List<Integer> planIds);

}
