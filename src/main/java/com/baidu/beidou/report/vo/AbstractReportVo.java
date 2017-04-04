package com.baidu.beidou.report.vo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.report.constant.QueryParameterConstant;

public abstract class AbstractReportVo implements java.io.Serializable{

	protected Integer userId;

	protected DecimalFormat df4 = new DecimalFormat("#.####");//四位小数
	protected DecimalFormat df6 = new DecimalFormat("#.######");//六位小数
	protected DecimalFormat df2 = new DecimalFormat("#.##");//两位小数
	
	protected ReportAccountInfo accountInfo;
	protected String[] headers;
	protected String summaryText;

	/** 所处层级 */
	protected String level;
	
	public AbstractReportVo(){}
	
	public AbstractReportVo(String level) {
		this.level = level;
	}
	
	/**
	 * getCsvReportAccountInfo:获取下载列表的账户信息
	 *
	 * @param level
	 * @see QueryParameterConstant.LEVEL
	 * @return  账户信息    
	*/
	public List<String[]> getCsvReportAccountInfo(){
		if(accountInfo.isCustomReport()){
			return accountInfo.getAccountInfoList();
		}
		List<String[]> result = new ArrayList<String[]>();
		
		String[] textValuePair = new String[2];
		textValuePair[0] = accountInfo.getReportText();
		textValuePair[1] = accountInfo.getReport();
		result.add(textValuePair);

		textValuePair = new String[2];
		textValuePair[0] = accountInfo.getAccountText();
		textValuePair[1] = accountInfo.getAccount();
		result.add(textValuePair);

		textValuePair = new String[2];
		textValuePair[0] = accountInfo.getDateRangeText();
		textValuePair[1] = accountInfo.getDateRange();
		result.add(textValuePair);

		textValuePair = new String[2];
		textValuePair[0] = accountInfo.getLevelText();
		textValuePair[1] = accountInfo.getLevel();
		result.add(textValuePair);
		
		return result;
	}
	
	/**
	 * getCsvReportHeader:获取下载列表列表头
	 *
	 * @param level
	 * @see QueryParameterConstant.LEVEL
	 * @return  列表头    
	*/
	public abstract String[] getCsvReportHeader();
	
	/**
	 * getCsvReportDetail:获取下载列表具体信息
	 *
	 * @param level
	 * @see QueryParameterConstant.LEVEL
	 * @return 列表详情信息    
	*/
	public abstract List<String[]> getCsvReportDetail();
	
	/**
	 * getCsvReportSummary: 获取汇总信息
	 *
	 * @param level 所属层级
	 * @see QueryParameterConstant.LEVEL
	 * @return 汇总信息     
	*/
	public abstract String[] getCsvReportSummary();
	
	
	public void setLevel(String level) {
		this.level = level;
	}

	public String getLevel() {
		return level;
	}

	public ReportAccountInfo getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(ReportAccountInfo accountInfo) {
		this.accountInfo = accountInfo;
	}

	public void setHeaders(String[] headers) {
		this.headers = headers;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
	
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

}
