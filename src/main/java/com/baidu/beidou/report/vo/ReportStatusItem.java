package com.baidu.beidou.report.vo;

/**
 * 报告列表的状态信息条目
 * @author yanjie
 *
 */
public class ReportStatusItem {
	private Integer totalPage;
	private String dateStart;
	private String dateEnd;
	
	public ReportStatusItem(){
		
	}

	public String getDateEnd() {
		return dateEnd;
	}

	public void setDateEnd(String dateEnd) {
		this.dateEnd = dateEnd;
	}

	public String getDateStart() {
		return dateStart;
	}

	public void setDateStart(String dateStart) {
		this.dateStart = dateStart;
	}

	public Integer getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(Integer totalPage) {
		this.totalPage = totalPage;
	}
	
	
}
