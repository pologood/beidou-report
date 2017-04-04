package com.baidu.beidou.report.vo;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;

public class ReportDayVo extends AbstractReportVo {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4016275400639650767L;

	protected List<ReportDayViewItem> details;
	protected ReportSumData summary;
	
	protected boolean showTransData;//是否显示转化数据列
	protected boolean transDataValid;//如果显示转化数据列，那么非valid的情况下应该显示-

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

		for (ReportDayViewItem item : details) {
			String[] detailStringArray = new String[headers.length];
			int index = 0;
			detailStringArray[index] = item.getShowDate();//日期
			
			if ( item.getSrchs() < 0 ) {//展现次数
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = "" + item.getSrchs();
			}
			
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
				detailStringArray[++index] = df4.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getCusur() == null || item.getCusur().doubleValue() < 0 ) {//独立访客点击率
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df4.format(item.getCusur().doubleValue() * 100) + "%";
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
					detailStringArray[++index] = "" + item.getDirectTrans();
					detailStringArray[++index] = "" + item.getIndirectTrans();					
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

	/**
	 * getCsvReportSummary: 获取汇总信息
	 * 
	 * @param level
	 *            所属层级
	 * @see QueryParameterConstant.LEVEL
	 * @return 汇总信息
	 */
	public String[] getCsvReportSummary() {
		int index = 0;
		String[] summaryStringArray = new String[headers.length];
		summaryStringArray[index] = summaryText;
		
		if (summary.getSrchs() < 0 ) {//展现
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = String.valueOf(summary.getSrchs());
		}
		
		if (summary.getSrchuv() < 0 ) {//展现独立访客
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = String.valueOf(summary.getSrchuv());
		}
		
		summaryStringArray[++index] = "";//展现频次
		summaryStringArray[++index] = "" + summary.getClks();
		
		if (summary.getClkuv() < 0 ) {//点击独立访客
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = String.valueOf(summary.getClkuv());
		}
		
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df4.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		summaryStringArray[++index] = "";//独立访客点击率
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}
		summaryStringArray[++index] = "";//平均独立访客点击价格
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm().doubleValue());
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

	public void setSummary(ReportSumData summary) {
		this.summary = summary;
	}

	public void setDetails(List<ReportDayViewItem> details) {
		this.details = details;
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
