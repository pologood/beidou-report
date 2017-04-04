package com.baidu.beidou.report.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.cprogroup.service.UnionSiteMgr;

public class LoadSiteInfotoMemery {
private static Log log = LogFactory.getLog(LoadSiteInfotoMemery.class);
	
	private UnionSiteMgr unionSiteMgr;
	
	/**
	 * @return the unionSiteMgr
	 */
	public UnionSiteMgr getUnionSiteMgr() {
		return unionSiteMgr;
	}


	/**
	 * @param unionSiteMgr the unionSiteMgr to set
	 */
	public void setUnionSiteMgr(UnionSiteMgr unionSiteMgr) {
		this.unionSiteMgr = unionSiteMgr;
	}		
	
	/**
	 * 载入新的的站点信息
	 * 2009-4-24
	 * zengyunfeng
	 * @version 1.1.3
	 * @throws Exception
	 */
	public void reloadSiteInfo() throws Exception
	{
		log.info("reloadSiteInfo is start");
		//加载内存
		unionSiteMgr.loadSiteInfo();
		//加载历史全库
		unionSiteMgr.loadSiteLiteInfo();
		
		log.info("reloadSiteInfo is over");
	}
	
	public void reloadSiteSizeInfo() throws Exception
	{
		log.info("reloadSiteSizeInfo is start");
		//加载内存
		unionSiteMgr.loadSiteSizeInfo();
		
		log.info("reloadSiteSizeInfo is over");
	}
}
