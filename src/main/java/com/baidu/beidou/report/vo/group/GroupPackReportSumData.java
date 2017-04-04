package com.baidu.beidou.report.vo.group;

import com.baidu.beidou.olap.vo.GroupPackViewItem;

public class GroupPackReportSumData extends GroupPackViewItem {
	/** 文字“汇总” */
	protected String summaryText;
	
	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
}
