package com.baidu.beidou.report.vo.group;

/**
 * 用作AT词Tab表格数据的key
 * 
 * @author wangchongjie
 * @fileName AttachItemKey.java
 * @dateTime 2014-4-12 下午4:55:16
 */
public class AttachItemKey {
	private Integer groupId;
	private Long attachId;
	private Integer attachType;
	

	public AttachItemKey(Integer groupId, Long attachId, Integer attachType) {
		if (groupId != null) {
			this.groupId = groupId;
		} else {
			this.groupId = 0;
		}
		
		if (attachId != null) {
			this.attachId = attachId;
		} else {
			this.attachId = 0L;
		}
		
		if (attachType != null) {
			this.attachType = attachType;
		} else {
			this.attachType = 0;
		}
	}
	
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof AttachItemKey)) {
			return false;
		}

		AttachItemKey castOther = (AttachItemKey) other;
		
		if (this.groupId != null && this.groupId.equals(castOther.getGroupId())
				&& this.attachId != null && this.attachId.equals(castOther.getAttachId())
				&& this.attachType != null && this.attachType.equals(castOther.getAttachType())) {
			return true;
		}

		return false;
	}
	
	public int hashCode() {
		int result = 17;

		result = 37 * result + (attachId == null ? 0 : attachId.hashCode());
		return result;
	}
	
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Long getAttachId() {
		return attachId;
	}

	public void setAttachId(Long attachId) {
		this.attachId = attachId;
	}

	public Integer getAttachType() {
		return attachType;
	}

	public void setAttachType(Integer attachType) {
		this.attachType = attachType;
	}
}
