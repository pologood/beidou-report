package com.baidu.beidou.system.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.baidu.beidou.system.service.ExperimentConstantMgr;
import com.baidu.ctclient.ITaskUsingErrorCode;

public class LoadExpConfCTTask implements ITaskUsingErrorCode {

	private static Log LOG = LogFactory.getLog(LoadExpConfCTTask.class);

	ExperimentConstantMgr experimentConstantMgr;
	
	public boolean execute() {
		try {
			experimentConstantMgr.loadExpConf();
			return true;
		} catch (Exception e) {
			LOG.error(e);
			return false;
		}

	}

	public ExperimentConstantMgr getExperimentConstantMgr() {
		return experimentConstantMgr;
	}

	public void setExperimentConstantMgr(ExperimentConstantMgr experimentConstantMgr) {
		this.experimentConstantMgr = experimentConstantMgr;
	}

}
