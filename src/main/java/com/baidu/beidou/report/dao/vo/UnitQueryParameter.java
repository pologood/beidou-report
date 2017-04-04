package com.baidu.beidou.report.dao.vo;


/**
 * <p>ClassName:QueryParameter
 * <p>Function: Plan的SQL查询参数
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-2-26
 * @since    星计划(beidou 2.0)
 */
public class UnitQueryParameter extends GroupQueryParameter {
	/** 指定的推广组 */
	protected Integer groupId;

	/** 
	 * 物料类型：1文字，2图片，3flash，5图文
	 *
	 */
	protected Integer[] wuliaoType;
	
	/** 推广组状态 */
	protected int[] groupState ;
	/** 是否包括这些推广组状态 */
	protected boolean includeGroupState ;

	/** 创意尺寸过滤条件 用于创意列表页  add by dongying since 体验优化三期 **/
	protected String height;
	protected String width;
	
	/**
	 * 计划属性，0-所有功能，1-仅无线
	 */
	protected Integer promotionType;

	public Integer getPromotionType() {
		return promotionType;
	}

	public void setPromotionType(Integer promotionType) {
		this.promotionType = promotionType;
	}

	
	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Integer[] getWuliaoType() {
		return wuliaoType;
	}

	public void setWuliaoType(Integer[] wuliaoType) {
		this.wuliaoType = wuliaoType;
	}

	public int[] getGroupState() {
		return groupState;
	}

	public void setGroupState(int[] groupState) {
		this.groupState = groupState;
	}

	public boolean isIncludeGroupState() {
		return includeGroupState;
	}

	public void setIncludeGroupState(boolean includeGroupState) {
		this.includeGroupState = includeGroupState;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}
	
}
