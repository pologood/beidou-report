package com.baidu.beidou.report.vo;

import java.io.Serializable;
import java.util.List;

/**
 * <p>ClassName:ReportAccountInfo
 * <p>Function: 下载CSV时的账户汇总信息
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-9
 */
public class ReportAccountInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	/** 报告 */
	protected String report;
	/** 文字“报告” */
	protected String reportText;
	/** 帐户 */
	protected String account;
	/** 文字“帐户” */
	protected String accountText;
	/** 日期范围 */
	protected String dateRange;
	/** 文字"日期范围" */
	protected String dateRangeText;
	/** 所属层级 */
	protected String level;
	/** 文字"所属层级" */
	protected String levelText;
	
	/**是否为定制报告*/
	protected boolean isCustomReport = false;
	/** 定制报告使用--报告账户信息List--String[0]=列名;String[1]=对应的值*/
	protected List<String[]> accountInfoList ;
	
	public String getReport() {
		return report;
	}
	public void setReport(String report) {
		this.report = report;
	}
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}
	public String getDateRange() {
		return dateRange;
	}
	public void setDateRange(String dateRange) {
		this.dateRange = dateRange;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public String getReportText() {
		return reportText;
	}
	public void setReportText(String reportText) {
		this.reportText = reportText;
	}
	public String getAccountText() {
		return accountText;
	}
	public void setAccountText(String accountText) {
		this.accountText = accountText;
	}
	public String getDateRangeText() {
		return dateRangeText;
	}
	public void setDateRangeText(String dateRangeText) {
		this.dateRangeText = dateRangeText;
	}
	public String getLevelText() {
		return levelText;
	}
	public void setLevelText(String levelText) {
		this.levelText = levelText;
	}
	
	public List<String[]> getAccountInfoList() {
		return accountInfoList;
	}
	/**
	 * 只有定制报告使用
	 * @param accountInfoList
	 */
	public void setAccountInfoList(List<String[]> accountInfoList) {
		isCustomReport = true;
		this.accountInfoList = accountInfoList;
	}
	public boolean isCustomReport() {
		return isCustomReport;
	}
	public void setCustomReport(boolean isCustomReport) {
		this.isCustomReport = isCustomReport;
	}
	
	
}
