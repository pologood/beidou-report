package com.baidu.beidou.report.vo.plan;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class PlanCustomReportVo extends AbstractReportVo {

	private static final long serialVersionUID = -2677398418291234149L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 消息头 */
	protected String[] headers;
	/** 明细 */
	protected List<PlanViewItem> details;
	/** 汇总信息 */
	protected PlanReportSumData summary;

	public PlanCustomReportVo(String level) {
		super(level);
	}

	public List<String[]> getCsvReportAccountInfo() {
			return accountInfo.getAccountInfoList();
	}

	public List<String[]> getCsvReportDetail() {

		List<String[]> result = new ArrayList<String[]>(details.size());

		String[] detailStringArray;
		for (PlanViewItem item : details) {
			detailStringArray = new String[headers.length];
			int index = 0;
			detailStringArray[index] = item.getDateRange();
			detailStringArray[++index] = item.getPlanName();// 推广计划名

			if (item.getSrchs() < 0) {// 展现次数
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = "" + item.getSrchs();
			}
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

	public String[] getCsvReportHeader() {
		return headers;
	}

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index] = summary.getSummaryText();
		summaryStringArray[++index] = "";

		if (summary.getSrchs() < 0) {// 展现次数
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = String.valueOf(summary.getSrchs());
		}
		summaryStringArray[++index] = "" + summary.getClks();// 点击次数

		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0) {// 点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df6.format(summary.getCtr().doubleValue() * 100) + "%";
		}
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0) {// 平均点击价格
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0) {// 千次展现成本
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm().doubleValue());
		}
		summaryStringArray[++index] = "" + summary.getCost();// 消费

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

	public List<PlanViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<PlanViewItem> details) {
		this.details = details;
	}

	public PlanReportSumData getSummary() {
		return summary;
	}

	public void setSummary(PlanReportSumData summary) {
		this.summary = summary;
	}
}
