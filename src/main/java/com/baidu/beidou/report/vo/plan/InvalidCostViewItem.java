package com.baidu.beidou.report.vo.plan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.util.ReportFieldFormatter;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.util.DateUtils;

public class InvalidCostViewItem  extends StatInfo implements Comparable<InvalidCostViewItem> {

	private static final long serialVersionUID = 4852911588946460337L;

	public static SimpleDateFormat viewDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	/** 账户分日报告统计显示日期 */
	private String showDate;

	/** 汇总属性：是否需要前端排序，当数据量小于1W时，前端排序 */
	private int needFrontSort = ReportConstants.Boolean.FALSE;

	/** 下载属性：账户分日数据下载汇总行文字信息 */
	private String summaryText;
	
	/**账户名称--定制报告列表使用*/
	private String userName;
	
	/**日期范围--定制报告列表使用*/
	private String dateRange;
	
	private long originalClks;
	
	private long realtimeFilterClks;
	
	private double realtimeFilterCost;
	
	private double realtimeFilterRate;
	
	@Override
	public void fillStatRecord(Map<String, Object> record) {
		super.fillStatRecord(record);
		if (!CollectionUtils.isEmpty(record)) {
			this.showDate = DateUtils.formatDorisReturnDate(record.get(ReportConstants.FROMDATE),
					viewDateFormat);
			dateRange = ReportFieldFormatter.formatContentDateRange((Date) record.get(ReportConstants.FROMDATE), (Date) record
					.get(ReportConstants.TODATE));
			this.originalClks = (Long)record.get(ReportConstants.ORIGINAL_CLKS);
			this.realtimeFilterClks = (Long)record.get(ReportConstants.REALTIME_FILTER_CLKS);
			this.realtimeFilterRate = (Double)record.get(ReportConstants.REALTIME_FILTER_RATE);
			Object rd = record.get(ReportConstants.REALTIME_FILTER_COST);
			if (rd != null){
				this.realtimeFilterCost = Double.parseDouble(rd.toString()) / 100.0d;
			} else {
				this.realtimeFilterCost = 0;
			}
			record.put(ReportConstants.COST, this.cost);
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("showDate =" + getShowDate() + " ");
		buffer.append("dateRange =" + getDateRange() + " ");
		buffer.append("originalClks =" + originalClks + " ");
		buffer.append("realtimeFilterClks =" + realtimeFilterClks + " ");
		buffer.append("realtimeFilterCost =" + realtimeFilterCost + " ");
		buffer.append("realtimeFilterRate =" + realtimeFilterRate + " ");
		return buffer.toString();
	}

	public int compareTo(InvalidCostViewItem o) {
		return this.showDate.compareTo(o.getShowDate());
	}

	public static SimpleDateFormat getViewDateFormat() {
		return viewDateFormat;
	}

	public static void setViewDateFormat(SimpleDateFormat viewDateFormat) {
		InvalidCostViewItem.viewDateFormat = viewDateFormat;
	}

	public String getShowDate() {
		return showDate;
	}

	public void setShowDate(String showDate) {
		this.showDate = showDate;
	}

	public int getNeedFrontSort() {
		return needFrontSort;
	}

	public void setNeedFrontSort(int needFrontSort) {
		this.needFrontSort = needFrontSort;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getDateRange() {
		return dateRange;
	}

	public void setDateRange(String dateRange) {
		this.dateRange = dateRange;
	}

	public long getOriginalClks() {
		return originalClks;
	}

	public void setOriginalClks(long originalClks) {
		this.originalClks = originalClks;
	}

	public long getRealtimeFilterClks() {
		return realtimeFilterClks;
	}

	public void setRealtimeFilterClks(long realtimeFilterClks) {
		this.realtimeFilterClks = realtimeFilterClks;
	}

	public double getRealtimeFilterCost() {
		return realtimeFilterCost;
	}

	public void setRealtimeFilterCost(double realtimeFilterCost) {
		this.realtimeFilterCost = realtimeFilterCost;
	}

	public double getRealtimeFilterRate() {
		return realtimeFilterRate;
	}

	public void setRealtimeFilterRate(double realtimeFilterRate) {
		this.realtimeFilterRate = realtimeFilterRate;
	}

}
