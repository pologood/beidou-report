package com.baidu.beidou.report.vo.plan;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

/**
 * 定制报告--账户效果报告下载VO
 * 
 * @author hujunhai 2013-1-6
 */
public class UserAccountCustomReportVo extends AbstractReportVo {
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

	public List<String[]> getCsvReportAccountInfo() {
		return accountInfo.getAccountInfoList();
	}

	@Override
	public List<String[]> getCsvReportDetail() {
		List<String[]> result = new ArrayList<String[]>(details.size());
		String[] detailStringArray;
		for (UserAccountViewItem item : details) {
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = item.getDateRange();
			detailStringArray[++index] = "" + item.getSrchs();// 展现次数
			detailStringArray[++index] = "" + item.getClks(); // 点击次数
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0) {// 点击率
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0) {// 平均点击价格
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0) {// 千次展现成本
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}
			detailStringArray[++index] = "" + item.getCost(); // 总费用
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
		summaryStringArray[++index] = "" + summary.getSrchs();// 展现次数
		summaryStringArray[++index] = "" + summary.getClks();// 点击次数
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0) {// 点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df6.format(summary.getCtr().doubleValue() * 100) + "%";
		}
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0) {// 平均点击价格
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = getRMBFormat(df2.format(summary.getAcp().doubleValue()));
		}
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0) {// 千次展现成本
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = getRMBFormat(df2.format(summary.getCpm().doubleValue()));
		}
		summaryStringArray[++index] = "" + summary.getCost();// 总消费

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
}
