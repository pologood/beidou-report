package com.baidu.beidou.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class RunGc extends QuartzJobBean{

private static Log log = LogFactory.getLog(RunGc.class);
	
	private int timeout;
	
	
	public void setTimeout(int timeout){
		this.timeout = timeout;
	}
	
	/***
	 * 
	 *
	 */
	private void gc() throws Exception
	{	
		Runtime rt=Runtime.getRuntime();
		long maxMemory=rt.maxMemory();
		long freeMemory=rt.freeMemory();
		log.info("gc:   free:"+freeMemory+"  ,max:"+maxMemory+", percent:"+(double)freeMemory/maxMemory);
		if((double)freeMemory/maxMemory < BeidouConstant.GC_MEMORY_PERCENT){
			rt.gc();
		}
	}
	protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
		try
		{			
			this.gc();
		}
		catch(Exception e)
		{
			log.error("gc failed");
			LogUtils.error(log, e);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunGc dii=new RunGc();
		try
		{
			dii.gc();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("finish");

	}

}
