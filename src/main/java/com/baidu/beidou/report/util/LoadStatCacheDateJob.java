package com.baidu.beidou.report.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.baidu.beidou.report.constant.ReportCache;
import com.baidu.beidou.report.service.ReportCacheService;

public class LoadStatCacheDateJob extends QuartzJobBean {

	private static Log log = LogFactory.getLog(LoadStatCacheDateJob.class);

	private ReportCacheService reportCacheService;


	public ReportCacheService getReportCacheService() {
		return reportCacheService;
	}

	public void setReportCacheService(ReportCacheService reportCacheService) {
		this.reportCacheService = reportCacheService;
	}

	protected void executeInternal(JobExecutionContext arg0)
			throws JobExecutionException {
		try {
			log.info("before load cache date : " + ReportCache.LATEST_STAT_CACHE_DATE);
			ReportCache.LATEST_STAT_CACHE_DATE = reportCacheService.queryStatCacheLastModifiedDate();
			log.info("after load cache date : " + ReportCache.LATEST_STAT_CACHE_DATE);
		} catch (Exception e) {
			log.error(e);
		}
	}

}
