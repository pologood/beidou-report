package com.baidu.beidou.report.vo.site;

import com.baidu.beidou.report.vo.SumData;

public class TradeSumData extends SumData{
	private int firstTradeCount;
	private int secondTradeCount;
	
	public TradeSumData(){
		
	}
	
	public TradeSumData(int firstTradeCount, int secondTradeCount, int needFrontSort){
		this.firstTradeCount = firstTradeCount;
		this.secondTradeCount = secondTradeCount;
		this.needFrontSort = needFrontSort;
	}

	public int getFirstTradeCount() {
		return firstTradeCount;
	}

	public void setFirstTradeCount(int firstTradeCount) {
		this.firstTradeCount = firstTradeCount;
	}

	public int getSecondTradeCount() {
		return secondTradeCount;
	}

	public void setSecondTradeCount(int secondTradeCount) {
		this.secondTradeCount = secondTradeCount;
	}
}
