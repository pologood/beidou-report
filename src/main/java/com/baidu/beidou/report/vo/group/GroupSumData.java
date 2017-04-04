package com.baidu.beidou.report.vo.group;

import com.baidu.beidou.report.vo.StatInfo;

public class GroupSumData extends StatInfo {
	
	/** 列表中总推广组个数 */
	protected int groupCount = 0;
	
	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}
}
