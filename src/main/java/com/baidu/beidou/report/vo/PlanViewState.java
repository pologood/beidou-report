package com.baidu.beidou.report.vo;


/**
 * viewstate的子类，包含显示状态和投放状态
 * add by zp，version：1.2.0
 * @author Administrator
 *
 */
public class PlanViewState{
	private String status;
	private Integer releaseState;//有效推广计划的投放状态，add by zp， version：1.2.0
	
	public Integer getReleaseState() {
		return releaseState;
	}
	public void setReleaseState(Integer releaseState) {
		this.releaseState = releaseState;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
		
	public PlanViewState(){
		
	}
	
	public PlanViewState(String status, Integer releaseState){
		this.status = status;
		this.releaseState = releaseState;
	}	
}