package com.baidu.beidou.report.vo.group;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.report.vo.StatInfo;


public class GroupQtViewItem extends StatInfo{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2316664582834495259L;
	
	/** 计划ID */
	private Integer planId;
	/** 计划名 */
	private String planName;
	/** 组ID */
	private Integer groupId;
	/** 组名 */
	private String groupName;
	/** beidou,keywordId */
	private Integer keywordId;
	/** 字面 */
	private String keyword;
	/** atomId */
	private Integer wordId ;
	/** 是否已经删除，
	 * true表示删除，false表示未删除； 
	 * true時keywordId為null； 
	 * */
	private boolean hasDel = false;
	
	/**
	 * 关键词质量度：1－1星，2－2星，3－3星,默认3星
	 */
	private Integer qualityDg = CproGroupConstant.KT_WORD_QUALITY_DEGREE_3;
	
	public GroupQtViewItem(){
		super();
	}
	
	public Integer getPlanId() {
		return planId;
	}
	public void setPlanId(Integer planId) {
		this.planId = planId;
	}
	public String getPlanName() {
		return planName;
	}
	public void setPlanName(String planName) {
		this.planName = planName;
	}
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public Integer getKeywordId() {
		return keywordId;
	}
	public void setKeywordId(Integer keywordId) {
		this.keywordId = keywordId;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public Integer getWordId() {
		return wordId;
	}
	public void setWordId(Integer wordId) {
		this.wordId = wordId;
	}

	public boolean isHasDel() {
		return hasDel;
	}

	public void setHasDel(boolean hasDel) {
		this.hasDel = hasDel;
	}

	public Integer getQualityDg() {
		return qualityDg;
	}

	public void setQualityDg(Integer qualityDg) {
		this.qualityDg = qualityDg;
	}
}
