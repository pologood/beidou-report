package com.baidu.beidou.report.vo.attach;

import com.baidu.beidou.report.vo.group.GroupSumData;



public class AttachReportSumData extends GroupSumData {

	private static final long serialVersionUID = -8084327718332124387L;
	
	/** 文字“汇总” */
	protected String summaryText;
	
	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
}
