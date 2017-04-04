package com.baidu.beidou.report.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.AccountStatService;
import com.baidu.beidou.olap.service.ReportByDayStatService;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportCproPlanDao;
import com.baidu.beidou.report.dao.vo.FrontBackendPlanStateMapping;
import com.baidu.beidou.report.dao.vo.PlanQueryParameter;
import com.baidu.beidou.report.facade.TransReportFacade;
import com.baidu.beidou.report.service.ReportCproPlanMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.ReportSumData;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.report.vo.comparator.ViewStateOrderComparator;
import com.baidu.beidou.report.vo.plan.PlanOffTimeVo;
import com.baidu.beidou.report.vo.plan.UserAccountReportVo;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.driver.exception.StorageServiceException;
import com.baidu.beidou.stat.facade.ReportFacade;
import com.baidu.beidou.stat.facade.UvReportFacade;
import com.baidu.beidou.stat.service.HolmesDataService;
import com.baidu.beidou.stat.service.TransDataService;
import com.baidu.beidou.stat.service.UvDataService;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.DateUtils;
import com.baidu.unbiz.olap.constant.OlapConstants;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.opensymphony.xwork2.TextProvider;

/**
 * <p>ClassName:ReportCproPlanMgrImpl
 * <p>Function: 推广计划报表服务
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-5
 */
public class ReportCproPlanMgrImpl implements ReportCproPlanMgr{

	private static Log logger = LogFactory.getLog(ReportCproPlanMgrImpl.class);
	private ReportCproPlanDao reportCproPlanDao;
	
	@Resource(name="accountStatServiceImpl")
	private AccountStatService accountStatMgr;
	
	@Resource(name="reportByDayStatServiceImpl")
	private ReportByDayStatService byDayStatService;
	
	/**
	 * added by liuhao05 since cpweb-492
	 */
	/**UV数据的查询接口*/
	private UvDataService uvDataService;
	
	/**Holmes数据的查询接口*/
	private HolmesDataService holmesDataService;
	
	/**UV数据的Facade接口*/
	private UvReportFacade uvReportFacade;
	
	/**Report的Facade接口*/
	private ReportFacade reportFacade;
	
	/**
	 * 转化数据的Facade接口
	 */
	private TransReportFacade transReportFacade;
	
