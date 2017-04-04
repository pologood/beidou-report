package com.baidu.beidou.report.vo;

import java.io.Serializable;

public class ReportSumData extends StatInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4730462431907758735L;

	private Long count = 0l;
	
	/** 文字“汇总” */
	private String summaryText;
	
	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

}
