//package com.baidu.beidou.report.facade.impl;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.baidu.beidou.cprogroup.bo.CproGroup;
//import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
//import com.baidu.beidou.cprogroup.service.CproGroupMgr;
//import com.baidu.beidou.cproplan.bo.CproPlan;
//import com.baidu.beidou.cproplan.constant.CproPlanConstant;
//import com.baidu.beidou.cproplan.service.CproPlanMgr;
//import com.baidu.beidou.cprounit.constant.CproUnitConstant;
//import com.baidu.beidou.cprounit.vo.UnitInfoView;
//import com.baidu.beidou.olap.vo.GroupViewItem;
//import com.baidu.beidou.olap.vo.PlanViewItem;
//import com.baidu.beidou.olap.vo.UserAccountViewItem;
//import com.baidu.beidou.report.ReportConstants;
//import com.baidu.beidou.report.facade.StandardCustomReportFacade;
//import com.baidu.beidou.report.vo.QueryParameter;
//import com.baidu.beidou.report.vo.StandardCustomReportVo;
//import com.baidu.beidou.stat.bean.OrderCriterion;
//import com.baidu.beidou.stat.driver.bo.Constant;
//import com.baidu.beidou.stat.facade.ReportFacade;
//import com.baidu.beidou.stat.service.StatService2;
//import com.baidu.beidou.stat.util.StatUtils;
//import com.baidu.beidou.user.bo.User;
//import com.baidu.beidou.user.constant.UserConstant;
//import com.baidu.beidou.user.service.UserMgr;
//import com.baidu.beidou.util.StateConvertUtils;
//
//public class StandardCustomReportFacadeImpl implements StandardCustomReportFacade{
//	
//	private UserMgr userMgr;	
//	private CproPlanMgr cproPlanMgr;	
//	private CproGroupMgr cproGroupMgr;
//	private StatService2 statMgr;
//	private ReportFacade reportFacade;
//	
//	private List<Map<String, Object>> groupData = new ArrayList<Map<String,Object>>(0);
//	private List<Map<String, Object>> planData = new ArrayList<Map<String,Object>>(0);
//	private List<Map<String, Object>> accountData = new ArrayList<Map<String,Object>>(0);
//	private List<Integer> groupIds = new ArrayList<Integer>(0);
//	private List<Integer> planIds = new ArrayList<Integer>(0);
//	
//	
//	public StandardCustomReportVo fetchStandardReportData(QueryParameter qp, Date from, Date to) {
//
//		StandardCustomReportVo vo = new StandardCustomReportVo();
//		
//		//step1:获取doris统计数据
//		this.fetchStatData(qp, from, to);
//		
//		//step2:merge db中的信息
//		this.mergeInfoData(qp);
//		
//		//step3:排序结果数据
//		this.sortData(qp);
//		
//		//step4:转化为前端使用的vo
//		vo = this.translateData(qp);
//		
//		return vo;
//	}
//
//	
//	private void fetchStatData(QueryParameter qp, Date from, Date to) {
//		
//		groupData = statMgr.queryGroupData(qp.getUserId(),
//				null, null, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT);
//		
//		if(groupData.size() > 0){
//			planData = rollupToPlan();
//			accountData = rollupToAccount();			
//		}
//	}
//
//	private void mergeInfoData(QueryParameter qp) {
//		
//		//无推广计划||推广组||账户信息则直接返回
//		if(groupData.size() == 0 || planData.size() == 0 || accountData.size() == 0){
//			return;
//		}
//		
//		Map<Integer, CproGroup> groupMap = new HashMap<Integer, CproGroup>(0);
//		Map<Integer, CproPlan> planMap = new HashMap<Integer, CproPlan>(0);
//		
//		//===基础数据准备===	
//		groupMap = StatUtils.converGroupListToMap(cproGroupMgr.findCproGroupByGroupIds(groupIds));
//		planMap = StatUtils.converPlanListToMap(cproPlanMgr.findCproPlanByPlanIds(planIds));
//		User user = userMgr.findUserBySFid(qp.getUserId());
//		
//		//1、merge推广组信息
//		for(Map<String, Object> row : groupData){
//			//推广计划ID转NAME
//			Integer planId = (Integer) row.get(ReportConstants.PLAN);
//			row.put(ReportConstants.PLANNAME, planMap.get(planId).getPlanName());
//			//推广组ID转NAME
//			Integer groupId = (Integer) row.get(ReportConstants.GROUP);
//			row.put(ReportConstants.GROUPNAME, groupMap.get(groupId).getGroupName());
//			//推广组状态
//			row.put(ReportConstants.STATUS, StateConvertUtils.convertGroupToStateView(
//					groupMap.get(groupId), 
//					planMap.get(planId)));			
//		}
//		
//		//2、merge推广计划信息
//		for(Map<String, Object> row : planData){	
//			//1、推广计划ID转NAME
//			Integer planId = (Integer) row.get(ReportConstants.PLAN);
//			row.put(ReportConstants.PLANNAME, planMap.get(planId).getPlanName());
//			//2、推广计划状态
//			row.put(ReportConstants.STATUS, StateConvertUtils.convertPlanToStateView(planMap.get(planId)));			
//		}
//		
//		//3、merge帐户信息
//		for(Map<String, Object> row : accountData){
//			//1、帐户INAME
//			row.put(ReportConstants.ACCOUNT, user==null?"客户":user.getUsername());
//			//2、状态状态
//			row.put(ReportConstants.STATUS, StateConvertUtils.convertUserToStateView(user));
//		}
//	}
//	
//	private void sortData(QueryParameter qp) {
//		
//		List<OrderCriterion> orderCriteria = new ArrayList<OrderCriterion>();
//		//默认：推广计划，按推广计划名正排
//		orderCriteria.add(new OrderCriterion(ReportConstants.PLAN, ReportConstants.SortOrder.ASC));
//		planData = reportFacade.sortData(planData, orderCriteria);
//		
//		//默认：推广计划，按推广计划名正排、推广组名正排
//		orderCriteria.clear();
//		orderCriteria.add(new OrderCriterion(ReportConstants.PLAN, ReportConstants.SortOrder.ASC));
//		orderCriteria.add(new OrderCriterion(ReportConstants.GROUP, ReportConstants.SortOrder.ASC));
//		groupData = reportFacade.sortData(groupData, orderCriteria);
//	}
//	
//	private StandardCustomReportVo translateData(QueryParameter qp){
//		
//		StandardCustomReportVo vo = new StandardCustomReportVo();
//		
//		//无推广计划||推广组||账户信息则直接返回
//		if(groupData.size() == 0 || planData.size() == 0 || accountData.size() == 0){
//			return vo;
//		}
//		
//		List<UserAccountViewItem> userLevelDetails = new ArrayList<UserAccountViewItem>();
//		List<PlanViewItem> planLevelDetails = new ArrayList<PlanViewItem>();
//		List<GroupViewItem> groupLevelDetails = new ArrayList<GroupViewItem>();
//		
//		//将group层级的list<Map>转化为List<obj>
//		for (Map<String, Object> row : groupData) {
//			GroupViewItem item = new GroupViewItem();
//			if (row != null) {
//				item.fillStatRecord(row);
//				item.setDateRange((String) row.get(ReportConstants.DATE_RANGE));
//				item.setGroupId((Integer) row.get(ReportConstants.GROUP));
//				item.setGroupName((String) row.get(ReportConstants.GROUPNAME));
//				item.setPlanId((Integer) row.get(ReportConstants.PLAN));
//				item.setPlanName((String) row.get(ReportConstants.PLANNAME));
//				item.setViewState((Integer) row.get(ReportConstants.STATUS));
//				groupLevelDetails.add(item);
//			}
//		}
//		//将计划层级的list<Map>转化为List<obj>
//		for (Map<String, Object> row : planData) {
//			PlanViewItem planItem = new PlanViewItem();
//			if (row != null) {
//				planItem.fillStatRecord(row);
//				planItem.setDateRange((String) row.get(ReportConstants.DATE_RANGE));
//				planItem.setPlanId((Integer) row.get(ReportConstants.PLAN));
//				planItem.setPlanName((String) row.get(ReportConstants.PLANNAME));
//				planItem.setViewState((Integer) row.get(ReportConstants.STATUS));
//				planLevelDetails.add(planItem);
//			}
//		}
//		//将账户层级的list<Map>转化为List<obj>
//		for (Map<String, Object> row : accountData) {
//			UserAccountViewItem accountItem = new UserAccountViewItem();
//			if (row != null) {
//				accountItem.fillStatRecord(row);
//				accountItem.setUserName((String) row.get(ReportConstants.ACCOUNT));
//				accountItem.setViewState((Integer)row.get(ReportConstants.STATUS));
//				userLevelDetails.add(accountItem);
//			}
//		}
//		
//		vo.setUserLevelDetails(userLevelDetails);
//		vo.setPlanLevelDetails(planLevelDetails);
//		vo.setGroupLevelDetails(groupLevelDetails);
//		return vo;
//	}
//
//	
//	private List<Map<String, Object>> rollupToPlan(){
//		
//		groupIds = new ArrayList<Integer>(groupData.size());		//TODO: id应该用set来去重
//		planData = new ArrayList<Map<String, Object>>();
//		
//		//需叠加的项
//		long srchs = 0l; 
//		long clks = 0l;
//		long cost = 0l;
//		
//		Integer currPlanId = 0;
//		
//		for(int i = 0; i < groupData.size(); i++){
//			
//			Map<String, Object> row = groupData.get(i);
//			
//			//收集groupIds
//			groupIds.add((Integer)row.get(ReportConstants.GROUP)); //check casting	
//			
//			Integer planId = (Integer)row.get(ReportConstants.PLAN);
//			
//			if(i == 0){
//				currPlanId = planId;
//			}else if(!currPlanId.equals(planId)){
//				//包装一行新的
//				Map<String, Object> newRow = makeDataRow(null, currPlanId, null, srchs, clks, cost, null, null, false);
//				planData.add(newRow);
//				//清空累加值
//				srchs = 0l;
//				clks = 0l;
//				cost = 0l;
//				//更新当前推广计划id
//				currPlanId = planId;
//			}
//			
//			Long oSrchs = ((Long) row.get(ReportConstants.SRCHS));
//			Long oClks = ((Long) row.get(ReportConstants.CLKS));
//			Long oCost = ((Long) row.get(ReportConstants.COST));
//			
//			srchs += (oSrchs == null ? 0 : oSrchs.longValue());
//			clks += (oClks == null ? 0 : oClks.longValue());
//			cost += (oCost == null ? 0 : oCost.longValue());
//	
//		}
//		Map<String, Object> lastRow = makeDataRow(null, currPlanId, null, srchs, clks, cost, null, null, false);
//		planData.add(lastRow);
//		
//		return planData;
//	}
//	
//	private List<Map<String, Object>> rollupToAccount(){
//		
//		if(planData == null) return null;
//		
//		planIds = new ArrayList<Integer>(planData.size());
//		accountData = new ArrayList<Map<String,Object>>(1);
//		
//		long srchs = 0l;
//		long clks = 0l;
//		long cost = 0l;
//		
//		for(Map<String, Object> row : planData){
//			
//			//收集planIds
//			planIds.add((Integer)row.get(ReportConstants.PLAN));
//			
//			srchs += (Long) row.get(ReportConstants.SRCHS);
//			clks += (Long) row.get(ReportConstants.CLKS);
//			cost += (Long) row.get(ReportConstants.COST);
//		}
//		
//		Map<String, Object> newRow = makeDataRow(null, null, null, srchs, clks, cost, null, null, false);
//		
//		accountData.add(newRow);
//		
//		return accountData;
//	}
//	
//	private Map<String, Object> makeDataRow(
//			Integer userId,
//			Integer planId, 
//			Integer groupId,
//			Long srchs, 
//			Long clks, 
//			Long cost,
//			Long budget,
//			Long price,
//			boolean isTotal){
//		
//		Map<String, Object> row = new HashMap<String, Object>();
//		
//		if(userId != null)
//			row.put(ReportConstants.USER, userId);
//		if(planId != null)
//			row.put(ReportConstants.PLAN, planId);
//		if(groupId != null)
//			row.put(ReportConstants.GROUP, groupId);
//		if(srchs != null)
//			row.put(ReportConstants.SRCHS, srchs);
//		if(clks != null)
//			row.put(ReportConstants.CLKS, clks);
//		if(cost != null)
//			row.put(ReportConstants.COST, cost); //单位：分；通过ReportFieldFormatter进行单位的转换
//		if(budget != null)
//			row.put(ReportConstants.BUDGET, budget); //单位：元；
//		if(price != null)
//			row.put(ReportConstants.PRICE, price); //单位：分；通过ReportFieldFormatter进行单位的转换
//		
//		//计算行
//		if(clks != null && srchs != null && srchs > 0)
//			row.put(ReportConstants.CTR, new Double(clks*1.0/srchs)); 
//		if(cost != null && clks != null && clks > 0)
//			row.put(ReportConstants.ACP, new Double(cost*0.01/clks)); //单位：分转元
//		if(cost != null && srchs != null && srchs > 0)
//			row.put(ReportConstants.CPM, new Double(cost*10.0/srchs)); //单位：分转元；per千次展现
//		
//		if(isTotal)
//			row.put(ReportConstants.TOTAL, ""); //最后的显示样式，由PackageData负责解释
//		
//		return row;
//	}
//	
//	protected String getCproGroupStatusName(CproGroup group, CproPlan plan){
//		int state = StateConvertUtils.convertGroupToStateView(group, plan);
//		return CproGroupConstant.GROUP_VIEW_STATE_NAME[state];
//	}
//	
//	protected String getCproPlanStatusName(CproPlan plan){
//		if(plan==null)
//			return "未知";
//		int state = StateConvertUtils.convertPlanToStateView(plan);
//		return CproPlanConstant.PLAN_VIEW_STATE_NAME[state];
//	}
//	
//	protected String getCproUnitStatusName(UnitInfoView unit){
//		if(unit==null)
//			return "未知";
//		int state = unit.getStateView().getViewState();
//		return CproUnitConstant.UNIT_STATE_NAME[state];
//	}
//	
//	protected String getUserAccountStatusName(User user){
//		int state = StateConvertUtils.convertUserToStateView(user);
//		return UserConstant.USER_VIEW_STATE_NAME[state];
//	}
//
//
//	public UserMgr getUserMgr() {
//		return userMgr;
//	}
//
//
//	public CproPlanMgr getCproPlanMgr() {
//		return cproPlanMgr;
//	}
//
//
//	public CproGroupMgr getCproGroupMgr() {
//		return cproGroupMgr;
//	}
//
//
//	public StatService2 getStatMgr() {
//		return statMgr;
//	}
//
//
//	public ReportFacade getReportFacade() {
//		return reportFacade;
//	}
//
//
//	public void setUserMgr(UserMgr userMgr) {
//		this.userMgr = userMgr;
//	}
//
//
//	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
//		this.cproPlanMgr = cproPlanMgr;
//	}
//
//
//	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
//		this.cproGroupMgr = cproGroupMgr;
//	}
//
//
//	public void setStatMgr(StatService2 statMgr) {
//		this.statMgr = statMgr;
//	}
//
//
//	public void setReportFacade(ReportFacade reportFacade) {
//		this.reportFacade = reportFacade;
//	}
//	
//}
