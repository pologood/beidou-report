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
//import org.apache.commons.lang.StringUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import com.baidu.beidou.cproplan.bo.CproPlan;
//import com.baidu.beidou.cproplan.dao.CproPlanDao;
//import com.baidu.beidou.report.ReportConstants;
//import com.baidu.beidou.report.constant.QueryParameterConstant;
//import com.baidu.beidou.report.facade.PlanCustomReportFacade;
//import com.baidu.beidou.report.util.ReportFieldFormatter;
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.ReportAccountInfo;
//import com.baidu.beidou.report.vo.plan.PlanCustomReportVo;
//import com.baidu.beidou.report.vo.plan.PlanReportSumData;
//import com.baidu.beidou.olap.vo.PlanViewItem;
//import com.baidu.beidou.stat.bean.OrderCriterion;
//import com.baidu.beidou.stat.driver.bo.Constant;
//import com.baidu.beidou.stat.facade.ReportFacade;
//import com.baidu.beidou.stat.service.StatService2;
//import com.baidu.beidou.stat.util.StatUtils;
//import com.baidu.beidou.user.bo.User;
//import com.baidu.beidou.util.DateUtils;
//import com.baidu.beidou.util.StateConvertUtils;
//import com.opensymphony.xwork2.TextProvider;
//
//public class PlanCustomReportFacadeImpl implements PlanCustomReportFacade {
//
//	private static Log logger = LogFactory.getLog(PlanCustomReportFacadeImpl.class);
//
//	private static int itemsPerLine = 8;
//
//	protected StatService2 statMgr = null;
//	private CproPlanDao cproPlanDao = null;
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
//	public CproPlanDao getCproPlanDao() {
//		return cproPlanDao;
//	}
//
//	public void setCproPlanDao(CproPlanDao cproPlanDao) {
//		this.cproPlanDao = cproPlanDao;
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
//	public List<PlanViewItem> findPlanStatData(QueryParameter queryParameter, Date from, Date to, List<String> infoForReport) {
//		if (queryParameter == null || from == null || to == null) {
//			return Collections.emptyList();
//		}
//		// 获取planIds
//		List<Integer> planIds = queryParameter.getPlanIds();
//
//		List<PlanViewItem> resultList = new ArrayList<PlanViewItem>();
//		// 1、获取doris统计数据
//		List<Map<String, Object>> statList = new ArrayList<Map<String, Object>>();
//		// 如果按统计字段排序则需要以统计字段的相对顺序进行结果的排序。
//		int orient = ReportConstants.SortOrder.ASC;
//		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(queryParameter.getOrder())) {
//			orient = ReportConstants.SortOrder.DES;
//		}
//		// 是否按统计字段排序
//		boolean isOrderByStatDataField = ReportConstants.isStatField(queryParameter.getOrderBy());
//
//		if (!isOrderByStatDataField) {
//			statList = statMgr.queryPlanData(queryParameter.getUserId(), planIds, from, to, null, 0,
//					queryParameter.getTimeUnit(), Constant.REPORT_TYPE_DEFAULT);
//		} else {// 使用doris排序，获取排好序的统计数据
//			statList = statMgr.queryPlanData(queryParameter.getUserId(), planIds, from, to, queryParameter.getOrderBy(), orient,
//					queryParameter.getTimeUnit(), Constant.REPORT_TYPE_DEFAULT);
//		}
//		logger.info("query for planData from doris,result count = " + (statList == null ? 0 : statList.size()));
//
//		// 2、DB中获取推广计划相关数据
//		List<CproPlan> planList = new ArrayList<CproPlan>();
//		if (queryParameter.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_ACCOUNT)) {
//			planList = cproPlanDao.findByUserId(queryParameter.getUserId());
//		} else {
//			planList = cproPlanDao.findCproPlanByPlanIds(planIds);
//		}
//		if (CollectionUtils.isEmpty(planList)) {
//			return Collections.emptyList();
//		}
//		makeReportInfo(infoForReport, planList, queryParameter.getLevel());
//
//		Map<Integer, CproPlan> planInfoMap = StatUtils.converPlanListToMap(planList);
//		// 3、填充DB数据至List<Map>中
//		if (CollectionUtils.isNotEmpty(statList)) {
//			for (Map<String, Object> row : statList) {
//				Integer planId = (Integer) row.get(ReportConstants.PLAN);
//				CproPlan planInfo = planInfoMap.get(planId);
//				if (row != null) {
//					String dateRange = ReportFieldFormatter.formatContentDateRange((Date) row.get(ReportConstants.FROMDATE),
//							(Date) row.get(ReportConstants.TODATE));
//					row.put("dateRange", dateRange);
//					if (planInfo != null) {
//						row.put("planName", planInfo.getPlanName());
//						row.put("viewState", StateConvertUtils.convertPlanToStateView(planInfo));
//					} else {
//						logger.warn("planInfo is null ,planId is " + planId);
//					}
//				}
//			}
//		}
//		// 4、非doris统计字段排序
//		if (!isOrderByStatDataField) {
//			List<OrderCriterion> orderCriterionList = reportFacade.makeCustomOrderCriteria(queryParameter.getOrderBy() + "/"
//					+ queryParameter.getOrder());
//			if (CollectionUtils.isNotEmpty(orderCriterionList)) {
//				statList = reportFacade.sortData(statList, orderCriterionList);
//			}
//		}
//		// 5、生成前端列表使用的List
//		if (CollectionUtils.isNotEmpty(statList)) {
//			for (Map<String, Object> row : statList) {
//				PlanViewItem planItem = new PlanViewItem();
//				if (row != null) {
//					planItem.fillStatRecord(row);
//					planItem.setDateRange((String) row.get("dateRange"));
//					planItem.setPlanId((Integer) row.get(ReportConstants.PLAN));
//					planItem.setPlanName((String) row.get("planName"));
//					planItem.setViewState((Integer) row.get("viewState"));
//					resultList.add(planItem);
//				}
//			}
//		}
//		logger.info("after padding, userData count =" + resultList.size() + ", and from ="
//				+ DateUtils.formatDate(from, "yyyyMMdd") + " ,to =" + DateUtils.formatDate(to, "yyyyMMdd"));
//		return resultList;
//	}
//
//	/**
//	 * 组装下载报告需要的推广计划名称信息
//	 * 
//	 * @param infoForReport
//	 * @param planList
//	 * @param level
//	 */
//	private void makeReportInfo(List<String> infoForReport, List<CproPlan> planList, String level) {
//		if (infoForReport != null) {
//			if (level.equals(QueryParameterConstant.LEVEL.LEVEL_ACCOUNT)) {
//				infoForReport.add("全部");
//			} else {
//				String planNames = "";
//				for (CproPlan plan : planList) {
//					planNames = planNames + "," + plan.getPlanName();
//				}
//				if (!StringUtils.isEmpty(planNames)) {
//					planNames = planNames.substring(1, planNames.length());
//				}
//				infoForReport.add(planNames);
//			}
//		}
//	}
//
//	public PlanReportSumData sumPlanStatData(List<PlanViewItem> planViewItemList) {
//		PlanReportSumData sumItem = new PlanReportSumData();
//		if (CollectionUtils.isNotEmpty(planViewItemList)) {
//			for (PlanViewItem item : planViewItemList) {
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
//	public PlanCustomReportVo getPlanCustomReportVo(User user, List<PlanViewItem> planViewItemList,
//			PlanReportSumData planReportSumData, List<String> infoForReport, TextProvider textProvider, String from, String to,
//			String level) {
//		if (user == null || textProvider == null || from == null || to == null) {
//			throw new IllegalArgumentException("plan/textProvider/from/to can't be null.");
//		}
//
//		PlanCustomReportVo reportVo = new PlanCustomReportVo(level);
//		reportVo.setAccountInfo(getAccountReportAccountInfo(user.getUsername(), infoForReport, textProvider, from, to));
//		reportVo.setHeaders(getPlanReportHeaderInfo(textProvider));
//		reportVo.setDetails(planViewItemList);
//		reportVo.setSummary(planReportSumData);
//		return reportVo;
//	}
//
//	private ReportAccountInfo getAccountReportAccountInfo(String userName, List<String> infoForReport, TextProvider textProvider,
//			String from, String to) {
//		if (textProvider == null) {
//			return null;
//		}
//		ReportAccountInfo accountInfo = new ReportAccountInfo();
//		List<String[]> infoList = new ArrayList<String[]>();
//		String[] textValuePair = new String[2];
//		textValuePair[0] = textProvider.getText("customreport.accountinfo.col.report");
//		textValuePair[1] = textProvider.getText("customreport.name.plan");
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
//
//		if (CollectionUtils.isNotEmpty(infoForReport)) {
//			infoList.addAll(formatInfo(textProvider.getText("customreport.accountinfo.col.plan"), infoForReport.get(0)));
//		}
//
//		accountInfo.setAccountInfoList(infoList);
//		return accountInfo;
//	}
//
//	private static List<String[]> formatInfo(String colName, String values) {
//		List<String[]> result = new ArrayList<String[]>();
//		if (StringUtils.isNotEmpty(values)) {
//			String[] array = values.split(",");
//			if (array != null) {
//				String[] row = new String[2]; // 每行两列，第一列是title，第二列是数据
//				row[0] = colName;// 第一行，title显示过滤标题
//
//				StringBuilder sb = new StringBuilder("");
//				for (int i = 0; i < array.length; i++) {
//					String value = array[i];
//					if (i % itemsPerLine > 0) {
//						sb.append(", ");
//					}
//					sb.append(value);
//					if (i % itemsPerLine == (itemsPerLine - 1)) {
//						// 换行
//						row[1] = sb.toString();
//						result.add(row);
//						row = new String[2];
//						row[0] = "";// 第二行及以后，都用空字符串做占位符
//						sb = new StringBuilder(); // 清空
//					} else if (i == array.length - 1) {
//						row[1] = sb.toString();
//					}
//				}
//				result.add(row);// 加入最后一行
//			}
//		}
//		return result;
//	}
//
//	private String[] getPlanReportHeaderInfo(TextProvider textProvider) {
//		if (textProvider == null) {
//			return ArrayUtils.EMPTY_STRING_ARRAY;
//		}
//		return textProvider.getText("customreport.plan.head").split(",");
//	}
//}
