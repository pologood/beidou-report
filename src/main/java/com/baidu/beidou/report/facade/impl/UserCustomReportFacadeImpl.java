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
//import com.baidu.beidou.report.facade.UserCustomReportFacade;
//import com.baidu.beidou.report.util.ReportFieldFormatter;
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.ReportAccountInfo;
//import com.baidu.beidou.report.vo.plan.UserAccountCustomReportVo;
//import com.baidu.beidou.olap.vo.UserAccountViewItem;
//import com.baidu.beidou.stat.bean.OrderCriterion;
//import com.baidu.beidou.stat.driver.bo.Constant;
//import com.baidu.beidou.stat.driver.exception.StorageServiceException;
//import com.baidu.beidou.stat.facade.ReportFacade;
//import com.baidu.beidou.stat.service.StatService2;
//import com.baidu.beidou.user.bo.User;
//import com.baidu.beidou.user.service.UserMgr;
//import com.baidu.beidou.util.DateUtils;
//import com.baidu.beidou.util.StateConvertUtils;
//import com.opensymphony.xwork2.TextProvider;
//
//public class UserCustomReportFacadeImpl implements UserCustomReportFacade {
//
//	private static Log logger = LogFactory.getLog(UserCustomReportFacadeImpl.class);
//	protected StatService2 statMgr = null;
//	protected UserMgr userMgr = null;
//	protected ReportFacade reportFacade = null;
//
//	public StatService2 getStatMgr() {
//		return statMgr;
//	}
//
//	public void setStatMgr(StatService2 statMgr) {
//		this.statMgr = statMgr;
//	}
//
//	public UserMgr getUserMgr() {
//		return userMgr;
//	}
//
//	public void setUserMgr(UserMgr userMgr) {
//		this.userMgr = userMgr;
//	}
//
//	public ReportFacade getReportFacade() {
//		return reportFacade;
//	}
//
//	public void setReportFacade(ReportFacade reportFacade) {
//		this.reportFacade = reportFacade;
//	}
//
//	public List<UserAccountViewItem> findUserAccountStatData(QueryParameter queryParameter, Date from, Date to) {
//		if (queryParameter == null || from == null || to == null) {
//			return Collections.emptyList();
//		}
//		List<UserAccountViewItem> resultList = new ArrayList<UserAccountViewItem>();
//		// doris統計数据
//		List<Map<String, Object>> statList = new ArrayList<Map<String, Object>>();
//		try {
//			// 1、获取统计数据
//			statList = statMgr.queryAUserData(queryParameter.getUserId(), from, to, queryParameter.getTimeUnit(),
//					Constant.REPORT_TYPE_DEFAULT);
//		} catch (StorageServiceException e) {
//			logger.error(e.getMessage());
//			return Collections.emptyList();
//		}
//		logger.info("query for userData from doris,result count = " + (statList == null ? 0 : statList.size()));
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
//					row.put("viewState", StateConvertUtils.convertUserToStateView(user));
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
//				UserAccountViewItem accountItem = new UserAccountViewItem();
//				if (row != null) {
//					accountItem.fillStatRecord(row);
//					accountItem.setUserName(user.getUsername());
//					accountItem.setViewState(StateConvertUtils.convertUserToStateView(user));
//					resultList.add(accountItem);
//				}
//			}
//		}
//		logger.info("after padding, userData count =" + resultList.size() + ", and from ="
//				+ DateUtils.formatDate(from, "yyyyMMdd") + " ,to =" + DateUtils.formatDate(to, "yyyyMMdd"));
//		return resultList;
//	}
//
//	public UserAccountViewItem sumUserAccountStatData(List<UserAccountViewItem> userViewItemList) {
//		UserAccountViewItem sumItem = new UserAccountViewItem();
//		if (CollectionUtils.isNotEmpty(userViewItemList)) {
//			for (UserAccountViewItem item : userViewItemList) {
//				sumItem.setSrchs(sumItem.getSrchs() + item.getSrchs());
//				sumItem.setClks(sumItem.getClks() + item.getClks());
//				sumItem.setCost(sumItem.getCost() + item.getCost());
//				sumItem.setSrchuv(sumItem.getSrchuv() + item.getSrchuv());
//				sumItem.setClkuv(sumItem.getClkuv() + item.getClkuv());
//			}
//		}
//		// 汇总“ctr,acp,cpm”
//		sumItem.generateExtentionFields();
//		sumItem.setSummaryText("总计");
//		logger.info("sum userAccountViewItem, iteminfo : " + sumItem.toString());
//		return sumItem;
//	}
//
//	public UserAccountCustomReportVo getUserAccountCustomReportVo(User user, List<UserAccountViewItem> userViewItemList,
//			UserAccountViewItem sumUserAccountItem, TextProvider textProvider, String from, String to) {
//		if (user == null || textProvider == null || from == null || to == null) {
//			throw new IllegalArgumentException("user/textProvider/from/to can't be null.");
//		}
//		UserAccountCustomReportVo reportVo = new UserAccountCustomReportVo();
//		reportVo.setAccountInfo(getAccountReportAccountInfo(user.getUsername(), textProvider, from, to));
//		reportVo.setHeaders(getAcountReportHeaderInfo(textProvider));
//		reportVo.setDetails(userViewItemList);
//		reportVo.setSummary(sumUserAccountItem);
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
//	private ReportAccountInfo getAccountReportAccountInfo(String userName, TextProvider textProvider, String from, String to) {
//		if (textProvider == null) {
//			return null;
//		}
//
//		ReportAccountInfo accountInfo = new ReportAccountInfo();
//		List<String[]> infoList = new ArrayList<String[]>();
//		String[] textValuePair = new String[2];
//		textValuePair[0] = textProvider.getText("customreport.accountinfo.col.report");
//		textValuePair[1] = textProvider.getText("customreport.name.account");
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
//	private String[] getAcountReportHeaderInfo(TextProvider textProvider) {
//		if (textProvider == null) {
//			return ArrayUtils.EMPTY_STRING_ARRAY;
//		}
//		return textProvider.getText("customreport.account.head").split(",");
//	}
//
//}
