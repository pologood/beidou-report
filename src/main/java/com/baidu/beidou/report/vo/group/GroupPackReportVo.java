package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.olap.vo.GroupPackViewItem;
import com.baidu.beidou.pack.constant.PackTypeConstant;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class GroupPackReportVo extends AbstractReportVo {

	private static final long serialVersionUID = 4282488146198415785L;

	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
	protected String[] headers;
	/** 明细 */
	protected List<GroupPackViewItem> details;
	/** 汇总信息 */
	protected GroupPackReportSumData summary;

	/** 是否为标准设置词表：如果是标准的，则没有受众组合那一列 */
	protected boolean isNormalReport;

	/** 需隐藏列下标 */
	protected static final int PLANNAME_INDEX = 2;
	protected static final int GROUPNAME_INDEX = 3;

	public GroupPackReportVo(String level) {
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

	public List<String[]> getCsvReportDetail() {

		List<String[]> result = new ArrayList<String[]>(details.size());

		String[] detailStringArray;
		for (GroupPackViewItem item : details) {
			int index = 0;
			detailStringArray = new String[headers.length];

			if (item.isHasDel()) {
				detailStringArray[index] = item.getPackName() + "(已删除)";
			} else {
				detailStringArray[index] = item.getPackName();
			}

			
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
				// 账户层级才显示该列
				detailStringArray[++index] = item.getPlanName();
			}
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)
					|| QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)) {
				// 账户层级或者计划层级显示该列
				detailStringArray[++index] = item.getGroupName();
			}
			
			Integer packType = item.getPackType()==null ? PackTypeConstant.TYPE_ADVANCED_PACK : item.getPackType();
			
			switch (packType) {
			case PackTypeConstant.TYPE_INTEREST_PACK:
				detailStringArray[++index] = "兴趣组合";
				break;
			case PackTypeConstant.TYPE_VISIT_PEOPLE_PACK:
				detailStringArray[++index] = "到访人群";
				break;
			case PackTypeConstant.TYPE_KEYWORD_PACK:
				detailStringArray[++index] = "关键词组合";
				break;
			case PackTypeConstant.TYPE_ADVANCED_PACK:
				detailStringArray[++index] = "高级组合";
				break;
			default:
				detailStringArray[++index] = "高级组合";
				break;
			}
			
			if (item.getPrice() < 0) {
				detailStringArray[++index] = "-";
			} else {
				// 需要乘虚而入100来表示百分比值
				detailStringArray[++index] = df4.format(item.getPrice());
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
				detailStringArray[++index] = "-";
			} else {
				// 需要乘虚而入100来表示百分比值
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
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				// 需要乘虚而入100来表示百分比值
				detailStringArray[++index] = df4.format(item.getCtr().doubleValue() * 100.0) + "%";
			}
			if (item.getCusur() == null || item.getCusur().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				// 需要乘虚而入100来表示百分比值
				detailStringArray[++index] = df4.format(item.getCusur().doubleValue() * 100.0) + "%";
			}

			if (item.getAcp() == null || item.getAcp().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = "" + df2.format(item.getAcp().doubleValue());
			}
			if (item.getCocur() == null || item.getCocur().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = "" + df2.format(item.getCocur().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				detailStringArray[++index] = "" + df2.format(item.getCpm().doubleValue());
			}
			if (item.getCost() < 0) {
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = "" + item.getCost();
			}

			if (item.getArrivalRate() == null
					|| item.getArrivalRate().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				// 需要乘虚而入100来表示百分比值
				detailStringArray[++index] = df2.format(item.getArrivalRate().doubleValue() * 100.0) + "%";
			}

			if (item.getHopRate() == null
					|| item.getHopRate().doubleValue() < 0) {
				detailStringArray[++index] = "-";
			} else {
				// 需要乘虚而入100来表示百分比值
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
		int colNum = headers.length;
		int increment = 0;//索引下标增加值
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan列，所以总列数要-1
			colNum -=1;
			increment = 1;//需要跳过plan
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(getLevel())) {
			//如果不是账户层级则需要隐藏掉plan和Group列，所以总列数要-2
			colNum -=2;
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

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		summaryStringArray[index] = summary.getSummaryText();
		
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {// 过滤Plan和GROUP两列
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
		} else if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)) {// 只过滤group一列
			summaryStringArray[++index] = "";
		}

		summaryStringArray[++index] = "";
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
			summaryStringArray[++index] = "-";
		} else {
			// 需要乘虚而入100来表示百分比值
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
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0) {
			summaryStringArray[++index] = "-";
		} else {
			// 需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(summary.getCtr().doubleValue() * 100.0) + "%";
		}
		if (summary.getCusur() == null || summary.getCusur().doubleValue() < 0) {
			summaryStringArray[++index] = "-";
		} else {
			// 需要乘虚而入100来表示百分比值
			summaryStringArray[++index] = df4.format(summary.getCusur().doubleValue() * 100.0) + "%";
		}

		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0) {
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}

		if (summary.getCocur() == null || summary.getCocur().doubleValue() < 0) {
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = "" + df2.format(summary.getCocur().doubleValue());
		}

		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0) {
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm()
					.doubleValue());
		}
		if (summary.getCost() < 0) {
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = "" + summary.getCost();
		}

		// 转化数据不做汇总
		summaryStringArray[++index] = "-";
		summaryStringArray[++index] = "-";
		summaryStringArray[++index] = "-";
		summaryStringArray[++index] = "-";
		summaryStringArray[++index] = "-";

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

	public List<GroupPackViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<GroupPackViewItem> details) {
		this.details = details;
	}

	public GroupPackReportSumData getSummary() {
		return summary;
	}

	public void setSummary(GroupPackReportSumData summary) {
		this.summary = summary;
	}
}
