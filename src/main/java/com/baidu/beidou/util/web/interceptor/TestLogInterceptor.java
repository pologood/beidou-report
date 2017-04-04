/**
 * SetUserIdInterceptor.java
 * 2008-6-30
 */
package com.baidu.beidou.util.web.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.util.TestLogUtils;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * 
 */
public class TestLogInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = 488877617917753325L;
	private static Log log = LogFactory.getLog(TestLogInterceptor.class);

	private String excludeProperties = null;
	private String includeProperties = null;
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {

		String result = invocation.invoke();
		// 设置id
		String actionName = invocation.getInvocationContext().getName();
		Object action = invocation.getAction();
//		invocation.get
		TestLogUtils.testInfo(action.getClass().getName()+"@"+actionName, action, excludeProperties, includeProperties, true);
		return result;
	}
	/**
	 * @return the excludeProperties
	 */
	public String getExcludeProperties() {
		return excludeProperties;
	}
	/**
	 * @param excludeProperties
	 *            the excludeProperties to set
	 */
	public void setExcludeProperties(String excludeProperties) {
		this.excludeProperties = excludeProperties;
	}
	/**
	 * @return the includeProperties
	 */
	public String getIncludeProperties() {
		return includeProperties;
	}
	
	/**
	 * @param includeProperties the includeProperties to set
	 */
	public void setIncludeProperties(String includeProperties) {
		this.includeProperties = includeProperties;
	}

		

}
