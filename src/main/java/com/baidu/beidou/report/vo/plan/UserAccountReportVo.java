/**
 * UserAccountReportVo.java  2011-11-30
 *
 * Copyright 2011 Baidu, Inc. All rights reserved.
 * Baidu PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.baidu.beidou.report.vo.plan;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

/**
 * @author kanghongwei
 * 
 *         推广首页--账户分日数据下载数据Vo
 */
public class UserAccountReportVo extends AbstractReportVo {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1681320204243487468L;

	/** 去除人民币符号：modified by kanghongwei since cpweb429(qtIM) */
	private static final String RMB_FORMAT = "";

	/** 账户信息 */
	private ReportAccountInfo accountInfo;
	/** 消息头 */
	private String[] headers;
	/** 明细 */
	private List<UserAccountViewItem> details;
	/** 汇总信息 */
	private UserAccountViewItem summary;

	protected boolean showTransData;//是否显示转化数据列
	protected boolean transDataValid;//如果显示转化数据列，那么非valid的情况下应该显示-
	
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

	@Override
	public List<String[]> getCsvReportDetail() {
		List<String[]> result = new ArrayList<String[]>(details.size());
		String[] detailStringArray;
		for (UserAccountViewItem item : details) {
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = item.getShowDate();
			detailStringArray[++index] = "" + item.getSrchs();//展现次数
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
			
			if (showTransData) {
				if (transDataValid) {
					if (item.getArrivalRate() == null || item.getArrivalRate().doubleValue() < 0 ) {//到达率
						detailStringArray[++index] = "-" ;
					} else {
						detailStringArray[++index] = df4.format(item.getArrivalRate().doubleValue() * 100) + "%";
					}
					if (item.getHopRate() == null || item.getHopRate().doubleValue() < 0 ) {//二跳率
						detailStringArray[++index] = "-" ;
					} else {
						detailStringArray[++index] = df4.format(item.getHopRate().doubleValue() * 100) + "%";
					}
					detailStringArray[++index] = "" + item.getResTimeStr();
					if ( item.getDirectTrans() < 0 ) {//直接转化
						detailStringArray[++index] = "-" ;
					} else {
						detailStringArray[++index] = "" + item.getDirectTrans();
					}
					if ( item.getIndirectTrans() < 0 ) {//间接转化
						detailStringArray[++index] = "-" ;
					} else {
						detailStringArray[++index] = "" + item.getIndirectTrans();
					}					
				} else {
					detailStringArray[++index] = "-";
					detailStringArray[++index] = "-";
					detailStringArray[++index] = "-";
					detailStringArray[++index] = "-";
					detailStringArray[++index] = "-";
				}
			}
			
			result.add(detailStringArray);
		}
		return result;
	}

	private String getRMBFormat(String value) {
		if (StringUtils.isEmpty(value)) {
			return RMB_FORMAT + 0.00;
		}
		return RMB_FORMAT + value;
	}

	@Override
	public String[] getCsvReportHeader() {
		return headers;
	}

	@Override
	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index] = summary.getSummaryText();
		summaryStringArray[++index] = "" + summary.getSrchs();
		summaryStringArray[++index] = "" + summary.getSrchuv();//展现独立访客
		summaryStringArray[++index] = "";//展现频次
		summaryStringArray[++index] = "" + summary.getClks();
		summaryStringArray[++index] = "" + summary.getClkuv();//点击独立访客
		
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df6.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		summaryStringArray[++index] = "";//独立访客点击率
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = getRMBFormat(df2.format(summary.getAcp().doubleValue()));
		}
		summaryStringArray[++index] = "";//平均独立访客点击价格
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {//千次展现成本
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = getRMBFormat(df2.format(summary.getCpm().doubleValue()));
		}
		summaryStringArray[++index] = "" + summary.getCost();//消费
		
		if (showTransData) {
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
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

	public List<UserAccountViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<UserAccountViewItem> details) {
		this.details = details;
	}

	public UserAccountViewItem getSummary() {
		return summary;
	}

	public void setSummary(UserAccountViewItem summary) {
		this.summary = summary;
	}

	public boolean isShowTransData() {
		return showTransData;
	}

	public void setShowTransData(boolean showTransData) {
		this.showTransData = showTransData;
	}

	public boolean isTransDataValid() {
		return transDataValid;
	}

	public void setTransDataValid(boolean transDataValid) {
		this.transDataValid = transDataValid;
	}
}
