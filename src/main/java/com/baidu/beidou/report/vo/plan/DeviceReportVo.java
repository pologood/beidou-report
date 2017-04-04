package com.baidu.beidou.report.vo.plan;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.olap.vo.DeviceViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class DeviceReportVo extends AbstractReportVo {

	private static final long serialVersionUID = -6148858956021915818L;
	
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers; 
	/** 明细 */
	protected List<DeviceViewItem> details;

	/** 汇总信息 */
	protected DeviceReportSumData summary;
	
	/** 推广计划列下标 */
	protected static final int PLANNAME_INDEX = 2;
	
	
	
	public DeviceReportVo(String level) {
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
		for (DeviceViewItem item : details ){
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = item.getDeviceId()==ReportConstants.DEVICE_WIRELESS? "移动设备" : "电脑设备";
			detailStringArray[++index] = item.getPlanName();
			detailStringArray[++index] = item.getPromotionType()==CproPlanConstant.PROMOTIONTYPE_WIRELESS? "仅无线" : "所有功能";
			
			if (item.getBidRatio() < 0 ) {//出价比例
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = String.valueOf(item.getBidRatio() / 100);
			}
			String srchs = item.getSrchs() < 0 ? "-" : String.valueOf(item.getSrchs());
			detailStringArray[++index] = srchs;//展现次数
			
			if ( item.getSrchuv() < 0 ) {//展现独立访客
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = "" + item.getSrchuv();
			}
			if (item.getSrsur() == null || item.getSrsur().doubleValue() < 0 ) {//展现频次
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getSrsur().doubleValue());
			}
			detailStringArray[++index] = "" + item.getClks(); //点击次数
			if ( item.getClkuv() < 0 ) {//点击独立访客
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = "" + item.getClkuv(); 
			}
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0 ) {//点击率
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getCusur() == null || item.getCusur().doubleValue() < 0 ) {//独立访客点击率
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df6.format(item.getCusur().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0 ) {//平均点击价格
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCocur() == null || item.getCocur().doubleValue() < 0 ) {//平均独立访客点击价格
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getCocur().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0 ) {
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}
			detailStringArray[++index] = "" + item.getCost(); 

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

		summaryStringArray[++index] = ""; //推广计划
		
		summaryStringArray[++index] = "";//推广类型
		
		summaryStringArray[++index] = "";//出价比例
		
		String srchs = summary.getSrchs() < 0 ? "-" : String.valueOf(summary.getSrchs());
		summaryStringArray[++index] = srchs;
		
		String srchuv = summary.getSrchuv() < 0 ? "-" : String.valueOf(summary.getSrchuv());
		summaryStringArray[++index] = srchuv;//展现独立访客
		
		summaryStringArray[++index] = "";//展现频次
		summaryStringArray[++index] = "" + summary.getClks();
		String clkuv = summary.getClkuv() < 0 ? "-" : String.valueOf(summary.getClkuv());
		summaryStringArray[++index] = clkuv;//点击独立访客
		
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df6.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		summaryStringArray[++index] = "";//平均独立访客点击价格
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}
		summaryStringArray[++index] = "";//平均独立访客点击价格
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {//千次展现成本
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm().doubleValue());
		}
		summaryStringArray[++index] = "" + summary.getCost();//消费
		
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


	public DeviceReportSumData getSummary() {
		return summary;
	}

	public void setSummary(DeviceReportSumData summary) {
		this.summary = summary;
	}
	
	public List<DeviceViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<DeviceViewItem> details) {
		this.details = details;
	}

}
