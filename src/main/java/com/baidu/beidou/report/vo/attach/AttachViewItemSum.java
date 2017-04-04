package com.baidu.beidou.report.vo.attach;

import com.baidu.beidou.report.vo.StatInfo;

public class AttachViewItemSum extends StatInfo {
	
	private static final long serialVersionUID = -7524418258609578409L;
	
	private Integer attachCount;
	
	
	public Integer getAttachCount() {
		return attachCount;
	}

	public void setAttachCount(Integer attachCount) {
		this.attachCount = attachCount;
	}

	public AttachViewItemSum(){
	}

	public AttachViewItemSum(Integer attachCount){
		this.attachCount = attachCount;
	}

	
}
