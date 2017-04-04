package com.baidu.beidou.report.constant;

import java.util.Date;

/**
 * <p>ClassName:ReportCache
 * <p>Function: 报表相关的Cache
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-2-28
 * @version  $Id: Exp $
 */
public class ReportCache {
	
	public static boolean cacheEnable = false;

	// 2 hours
  	public static int expireTime = 2 * 60 * 60;
	
	public static interface CACHE_KEY {
		public static final String ACCOUNT = "account";
		public static final String PLAN = "plan";
		public static final String GROUP = "group";
		public static final String UNIT = "unit";
		public static final String KEYWORD = "keyword";
		public static final String MAINSITE = "mainsite";
		public static final String SUBSITE = "subsite";
	}
	
	/** 
	 * 最新缓存的时间，来自于sysnvtab中的latestStatCacheDate
	 * 当前为每10分钟由quartz任务自动更新一次，这样就不需要生成缓存的任务来触发更新。
	 */
	public static Date LATEST_STAT_CACHE_DATE;
	
	
 	public static boolean isCacheEnable() {
		return cacheEnable;
	}

	public void setCacheEnable(boolean cacheEnable) {
		ReportCache.cacheEnable = cacheEnable;
	}

	public void setExpireTime(int expireTime) {
		ReportCache.expireTime = expireTime;
	}

}
