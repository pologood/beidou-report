package com.baidu.beidou.report.vo.comparator;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.olap.vo.GroupKtViewItem;
import com.baidu.beidou.util.StringUtils;

/**
 * ClassName: GroupKtComparator
 * Function: 关键词比较器，用于关键词tab以及受众组合详情页关键词子tab
 *
 * @author genglei
 * @version beidou 3 plus
 * @date 2012-9-10
 */
public class GroupKtComparator implements Comparator<GroupKtViewItem> {
	private int order;
	private String col;
	public GroupKtComparator(String col, int order) {
		this.col = col;
		this.order = order;
	}
	
	private int compareByString(String v1, String v2) {
		Collator collator=Collator.getInstance(java.util.Locale.CHINA);
		if(StringUtils.isEmpty(v1)) {
			return -1 * order;
		}
		if(StringUtils.isEmpty(v2)) {
			return order;
		}		
		return order * collator.compare(v1,v2);
	}
	
	public int compare(GroupKtViewItem o1, GroupKtViewItem o2) {
		if (o1 == null) {
			return -1 * order;
		}
		if (o2 == null) {
			return 1 * order;
		}
		if (ReportConstants.KEYWORD.equals(col)) {
			return compareByString(o1.getKeyword(), o2.getKeyword());
		}
		if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
			return compareByString(o1.getPlanName(), o2.getPlanName());
		}
		if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
			return compareByString(o1.getGroupName(), o2.getGroupName());
		}
		if (ReportConstants.PACKNAME.equals(col)) {
			return compareByString(o1.getPackName(), o2.getPackName());
		}
		
		if (ReportConstants.KT_QUALITY.equals(col)) {
			return order * o1.getQualityDg().compareTo(o2.getQualityDg());
		}
		
		if (ReportConstants.SRCHS.equals(col)) {
			return order * (int) (o1.getSrchs() - o2.getSrchs());
		}
		
		if (ReportConstants.SRCHUV.equals(col)) {
			return order * (int) (o1.getSrchuv() - o2.getSrchuv());
		}
		
		if (ReportConstants.SRSUR.equals(col)) {
			BigDecimal b1 = o1.getSrsur();
			BigDecimal b2 = o2.getSrsur();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.CLKS.equals(col)) {
			return order * (int) (o1.getClks() - o2.getClks());
		}
		
		if (ReportConstants.CLKUV.equals(col)) {
			return order * (int) (o1.getClkuv() - o2.getClkuv());
		}
		
		if (ReportConstants.CTR.equals(col)) {
			BigDecimal b1 = o1.getCtr();
			BigDecimal b2 = o2.getCtr();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.CUSUR.equals(col)) {
			BigDecimal b1 = o1.getCusur();
			BigDecimal b2 = o2.getCusur();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.ACP.equals(col)) {
			BigDecimal b1 = o1.getAcp();
			BigDecimal b2 = o2.getAcp();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.COCUR.equals(col)) {
			BigDecimal b1 = o1.getCocur();
			BigDecimal b2 = o2.getCocur();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.CPM.equals(col)) {
			BigDecimal b1 = o1.getCpm();
			BigDecimal b2 = o2.getCpm();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.COST.equals(col)) {
			if (o1.getCost() > o2.getCost()) {
				return order * 1;
			} else if (o1.getCost() < o2.getCost()) {
				return order * -1;
			} else {
				return 0;
			}
		}
		
		if (ReportConstants.ARRIVAL_RATE.equals(col)) {
			BigDecimal b1 = o1.getArrivalRate();
			BigDecimal b2 = o2.getArrivalRate();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}

		if (ReportConstants.HOP_RATE.equals(col)) {
			BigDecimal b1 = o1.getHopRate();
			BigDecimal b2 = o2.getHopRate();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.RES_TIME_STR.equals(col)) {
			return order * o1.getResTimeStr().compareTo(o2.getResTimeStr());
		}
		
		if (ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
			return order * (int) (o1.getDirectTrans() - o2.getDirectTrans());
		}
		
		if (ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
			return order * (int) (o1.getIndirectTrans() - o2.getIndirectTrans());
		}
		
		return order;
	}
}
