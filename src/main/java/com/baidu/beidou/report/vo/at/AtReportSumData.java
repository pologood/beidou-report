package com.baidu.beidou.report.vo.at;

import com.baidu.beidou.report.vo.group.GroupSumData;



public class AtReportSumData extends GroupSumData {

	private static final long serialVersionUID = -6319218878525869462L;
	
	/** 文字“汇总” */
	protected String summaryText;
	
	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
}
