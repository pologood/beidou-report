package com.baidu.beidou.report.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.olap.vo.UnitViewItem;

public class TransReportVo extends AbstractReportVo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3159196718141469210L;
	
	protected List details;
	protected StatInfo summary;
	protected int dimensionType;

	public int getDimensionType() {
		return dimensionType;
	}

	public void setDimensionType(int dimensionType) {
		this.dimensionType = dimensionType;
	}

	@Override
	public List<String[]> getCsvReportDetail() {
		
		List<String[]> rows = new ArrayList<String[]>();
		
		if (dimensionType == ReportWebConstants.TRANS_DATA_DIMENSION.PLAN) {
			List<PlanViewItem> rawDataList = (List<PlanViewItem>) details;
			int columnCount = this.headers.length;
			for (PlanViewItem obj : rawDataList) {
				int i = 0;
				String[] row = new String[columnCount];
				row[i++] = obj.getPlanName();
				row[i++] = "" + obj.getSrchs();
				row[i++] = "" + obj.getClks();
				row[i++] = "" + obj.getCost();
				row[i++] = "" + obj.getDirectTrans();
				row[i++] = "" + obj.getIndirectTrans();
				rows.add(row);
			}
			
		} else if (dimensionType == ReportWebConstants.TRANS_DATA_DIMENSION.GROUP) {
			List<GroupViewItem> rawDataList = (List<GroupViewItem>) details;
			int columnCount = this.headers.length;
			for (GroupViewItem obj : rawDataList) {
				int i = 0;
				String[] row = new String[columnCount];
				row[i++] = obj.getGroupName();
				row[i++] = obj.getPlanName();
				row[i++] = "" + obj.getSrchs();
				row[i++] = "" + obj.getClks();
				row[i++] = "" + obj.getCost();
				row[i++] = "" + obj.getDirectTrans();
				row[i++] = "" + obj.getIndirectTrans();
				rows.add(row);
			}

		} else if (dimensionType == ReportWebConstants.TRANS_DATA_DIMENSION.UNIT) {
			List<UnitViewItem> rawDataList = (List<UnitViewItem>) details;
			int columnCount = this.headers.length;
			for (UnitViewItem obj : rawDataList) {
				int i = 0;
				String[] row = new String[columnCount];
				row[i++] = obj.getTitle();
				row[i++] = obj.getPlanName();
				row[i++] = obj.getGroupName();
				row[i++] = "" + obj.getSrchs();
				row[i++] = "" + obj.getClks();
				row[i++] = "" + obj.getCost();
				row[i++] = "" + obj.getDirectTrans();
				row[i++] = "" + obj.getIndirectTrans();
				rows.add(row);
			}

		} else if (dimensionType == ReportWebConstants.TRANS_DATA_DIMENSION.GROUP_SITE) {
			List<Map<String,Object>> rawDataList = (List<Map<String,Object>>) details;
			int columnCount = this.headers.length;
			for (Map<String,Object> obj : rawDataList) {
				int i = 0;
				String[] row = new String[columnCount];
				row[i++] = obj.get(ReportConstants.MAINSITE).toString();
				row[i++] = obj.get("planName").toString();
				row[i++] = obj.get("groupName").toString();
				row[i++] = obj.get(ReportConstants.SRCHS).toString();
				row[i++] = obj.get(ReportConstants.CLKS).toString();
				row[i++] = "" + obj.get(ReportConstants.COST).toString();
				row[i++] = obj.get(ReportConstants.DIRECT_TRANS_CNT).toString();
				row[i++] = obj.get(ReportConstants.INDIRECT_TRANS_CNT).toString();
				rows.add(row);
			}
		} 
		
		
		return rows;
	}

	@Override
	public String[] getCsvReportHeader() {
		return headers;
	}

	public StatInfo getSummary() {
		return summary;
	}

	public void setSummary(StatInfo summary) {
		this.summary = summary;
	}

	@Override
	public String[] getCsvReportSummary() {
		
		int columnCount = this.headers.length;
		String[] sumRow = new String[columnCount];
		for (int i = 0; i < columnCount; i++) {
			if (i == columnCount -3 ) {
				String costSum = "" + summary.getCost();//倒数第三列为消费
				sumRow[i] = costSum;
			} else if (i == columnCount - 4) {
				String clksSum = "" + summary.getClks();//倒数第四列为消费
				sumRow[i] = clksSum;
			} else if (i == columnCount - 5) {
				String srchsSum = "" + summary.getSrchs();//倒数第五列为消费
				sumRow[i] = srchsSum;
			} else {
				sumRow[i] = "";//其它列不汇总
			}
		}
		return sumRow;		
	}

	public List getDetails() {
		return details;
	}

	public void setDetails(List details) {
		this.details = details;
	}
	
}
