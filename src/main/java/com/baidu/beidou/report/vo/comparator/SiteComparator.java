package com.baidu.beidou.report.vo.comparator;

import java.util.Comparator;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.olap.vo.SiteViewItem;

public class SiteComparator extends AbstractComparator implements Comparator<SiteViewItem>{

	
	public SiteComparator(QueryParameter qp){
		super(qp);
	}
	
	public int compare(SiteViewItem o1, SiteViewItem o2) {
		if(ReportConstants.SRCHS.equals(qp.getOrderBy())){
			return super.compareBySrchs(o1, o2);
		}
		if(ReportConstants.CLKS.equals(qp.getOrderBy())){
			return super.compareByClks(o1, o2);
		}
		if(ReportConstants.COST.equals(qp.getOrderBy())){
			return super.compareByCost(o1, o2);
		}
		if(ReportConstants.CPM.equals(qp.getOrderBy())){
			return super.compareByCpm(o1, o2);
		}
		if(ReportConstants.CTR.equals(qp.getOrderBy())){
			return super.compareByCtr(o1, o2);
		}
		if(ReportConstants.ACP.equals(qp.getOrderBy())){
			return super.compareByAcp(o1, o2);
		}
		if(ReportConstants.INDIRECT_TRANS_CNT.equals(qp.getOrderBy())){
			return super.compareByIndirectTrans(o1, o2);
		}
		if(ReportConstants.DIRECT_TRANS_CNT.equals(qp.getOrderBy())){
			return super.compareByDirectTrans(o1, o2);
		}
		if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())){
			return order * (o1.getGroupStateFlag() - o2.getGroupStateFlag());
		}
		if(ReportConstants.GROUP.equals(qp.getOrderBy())){
			return order * (o1.getGroupName().compareTo(o2.getGroupName()));
		}
		if(ReportConstants.PLAN.equals(qp.getOrderBy())){
			return order * (o1.getPlanName().compareTo(o2.getPlanName()));
		}
		if(ReportConstants.PRICE.equals(qp.getOrderBy())){
			return order * (int)(o1.getPrice() - o2.getPrice());
		}		
		if(ReportConstants.SRCHUV.equals(qp.getOrderBy())){
			return super.compareBySrchuv(o1, o2);
		}
		if(ReportConstants.CLKUV.equals(qp.getOrderBy())){
			return super.compareByClkuv(o1, o2);
		}
		if(ReportConstants.SRSUR.equals(qp.getOrderBy())){
			return super.compareBySrsur(o1, o2);
		}
		if(ReportConstants.CUSUR.equals(qp.getOrderBy())){
			return super.compareByCusur(o1, o2);
		}
		if(ReportConstants.COCUR.equals(qp.getOrderBy())){
			return super.compareByCocur(o1, o2);
		}
		return 0;
	}

}
