package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.product.ProductViewItem;

public class ProductReportVo extends AbstractReportVo {

	private static final long serialVersionUID = 4548042867126313776L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers; 
	/** 明细 */
	protected List<ProductViewItem> details;

	/** 汇总信息 */
	protected ProductReportSumData summary;
	
	/** 推广计划列下标 */
	protected static final int PLANNAME_INDEX = 2;
	
	public ProductReportVo(String level) {
		super(level);
	}
	

	public List<String[]> getCsvReportAccountInfo() {
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

	public List<String[]>  getCsvReportDetail() {

		List<String[]> result = new ArrayList<String[]>(details.size());
		
		String[] detailStringArray ;
		for (ProductViewItem item : details ){
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = String.valueOf(item.getProductId());
			detailStringArray[++index] = item.getProductName();
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
				//账户层级才显示该列
				detailStringArray[++index] = item.getPlanName();
			}
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level) 
					|| QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)){
				// 账户层级或者计划层级显示该列
				detailStringArray[++index] = item.getGroupName();
			}
			
			String srchs = item.getSrchs() < 0 ? "-" : String.valueOf(item.getSrchs());
			detailStringArray[++index] = srchs;//展现次数
			
			detailStringArray[++index] = "" + item.getClks(); //点击次数
			
			detailStringArray[++index] = "" + item.getCost(); 

			if (item.getCtr() == null || item.getCtr().doubleValue() < 0 ) {//点击率
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0 ) {//平均点击价格
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0 ) {
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}

			result.add(detailStringArray);
		}
		return result;
	}

	public String[] getCsvReportHeader() {
		return headers;
	}

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index] = summary.getSummaryText();
		summaryStringArray[++index] = "";
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {//过滤Plan和GROUP两列
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
		} else if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)) {//只过滤group一列
			summaryStringArray[++index] = "";
		}
		
		String srchs = summary.getSrchs() < 0 ? "-" : String.valueOf(summary.getSrchs());
		summaryStringArray[++index] = srchs;
		
		summaryStringArray[++index] = "" + summary.getClks();
		
		summaryStringArray[++index] = "" + summary.getCost();//消费
		
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df6.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {//千次展现成本
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm().doubleValue());
		}
		
		return summaryStringArray;
	}

	public ReportAccountInfo getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(ReportAccountInfo accountInfo) {
		this.accountInfo = accountInfo;
	}

	public String[] getHeaders() {
		return headers;
	}

	public void setHeaders(String[] headers) {
		this.headers = headers;
	}


	public ProductReportSumData getSummary() {
		return summary;
	}

	public void setSummary(ProductReportSumData summary) {
		this.summary = summary;
	}
	
	public List<ProductViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<ProductViewItem> details) {
		this.details = details;
	}

}
