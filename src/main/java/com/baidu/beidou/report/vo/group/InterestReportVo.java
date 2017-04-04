package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.Interest;
import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.olap.vo.InterestViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;

@SuppressWarnings("serial")
public class InterestReportVo extends AbstractReportVo {
	
	private List<InterestAssistant> details;
	private InterestReportSumData sumData;
	private Map<Integer,Interest> interestMap;
	
	/** 推广计划列下标 */
	protected static final int PLANNAME_INDEX = 3;
	
	public InterestReportVo(){}
	
	public InterestReportVo(String level){
		super(level);
	}

	@Override
	public List<String[]> getCsvReportDetail() {
		if(CollectionUtils.isEmpty(details)){
			return new ArrayList<String[]>(0);
		}
		List<String[]> result = new ArrayList<String[]>(details.size());		
		String[] detailStringArray ;
		for (InterestAssistant items : details ){
			for(InterestViewItem item : items.getInterestViewItem()){
				int index = 0;
				detailStringArray = new String[headers.length];

				//为一级兴趣或兴趣组合
				if(item.getType()!=2){
					detailStringArray[index] = item.getInterestName();
					detailStringArray[++index] = ""; 
				//为二级兴趣，需要补足一级兴趣名称
				}else{
					int firstInterestId = item.getFirstInterestId();
					detailStringArray[index] = this.interestMap.get(firstInterestId).getName();
					detailStringArray[++index] = item.getInterestName();
				}
				
				if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
					//账户层级才显示该列
					detailStringArray[++index] = item.getPlanName();
				}
				if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)
						|| QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)){
					detailStringArray[++index] = item.getGroupName();
				}
				
				//推广组状态
				detailStringArray[++index] = CproGroupConstant.GROUP_VIEW_STATE_NAME[item.getViewState()];
				
				// 受众组合列和出价列
				detailStringArray[++index] = item.getPackName();
				detailStringArray[++index] = String.valueOf(item.getPrice());
				
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
		//需要根据Level隐藏掉一些列
		int colNum = headers.length;
		int increment = 0;//索引下标增加值
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan列，所以总列数要-1
			colNum -=1;
			increment = 1;//需要跳过plan
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan和Group列，所以总列数要-2
			colNum -= 2;
			increment = 2;//需要跳过plan和group
		}
		String[] result = new String[colNum];
		for ( int col = 0; col < colNum; col++ ) {
			if (col < (PLANNAME_INDEX - 1) ) {//小于groupName的列可以直接输出，大于等于的时候可能需要略过该列。
				result[col] = headers[col];
			} else {
				//从第二列开始可能需要+increment
				result[col] = headers[col + increment];
			}
		}
		return result;
	}

	@Override
	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index++] = sumData.getSummaryText();
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
			summaryStringArray[++index] = "";
		}
		if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)
				|| QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)){
			summaryStringArray[++index] = "";
		}
		
		// 推广组状态
		summaryStringArray[++index] = "";
		
		// 新增受众组合以及出价列
		summaryStringArray[++index] = "";
		summaryStringArray[++index] = "";
		
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

	public void setSumData(InterestReportSumData sumData) {
		if(sumData == null){
			sumData = new InterestReportSumData();
			sumData.generateExtentionFields();
		}
		this.sumData = sumData;
	}

	public Map<Integer, Interest> getInterestMap() {
		return interestMap;
	}

	public void setInterestMap(Map<Integer, Interest> interestMap) {
		this.interestMap = interestMap;
	}
	
}
