package com.baidu.beidou.report.vo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.util.CollectionsUtil;

public class StandardCustomReportVo extends AbstractReportVo {
	
	private static Log LOG = LogFactory.getLog(StandardCustomReportVo.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -4325401473728787665L;
	
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	
	/** 账户层级列头 */
	protected String userLevelReportName; 
	/** 计划层级列头 */
	protected String planLevelReportName; 
	/** 推广组层级列头 */
	protected String groupLevelReportName; 
	
	/** 账户层级列头 */
	protected String[] userLevelHeaders; 
	/** 计划层级列头 */
	protected String[] planLevelHeaders; 
	/** 推广组层级列头 */
	protected String[] groupLevelHeaders; 
	
	/** 账户明细 */
	protected List<UserAccountViewItem> userLevelDetails;
	/** 计划明细 */
	protected List<PlanViewItem> planLevelDetails;
	/** 推广组明细 */
	protected List<GroupViewItem> groupLevelDetails;
	
	public List<String[]> getCsvUserLevelDetail() {
		
		List<String[]> result = new ArrayList<String[]>();
		if(CollectionUtils.isEmpty(userLevelDetails)){
			return result;
		}
		
		String[] detailStringArray;
		for (UserAccountViewItem item : userLevelDetails) {
			int index = 0;
			detailStringArray = new String[userLevelHeaders.length];
			detailStringArray[index] = item.getUserName();//账户名
			detailStringArray[++index] = UserConstant.USER_VIEW_STATE_NAME[item.getViewState()];//当前状态
			detailStringArray[++index] = "" + item.getSrchs();// 展现次数
			detailStringArray[++index] = "" + item.getClks(); // 点击次数
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0) {// 点击率
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0) {// 平均点击价格
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0) {// 千次展现成本
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}
			detailStringArray[++index] = "" + item.getCost(); // 总费用
			result.add(detailStringArray);
		}
		return result;
	}
	
	public List<String[]> getCsvPlanLevelDetail() {
		
		List<String[]> result = new ArrayList<String[]>();
		if(CollectionUtils.isEmpty(planLevelDetails)){
			return result;
		}
		
		String[] detailStringArray;
		for (PlanViewItem item : planLevelDetails) {
			detailStringArray = new String[planLevelHeaders.length];
			int index = 0;
			detailStringArray[index] = item.getPlanName();// 推广计划名
			detailStringArray[++index] = CproPlanConstant.PLAN_VIEW_STATE_NAME[item.getViewState()];// 推广计划状态

			if (item.getSrchs() < 0) {// 展现次数
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = "" + item.getSrchs();
			}
			detailStringArray[++index] = "" + item.getClks(); // 点击次数
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0) {// 点击率
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0) {// 平均点击价格
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0) {// 千次展现成本
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}
			detailStringArray[++index] = "" + item.getCost(); // 总费用

			result.add(detailStringArray);
		}
		return result;
	}
	
	public List<String[]> getCsvGroupLevelDetail() {

		List<String[]> result = new ArrayList<String[]>();
		if(CollectionUtils.isEmpty(groupLevelDetails)){
			return result;
		}
		
		String[] detailStringArray;
		for (GroupViewItem item : groupLevelDetails) {
			int index = 0;
			detailStringArray = new String[groupLevelHeaders.length];
			detailStringArray[index] = item.getPlanName();// 推广计划名
			detailStringArray[++index] = item.getGroupName();// 推广组名
			detailStringArray[++index] = CproGroupConstant.GROUP_VIEW_STATE_NAME[item.getViewState()];// 推广组状态
			
			if (item.getSrchs() < 0) {// 展现次数
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = "" + item.getSrchs();
			}
			detailStringArray[++index] = "" + item.getClks(); // 点击次数
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0) {// 点击率
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0) {// 平均点击价格
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0) {// 千次展现成本
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}
			detailStringArray[++index] = "" + item.getCost(); // 总费用

			result.add(detailStringArray);
		}
		return result;
	}
	
	public List<String[]> getCsvReportAccountInfo() {
		List<String[]> result = new ArrayList<String[]>();
		
		String[] textValuePair = new String[2];

		textValuePair = new String[2];
		textValuePair[0] = accountInfo.getDateRange();
		result.add(textValuePair);
		
		return result;
	}

	public String[] getCsvUserLevelHeader() {
		
		return userLevelHeaders;
	}
	
	public String[] getCsvPlanLevelHeader() {
		
		return planLevelHeaders;
	}
	
	public String[] getCsvGroupLevelHeader() {
		
		return groupLevelHeaders;
	}

	
	/****** getter and setter ******/
	public static void main(String[] args) {
		System.out.println("");
	}

	public ReportAccountInfo getAccountInfo() {
		return accountInfo;
	}

	public String getUserLevelReportName() {
		return userLevelReportName;
	}

	public String getPlanLevelReportName() {
		return planLevelReportName;
	}

	public String getGroupLevelReportName() {
		return groupLevelReportName;
	}

	public String[] getUserLevelHeaders() {
		return userLevelHeaders;
	}

	public String[] getPlanLevelHeaders() {
		return planLevelHeaders;
	}

	public String[] getGroupLevelHeaders() {
		return groupLevelHeaders;
	}

	public List<UserAccountViewItem> getUserLevelDetails() {
		return userLevelDetails;
	}

	public List<PlanViewItem> getPlanLevelDetails() {
		return planLevelDetails;
	}

	public List<GroupViewItem> getGroupLevelDetails() {
		return groupLevelDetails;
	}

	public void setAccountInfo(ReportAccountInfo accountInfo) {
		this.accountInfo = accountInfo;
	}

	public void setUserLevelReportName(String userLevelReportName) {
		this.userLevelReportName = userLevelReportName;
	}

	public void setPlanLevelReportName(String planLevelReportName) {
		this.planLevelReportName = planLevelReportName;
	}

	public void setGroupLevelReportName(String groupLevelReportName) {
		this.groupLevelReportName = groupLevelReportName;
	}

	public void setUserLevelHeaders(String[] userLevelHeaders) {
		this.userLevelHeaders = userLevelHeaders;
	}

	public void setPlanLevelHeaders(String[] planLevelHeaders) {
		this.planLevelHeaders = planLevelHeaders;
	}

	public void setGroupLevelHeaders(String[] groupLevelHeaders) {
		this.groupLevelHeaders = groupLevelHeaders;
	}

	public void setUserLevelDetails(List<UserAccountViewItem> userLevelDetails) {
		this.userLevelDetails = userLevelDetails;
	}

	public void setPlanLevelDetails(List<PlanViewItem> planLevelDetails) {
		this.planLevelDetails = planLevelDetails;
	}

	public void setGroupLevelDetails(List<GroupViewItem> groupLevelDetails) {
		this.groupLevelDetails = groupLevelDetails;
	}

	@Override
	@Deprecated
	public List<String[]> getCsvReportDetail() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public String[] getCsvReportHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public String[] getCsvReportSummary() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
