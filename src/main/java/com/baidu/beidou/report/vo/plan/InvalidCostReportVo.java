package com.baidu.beidou.report.vo.plan;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class InvalidCostReportVo extends AbstractReportVo {
	private static final long serialVersionUID = 1681320204243487468L;

	private static final String RMB_FORMAT = "";

	/** 账户信息 */
	private ReportAccountInfo accountInfo;
	/** 消息头 */
	private String[] headers;
	/** 明细 */
	private List<InvalidCostViewItem> details;
	/** 汇总信息 */
	private InvalidCostViewItem summary;

	public List<String[]> getCsvReportAccountInfo() {
		return accountInfo.getAccountInfoList();
	}

	@Override
	public List<String[]> getCsvReportDetail() {
		List<String[]> result = new ArrayList<String[]>(details.size());
		String[] detailStringArray;
		for (InvalidCostViewItem item : details) {
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = item.getDateRange();
			detailStringArray[++index] = "" + item.getOriginalClks();
			detailStringArray[++index] = "" + item.getRealtimeFilterClks();
			if (item.getRealtimeFilterRate() <= 0) {
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = df6.format(item.getRealtimeFilterRate() * 100) + "%";
			}
			detailStringArray[++index] = "" + item.getRealtimeFilterCost();
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
		summaryStringArray[++index] = "" + summary.getOriginalClks();
		summaryStringArray[++index] = "" + summary.getRealtimeFilterClks();
		if (summary.getRealtimeFilterRate() < 0) {
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df6.format(summary.getRealtimeFilterRate() * 100) + "%";
		}
		summaryStringArray[++index] = "" + summary.getRealtimeFilterCost();

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

	public List<InvalidCostViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<InvalidCostViewItem> details) {
		this.details = details;
	}

	public InvalidCostViewItem getSummary() {
		return summary;
	}

	public void setSummary(InvalidCostViewItem summary) {
		this.summary = summary;
	}

}
