package com.baidu.beidou.report.vo.plan;

import com.baidu.beidou.report.util.ReportFieldFormatter;



public class PlanReportSumData extends PlanSumData {
	/** 文字“汇总” */
	protected String summaryText;
	
	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}
	
	/**
	 * generateExtentionFields:生成扩展属性
	 *      
	 * @since 
	*/
	public void generateExtentionFields() {
		double ctr = -1;
		double cpm = -1;
		 
		if (0 != srchs){
			if (clks > 0) {
				ctr = (clks * 1.0d) / srchs ;
			}
			if(cost > 0) {
				cpm = (cost * 1000.0d) / srchs;
			}
		}
		
		double acp = -1;
		if (0 != clks){
			if (cost > 0) {
				acp = cost / (clks * 1.0d);
			}
		}
		
		this.setCtr(ctr);
		this.setAcp(acp);
		this.setCpm(cpm);
		
		/**
		 * added by liuhao sincebeidou3.x
		 */
		
		//UV数据扩展列
		double srsur = -1;
		double cusur = -1;
		double cocur = -1;
		
		if(0 != srchuv){
			srsur = srchs / (srchuv * 1.0d);
			srsur = Double.parseDouble(ReportFieldFormatter.format2Decimal(srsur));
			cusur = clkuv / (srchuv * 1.0d);
		}
		if(0 != clkuv){
			cocur = cost / (clkuv * 1.0d);
		}
				
		this.setSrsur(srsur);
		this.setCusur(cusur);
		this.setCocur(cocur);
		
		double arrivalRate = -1;
		double hopRate = -1;
		
		//holmes数据扩展列
		if(0 != holmesClks){
			arrivalRate = arrivalCnt / (holmesClks * 1.0d);
		}
		if(0 != arrivalCnt){
			hopRate = hopCnt / (arrivalCnt * 1.0d);
		}
		this.setArrivalRate(arrivalRate);
		this.setHopRate(hopRate);
		
		Double avgResTime = -1d;
		if (0 != effectArrCnt) {
			avgResTime = resTime / (effectArrCnt * 1.0d);
		}
		this.setAvgResTime(avgResTime);
		String resTimeStr = ReportFieldFormatter.formatResTime((int)Math.round(avgResTime)); 
		this.setResTimeStr(resTimeStr);
	}
}
