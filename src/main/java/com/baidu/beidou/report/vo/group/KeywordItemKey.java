package com.baidu.beidou.report.vo.group;

/**
 * ClassName: KeywordItemKey
 * Function: 用作关键词Tab表格数据的key
 *
 * @author genglei
 * @version beidou 3 plus
 * @date 2012-8-27
 */
public class KeywordItemKey {
	private Integer groupId;
	private Long gpId;
	private Long wordId;
	
	public KeywordItemKey(Integer groupId, Long gpId, Long wordId) {
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
		
		if (wordId != null) {
			this.wordId = wordId;
		} else {
			this.wordId = 0L;
		}
	}
	
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof KeywordItemKey)) {
			return false;
		}

		KeywordItemKey castOther = (KeywordItemKey) other;
		
		if (this.groupId != null && this.groupId.equals(castOther.getGroupId())
				&& this.gpId != null && this.gpId.equals(castOther.getGpId())
				&& this.wordId != null && this.wordId.equals(castOther.getWordId())) {
			return true;
		}

		return false;
	}
	
	public int hashCode() {
		int result = 17;

		result = 37 * result + (wordId == null ? 0 : wordId.hashCode());
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
	public Long getWordId() {
		return wordId;
	}
	public void setWordId(Long wordId) {
		this.wordId = wordId;
	}
}
