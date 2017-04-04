package com.baidu.beidou.report.vo;

import com.baidu.beidou.report.ReportConstants;

public class SumData extends StatInfo{

	/** 是否需要前端排序，前端规则为返回条数<=1W */
	protected int needFrontSort = ReportConstants.Boolean.FALSE;

	public int getNeedFrontSort() {
		return needFrontSort;
	}

	public void setNeedFrontSort(int needFrontSort) {
		this.needFrontSort = needFrontSort;
	}
	
}
