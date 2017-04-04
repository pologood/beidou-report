package com.baidu.beidou.report.vo.unit;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.util.GroupTypeUtil;
import com.baidu.beidou.cprounit.constant.CproUnitConstant;
import com.baidu.beidou.cprounit.validate.ImageScale;
import com.baidu.beidou.olap.vo.UnitViewItem;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

public class UnitReportVo extends AbstractReportVo {
	
	private static Log LOG = LogFactory.getLog(UnitReportVo.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -4325401473728787662L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：含planName,groupname列。 */
	protected String[] headers; 
	/** 明细 */
	protected List<UnitViewItem> details;
	/** 汇总信息 */
	protected UnitReportSumData summary;
	/** 推广计划列下标 */
	protected static final int PLANNAME_INDEX = 9;  // modified by kanghongwei since cpweb429(qtIM)
	/** 推广组列下标 */
	@Deprecated
	protected static final int GROUPNAME_INDEX = 9;  // modified by kanghongwei since cpweb429(qtIM)
	
	/** 描述一、二在图片创意下显示的内容 */
	protected static final String NOINFO= "N/A";
	
	protected boolean showTransData;//是否显示转化数据列
	protected boolean transDataValid;//如果显示转化数据列，那么非valid的情况下应该显示-
	
	public UnitReportVo(String level) {
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
		for (UnitViewItem item : details ){
			int index = 0;
			detailStringArray = new String[headers.length];
			
			String ideaTypeName = getUnitDisplayTypeString(item);

			// 新增推广单元id(add by kanghongwei since cpweb429 qtim)
			detailStringArray[index] = String.valueOf(item.getUnitId());
			detailStringArray[++index] = ideaTypeName;
			detailStringArray[++index] = item.getTitle() == null ? "" : item.getTitle();
			if (item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL || item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL_WITH_ICON) {
				detailStringArray[++index] = item.getDescription1() == null ? "" : item.getDescription1();
				detailStringArray[++index] = item.getDescription2() == null ? "" : item.getDescription2();
			} else {
				detailStringArray[++index] = NOINFO; 
				detailStringArray[++index] = NOINFO;
			}
			detailStringArray[++index] = item.getShowUrl() == null ? "" : item.getShowUrl();
			detailStringArray[++index] = item.getTargetUrl() == null ? "" : item.getTargetUrl();
			
			if((item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_PICTURE || item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_FLASH)
					&& item.getWidth() > 0 && item.getHeight() > 0 ) {
				detailStringArray[++index] = item.getWidth() + " * " + item.getHeight();
			} else {
				detailStringArray[++index] = "";
			}

			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {
				//账户层级才显示该列
				detailStringArray[++index] = item.getPlanName();
			}
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level) 
					|| QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)){
				detailStringArray[++index] = item.getGroupName();
			}
			
			detailStringArray[++index] = CproUnitConstant.UNIT_STATE_NAME[item.getViewState()];
			detailStringArray[++index] = "" + item.getSrchs();//展现次数
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


	private String getUnitDisplayTypeString(UnitViewItem unit) {
		//需要展现成：文字、固定图片、悬浮图片、贴片图片

		String ideaTypeName = "";
		if(unit.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL || unit.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL_WITH_ICON) {
			//文字或图文
			ideaTypeName = CproUnitConstant.WULIAO_TYPE_NAME[unit.getWuliaoType()];
		} else {
			//展现类型+图片
			ideaTypeName = getImageUnitDisplayTypeName(unit.getGroupType(),unit.getWidth(),unit.getHeight())
			                   + CproUnitConstant.WULIAO_TYPE_NAME[CproUnitConstant.MATERIAL_TYPE_PICTURE];;
		}
		return ideaTypeName;
	}


	private static String getImageUnitDisplayTypeName(int groupType,int width,int height) {
		int[] allGroupTypes = new int[]{
				CproGroupConstant.GROUP_TYPE_FIXED,
				CproGroupConstant.GROUP_TYPE_FLOW,
				CproGroupConstant.GROUP_TYPE_FILM
		};
		for (int singleBitGroupType : allGroupTypes) {
			if (GroupTypeUtil.isIntersectionNotEmpty(groupType, singleBitGroupType)
					&&
				ImageScale.isImageSizeValid(singleBitGroupType, width, height)
			) {
				return GroupTypeUtil.getGroupTypeString(singleBitGroupType);
			}
		}
		String message = "不支持的展示类型或尺寸：groupType={0},width={1},height={2}";
		LOG.error(message);
		return "未知类型";
//		throw new IllegalArgumentException(MessageFormat.format(message, groupType, width, height));
	}

	public static void main(String[] args) {
		System.out.println(getImageUnitDisplayTypeName(2,300,250));
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
			if (col < (PLANNAME_INDEX - 1 )) {//小于planName的列可以直接输出，大于等于的时候可能需要略过该列。
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
		// 汇总信息的“创意id”列不显示(add by kanghongwei since cpweb429 qtim)
		summaryStringArray[index] = String.valueOf("");
		summaryStringArray[++index] = summary.getSummaryText();
		summaryStringArray[++index] = "";//标题
		summaryStringArray[++index] = "";//描述1
		summaryStringArray[++index] = "";//描述2
		summaryStringArray[++index] = "";//显示链接
		summaryStringArray[++index] = "";//点击链接
		summaryStringArray[++index] = "";//尺寸
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(level)) {//过滤Plan和GROUP两列
			summaryStringArray[++index] = "";
			summaryStringArray[++index] = "";
		} else if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(level)) {//只过滤group一列
			summaryStringArray[++index] = "";
		}
		summaryStringArray[++index] = "";//状态
		summaryStringArray[++index] = "" + summary.getSrchs();
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

	public List<UnitViewItem> getDetails() {
		return details;
	}

	public void setDetails(List<UnitViewItem> details) {
		this.details = details;
	}

	public UnitReportSumData getSummary() {
		return summary;
	}

	public void setSummary(UnitReportSumData summary) {
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
