/**
 * 
 */
package com.baidu.beidou.util;

import java.util.HashMap;
import java.util.Map;

import com.baidu.beidou.report.ReportConstants;

/**
 * @author zengyunfeng
 * @version 1.0.0
 */
public class BeidouConstant {
	
	public final static String FILE_PATH_PREF = "/WEB-INF/classes/";
	
	/**
	 * result type --json
	 */
	public static final String JSON = "json";
	
	public static final int JSON_OPERATE_SUCCESS = 0;
	
	public static final int JSON_OPERATE_FAILED = 1;
	
	public static final int JSON_VALIDATION_SUCCESS = 0;
	public static final int JSON_VALIDATION_FAILED = 1;
	
	public static final int JSON_BOOLEAN_TRUE = 1;
	public static final int JSON_BOOLEAN_FALSE = 0;
	
	  
	  public static final double GC_MEMORY_PERCENT = 0.5; 
	  
//		排序的顺序--升序
		public static final String SORTORDER_ASC = "asc";
//		排序的顺序--降序
		public static final String SORTORDER_DESC = "desc";	

	/* added by yanjie at 20090301 */
	public static final Map<String, Integer> QUICK_MAPPING = new HashMap<String, Integer>();
	static {
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_TODAY, ReportConstants.QUICK_TODY);
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY, ReportConstants.QUICK_YEST);
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS, ReportConstants.QUICK_L7DS);
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK, ReportConstants.QUICK_LWEK);
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH, ReportConstants.QUICK_LMON);
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH, ReportConstants.QUICK_CMON);
		QUICK_MAPPING.put(BeidouCoreConstant.PAGE_DATE_RANGE_ALLDATE, ReportConstants.QUICK_ALLT);
	}
	/* added end */
	
	/**
	 * 当前请求是否为json请求，存入context的名称
	 */
	public static final String INTERCEPTOR_JSON_NAME = "INTERCEPTOR_JSON_ENABLE";
	
	/**
	 * 判断是否为网盟推广首页标签
	 * @param request
	 * @return
	 */
	public static boolean isAccount(String request){
		if(isCproPlan(request)||isMyaccount(request)||isReport(request)||isTool(request)){
			return false;
		}
		return true;
	}
	
	/**
	 * 判断是否为网盟推广管理标签
	 * @param request
	 * @return
	 */
	public static boolean isCproPlan(String request){
		if(request.startsWith("/cprounit/")){
			return true;
		}else if(request.startsWith("/cprogroup/")){
			return true;
		}else if(request.startsWith("/cproplan/")){
			return true;
		}
		return false;
	}
	
	/**
	 * 判断是否为我的账户标签
	 * @param request
	 * @return
	 */
	public static boolean isMyaccount(String request){
		if(request.startsWith("/bulletin/")){
			return true;
		}else if(request.startsWith("/account/")){
			return true;
		}
		return false;
	}
	
	/**
	 * 判断是否为我的账户标签
	 * @param request
	 * @return
	 */
	public static boolean isReport(String request){
		if(request.startsWith("/stat/")){
			return true;
		}
		return false;
	}
	
	/**
	 * 判断是否为工具标签
	 * @param request
	 * @return
	 */
	public static boolean isTool(String request){
		if(request.startsWith("/tool/")){
			return true;
		}
		return false;
	}
	
	/**
	 * 默认每页记录数
	 * added by zhuqian
	 * @version 1.1.3
	 */
	public static final int PAGE_SIZE = 20;
	
}
