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

public class UnitBySiteCustomReportVo extends AbstractReportVo {
	
	private static Log LOG = LogFactory.getLog(UnitReportVo.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -4325401473728787556L;
	/** 账户信息 */
	protected ReportAccountInfo accountInfo;
	/** 列头：含planName,groupname列。 */
	protected String[] headers; 
	/** 明细 */
	protected List<UnitViewItem> details;
	/** 汇总信息 */
	protected UnitReportSumData summary;
	
	/** 描述一、二在图片创意下显示的内容 */
	protected static final String NOINFO= "N/A";
	
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

		//添加计划、推广组信息
		result.addAll(accountInfo.getAccountInfoList());
		
		return result;
	}

	public List<String[]>  getCsvReportDetail() {

		List<String[]> result = new ArrayList<String[]>(details.size());
		
		String[] detailStringArray ;
		for (UnitViewItem item : details ){
			int index = 0;
			detailStringArray = new String[headers.length];
			
			
			detailStringArray[index] = item.getDateRange();
			String wuliaoTypeName = item.getWuliaoTypeName();
			detailStringArray[++index] = wuliaoTypeName;
			detailStringArray[++index] = item.getTitle() == null ? "" : item.getTitle();
			
			if (item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL || item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL_WITH_ICON) {
				detailStringArray[++index] = item.getDescription1() == null ? "" : item.getDescription1();
				detailStringArray[++index] = item.getDescription2() == null ? "" : item.getDescription2();
			} else {
				detailStringArray[++index] = NOINFO; 
				detailStringArray[++index] = NOINFO;
			}
			
			detailStringArray[++index] = item.getSiteUrl() == null ? "" : item.getSiteUrl();
			
			detailStringArray[++index] = "" + item.getSrchs();//展现次数
			detailStringArray[++index] = "" + item.getClks(); //点击次数
			
			if (item.getCtr() == null || item.getCtr().doubleValue() < 0 ) {//点击率
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df6.format(item.getCtr().doubleValue() * 100) + "%";
			}
			if (item.getAcp() == null || item.getAcp().doubleValue() < 0 ) {//平均点击价格
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getAcp().doubleValue());
			}
			if (item.getCpm() == null || item.getCpm().doubleValue() < 0 ) {
				detailStringArray[++index] = "-" ;
			} else {
				detailStringArray[++index] = df2.format(item.getCpm().doubleValue());
			}
			
			detailStringArray[++index] = "" + item.getCost(); 
			
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
		
		return headers;
	}

	public String[] getCsvReportSummary() {
		String[] summaryStringArray = new String[headers.length];
		int index = 0;
		
		summaryStringArray = new String[headers.length];
		
		summaryStringArray[index] = summaryText;	//日期范围&总计
		summaryStringArray[++index] = "";	//类型
		summaryStringArray[++index] = "";	//标题
		summaryStringArray[++index] = "";	//描述1
		summaryStringArray[++index] = "";	//描述2
		summaryStringArray[++index] = "";	//网站

		summaryStringArray[++index] = "" + summary.getSrchs();
		summaryStringArray[++index] = "" + summary.getClks();	
		if (summary.getCtr() == null || summary.getCtr().doubleValue() < 0 ) {//点击率
			summaryStringArray[++index] = "-";
		} else {
			summaryStringArray[++index] =df6.format(summary.getCtr().doubleValue()  * 100 ) + "%";
		}
		if (summary.getAcp() == null || summary.getAcp().doubleValue() < 0 ) {//平均点击价格
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getAcp().doubleValue());
		}

		if (summary.getCpm() == null || summary.getCpm().doubleValue() < 0 ) {//千次展现成本
			summaryStringArray[++index] = "-" ;
		} else {
			summaryStringArray[++index] = df2.format(summary.getCpm().doubleValue());
		}
		summaryStringArray[++index] = "" + summary.getCost();//消费
		
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
}
