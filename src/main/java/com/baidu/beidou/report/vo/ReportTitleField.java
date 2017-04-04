package com.baidu.beidou.report.vo;

/**
 * 报告列表的推广计划、推广组标题列
 * @author yanjie
 *
 */
public class ReportTitleField {
	private Integer id;
	private String title;
	private Integer type;
	
	public ReportTitleField(){
		
	}

	public ReportTitleField(Integer id, String title){
		this.id = id;
		this.title = title;
	}

	public ReportTitleField(Integer id, String title, Integer type){
		this.id = id;
		this.title = title;
		this.type = type;
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}
	
	
}
