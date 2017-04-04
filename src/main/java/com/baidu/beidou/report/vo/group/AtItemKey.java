package com.baidu.beidou.report.vo.group;

/**
 * 用作AT词Tab表格数据的key
 * 
 * @author wangchongjie
 * @fileName AtItemKey.java
 * @dateTime 2014-4-12 下午4:55:16
 */
public class AtItemKey {
	private Integer groupId;
	private Long atId;
	private Integer atType;
	

	public AtItemKey(Integer groupId, Long atId, Integer atType) {
		if (groupId != null) {
			this.groupId = groupId;
		} else {
			this.groupId = 0;
		}
		
		if (atId != null) {
			this.atId = atId;
		} else {
			this.atId = 0L;
		}
		
		if (atType != null) {
			this.atType = atType;
		} else {
			this.atType = 0;
		}
	}
	
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof AtItemKey)) {
			return false;
		}

		AtItemKey castOther = (AtItemKey) other;
		
		if (this.groupId != null && this.groupId.equals(castOther.getGroupId())
				&& this.atId != null && this.atId.equals(castOther.getAtId())
				&& this.atType != null && this.atType.equals(castOther.getAtType())) {
			return true;
		}

		return false;
	}
	
	public int hashCode() {
		int result = 17;

		result = 37 * result + (atId == null ? 0 : atId.hashCode());
		return result;
	}
	
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Long getAtId() {
		return atId;
	}

	public void setAtId(Long atId) {
		this.atId = atId;
	}

	public Integer getAtType() {
		return atType;
	}

	public void setAtType(Integer atType) {
		this.atType = atType;
	}
}
