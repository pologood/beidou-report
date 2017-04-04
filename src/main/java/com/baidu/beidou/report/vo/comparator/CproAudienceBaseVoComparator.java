package com.baidu.beidou.report.vo.comparator;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.vo.audience.AudienceAssistantVo;
import com.baidu.beidou.report.vo.audience.BaseAudienceVo;
import com.baidu.beidou.util.StringUtils;

public class CproAudienceBaseVoComparator implements Comparator<BaseAudienceVo>  {

	int order;
	String col;
	public CproAudienceBaseVoComparator(String col, int order) {
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
	
	public int compare(BaseAudienceVo o1, BaseAudienceVo o2) {
		/**
		 * 定义比较器的比较规则
		 */
		if(o1 == null) {
			return -1 * order;
		}
		if(o2 == null) {
			return 1 * order;
		}
		
		if(col.equals("name")) {
			return compareByString(o1.getName(), o2.getName());
		}
		
		/**
		 * 增加受众数据和转化数据added by liuhao since cpweb-492
		 */
		if(ReportConstants.SRCHUV.equals(col)) {
			return order * (int)(o1.getSrchuv() - o2.getSrchuv());
		}
		
		if(ReportConstants.CLKUV.equals(col)) {
			return order * (int)(o1.getClkuv() - o2.getClkuv());
		}
		
		if(ReportConstants.TRANSUV.equals(col)) {
			return order * (int)(o1.getClkuv() - o2.getClkuv());
		}
		
		if(ReportConstants.SRCH_UV_PROP.equals(col)) {
			Double b1 = o1.getSrchuvp();
			Double b2 = o2.getSrchuvp();
			if(b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if(ReportConstants.CLK_UV_PROP.equals(col)) {
			Double b1 = o1.getClkuvp();
			Double b2 = o2.getClkuvp();
			if(b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		if(ReportConstants.TRANS_UV_PROP.equals(col)) {
			Double b1 = o1.getTransuvp();
			Double b2 = o2.getTransuvp();
			if(b1 == null) {
				return -1 * order;
			}
			if (b2 == null) {
				return order;
			}
			return order * b1.compareTo(b2);
		}
		
		return order;
	}			
}