package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.ReportSumData;

public class GroupCustomReportVo extends AbstractReportVo {
	private static final long serialVersionUID = 811342840320886519L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers;
	/** 明细 */
	protected List<GroupViewItem> details;
	/** 汇总信息 */
	protected ReportSumData summary;

	/** 推广计划列下标 */
	protected static final int PLANNAME_INDEX = 2;

	public List<String[]> getCsvReportAccountInfo() {
		return accountInfo.getAccountInfoList();
	}

	public List<String[]> getCsvReportDetail() {

		List<String[]> result = new ArrayList<String[]>(details.size());

		String[] detailStringArray;
		for (GroupViewItem item : details) {
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = item.getDateRange();// 日期范围
			detailStringArray[++index] = item.getPlanName();// 推广计划名
			detailStringArray[++index] = item.getGroupName();// 推广组名
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

	public List<GroupViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<GroupViewItem> details) {
		this.details = details;
	}

	public ReportSumData getSummary() {
		return summary;
	}

	public void setSummary(ReportSumData summary) {
		this.summary = summary;
	}

}
