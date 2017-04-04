//package com.baidu.beidou.report.facade.impl;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.collections.CollectionUtils;
//import org.apache.commons.lang.ArrayUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import com.baidu.beidou.report.ReportConstants;
//import com.baidu.beidou.report.facade.InvalidCostReportFacade;
//import com.baidu.beidou.report.util.ReportFieldFormatter;
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.ReportAccountInfo;
//import com.baidu.beidou.report.vo.plan.InvalidCostReportVo;
//import com.baidu.beidou.report.vo.plan.InvalidCostViewItem;
//import com.baidu.beidou.stat.bean.OrderCriterion;
//import com.baidu.beidou.stat.driver.bo.Constant;
//import com.baidu.beidou.stat.driver.exception.StorageServiceException;
//import com.baidu.beidou.stat.facade.ReportFacade;
//import com.baidu.beidou.stat.service.AntiStatService;
//import com.baidu.beidou.user.bo.User;
//import com.baidu.beidou.user.service.UserMgr;
//import com.baidu.beidou.util.DateUtils;
//import com.opensymphony.xwork2.TextProvider;
//
//public class InvalidCostReportFacadeImpl implements InvalidCostReportFacade {
//
//	private static Log logger = LogFactory.getLog(InvalidCostReportFacadeImpl.class);
//	protected AntiStatService antiStatService = null;
//	protected UserMgr userMgr = null;
//	protected ReportFacade reportFacade = null;
//	
//	public AntiStatService getAntiStatService() {
//		return antiStatService;
//	}
//	public void setAntiStatService(AntiStatService antiStatService) {
//		this.antiStatService = antiStatService;
//	}
//	public UserMgr getUserMgr() {
//		return userMgr;
//	}
//	public void setUserMgr(UserMgr userMgr) {
//		this.userMgr = userMgr;
//	}
//	public ReportFacade getReportFacade() {
//		return reportFacade;
//	}
//	public void setReportFacade(ReportFacade reportFacade) {
//		this.reportFacade = reportFacade;
//	}
//	
//	public List<InvalidCostViewItem> findInvalidCostStatData(QueryParameter queryParameter, Date from, Date to) {
//		if (queryParameter == null || from == null || to == null) {
//			return Collections.emptyList();
//		}
//		List<InvalidCostViewItem> resultList = new ArrayList<InvalidCostViewItem>();
//		// doris統計数据
//		List<Map<String, Object>> statList = new ArrayList<Map<String, Object>>();
//		try {
//			// 1、获取统计数据
//			statList = antiStatService.queryAntiData(queryParameter.getUserId(), from, to, queryParameter.getTimeUnit(),
//					Constant.REPORT_TYPE_DEFAULT);
//		} catch (StorageServiceException e) {
//			logger.error(e.getMessage());
//			return Collections.emptyList();
//		}
//		logger.info("query for antiData from doris,result count = " + (statList == null ? 0 : statList.size()));
//
//		// 2、获取DB数据
//		User user = userMgr.findUserBySFid(queryParameter.getUserId());
//		if (user == null) {
//			logger.warn("user is null");
//			return Collections.emptyList();
//		}
//		// 3、填充DB数据至List<Map>中
//		if (CollectionUtils.isNotEmpty(statList)) {
//			for (Map<String, Object> row : statList) {
//				if (row != null) {
//					String dateRange = ReportFieldFormatter.formatContentDateRange((Date) row.get(ReportConstants.FROMDATE),
//							(Date) row.get(ReportConstants.TODATE));
//					row.put("dateRange", dateRange);
//					row.put("userName", user.getUsername());
//				}
//			}
//		}
//		// 4、排序
//		List<OrderCriterion> orderCriterionList = reportFacade.makeCustomOrderCriteria(queryParameter.getOrderBy() + "/"
//				+ queryParameter.getOrder());
//		if (CollectionUtils.isNotEmpty(orderCriterionList)) {
//			statList = reportFacade.sortData(statList, orderCriterionList);
//		}
//		// 5、生成前端列表使用的List
//		if (CollectionUtils.isNotEmpty(statList)) {
//			for (Map<String, Object> row : statList) {
//				InvalidCostViewItem item = new InvalidCostViewItem();
//				if (row != null) {
//					item.fillStatRecord(row);
//					item.setUserName(user.getUsername());
//					resultList.add(item);
//				}
//			}
//		}
//		logger.info("after padding, antiData count =" + resultList.size() + ", and from ="
//				+ DateUtils.formatDate(from, "yyyyMMdd") + " ,to =" + DateUtils.formatDate(to, "yyyyMMdd"));
//		return resultList;
//	}
//	public InvalidCostViewItem sumInvalidCostStatData(List<InvalidCostViewItem> viewItemList) {
//		InvalidCostViewItem sumItem = new InvalidCostViewItem();
//		if (CollectionUtils.isNotEmpty(viewItemList)) {
//			for (InvalidCostViewItem item : viewItemList) {
//				sumItem.setOriginalClks(sumItem.getOriginalClks() + item.getOriginalClks());
//				sumItem.setRealtimeFilterClks(sumItem.getRealtimeFilterClks() + item.getRealtimeFilterClks());
//				sumItem.setRealtimeFilterCost(sumItem.getRealtimeFilterCost() + item.getRealtimeFilterCost());
//			}
//		}
//		if (sumItem.getOriginalClks() >0) {
//			sumItem.setRealtimeFilterRate((double)sumItem.getRealtimeFilterClks()/(double)sumItem.getOriginalClks());
//		}else {
//			sumItem.setRealtimeFilterRate(0.0);
//		}
//		sumItem.setSummaryText("总计");
//		logger.info("sum InvalidCostViewItem, iteminfo : " + sumItem.toString());
//		return sumItem;
//	}
//	public InvalidCostReportVo getInvalidCostReportVo(User user, List<InvalidCostViewItem> viewItemList,
//			InvalidCostViewItem sumItem, TextProvider textProvider, String from, String to) {
//		if (user == null || textProvider == null || from == null || to == null) {
//			throw new IllegalArgumentException("user/textProvider/from/to can't be null.");
//		}
//		InvalidCostReportVo reportVo = new InvalidCostReportVo();
//		reportVo.setAccountInfo(getAccountInfo(user.getUsername(), textProvider, from, to));
//		reportVo.setHeaders(getHeaderInfo(textProvider));
//		reportVo.setDetails(viewItemList);
//		reportVo.setSummary(sumItem);
//		return reportVo;
//	}
//	
//	/**
//	 * 获取定制报告账户部分信息
//	 * 
//	 * @param userName
//	 * @param textProvider
//	 * @param from
//	 * @param to
//	 * @return
//	 */
//	private ReportAccountInfo getAccountInfo(String userName, TextProvider textProvider, String from, String to) {
//		if (textProvider == null) {
//			return null;
//		}
//
//		ReportAccountInfo accountInfo = new ReportAccountInfo();
//		List<String[]> infoList = new ArrayList<String[]>();
//		String[] textValuePair = new String[2];
//		textValuePair[0] = textProvider.getText("customreport.accountinfo.col.report");
//		textValuePair[1] = textProvider.getText("customreport.name.invalidcost");
//		infoList.add(textValuePair);
//
//		textValuePair = new String[2];
//		textValuePair[0] = textProvider.getText("customreport.accountinfo.col.account");
//		textValuePair[1] = userName;
//		infoList.add(textValuePair);
//
//		textValuePair = new String[2];
//		textValuePair[0] = textProvider.getText("customreport.accountinfo.col.daterange");
//		textValuePair[1] = from + " - " + to;
//		infoList.add(textValuePair);
//		accountInfo.setAccountInfoList(infoList);
//		return accountInfo;
//	}
//	
//	/**
//	 * 获取账户效果报告标题头
//	 * 
//	 * @param textProvider
//	 * @return
//	 */
//	private String[] getHeaderInfo(TextProvider textProvider) {
//		if (textProvider == null) {
//			return ArrayUtils.EMPTY_STRING_ARRAY;
//		}
//		return textProvider.getText("customreport.invalidcost.head").split(",");
//	}
//
//}
