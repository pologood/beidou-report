package com.baidu.beidou.report.dao.vo;

import java.util.Comparator;
import java.util.Map;

import com.baidu.beidou.report.ReportConstants;


/**
 * 对返回结果排序时所用的比较类
 */
public class SortComparator implements Comparator<Map<String, Object>>{
	String column;
	int order;
	
	/**
	 * 排序字段与顺序
	 * @param col，对应ReportConstants中的定义
	 * @param od，对应ReportConstants中的定义
	 */
	public SortComparator(String col, int od){
		column = col;
		order = od;
	}
	
	public int compare(Map<String,Object> a, Map<String,Object> b){
		Object av = a.get(column);
		if (av == null) {
			return -1 * order;
		}
		Object bv = b.get(column);
		if (bv == null) {
			return 1 * order;
		}
		Double v1 = Double.valueOf(av.toString());
		Double v2 = Double.valueOf(bv.toString());
		int sign = Double.compare(v1, v2);
		if (order == ReportConstants.SortOrder.DES){
			sign = -sign;
		}
		return sign;
	}
}
