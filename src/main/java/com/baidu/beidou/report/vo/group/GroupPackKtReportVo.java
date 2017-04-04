package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.olap.vo.GroupKtViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class GroupPackKtReportVo extends AbstractReportVo {
	
	private static final long serialVersionUID = -7434240776070437674L;
	
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers; 
	/** 明细 */
	protected List<GroupKtViewItem> details;
	/** 汇总信息 */
	protected GroupKtReportSumData summary;
	
	public GroupPackKtReportVo(String level) {
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
		return headers;
	}

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		// 汇总信息的“wordId”列不显示
		summaryStringArray[index] = String.valueOf("");
		summaryStringArray[++index] = summary.getSummaryText();
		
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
		if (summary.getCusur() == null || summary.getCusur().doubleValue() < 0 ) {
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
		
		if (summary.getCocur() == null || summary.getCocur().doubleValue() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + df2.format(summary.getCocur().doubleValue());
		}
		
		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0) {
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
}
