/**
 * LoginInterceptor.java
 * 2008-6-26
 */
package com.baidu.beidou.util.web.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.util.BeidouCoreConstant;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * 
 */
public class DefaultExceptionInterceptor extends AbstractInterceptor {

	private static final Log LOG = LogFactory.getLog(DefaultExceptionInterceptor.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	public String intercept(ActionInvocation arg0) throws Exception {
		try {
			return arg0.invoke();
		} catch (Exception e) {
			if (ServletActionContext.getResponse().isCommitted()) {
				//如果是由于用户中断引起的异常，记录一个warn就行
				LOG.warn(e.getMessage(), e);
				return Action.NONE;
			} else {
				Throwable baseCause = e;
		        while (baseCause.getCause() != null) {
		            baseCause = baseCause.getCause();
		        }
		        LOG.warn(e.getMessage(), e);
		        if(baseCause!=null && baseCause.getMessage().equalsIgnoreCase("The MySQL server is running with the --read-only option so it cannot execute this statement")){
					return BeidouCoreConstant.NO_AUTH_JSON;
		        }else{
		        	throw e;
		        }
			}
			
		}
	}	
}
