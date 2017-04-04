package com.baidu.beidou.report.vo.comparator;

import java.util.Comparator;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.olap.vo.ViewStateOrderable;

public class ViewStateOrderComparator implements Comparator<ViewStateOrderable>{
		
		private int order = 1;
		public ViewStateOrderComparator(String myOrder) {
			if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(myOrder)) {
				order = ReportConstants.SortOrder.DES;
			}
		}
		public ViewStateOrderComparator(int myOrder) {
			if(myOrder < 0 ) {
				order = -1;
			}
		}

		public int compare(ViewStateOrderable o1, ViewStateOrderable o2) {
			if(o2 == null) {
				return order;
			} else if (o1 == null) {
				return -1 * order;
			} else {
				return o1.getViewStateOrder() > o2.getViewStateOrder() ? order : -1 * order;
			}
		}

}
