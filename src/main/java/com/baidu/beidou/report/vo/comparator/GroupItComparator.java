package com.baidu.beidou.report.vo.comparator;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.vo.group.InterestAssistant;
import com.baidu.beidou.util.StringUtils;

public class GroupItComparator implements Comparator<InterestAssistant> {
	private int order;
	private String col;
	public GroupItComparator(String col, int order) {
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
	
	public int compare(InterestAssistant o1, InterestAssistant o2) {
		if (o1 == null) {
			return -1 * order;
		}
		if (o2 == null) {
			return 1 * order;
		}
		if (col.equals("interestName")) {
			if (o1.getSelfInterestId() == 0) {
				return 1 * order;
			}
			if (o2.getSelfInterestId() == 0) {
				return -1 * order;
			}
			if ((o1.getOrderId() == -1) && (o2.getOrderId() == -1)) {
				return order * (int) (o1.getSelfInterestId() - o2.getSelfInterestId());
			} else if ((o1.getOrderId() > -1) && (o2.getOrderId() > -1)) {
				return order * (int) (o1.getOrderId() - o2.getOrderId());
			} else {
				return -1 * order * (int) (o1.getOrderId() - o2.getOrderId());
			}
		}
		if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
			return compareByString(o1.getSelfInterest().getPlanName(), o2.getSelfInterest().getPlanName());
		}
		if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
			return compareByString(o1.getSelfInterest().getGroupName(), o2.getSelfInterest().getGroupName());
		}
		
		if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equalsIgnoreCase(col)) {
			return -1 * order * o1.getSelfInterest().getViewState().compareTo(o2.getSelfInterest().getViewState());
		}
		
		if (ReportConstants.PACKNAME.equals(col)) {
			return compareByString(o1.getSelfInterest().getPackName(), o2.getSelfInterest().getPackName());
		}
		
		if (ReportConstants.PRICE.equals(col)) {
			Double b1 = o1.getSelfInterest().getPrice();
			Double b2 = o2.getSelfInterest().getPrice();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}

		if (ReportConstants.SRCHS.equals(col)) {
			if ((o1.getSelfInterest().getSrchs() - o2.getSelfInterest().getSrchs()) == 0) {
				return o1.getSelfInterestId() - o2.getSelfInterestId();
			} else {
				return order * (int) (o1.getSelfInterest().getSrchs() - o2.getSelfInterest().getSrchs());
			}
		}

		if (ReportConstants.SRCHUV.equals(col)) {
			return order * (int) (o1.getSelfInterest().getSrchuv() - o2.getSelfInterest().getSrchuv());
		}

		if (ReportConstants.SRSUR.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getSrsur();
			BigDecimal b2 = o2.getSelfInterest().getSrsur();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}

		if (ReportConstants.CLKS.equals(col)) {
			return order * (int) (o1.getSelfInterest().getClks() - o2.getSelfInterest().getClks());
		}

		if (ReportConstants.CLKUV.equals(col)) {
			return order * (int) (o1.getSelfInterest().getClkuv() - o2.getSelfInterest().getClkuv());
		}

		if (ReportConstants.CTR.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getCtr();
			BigDecimal b2 = o2.getSelfInterest().getCtr();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.CUSUR.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getCusur();
			BigDecimal b2 = o2.getSelfInterest().getCusur();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}

		if (ReportConstants.ACP.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getAcp();
			BigDecimal b2 = o2.getSelfInterest().getAcp();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.COCUR.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getCocur();
			BigDecimal b2 = o2.getSelfInterest().getCocur();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}

		if (ReportConstants.CPM.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getCpm();
			BigDecimal b2 = o2.getSelfInterest().getCpm();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.COST.equals(col)) {
			if (o1.getSelfInterest().getCost() > o2.getSelfInterest().getCost()) {
				return order * 1;
			} else if (o1.getSelfInterest().getCost() < o2.getSelfInterest().getCost()) {
				return order * -1;
			} else {
				return 0;
			}
		}
		
		if (ReportConstants.ARRIVAL_RATE.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getArrivalRate();
			BigDecimal b2 = o2.getSelfInterest().getArrivalRate();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}

		if (ReportConstants.HOP_RATE.equals(col)) {
			BigDecimal b1 = o1.getSelfInterest().getHopRate();
			BigDecimal b2 = o2.getSelfInterest().getHopRate();
			if (b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if (ReportConstants.RES_TIME_STR.equals(col)) {
			return order * o1.getSelfInterest().getResTimeStr().compareTo(o2.getSelfInterest().getResTimeStr());
		}
		
		if (ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
			return order * (int) (o1.getSelfInterest().getDirectTrans() - o2.getSelfInterest().getDirectTrans());
		}
		
		if (ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
			return order * (int) (o1.getSelfInterest().getIndirectTrans() - o2.getSelfInterest().getIndirectTrans());
		}
		return order;
	}
}
