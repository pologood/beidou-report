package com.baidu.beidou.report.service.impl;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.baidu.beidou.report.service.ReportCacheService;

@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
@TransactionConfiguration(transactionManager = "xdbTransactionManager")
public class ReportCacheServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {
	
	@Resource
	ReportCacheService reportCacheService = null;
	
	public void setDataSource(@Qualifier("xdbMultiDataSource") DataSource dataSource) {
        super.setDataSource(dataSource);
    }
	
	@Test
	public final void testPutAndGetDataFromCache() throws Exception {
		
		reportCacheService.putIntoCache("Jay", "just for test!");
		String txt = reportCacheService.getFromCache("Jay");
		
		System.out.println(txt);
	}

}