	/**
	 * 转化数据的查询接口
	 */
	private TransDataService transDataService;
	
	
	public List<PlanViewItem> findPlanViewItemWithoutPagable(QueryParameter qp) {
		
		if(qp == null || qp.getUserId() == null) {
			logger.error(" userId must not be null when excute the query");
			return new ArrayList<PlanViewItem>();
		}
		boolean shouldPagable = false;//方便后续扩展
		PlanQueryParameter queryParam = makePlanQueryParameter(qp, shouldPagable);
		List<PlanViewItem> result = reportCproPlanDao.findCproPlanReportInfo(queryParam);

		if (shouldPagable && 
				!CollectionUtils.isEmpty(result) && 
				ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())) {
			//如果按状态排序则在内存中排。

			Collections.sort(result, new ViewStateOrderComparator(qp.getOrder()));
		}
		return result;
	}
	
	protected PlanQueryParameter makePlanQueryParameter(QueryParameter qp, boolean shouldPagable) {
		
		PlanQueryParameter queryParam = new PlanQueryParameter();
		queryParam.setUserId(qp.getUserId());
		queryParam.setIds(qp.getIds());
		queryParam.setName(qp.getKeyword());
		queryParam.setPromotionType(qp.getPromotionType());
		
		FrontBackendPlanStateMapping stateMapping = (FrontBackendPlanStateMapping)qp.getStateMapping();
		if (stateMapping != null  && stateMapping.needFilterByState()) {
			
			queryParam.setStateInclude(stateMapping.isIncludePlanState());//采用in | not in
			
			queryParam.setBudgetOver(stateMapping.isHasBudgetOver());//添加已下线状态
			
			queryParam.setState(stateMapping.getPlanStateWithoutBudgetOver());
		}
		/* 由于需要查询汇总信息，因此当前不做分页查询。
		//是否需要设置分页信息：分页信息不全或者按“状态”进行排序
		  if (!qp.noPagerInfo()
				&& !ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())) {
			queryParam.setPage(qp.getPage());
			queryParam.setPageSize(qp.getPageSize());
		}*/
		if(shouldPagable
				&& !ReportConstants.isStatField(qp.getOrderBy()) 
				&& !ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())) {
			
			//如果按照状态来排序的话由于在DB中不方便，因此由内存来排序
			
			queryParam.setSortColumn(ReportWebConstants.PLAN_FRONT_BACKEND_PARAMNAME_MAPPING.get(qp.getOrderBy()));
			queryParam.setSortOrder(qp.getOrder());
		}
		return queryParam;
	}

	public List<PlanOffTimeVo> findCproPlanOfftime(List<Integer> planIds) {
		return reportCproPlanDao.findCproPlanOfftime(planIds);
	}
	public List<Integer> findInSchedualPlanIds(int userId, List<Integer> planIds) {
		return reportCproPlanDao.findInSchedualPlanIds(userId, planIds);
	}

	public List<Integer> findPauseInSchedualPlanIds(int userId, List<Integer> planIds) {
		return reportCproPlanDao.findPauseInSchedualPlanIds(userId, planIds);
	}
	
	public List<UserAccountViewItem> findUserAccountStatData(QueryParameter queryParameter,
			Date from, Date to) {
		
		if (queryParameter == null || from == null || to == null) {
			return Collections.emptyList();
		}
		
		List<UserAccountViewItem> resultList = new ArrayList<UserAccountViewItem>();
		List<UserAccountViewItem> dorisList = new ArrayList<UserAccountViewItem>();
		
		//需要返回的数据
		List<Map<String, Object>> mergedList = null;
		List<UserAccountViewItem> olapList = null;
		List<Map<String, Object>> uvList = new ArrayList<Map<String, Object>>();
		
		try {
			olapList = accountStatMgr.queryAUserData(queryParameter.getUserId(), from, to, ReportConstants.TU_DAY);
			
			//2、获取uv数据
			uvList = uvDataService.queryUserData(queryParameter.getUserId(), from, to, 
					null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			//3、如果需要获取转化数据，则获取转化数据并合并
			boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(queryParameter.getUserId(),from,to,false);
			
			if (needToFetchTransData){
				
				TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(queryParameter.getUserId());
				
				//4、获取转化数据
				List<Map<String, Object>> tranData = transDataService.queryUserData(queryParameter.getUserId(), 
										tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
										from, to, null, 0,ReportConstants.TU_DAY,Constant.REPORT_TYPE_DEFAULT, 0, 0);
								
				//5、获取Holmes数据
				List<Map<String, Object>> holmesData = holmesDataService.queryUserData(queryParameter.getUserId(), null, null, 
													   from, to, null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
				
				//6、Merge统计数据、UV数据、Holmes数据、转化数据
				mergedList = this.reportFacade.mergeTransHolmesAndUvData(Constant.DorisDataType.UV, tranData, holmesData, uvList, ReportConstants.FROMDATE);		
			} else {			
				
				//6、Merge统计数据和UV数据
				mergedList = uvList;
			}
			
		} catch (StorageServiceException e) {
			logger.error(e.getMessage());
			return null;
		}
		logger.info("query for userData from doris,result count = "	+ (mergedList == null ? 0 : mergedList.size()));
		
		//7、填充数据
		if (CollectionUtils.isNotEmpty(mergedList)) {
			for (Map<String, Object> row : mergedList) {
				UserAccountViewItem accountItem = new UserAccountViewItem();
				if (row != null) {
					accountItem.fillStatRecord(row);
					accountItem.generateExtentionFields();
					dorisList.add(accountItem);
				}
			}
		}
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, UserAccountViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		Set<String> recordDateSet = new HashSet<String>();
		
		if(CollectionUtils.isNotEmpty(resultList)){
			for(UserAccountViewItem item : resultList){
				recordDateSet.add(item.getShowDate());
			}
		}
		
		// 填充
		paddingUserAccountData(resultList, recordDateSet, from, to);
		
		//8、按照统计字段过滤
		resultList = filterByStatField(queryParameter, resultList);
		
		ReportUtil.generateExtentionFields(resultList);
		
		logger.info("after padding, userData count =" + resultList.size() + ", and from ="
				+ DateUtils.formatDate(from, "yyyyMMdd") + " ,to ="
				+ DateUtils.formatDate(to, "yyyyMMdd"));
		Collections.sort(resultList);
		return resultList;
	}
	
	/**
	 * 按统计字段进行过滤
	 * @param qp 查询参数
	 * @param vos 待过滤列表
	*/
	private <T extends StatInfo> List<T> filterByStatField(QueryParameter qp, List<T> vos) {
		
		if(org.apache.commons.collections.CollectionUtils.isEmpty(vos) 
				|| !qp.hasStatField4Filter()) {
			return vos;
		} else {
			for ( int i = vos.size() - 1; i >=0; i--) {
				if (ReportWebConstants.filter(qp, vos.get(i))) {
					vos.remove(vos.get(i));
				}
			}
			return vos;
		}
	}
	
	private void paddingUserAccountData(List<UserAccountViewItem> accountViewList,
			Set<String> recordDateSet, Date from, Date to) {
		List<String> allDateList = DateUtils.getDayListBetween2Day(DateUtils.formatDorisReturnDate(
				from, UserAccountViewItem.viewDateFormat), DateUtils.formatDorisReturnDate(to,
				UserAccountViewItem.viewDateFormat), UserAccountViewItem.viewDateFormat);
		for (String dateInall : allDateList) {
			if (!recordDateSet.contains(dateInall)) {
				UserAccountViewItem missedViewItem = new UserAccountViewItem();
				missedViewItem.setShowDate(dateInall);
				// 设置默认值
				missedViewItem.fillZeroStat();
				accountViewList.add(missedViewItem);
			}
		}
	}
	
	public List<UserAccountViewItem> pageUserAccountData(List<UserAccountViewItem> accountViewList,
			QueryParameter queryParameter) {
		if (CollectionUtils.isEmpty(accountViewList) || queryParameter == null) {
			return Collections.emptyList();
		}
		if (accountViewList.size() <= ReportWebConstants.FRONT_SORT_THRESHOLD) {
			return accountViewList;
		}
		// 分页前排序
		sortUserAccountData(accountViewList, queryParameter.getOrderBy(), queryParameter.getOrder());
		int page = 0;
		int pageSize = ReportConstants.PAGE_SIZE;
		if (queryParameter.getPage() != null && queryParameter.getPage() > 1) {
			page = queryParameter.getPage();
		}
		if (queryParameter.getPageSize() != null && queryParameter.getPageSize() > 0) {
			pageSize = queryParameter.getPageSize();
		}
		accountViewList = ReportWebConstants.subListinPage(accountViewList, page, pageSize);
		return accountViewList;
	}

	private void sortUserAccountData(List<UserAccountViewItem> accountViewList, String orderby,
			String order) {
		if (CollectionUtils.isEmpty(accountViewList) || StringUtils.isEmpty(orderby)
				|| StringUtils.isEmpty(order) || ReportConstants.isStatField(orderby)
				|| (!ReportConstants.SHOW_DATE.equalsIgnoreCase(orderby))) {
			return;
		}
		Comparator<UserAccountViewItem> comparator = null;
		if (BeidouConstant.SORTORDER_DESC.equalsIgnoreCase(order)) {
			comparator = new Comparator<UserAccountViewItem>() {
				public int compare(UserAccountViewItem item1, UserAccountViewItem item2) {
					if (item1 == null || item2 == null || item1.getShowDate() == null
							|| item2.getShowDate() == null) {
						return 0;
					}
					return item2.getShowDate().compareTo(item1.getShowDate());
				}
			};
			Collections.sort(accountViewList, comparator);
			return;
		}
		comparator = new Comparator<UserAccountViewItem>() {
			public int compare(UserAccountViewItem item1, UserAccountViewItem item2) {
				if (item1 == null || item2 == null || item1.getShowDate() == null
						|| item2.getShowDate() == null) {
					return 0;
				}
				return item1.getShowDate().compareTo(item2.getShowDate());
			}
		};
		Collections.sort(accountViewList, comparator);
	}

	public UserAccountViewItem sumUserAccountStatData(List<UserAccountViewItem> userViewItemList) {
		UserAccountViewItem sumItem = new UserAccountViewItem();
		if (CollectionUtils.isNotEmpty(userViewItemList)) {
			for (UserAccountViewItem item : userViewItemList) {
				sumItem.setSrchs(sumItem.getSrchs() + item.getSrchs());
				sumItem.setClks(sumItem.getClks() + item.getClks());
				sumItem.setCost(sumItem.getCost() + item.getCost());
				sumItem.setSrchuv(sumItem.getSrchuv() + item.getSrchuv());
				sumItem.setClkuv(sumItem.getClkuv() + item.getClkuv());
			}
		}
		// 汇总“ctr,acp,cpm”
		sumItem.generateExtentionFields();
		// 是否前端排序
		if (userViewItemList.size() <= ReportWebConstants.FRONT_SORT_THRESHOLD) {
			sumItem.setNeedFrontSort(ReportConstants.Boolean.TRUE);
		}
		logger.info("sum userAccountViewItem, iteminfo : " + sumItem.toString());
		return sumItem;
	}
	
	public UserAccountReportVo getDownloadAccountReportVo(User user,
			List<UserAccountViewItem> userViewItemList, UserAccountViewItem sumUserAccountItem,
			TextProvider textProvider, String from, String to) {
		if (user == null || textProvider == null || from == null || to == null) {
			throw new IllegalArgumentException("user/textProvider/from/to can't be null.");
		}
		boolean showTransData = this.transReportFacade.isTransToolSigned(user.getUserid(), false);
		Date datefrom, dateto;
		try{
			datefrom = DateUtils.strToDate(from);
			dateto = DateUtils.strToDate(to);
		}catch(Exception e){
			datefrom = null;
			dateto = null;
		}
		boolean transDataValid = this.transReportFacade.needToFetchTransData(user.getUserid(), datefrom, dateto, false);
		
		UserAccountReportVo reportVo = new UserAccountReportVo();
		reportVo.setShowTransData(showTransData);
		reportVo.setTransDataValid(transDataValid);
		reportVo.setAccountInfo(getAccountReportAccountInfo(user.getUsername(), textProvider, from,	to));
		reportVo.setHeaders(getAcountReportHeaderInfo(textProvider,showTransData));
		reportVo.setDetails(userViewItemList);
		reportVo.setSummary(getAccountReportSumInfo(sumUserAccountItem, textProvider));
		return reportVo;
	}

	private ReportAccountInfo getAccountReportAccountInfo(String userName,
			TextProvider textProvider, String from, String to) {
		if (textProvider == null) {
			return null;
		}
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(textProvider.getText("download.account.report.account"));
		accountInfo.setReportText(textProvider.getText("download.account.report"));
		accountInfo.setAccount(userName);
		accountInfo.setAccountText(textProvider.getText("download.account.account"));
		accountInfo.setDateRange(from + " - " + to);
		accountInfo.setDateRangeText(textProvider.getText("download.account.daterange"));
		accountInfo.setLevel(textProvider.getText("download.account.level.allplan"));
		accountInfo.setLevelText(textProvider.getText("download.account.level"));
		return accountInfo;
	}

	private String[] getAcountReportHeaderInfo(TextProvider textProvider, boolean showTransData) {
		if (textProvider == null) {
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}
		if (showTransData) {
			return textProvider.getText("download.account.head.col.has.trans").split(",");
		} else {
			return textProvider.getText("download.account.head.col").split(",");
		}
	}

	private UserAccountViewItem getAccountReportSumInfo(
			UserAccountViewItem sumUserReportAccountItem, TextProvider textProvider) {
		if (sumUserReportAccountItem == null || textProvider == null) {
			return null;
		}
		sumUserReportAccountItem.setSummaryText(textProvider.getText("download.summary"));
		return sumUserReportAccountItem;
	}
	
	public List<ReportDayViewItem> getPlanReportDayViewItems(Integer userId, QueryParameter qp, 
			Date from, Date to) {
		if (userId == null || userId < 0 || qp == null || from == null || to == null) {
			return Collections.emptyList();
		}
		List<ReportDayViewItem> resultList = new ArrayList<ReportDayViewItem>();
		List<ReportDayViewItem> dorisList = null;
		List<Map<String, Object>> mergedData = null;
		
		List<ReportDayViewItem> olapList = byDayStatService.queryByDayData(userId,
				Arrays.asList(new Integer[]{qp.getPlanId()}), from, to, ReportConstants.TU_DAY);
		
		//2、获取UV数据
		List<Map<String, Object>> uvData = this.uvDataService.queryPlanDataByTime(userId, Arrays.asList(new Integer[]{qp.getPlanId()}), 
																from, to, null, 0, 
																ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		//3、判断是否有转化数据
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);
		
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//4、获取转化数据
			List<Map<String, Object>> transData = transDataService.queryPlanData(userId, 
										tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
										Arrays.asList(new Integer[]{qp.getPlanId()}), from, to, null, 0, 
										ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);	
			
			//5、获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryPlanData(userId, null, null, Arrays.asList(new Integer[]{qp.getPlanId()}), 
																				from, to, null, 0, 
																				ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);	
			
			//6、Merge统计数据、UV数据、Holmes数据、转化数据，不需要排序，FROMDATE为Key
			mergedData = this.reportFacade.mergeTransHolmesAndUvData(Constant.DorisDataType.UV, transData, holmesData, uvData, ReportConstants.FROMDATE);		
			
		} else {
			//6、Merge统计数据、UV数据（FROMDATE为Key）
			mergedData = uvData;
		}
		
		if ("showDate".equalsIgnoreCase(qp.getOrderBy())
				&& "desc".equalsIgnoreCase(qp.getOrder())) {
			Collections.reverse(mergedData);
		}
		
		dorisList = ReportUtil.transformStatDataByDay(mergedData);
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, ReportDayViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		//7、按照统计字段过滤  modified by zhuxiaoling since cpweb-550
		resultList = filterByStatField(qp, resultList);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}
	
	public ReportSumData sumReportDayStatData(List<ReportDayViewItem> itemList) {
		ReportSumData sumItem = new ReportSumData();
		Long count = 0L;
		if (CollectionUtils.isNotEmpty(itemList)) {
			for (ReportDayViewItem item : itemList) {
				sumItem.setSrchs(sumItem.getSrchs() + item.getSrchs());
				sumItem.setClks(sumItem.getClks() + item.getClks());
				sumItem.setCost(sumItem.getCost() + item.getCost());
				sumItem.setSrchuv(sumItem.getSrchuv() + item.getSrchuv());
				sumItem.setClkuv(sumItem.getClkuv() + item.getClkuv());
				count++;
			}
		}
		// 汇总“ctr,acp,cpm”
		sumItem.generateExtentionFields();
		sumItem.setCount(count);
		
		return sumItem;
	}

	public ReportCproPlanDao getReportCproPlanDao() {
		return reportCproPlanDao;
	}

	public void setReportCproPlanDao(ReportCproPlanDao reportCproPlanDao) {
		this.reportCproPlanDao = reportCproPlanDao;
	}

	public UvDataService getUvDataService() {
		return uvDataService;
	}

	public void setUvDataService(UvDataService uvDataService) {
		this.uvDataService = uvDataService;
	}

	public HolmesDataService getHolmesDataService() {
		return holmesDataService;
	}

	public void setHolmesDataService(HolmesDataService holmesDataService) {
		this.holmesDataService = holmesDataService;
	}

	public UvReportFacade getUvReportFacade() {
		return uvReportFacade;
	}

	public void setUvReportFacade(UvReportFacade uvReportFacade) {
		this.uvReportFacade = uvReportFacade;
	}

	public ReportFacade getReportFacade() {
		return reportFacade;
	}

	public void setReportFacade(ReportFacade reportFacade) {
		this.reportFacade = reportFacade;
	}

	public TransReportFacade getTransReportFacade() {
		return transReportFacade;
	}

	public void setTransReportFacade(TransReportFacade transReportFacade) {
		this.transReportFacade = transReportFacade;
	}

	public TransDataService getTransDataService() {
		return transDataService;
	}

	public void setTransDataService(TransDataService transDataService) {
		this.transDataService = transDataService;
	}
	
	public AccountStatService getAccountStatMgr() {
		return accountStatMgr;
	}

	public void setAccountStatMgr(AccountStatService accountStatMgr) {
		this.accountStatMgr = accountStatMgr;
	}

}