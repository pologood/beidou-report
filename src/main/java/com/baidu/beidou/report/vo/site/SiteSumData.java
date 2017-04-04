package com.baidu.beidou.report.vo.site;

import com.baidu.beidou.report.vo.SumData;

public class SiteSumData extends SumData{
	public SiteSumData(){
		super();
	}
	
	public SiteSumData(int count, int needFrontSort){
		this.count = count;
		this.needFrontSort = needFrontSort;
	}
	
	private int count = 0;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
