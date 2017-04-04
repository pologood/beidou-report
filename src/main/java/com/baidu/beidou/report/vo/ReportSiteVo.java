package com.baidu.beidou.report.vo;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.olap.vo.ReportSiteViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;

public class ReportSiteVo extends AbstractReportVo {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4016275400639650767L;

	protected List<ReportSiteViewItem> details;
	protected ReportSumData summary;

	/**
	 * getCsvReportHeader:获取下载列表列表头
	 * 
	 * @param level
	 * @see QueryParameterConstant.LEVEL
	 * @return 列表头
	 */
	public String[] getCsvReportHeader() {
		return headers;
	}

	/**
	 * getCsvReportDetail:获取下载列表具体信息
	 * 
	 * @param level
	 * @see QueryParameterConstant.LEVEL
	 * @return 列表详情信息
	 */
	public List<String[]> getCsvReportDetail() {
		List<String[]> result = new ArrayList<String[]>(details.size());

		for (ReportSiteViewItem item : details) {
			String[] itemArr = new String[headers.length];
			itemArr[0] = item.getSite();
			
			if ( item.getSrchs() < 0 ) {//展现次数
				itemArr[1] = "-" ;
			} else {
				itemArr[1] = "" + item.getSrchs();
			}
			
			itemArr[2] = String.valueOf(item.getClks());
			if (item.getCtr() == null || item.getCtr().doubleValue() <= 0) {
				itemArr[3] = "-";
			} else {
				itemArr[3] = df4.format(item.getCtr().doubleValue() * 100)
						+ "%";
			}

			if (item.getAcp() == null || item.getAcp().doubleValue() <= 0) {
				itemArr[4] = "-";
			} else {
				itemArr[4] = df2.format(item.getAcp().doubleValue());
			}

			if (item.getCpm() == null || item.getCpm().doubleValue() <= 0) {
				itemArr[5] = "-";
			} else {
				itemArr[5] = df2.format(item.getCpm().doubleValue());
			}
			itemArr[6] = "" + item.getCost();

			result.add(itemArr);
		}
		return result;
	}

	/**
	 * getCsvReportSummary: 获取汇总信息
	 * 
	 * @param level
	 *            所属层级
	 * @see QueryParameterConstant.LEVEL
	 * @return 汇总信息
	 */
	public String[] getCsvReportSummary() {
		String[] itemArr = new String[headers.length];
		itemArr[0] = summaryText;
		
		if (summary.getSrchs() < 0 ) {//展现
			itemArr[1] = "-";
		} else {
			itemArr[1] = String.valueOf(summary.getSrchs());
		}
		
		itemArr[2] = String.valueOf(summary.getClks());

		if (summary.getCtr() == null || summary.getCtr().doubleValue() <= 0) {
			itemArr[3] = "-";
		} else {
			itemArr[3] = df4.format(summary.getCtr().doubleValue() * 100) + "%";
		}

		if (summary.getAcp() == null || summary.getAcp().doubleValue() <= 0) {
			itemArr[4] = "-";
		} else {
			itemArr[4] = df2.format(summary.getAcp().doubleValue());
		}

		if (summary.getCpm() == null || summary.getCpm().doubleValue() <= 0) {
			itemArr[5] = "-";
		} else {
			itemArr[5] = df2.format(summary.getCpm().doubleValue());
		}
		itemArr[6] = "" + summary.getCost();

		return itemArr;
	}

	public void setSummary(ReportSumData summary) {
		this.summary = summary;
	}

	public void setDetails(List<ReportSiteViewItem> details) {
		this.details = details;
	}
}
