package com.baidu.beidou.report.service.impl;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.report.constant.ReportCache;
import com.baidu.beidou.report.dao.ReportCacheDao;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.util.DateScaleHelper;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;

public class ReportCacheServiceImpl implements ReportCacheService {
	
	 private static final Log log = LogFactory.getLog(ReportCacheServiceImpl.class);

	ReportCacheDao reportCacheDao;
	
	public Date queryStatCacheLastModifiedDate() {
		return reportCacheDao.queryStatCacheLastModifiedDate();
	}

	public ReportCacheDao getReportCacheDao() {
		return reportCacheDao;
	}

	public void setReportCacheDao(ReportCacheDao reportCacheDao) {
		this.reportCacheDao = reportCacheDao;
	}

	 // keep ten hours cache
    public <T extends Serializable> void putIntoCache(String key, T t) {
        BeidouCacheInstance.getInstance().memcacheSetBig(key, t, ReportCache.expireTime);
        log.info("put key=[" + key + "] in cache");
    }

    public <T extends Serializable> T getFromCache(String key) {
        @SuppressWarnings("unchecked")
        T t = (T) (BeidouCacheInstance.getInstance().memcacheGet(key));
        if (t != null) {
            log.info("hit key=[" + key + "] in cache");
            return t;
        } else {
            log.info("miss key=[" + key + "] in cache");
            return null;
        }
    }

    public boolean shouldCache(Date from, Date to) {
        if (ReportCache.cacheEnable) {
            return internalShouldCache(from, to);
        } else {
            return false;
        }
    }

    /**
     * yesterday, last seven days, last week, current month, last month,
     * 
     * @param startDate
     * @param endDate
     * @return
     */
    private static boolean internalShouldCache(Date from, Date to) {
    	
    	if(DateScaleHelper.isYesterday(from, to)) {
    		return true;
    	}
    	if(DateScaleHelper.isLastSevenDays(from, to))
    	{
    		return true;
    	}
    	if(DateScaleHelper.isLastWeek(from, to)) 
    	{
    		return true;
    	}
    	if(DateScaleHelper.isThisMonth(from, to)) 
    	{
    		return true;
    	}
    	if(DateScaleHelper.isLastMonth(from, to)) 
    	{
    		return true;
    	}
    	if(DateScaleHelper.isLastSeason(from, to)) 
    	{
    		return true;
    	}
    	
    	return false;
    }
    
    public static boolean includesToday(Date end) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(end);
        Calendar c2 = Calendar.getInstance();
        int y1 = c1.get(Calendar.YEAR);
        int y2 = c2.get(Calendar.YEAR);
        if (y1 > y2) {
            return true;
        } else if (y1 < y2) {
            return false;
        } else { // same year
            return c1.get(Calendar.DAY_OF_YEAR) >= c2.get(Calendar.DAY_OF_YEAR);
        }
    }
	
}
