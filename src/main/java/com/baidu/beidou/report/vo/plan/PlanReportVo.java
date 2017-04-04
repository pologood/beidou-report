package com.baidu.beidou.report.vo.plan;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class PlanReportVo extends AbstractReportVo {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2677398418291234149L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 消息头 */
	protected String[] headers; 
	/** 明细 */
	protected List<PlanViewItem> details;
	/** 汇总信息 */
	protected PlanReportSumData summary;

	protected boolean showTransData;//是否显示转化数据列
	protected boolean transDataValid;//如果显示转化数据列，那么非valid的情况下应该显示-
	
	public PlanReportVo(String level) {
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
		
		for (PlanViewItem item : details ){
			detailStringArray = new String[headers.length];
			int i = 0;
			detailStringArray[i] = item.getPlanName();//推广计划名
			detailStringArray[++i] = CproPlanConstant.PLAN_VIEW_STATE_NAME[item.getViewState()];//推广计划状态
			detailStringArray[++i] = CproPlanConstant.PLAN_PROMOTION_TYPE_NAME[item.getPromotionType()];//推广计划类型
			detailStringArray[++i] = "" + item.getBudget();//推广计划预算
			
			if ( item.getSrchs() < 0 ) {//展现次数
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = "" + item.getSrchs();
			}
			
			if ( item.getSrchuv() < 0 ) {//展现独立访客
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = "" + item.getSrchuv();
			}
			if (item.getSrsur() == null || item.getSrsur().doubleValue() < 0 ) {//展现频次
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = df2.format(item.getSrsur().doubleValue());
			}
			detailStringArray[++i] = "" + item.getClks(); //点击次数
			if ( item.getClkuv() < 0 ) {//点击独立访客
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = "" + item.getClkuv(); 
			}
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0 ) {//点击率
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getCusur() == null || item.getCusur().doubleValue() < 0 ) {//独立访客点击率
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = df6.format(item.getCusur().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0 ) {//平均点击价格
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCocur() == null || item.getCocur().doubleValue() < 0 ) {//平均独立访客点击价格
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = df2.format(item.getCocur().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0 ) {
				detailStringArray[++i] = "-" ;
			} else {
				detailStringArray[++i] = df2.format(item.getCpm().doubleValue());
			}
			detailStringArray[++i] = "" + item.getCost(); 
			
			if (showTransData) {
				if (transDataValid) {
					if (item.getArrivalRate() == null || item.getArrivalRate().doubleValue() < 0 ) {//到达率
						detailStringArray[++i] = "-" ;
					} else {
						detailStringArray[++i] = df4.format(item.getArrivalRate().doubleValue() * 100) + "%";
					}
					if (item.getHopRate() == null || item.getHopRate().doubleValue() < 0 ) {//二跳率
						detailStringArray[++i] = "-" ;
					} else {
						detailStringArray[++i] = df4.format(item.getHopRate().doubleValue() * 100) + "%";
					}
					detailStringArray[++i] = "" + item.getResTimeStr();
					if ( item.getDirectTrans() < 0 ) {//直接转化
						detailStringArray[++i] = "-" ;
					} else {
						detailStringArray[++i] = "" + item.getDirectTrans();
					}
					if ( item.getIndirectTrans() < 0 ) {//间接转化
						detailStringArray[++i] = "-" ;
					} else {
						detailStringArray[++i] = "" + item.getIndirectTrans();
					}				
				} else {
					detailStringArray[++i] = "-";
					detailStringArray[++i] = "-";
					detailStringArray[++i] = "-";
					detailStringArray[++i] = "-";
					detailStringArray[++i] = "-";
				}
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
		int i = 0;
		summaryStringArray[i] = summary.getSummaryText();
		summaryStringArray[++i] = "";
		summaryStringArray[++i] = "";
		summaryStringArray[++i] = "" + summary.getSumBudget();
		
		if (summary.getSrchs() < 0 ) {//展现
			summaryStringArray[++i] = "-";
		} else {
			summaryStringArray[++i] = String.valueOf(summary.getSrchs());
		}
		
		if (summary.getSrchuv() < 0 ) {//展现独立访客
			summaryStringArray[++i] = "-";
		} else {
			summaryStringArray[++i] = String.valueOf(summary.getSrchuv());
		}
		
		summaryStringArray[++i] = "";//展现频次
		summaryStringArray[++i] = "" + summary.getClks();
		
		
		if (summary.getClkuv() < 0 ) {//点击独立访客
			summaryStringArray[++i] = "-";
		} else {
			summaryStringArray[++i] = String.valueOf(summary.getClkuv());
		}
		
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++i] = "-";
		} else {
			summaryStringArray[++i] =df6.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		summaryStringArray[++i] = "";//展现频次
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++i] = "-" ;
		} else {
			summaryStringArray[++i] = df2.format(summary.getAcp().doubleValue());
		}
		summaryStringArray[++i] = "";//展现频次
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {
			summaryStringArray[++i] = "-" ;
		} else {
			summaryStringArray[++i] = df2.format(summary.getCpm().doubleValue());
		}
		summaryStringArray[++i] = "" + summary.getCost();//消费
		
		if (showTransData) {
			summaryStringArray[++i] = "";
			summaryStringArray[++i] = "";
			summaryStringArray[++i] = "";
			summaryStringArray[++i] = "";
			summaryStringArray[++i] = "";
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
