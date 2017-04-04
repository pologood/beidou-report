package com.baidu.beidou.report.vo.app;

import com.baidu.beidou.report.vo.StatInfo;

public class AppViewItemSum extends StatInfo {
	private static final long serialVersionUID = -852467456190892073L;
	private Integer appCount;
	
	public Integer getAppCount() {
		return appCount;
	}
	public void setAppCount(Integer appCount) {
		this.appCount = appCount;
	}
	
	public AppViewItemSum(){
	}

	public AppViewItemSum(Integer appCount){
		this.appCount = appCount;
	}

	
}
