//package com.baidu.beidou.report.facade.impl;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.apache.commons.collections.CollectionUtils;
//
//import com.baidu.beidou.cprogroup.bo.CproGroup;
//import com.baidu.beidou.cprogroup.service.CproGroupMgr;
//import com.baidu.beidou.cproplan.bo.CproPlan;
//import com.baidu.beidou.cproplan.service.CproPlanMgr;
//import com.baidu.beidou.cprounit.constant.CproUnitConstant;
//import com.baidu.beidou.cprounit.service.CproUnitMgr;
//import com.baidu.beidou.cprounit.vo.UnitInfoView;
//import com.baidu.beidou.olap.vo.UnitViewItem;
//import com.baidu.beidou.report.ReportConstants;
//import com.baidu.beidou.report.facade.UnitCustomReportFacade;
//import com.baidu.beidou.report.util.ReportFieldFormatter;
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.stat.bean.OrderCriterion;
//import com.baidu.beidou.stat.driver.bo.Constant;
//import com.baidu.beidou.stat.facade.ReportFacade;
//import com.baidu.beidou.stat.service.StatService2;
//import com.baidu.beidou.stat.service.impl.MultiUnitSiteStatSupport;
//import com.baidu.beidou.stat.util.StatUtils;
//public class UnitCustomReportFacadeImpl implements UnitCustomReportFacade{
//	
//	private CproPlanMgr cproPlanMgr;	
//	private CproGroupMgr cproGroupMgr;
//	private CproUnitMgr cproUnitMgr;
//	private StatService2 statMgr;
//	private MultiUnitSiteStatSupport statService;
//	private ReportFacade reportFacade;
//	
//	private Set<Integer> groupIdSet = new HashSet<Integer>(0);
//	private Set<Integer> planIdSet = new HashSet<Integer>(0);
//	private Set<Long> unitIdSet = new HashSet<Long>(0);
//	
//	
//	public List<Map<String, Object>> fetchStatData(QueryParameter qp, Date from, Date to){
//		
//		List<Map<String, Object>> statData = null;
//		
//		if(qp.getIsBySite().equals(ReportConstants.SiteLevel.MAINSITE)){ //分主域
//			//由前端保证分网站时只有一个推广组
//			List<Long> unitIds = fetchUnitIdsByGroupId(qp);
//			if(CollectionUtils.isEmpty(unitIds)){
//				return statData;
//			}
//			statData = statService.queryMultiUnitDataByMainsite(qp.getUserId(), unitIds, from, to, null, 
//					0, qp.getTimeUnit(), Constant.REPORT_TYPE_DEFAULT);
//		} else if (qp.getIsBySite().equals(ReportConstants.SiteLevel.SITE)){ //分二级域
//			//由前端保证分网站时只有一个推广组
//			List<Long> unitIds = fetchUnitIdsByGroupId(qp);
//			if(CollectionUtils.isEmpty(unitIds)){
//				return statData;
//			}
//			statData = statService.queryMultiUnitDataBySubsite(qp.getUserId(), unitIds, null, from, to, 
//					null, 0, qp.getTimeUnit(), Constant.REPORT_TYPE_DEFAULT);
//		} else { //不分域
//			statData = statMgr.queryUnitData(qp.getUserId(), qp.getPlanIds(), qp.getGroupIds(),
//					null, from, to, null, 0, qp.getTimeUnit(), Constant.REPORT_TYPE_DEFAULT);
//		}
//		return statData;
//	}
//	
//	public void sortData(List<Map<String, Object>> data, QueryParameter qp) {
//		
//		String sortString = null;
//		if(null != qp.getOrderBy() && null != qp.getOrder()){
//			sortString=""+ReportConstants.getBackOrderBy(qp.getOrderBy())+"/"+qp.getOrder();
//		}
//		List<OrderCriterion> orderCriteria = reportFacade.makeCustomOrderCriteria(sortString);
//		
//		if(orderCriteria == null || orderCriteria.size() == 0){
//			//默认：按时间排序，后按推广计划名
//			orderCriteria = new ArrayList<OrderCriterion>();
//			orderCriteria.add(new OrderCriterion(ReportConstants.DATE_RANGE, ReportConstants.SortOrder.DES));
//			
//			if(qp.getIsBySite().equals(ReportConstants.SiteLevel.MAINSITE)){
//				orderCriteria.add(new OrderCriterion(ReportConstants.MAINSITE, ReportConstants.SortOrder.ASC));
//			}else if(qp.getIsBySite().equals(ReportConstants.SiteLevel.SITE)){
//				orderCriteria.add(new OrderCriterion(ReportConstants.SITE, ReportConstants.SortOrder.ASC));
//			}else{
//				orderCriteria.add(new OrderCriterion(ReportConstants.PLAN, ReportConstants.SortOrder.ASC));
//				orderCriteria.add(new OrderCriterion(ReportConstants.GROUP, ReportConstants.SortOrder.ASC));
//			}
//		}
//		data = reportFacade.sortData(data, orderCriteria);
//		
//	}
//	
//	public void mergeInfoData(List<Map<String, Object>> data, QueryParameter qp) {
//		
//		if(CollectionUtils.isEmpty(data)){
//			return;
//		}
//		
//		collectIds(data);
//		List<Integer> planIds = Arrays.asList(planIdSet.toArray(new Integer[0]));
//		List<Integer> groupIds = Arrays.asList(groupIdSet.toArray(new Integer[0]));
//		List<Long> unitIds = Arrays.asList(unitIdSet.toArray(new Long[0]));
//		
//		//===基础数据准备,获取DB数据===
//		Map<Integer, CproGroup> groupMap = StatUtils.converGroupListToMap(
//				cproGroupMgr.findCproGroupByGroupIds(groupIds));
//		Map<Integer, CproPlan> planMap = StatUtils.converPlanListToMap(
//				cproPlanMgr.findCproPlanByPlanIds(planIds));
//		
//		//TODO: 推广单元比较复杂，暂时逐个查询，后继需要优化这个查询的性能！
//		Map<Long, UnitInfoView> unitMap = new HashMap<Long, UnitInfoView>(unitIds.size());
//		for(Long unitid : unitIds){
//			UnitInfoView view = cproUnitMgr.findUnitById(qp.getUserId(), unitid);
//			if(view!=null){
//				unitMap.put(view.getUnitid(), view);
//			}
//		}
//		
//		CproGroup oneChosenGroup = null; //分网站列表中用户选择，有且仅有一个推广组
//		if( ! qp.getIsBySite().equals(ReportConstants.SiteLevel.NONE)){
//			List<CproGroup> groups = cproGroupMgr.findCproGroupByGroupIds(qp.getGroupIds());
//			oneChosenGroup = groups.get(0);
//		}
//		
//		for(Map<String, Object> row : data){
//			
//			if(qp.getIsBySite().equals(ReportConstants.SiteLevel.NONE)){
//				//推广计划ID增NAME
//				Integer planId = (Integer) row.get(ReportConstants.PLAN);
//				row.put(ReportConstants.PLANNAME, planMap.get(planId).getPlanName());
//				
//				//推广组ID增NAME
//				Integer groupId = (Integer) row.get(ReportConstants.GROUP);
//				row.put(ReportConstants.GROUPNAME, groupMap.get(groupId).getGroupName());	
//				row.put(ReportConstants.GROUP_STATE, groupMap.get(groupId).getGroupState());	
//			}else{
//				if(null != oneChosenGroup){
//					row.put(ReportConstants.GROUP_STATE, oneChosenGroup.getGroupState());
//				}
//			}
//			
//			//推广单元ID转成多列unit信息
//			Long unitId = (Long) row.get(ReportConstants.UNIT);
//			UnitInfoView view = unitMap.get(unitId);
//			row = insertUnitInfo(row, view);
//			if(null != view && null != view.getStateView()){
//				row.put(ReportConstants.STATUS, view.getStateView().getViewState());
//			}
//			row.put(ReportConstants.STATUS_NAME, getCproUnitStatusName(view));
//			
//			String dateRange = 
//				ReportFieldFormatter.formatContentDateRange((Date)row.get(ReportConstants.FROMDATE), 
//										(Date)row.get(ReportConstants.TODATE));
//			row.put(ReportConstants.DATE_RANGE, dateRange);
//		}
//	}
//	
//	public List<UnitViewItem> translateData(List<Map<String, Object>> data, QueryParameter qp){
//		
//		List<UnitViewItem> infoData = new ArrayList<UnitViewItem>(0);
//		
//		if(CollectionUtils.isEmpty(data)){
//			return infoData;
//		}
//		
//		for(Map<String, Object> row : data){
//			UnitViewItem item = new UnitViewItem();
//			
//			if(qp.getIsBySite().equals(ReportConstants.SiteLevel.MAINSITE)){
//				//主域
//				item.setSiteUrl(String.valueOf(row.get(ReportConstants.MAINSITE)));
//			}else if(qp.getIsBySite().equals(ReportConstants.SiteLevel.SITE)){
//				//二级域
//				item.setSiteUrl(String.valueOf(row.get(ReportConstants.SITE)));
//			}else{
//				//计划、推广组信息
//				item.setPlanId((Integer) row.get(ReportConstants.PLAN));
//				item.setPlanName(String.valueOf(row.get(ReportConstants.PLANNAME)));
//				item.setPlanId((Integer) row.get(ReportConstants.GROUP));
//				item.setGroupName(String.valueOf(row.get(ReportConstants.GROUPNAME)));
//			}
//			item.setGroupState((Integer)row.get(ReportConstants.GROUP_STATE));
//			item.setUnitId((Long) row.get(ReportConstants.UNIT));
//			item.setTitle(String.valueOf(row.get(ReportConstants.UNIT_TITLE)));
//			item.setDescription1(String.valueOf(row.get(ReportConstants.UNIT_DESC1)));
//			item.setDescription2(String.valueOf(row.get(ReportConstants.UNIT_DESC2)));
//			item.setTargetUrl(String.valueOf(row.get(ReportConstants.UNIT_TARGETURL)));
//			item.setShowUrl(String.valueOf(row.get(ReportConstants.UNIT_SHOWURL)));
//			item.setWidth((Integer)row.get(ReportConstants.UNIT_WIDTH));
//			item.setHeight((Integer)row.get(ReportConstants.UNIT_HEIGHT));
//			item.setDateRange(row.get(ReportConstants.DATE_RANGE).toString());
//			item.setWuliaoType((Integer)row.get(ReportConstants.WULIAO_TYPE));
//			item.setWuliaoTypeName(String.valueOf(row.get(ReportConstants.UNIT_TYPE)));
//			item.setSize(String.valueOf(row.get(ReportConstants.UNIT_SIZE)));
//			item.setState((Integer)row.get(ReportConstants.STATUS));
//			item.setStateName(String.valueOf(row.get(ReportConstants.STATUS_NAME)));
//			
//			//填充基础统计数据
//			item.fillStatRecord(row);
//			
//			infoData.add(item);
//		}
//		
//		
//		return infoData;
//	}
//	
//	
//	public void fillFilterContent(QueryParameter qp, List<String[]> accountInfoList){
//		
//		List<CproGroup> groupFilterList = cproGroupMgr.findCproGroupByGroupIds(qp.getGroupIds());
//		List<CproPlan> planFilterList = cproPlanMgr.findCproPlanByPlanIds(qp.getPlanIds());
//				
//		List<List<String>> table;
//		//== 抬头：查询条件 ==
//		table = StatUtils.makeCustomReportFilterContent(planFilterList, groupFilterList);
//		for(List<String> row : table){
//			String [] strs= new String [row.size()];
//			
//			int i = 0;
//			for(String s : row){
//				strs[i] = s;
//				i++;
//			}
//			accountInfoList.add(strs);
//		}
//	}
//	
//	private List<Long> fetchUnitIdsByGroupId(QueryParameter qp){
//
//		Integer userId = qp.getUserId();
//		
//		Integer groupId = qp.getGroupIds().get(0);
//		List<Integer> groupIds = new ArrayList<Integer>(1);
//		groupIds.add(groupId);
//		
//		List<Long> unitIds = cproUnitMgr.findUnitIdsByGroupIds(userId, groupIds);
//		return unitIds;
//	}
//	
//	private void collectIds(List<Map<String, Object>> data){
//		
//		if(data == null || data.size() == 0) return;
//		
//		planIdSet = new HashSet<Integer>();
//		groupIdSet = new HashSet<Integer>();
//		unitIdSet = new HashSet<Long>();
//		
//		for(Map<String, Object> row : data){
//			
//			Integer planId = (Integer) row.get(ReportConstants.PLAN);
//			Integer groupId = (Integer) row.get(ReportConstants.GROUP);
//			Long unitId = (Long) row.get(ReportConstants.UNIT);
//			if(planId != null){
//				planIdSet.add(planId);
//			}
//			if(groupId != null){
//				groupIdSet.add(groupId);
//			}
//			if(unitId != null){
//				unitIdSet.add(unitId);
//			}
//			
//		}
//		
//	}
//	
//	protected String getCproUnitStatusName(UnitInfoView unit){
//		if(unit==null)
//			return "未知";
//		int state = unit.getStateView().getViewState();
//		return CproUnitConstant.UNIT_STATE_NAME[state];
//	}
//
//	private Map<String, Object> insertUnitInfo(Map<String, Object> row, UnitInfoView view){
//		if(view == null) return row;
//		
//		int type = view.getWuliaoType();
//		
//		row.put(ReportConstants.UNIT_TYPE, CproUnitConstant.WULIAO_TYPE_NAME[type]);
//		row.put(ReportConstants.WULIAO_TYPE, type);
//		row.put(ReportConstants.UNIT_TITLE, view.getTitle());
//		
//		//出文字推广单元外，描述字段为"N/A"
//		if(type == CproUnitConstant.MATERIAL_TYPE_LITERAL || type == CproUnitConstant.MATERIAL_TYPE_LITERAL_WITH_ICON){
//			row.put(ReportConstants.UNIT_DESC1, view.getDescription1());
//			row.put(ReportConstants.UNIT_DESC2, view.getDescription2());
//		}else{
//			row.put(ReportConstants.UNIT_DESC1, ReportConstants.FIELD_NA);
//			row.put(ReportConstants.UNIT_DESC2, ReportConstants.FIELD_NA);
//		}
//		row.put(ReportConstants.UNIT_TARGETURL, view.getTargetUrl());
//		row.put(ReportConstants.UNIT_SHOWURL, view.getShowUrl());
//		row.put(ReportConstants.UNIT_WIDTH, view.getWidth());
//		row.put(ReportConstants.UNIT_HEIGHT, view.getHeight());
//		row.put(ReportConstants.UNIT_SIZE, ReportFieldFormatter.formatImageSize(view.getWidth(), view.getHeight()));
//		
//		return row;
//	}
//	
//
//	public CproPlanMgr getCproPlanMgr() {
//		return cproPlanMgr;
//	}
//
//	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
//		this.cproPlanMgr = cproPlanMgr;
//	}
//
//	public CproGroupMgr getCproGroupMgr() {
//		return cproGroupMgr;
//	}
//
//	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
//		this.cproGroupMgr = cproGroupMgr;
//	}
//
//	public CproUnitMgr getCproUnitMgr() {
//		return cproUnitMgr;
//	}
//
//	public void setCproUnitMgr(CproUnitMgr cproUnitMgr) {
//		this.cproUnitMgr = cproUnitMgr;
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
//	public StatService2 getStatMgr() {
//		return statMgr;
//	}
//
//	public void setStatMgr(StatService2 statMgr) {
//		this.statMgr = statMgr;
//	}
//
//	public MultiUnitSiteStatSupport getStatService() {
//		return statService;
//	}
//
//	public void setStatService(MultiUnitSiteStatSupport statService) {
//		this.statService = statService;
//	}
//	
//}
