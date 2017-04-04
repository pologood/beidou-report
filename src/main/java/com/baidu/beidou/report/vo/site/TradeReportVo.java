package com.baidu.beidou.report.vo.site;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.olap.vo.TradeViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;

@SuppressWarnings("serial")
public class TradeReportVo extends AbstractReportVo {
	
	private List<TradeAssistant> details;
	private TradeSumData sumData;
	
	protected boolean showTransData;//是否显示转化数据列
	protected boolean transDataValid;//如果显示转化数据列，那么非valid的情况下应该显示-
	
	public TradeReportVo(){}
	
	public TradeReportVo(String level){
		super(level);
	}

	@Override
	public List<String[]> getCsvReportDetail() {
		if(CollectionUtils.isEmpty(details)){
			return new ArrayList<String[]>(0);
		}
		List<String[]> result = new ArrayList<String[]>(details.size());		
		String[] detailStringArray ;
		for (TradeAssistant items : details ){
			for(TradeViewItem item : items.getTradeViewItem()){
				int index = 0;
				detailStringArray = new String[headers.length];

				//modify by dongying since cpweb-611-1
				if(item.getSecondTradeId() == 0){
					detailStringArray[index] = item.getFirstTrade();
					detailStringArray[++index] = "";
				}else{
					detailStringArray[index] = item.getFirstTrade();
					detailStringArray[++index] = item.getSecondTrade();
				}
				
				if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
					//账户层级才显示该列
					detailStringArray[++index] = item.getPlanName();
				}
				if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)
						|| QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)){
					detailStringArray[++index] = item.getGroupName();
				}
				detailStringArray[++index] = CproGroupConstant.GROUP_VIEW_STATE_NAME[item.getViewState()];		
				detailStringArray[++index] = "" + item.getPrice();

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
					detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
				}
				if (item.getCusur() == null || item.getCusur().doubleValue() < 0 ) {//独立访客点击率
					detailStringArray[++index] = "-" ;
				} else {
					detailStringArray[++index] = df6.format(item.getCusur().doubleValue() * 100) + "%";
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
						if ( item.getDirectTrans() < 0 ) {//直接转化
							detailStringArray[++index] = "-" ;
						} else {
							detailStringArray[++index] = "" + item.getDirectTrans();
						}
						if ( item.getIndirectTrans() < 0 ) {//间接转化
							detailStringArray[++index] = "-" ;
						} else {
							detailStringArray[++index] = "" + item.getIndirectTrans();
						}						
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
		summaryStringArray[index++] = super.summaryText;
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
			summaryStringArray[++index] = "";
		}
		if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)
				|| QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)){
			summaryStringArray[++index] = "";
		}
		summaryStringArray[++index] = "";//状态
		summaryStringArray[++index] = "";//出价
		if ( sumData.getSrchs() < 0 ) {//展现次数
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getSrchs();
		}
		
		if ( sumData.getSrchuv() < 0 ) {//展现独立访客
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getSrchuv();
		}
		
		summaryStringArray[++index] = "";//展现频次
		summaryStringArray[++index] = "" + sumData.getClks();
		
		if ( sumData.getClkuv() < 0 ) {//点击独立访客
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + sumData.getClkuv();
		}
		
		if (sumData.getCtr() == null || sumData.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df6.format(sumData.getCtr().doubleValue()  * 100 ) + "%";
		}
		summaryStringArray[++index] = "";//独立访客点击率
		if (sumData.getAcp() == null || sumData.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(sumData.getAcp().doubleValue());
		}
		summaryStringArray[++index] = "";//平均独立访客点击价格
		if (sumData.getCpm() == null || sumData.getCpm().doubleValue() < 0 ) {//千次展现成本
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(sumData.getCpm().doubleValue());
		}
		summaryStringArray[++index] = "" + sumData.getCost();//消费
		
		if (showTransData) {
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
		}
		
		return summaryStringArray;
	}

	public List<TradeAssistant> getDetails() {
		return details;
	}

	public void setDetails(List<TradeAssistant> details) {
		this.details = details;
	}

	public void setSumData(TradeSumData sumData) {
		if(sumData == null){
			sumData = new TradeSumData();
			sumData.generateExtentionFields();
		}
		this.sumData = sumData;
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
