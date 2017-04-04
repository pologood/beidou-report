package com.baidu.beidou.report.service;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>ClassName:ReportCacheService
 * <p>Function: 提供缓存相关服务
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-1
 * @version  $Id: Exp $
 */
public interface ReportCacheService {

	/**
	 * 获取统计数据缓存最后更新时间
	 * @return
	 */
	public Date queryStatCacheLastModifiedDate();
	
	/**
	 * 将报表数据缓存至cache中
	 */
	public <T extends Serializable> void putIntoCache(String key, T t);
	 
	/**
	 * 从缓存中获取缓存数据
	 */
	public <T extends Serializable> T getFromCache(String key);
	
	/**
	 * 判断是否可以缓存
	 */
	public boolean shouldCache(Date from, Date to);
}
