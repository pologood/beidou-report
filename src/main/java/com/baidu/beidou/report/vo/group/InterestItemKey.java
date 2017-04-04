package com.baidu.beidou.report.vo.group;

public class InterestItemKey {
	private Integer groupId;
	private Long gpId;
	private Integer interestId;
	
	public InterestItemKey(Integer groupId, Long gpId, Integer interestId) {
		if (groupId != null) {
			this.groupId = groupId;
		} else {
			this.groupId = 0;
		}
		
		if (gpId != null) {
			this.gpId = gpId;
		} else {
			this.gpId = 0L;
		}
		
		if (interestId != null) {
			this.interestId = interestId;
		} else {
			this.interestId = 0;
		}
	}
	
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof InterestItemKey)) {
			return false;
		}

		InterestItemKey castOther = (InterestItemKey) other;
		
		if (this.groupId != null && this.groupId.equals(castOther.getGroupId())
				&& this.gpId != null && this.gpId.equals(castOther.getGpId())
				&& this.interestId != null && this.interestId.equals(castOther.getInterestId())) {
			return true;
		}

		return false;
	}
	
	public int hashCode() {
		int result = 17;

		result = 37 * result + (interestId == null ? 0 : interestId.hashCode());
		return result;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Long getGpId() {
		return gpId;
	}

	public void setGpId(Long gpId) {
		this.gpId = gpId;
	}

	public Integer getInterestId() {
		return interestId;
	}

	public void setInterestId(Integer interestId) {
		this.interestId = interestId;
	}
}
