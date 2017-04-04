package com.baidu.beidou.report.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.util.LogUtils;
import com.baidu.ctclient.ITaskUsingErrorCode;

public class LoadSiteCTTask implements ITaskUsingErrorCode{
	private static Log LOG = LogFactory.getLog(LoadSiteCTTask.class);
	private LoadSiteInfotoMemery task = null;
	/* (non-Javadoc)
	 * @see com.baidu.ctclient.ITaskUsingErrorCode#execute()
	 */
	public boolean execute() {
		try {
			task.reloadSiteInfo();
			return true;
		} catch (Exception e) {
			LogUtils.fatal(LOG, e.getMessage(), e);
			return false;
		}
		
	}
	
	/**
	 * @param task the task to set
	 */
	public void setTask(LoadSiteInfotoMemery task) {
		this.task = task;
	}
}
