package com.baidu.beidou.report.web.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.baidu.beidou.cprogroup.service.CproGroupConstantMgr;
import com.baidu.beidou.cprounit.service.LoadRefuseReasonMgr;
import com.baidu.beidou.report.ApplicationResources;
import com.baidu.beidou.report.constant.ReportCache;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.report.service.UvStatMgr;
import com.baidu.beidou.system.service.ExperimentConstantMgr;
import com.baidu.beidou.util.BeanUtils;
import com.baidu.beidou.util.ServiceLocator;

public class InitSystemListener implements ServletContextListener {

	private static final Log log = LogFactory.getLog(InitSystemListener.class);

	public void contextInitialized(ServletContextEvent event) {

		BeanFactory factory = WebApplicationContextUtils.getWebApplicationContext(event.getServletContext());
		BeanUtils.setFactory(factory);
		ServiceLocator.getInstance().setFactory(factory);
		
//		初始化推广组配置
		ReportCacheService reportCacheService = (ReportCacheService)factory.getBean("reportCacheService");
		ReportCache.LATEST_STAT_CACHE_DATE = reportCacheService.queryStatCacheLastModifiedDate();
		log.info(" Load stat cache from beidou.sysnvtab finished" );
		
//		初始化推广组配置
		CproGroupConstantMgr cproGroupConstantMgr = (CproGroupConstantMgr) factory.getBean("cproGroupConstantMgr");
		cproGroupConstantMgr.loadSystemConfForReport();

		// 初始化拒绝理由
		LoadRefuseReasonMgr loadRefuseReasonMgr = (LoadRefuseReasonMgr) factory.getBean("loadRefuseReasonMgr");
		loadRefuseReasonMgr.loadRefuseReason();
		
		//初始化小流量配置
		ExperimentConstantMgr experimentConstantMgr = (ExperimentConstantMgr) factory.getBean("experimentConstantMgr");
		experimentConstantMgr.loadExpConf();
		
		// 初始化UV统计数据，包含兴趣/性别UV占比等
		UvStatMgr uvStatMgr = (UvStatMgr) factory.getBean("uvStatMgr");
		log.info("Refresh cache for uv stat data is executing now...");
		uvStatMgr.refreshCache();
		log.info("Refresh cache for uv stat data finished");
		
		//定制报告资源初始化
		ApplicationResources.init(event.getServletContext());
	}

	public void contextDestroyed(ServletContextEvent event) {

	}
}
