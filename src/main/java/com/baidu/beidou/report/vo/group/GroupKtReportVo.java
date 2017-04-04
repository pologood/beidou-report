package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.olap.vo.GroupKtViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class GroupKtReportVo extends AbstractReportVo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4348407897436610454L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers; 
	/** 明细 */
	protected List<GroupKtViewItem> details;
	/** 汇总信息 */
	protected GroupKtReportSumData summary;
	
	/** 是否为标准设置词表：如果是标准的，则没有受众组合那一列 */
	protected boolean isNormalReport;
	
	/** 需隐藏列下标 */
	protected static final int PLANNAME_INDEX = 3;
	protected static final int GROUPNAME_INDEX = 4;
	protected static final int PACKID_INDEX = 5;
	protected static final int PACKNAME_INDEX = 6;
	
	public GroupKtReportVo(String level) {
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
		for (GroupKtViewItem item : details ){
			int index = 0;
			detailStringArray = new String[headers.length];
			
			// 添加wordId列
			detailStringArray[index] = String.valueOf(item.getWordId());
			if(item.isHasDel()) {
				detailStringArray[++index] = item.getKeyword() + "(已删除)";
			} else {
				detailStringArray[++index] = item.getKeyword();
			}
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
				//账户层级才显示该列
				detailStringArray[++index] = item.getPlanName();
			}
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level) 
					|| QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)){
				// 账户层级或者计划层级显示该列
				detailStringArray[++index] = item.getGroupName();
			}
			if (!isNormalReport) {
				// 受众组合词表显示gpId和受众组合列
				detailStringArray[++index] = String.valueOf(item.getGpId());
				detailStringArray[++index] = item.getPackName();
			}
			
			switch(item.getQualityDg()){//删除二星即受限，二三星合并，added by liuhao05 since cpwep443
			case CproGroupConstant.KT_WORD_QUALITY_DEGREE_1:
				detailStringArray[++index] = "禁止";
				break;
			case CproGroupConstant.KT_WORD_QUALITY_DEGREE_3:
				detailStringArray[++index] = "正常";
				break;
			default:
				detailStringArray[++index] = "正常";
				break;
			}
			
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
				detailStringArray[++index] = "" + df2.format(item.getAcp().doubleValue());
			}
			if (item.getCocur() == null || item.getCocur().doubleValue() < 0 ) {
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = "" + df2.format(item.getCocur().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0 ) {
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = "" + df2.format(item.getCpm().doubleValue());
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
		return result;
	}

	public String[] getCsvReportHeader() {

		//需要根据Level隐藏掉一些列
		int preColNum = headers.length;
		int colNum = preColNum;
		boolean planNameFlag = false;
		boolean groupNameFlag = false;
		boolean packIdFlag = false;
		boolean packNameFlag = false;
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan列，所以总列数要-1
			colNum -=1;
			planNameFlag = true;//需要跳过plan
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan和Group列，所以总列数要-2
			colNum -=2;
			planNameFlag = true;//需要跳过plan和group
			groupNameFlag = true;
		}
		if (isNormalReport) {
			// 如果是标准词表则需要隐藏掉gpId和受众组合列，所以总列数要-2
			colNum -= 2;
			packIdFlag = true;;
			packNameFlag = true;;
		}
		
		String[] result = new String[colNum];
		int index = 0;
		for ( int col = 0; col < preColNum; col++ ) {
			if ((col == (PLANNAME_INDEX - 1) && planNameFlag)
					|| (col == (GROUPNAME_INDEX - 1) && groupNameFlag)
					|| (col == (PACKID_INDEX - 1) && packIdFlag)
					|| (col == (PACKNAME_INDEX - 1) && packNameFlag)) {
				continue;
			} else {
				result[index++] = headers[col];
			}
		}
		return result;
	}

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		// 汇总信息的“wordId”列不显示
		summaryStringArray[index] = String.valueOf("");
		summaryStringArray[++index] = summary.getSummaryText();
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {//过滤Plan和GROUP两列
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
		} else if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)) {//只过滤group一列
			summaryStringArray[++index] = "";
		}
		if (!isNormalReport) {
			// 受众组合词表增加该gpId和受众组合列
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
		}
		
		summaryStringArray[++index] = "";
		
		if (summary.getSrchs() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getSrchs();
		}
		if (summary.getSrchuv() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getSrchuv();
		}
		if (summary.getSrsur() == null || summary.getSrsur().doubleValue() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			//需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(summary.getSrsur().doubleValue());
		}
		
		if (summary.getClks() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getClks();
		}
		if (summary.getClkuv() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getClkuv();
		}
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			//需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(summary.getCtr().doubleValue() * 100.0) + "%";
		}
		if (summary.getCusur() == null || summary.getCusur().doubleValue() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			//需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(summary.getCusur().doubleValue() * 100.0) + "%";
		}
		
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}
		
		if (summary.getCocur() == null || summary.getCocur().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + df2.format(summary.getCocur().doubleValue());
		}
		
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm().doubleValue());
		}
		if (summary.getCost() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getCost();
		}
		
		// 转化数据不做汇总
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		summaryStringArray[++index] = "-" ;
		
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

	public List<GroupKtViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<GroupKtViewItem> details) {
		this.details = details;
	}

	public GroupKtReportSumData getSummary() {
		return summary;
	}

	public void setSummary(GroupKtReportSumData summary) {
		this.summary = summary;
	}


	public boolean isNormalReport() {
		return isNormalReport;
	}


	public void setNormalReport(boolean isNormalReport) {
		this.isNormalReport = isNormalReport;
	}
}
