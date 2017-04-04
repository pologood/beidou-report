package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.util.GroupTypeUtil;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class GroupReportVo extends AbstractReportVo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 811342840320886519L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers; 
	/** 明细 */
	protected List<GroupViewItem> details;
	/** 汇总信息 */
	protected GroupReportSumData summary;
	
	/** 推广计划列下标 */
	protected static final int PLANNAME_INDEX = 2;
	
	protected boolean showTransData;//是否显示转化数据列
	protected boolean transDataValid;//如果显示转化数据列，那么非valid的情况下应该显示-
	
	
	public GroupReportVo(String level) {
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
		for (GroupViewItem item : details ){
			int index = 0;
			detailStringArray = new String[headers.length];
			detailStringArray[index] = item.getGroupName();//推广组名
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
				//账户层级才显示该列
				detailStringArray[++index] = item.getPlanName();//推广计划名
			}
			detailStringArray[++index] = CproGroupConstant.GROUP_VIEW_STATE_NAME[item.getViewState()];//推广组状态
			detailStringArray[++index] = GroupTypeUtil.getGroupTypeString(item.getGroupType());//推广组类型
			detailStringArray[++index] = "" + item.getPrice();//出价
			
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
		return result;
	}

	public String[] getCsvReportHeader() {

		//需要根据Level隐藏掉一些列
		int colNum = headers.length;
		int increment = 0;//索引下标增加值
		if (!QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan列，所以总列数要-1
			colNum -=1;
			increment = 1;//需要跳过plan
		}
		String[] result = new String[colNum];
		for ( int col = 0; col < colNum; col++ ) {
			if (col < (PLANNAME_INDEX-1)) {//小于planName的列可以直接输出，大于等于的时候可能需要略过该列。
				result[col] = headers[col];
			} else {
				//从第三列开始可能需要+1
				result[col] = headers[col + increment];
			}
		}
		return result;
	}

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index] = summary.getSummaryText();
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {//过滤Plan
			summaryStringArray[++index] = "";
		}
		summaryStringArray[++index] = "";//状态
		summaryStringArray[++index] = "";//展现类型
		summaryStringArray[++index] = "";//出价
		
		if ( summary.getSrchs() < 0 ) {//展现次数
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getSrchs();
		}
		
		summaryStringArray[++index] = -1 == summary.getSrchuv() ? "-" : String.valueOf(summary.getSrchuv());//展现独立访客
		summaryStringArray[++index] = "";//展现频次
		summaryStringArray[++index] = "" + summary.getClks();
		summaryStringArray[++index] = -1 == summary.getClkuv() ? "-" : String.valueOf(summary.getClkuv());//点击独立访客
		
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df6.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		summaryStringArray[++index] = "";//独立访客点击率
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}
		summaryStringArray[++index] = "";//平均独立访客点击价格
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {//千次展现成本
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

	public GroupReportSumData getSummary() {
		return summary;
	}

	public void setSummary(GroupReportSumData summary) {
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
