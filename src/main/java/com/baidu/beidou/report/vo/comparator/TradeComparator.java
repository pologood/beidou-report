package com.baidu.beidou.report.vo.comparator;

import java.util.Comparator;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.site.TradeAssistant;

public class TradeComparator extends AbstractComparator implements Comparator<TradeAssistant>{
	public TradeComparator(QueryParameter qp){
		super(qp);
	}

	public int compare(TradeAssistant o1, TradeAssistant o2) {
		int ret = 0;
		if(ReportConstants.SRCHS.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareBySrchs(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.CLKS.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByClks(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.COST.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByCost(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.CPM.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByCpm(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.CTR.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByCtr(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.ACP.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByAcp(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.SRCHUV.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareBySrchuv(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.CLKUV.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByClkuv(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.SRSUR.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareBySrsur(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.CUSUR.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByCusur(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.ARRIVAL_RATE.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByArrivalRate(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.HOP_RATE.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByHopRate(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.RES_TIME_STR.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByResTime(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.INDIRECT_TRANS_CNT.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByIndirectTrans(o1.getSelfTrade(), o2.getSelfTrade());
		}else if(ReportConstants.DIRECT_TRANS_CNT.equalsIgnoreCase(qp.getOrderBy())){
			ret = super.compareByDirectTrans(o1.getSelfTrade(), o2.getSelfTrade());
		}else{
			String innerOrder = ReportWebConstants.SITE_FRONT_BACKEND_PARAMNAME_MAPPING.get(qp.getOrderBy());
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equalsIgnoreCase(innerOrder)){
				ret = order * (o1.getSelfTrade().getGroupStateFlag() - o2.getSelfTrade().getGroupStateFlag());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(innerOrder)){
				ret = order * (o1.getSelfTrade().getGroupName().compareTo(o2.getSelfTrade().getGroupName()));
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(innerOrder)){
				ret = order * (o1.getSelfTrade().getPlanName().compareTo(o2.getSelfTrade().getPlanName()));
			}
			if(ReportConstants.PRICE.equalsIgnoreCase(innerOrder)){
				double div = o1.getSelfTrade().getPrice() - o2.getSelfTrade().getPrice();
				if(div > 0){
					ret = order * 1;
				}else if(div == 0){
					ret = 0;
				}else{
					ret = order * -1;
				}				
			}
		}
		if(ret == 0){
			ret = order * (o1.getSelfTrade().getFirstTradeId() - o2.getSelfTrade().getFirstTradeId());
		}
		return ret;
	}
}
