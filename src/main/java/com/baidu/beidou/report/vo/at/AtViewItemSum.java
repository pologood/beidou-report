package com.baidu.beidou.report.vo.at;

import com.baidu.beidou.report.vo.StatInfo;

public class AtViewItemSum extends StatInfo {
	
	private static final long serialVersionUID = 110920531749632246L;
	
	private Integer atCount;
	
	
	public Integer getAtCount() {
		return atCount;
	}

	public void setAtCount(Integer atCount) {
		this.atCount = atCount;
	}

	public AtViewItemSum(){
	}

	public AtViewItemSum(Integer atCount){
		this.atCount = atCount;
	}

	
}
