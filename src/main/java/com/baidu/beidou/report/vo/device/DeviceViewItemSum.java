package com.baidu.beidou.report.vo.device;

import com.baidu.beidou.report.vo.StatInfo;

public class DeviceViewItemSum extends StatInfo {

	private static final long serialVersionUID = -293301590337637843L;
	
	private Integer deviceCount;
	

	
	public Integer getDeviceCount() {
		return deviceCount;
	}

	public void setDeviceCount(Integer deviceCount) {
		this.deviceCount = deviceCount;
	}

	public DeviceViewItemSum(){
	}

	public DeviceViewItemSum(Integer deviceCount){
		this.deviceCount = deviceCount;
	}

	
}
