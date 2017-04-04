package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.olap.vo.InterestViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;

public class GroupPackItReportVo extends AbstractReportVo {
	
	private static final long serialVersionUID = -7151911750053179752L;
	
	private List<InterestAssistant> details;
	private InterestReportSumData sumData;
	
	public GroupPackItReportVo(){}
	
	public GroupPackItReportVo(String level){
		super(level);
	}
	
	@Override
	public List<String[]> getCsvReportDetail() {
		if (CollectionUtils.isEmpty(details)) {
			return new ArrayList<String[]>(0);
		}
		List<String[]> result = new ArrayList<String[]>(details.size());		
		String[] detailStringArray ;
		for (InterestAssistant items : details ){
			for(InterestViewItem item : items.getInterestViewItem()){
				int index = 0;
				detailStringArray = new String[headers.length];
				String interestName = item.getInterestName();
				detailStringArray[index] = interestName;
				
				if (item.getSrchs() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getSrchs();
				}
				if (item.getSrchuv() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getSrchuv();
				}
				if (item.getSrsur() == null || item.getSrsur().doubleValue() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					//需要乘虚而入100来表示百分比值
					detailStringArray[++index] = df2.format(item.getSrsur().doubleValue());
				}
				
				if (item.getClks() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getClks();
				}
				if (item.getClkuv() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getClkuv();
				}
				if (item.getCtr() == null || item.getCtr().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {				
					//需要乘虚而入100来表示百分比值
					detailStringArray[++index] = df4.format(item.getCtr().doubleValue() * 100.0) + "%";
				}
				if (item.getCusur() == null || item.getCusur().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {
					//需要乘虚而入100来表示百分比值
					detailStringArray[++index] = df4.format(item.getCusur().doubleValue() * 100.0) + "%";
				}
				if (item.getAcp() == null || item.getAcp().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
				}
				if (item.getCocur() == null || item.getCocur().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + df2.format(item.getCocur().doubleValue());
				}
				if (item.getCpm() == null || item.getCpm().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
				}
				if (item.getCost() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getCost();
				}
				
				if (item.getArrivalRate() == null || item.getArrivalRate().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {
					//需要乘虚而入100来表示百分比值
					detailStringArray[++index] = df2.format(item.getArrivalRate().doubleValue() * 100.0) + "%";
				}
				
				if (item.getHopRate() == null || item.getHopRate().doubleValue() < 0 ) {
					detailStringArray[++index] = "-" ;
				} else {
					//需要乘虚而入100来表示百分比值
					detailStringArray[++index] = df2.format(item.getHopRate().doubleValue() * 100.0) + "%";
				}
				
				detailStringArray[++index] = item.getResTimeStr();
				if (item.getDirectTrans() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getDirectTrans();
				}
				if (item.getIndirectTrans() < 0) {
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = "" + item.getIndirectTrans();
				}
				
				result.add(detailStringArray);
			}
		}
		return result;
	}

	@Override
	public String[] getCsvReportHeader() {
		return headers;
	}

	@Override
	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index] = sumData.getSummaryText();
		
		if (sumData.getSrchs() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getSrchs();
		}
		if (sumData.getSrchuv() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getSrchuv();
		}
		if (sumData.getSrsur() == null || sumData.getSrsur().doubleValue() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			//需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(sumData.getSrsur().doubleValue());
		}
		
		if (sumData.getClks() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getClks();
		}
		if (sumData.getClkuv() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getClkuv();
		}
		if (sumData.getCtr() == null || sumData.getCtr().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df4.format(sumData.getCtr().doubleValue() * 100) + "%";
		}
		if (sumData.getCusur() == null || sumData.getCusur().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			//需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(sumData.getCusur().doubleValue() * 100.0) + "%";
		}
		
		if (sumData.getAcp() == null || sumData.getAcp().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(sumData.getAcp().doubleValue());
		}
		if (sumData.getCocur() == null || sumData.getCocur().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + df2.format(sumData.getCocur().doubleValue());
		}
		
		if (sumData.getCpm() == null || sumData.getCpm().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(sumData.getCpm().doubleValue());
		}
		if (sumData.getCost() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getCost();
		}
		
		// 转化数据不做汇总
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		return summaryStringArray;
	}

	public List<InterestAssistant> getDetails() {
		return details;
	}

	public void setDetails(List<InterestAssistant> details) {
		this.details = details;
	}

	public InterestReportSumData getSumData() {
		return sumData;
	}

	public void setSumData(InterestReportSumData sumData) {
		if (sumData == null) {
			sumData = new InterestReportSumData();
			sumData.generateExtentionFields();
		}
		this.sumData = sumData;
	}
}
