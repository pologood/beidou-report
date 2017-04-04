/**
 * 
 */
package com.baidu.beidou.user.web.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.user.service.UserMgr;
import com.baidu.beidou.util.BeanUtils;
import com.baidu.beidou.util.BeidouConstant;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * @version 1.2.0
 * 
 */
public class UserNameInterceptor extends AbstractInterceptor {

	private static final Log log = LogFactory
			.getLog(UserNameInterceptor.class);
	
	private boolean enable = false;
	private String userIdName = "navigate.userId";
	private String userNameName = "navigate.userName";
	private transient UserMgr userMgr = null;

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {		
		if (userMgr == null) {
			userMgr = (UserMgr)BeanUtils.getBean("userMgr");
		}
		boolean navigate = enable;
		
		if(navigate){
			ActionContext context = ActionContext.getContext();
			Boolean isJson = (Boolean) context.get(BeidouConstant.INTERCEPTOR_JSON_NAME);
			if (isJson == null) {
				isJson = false;
			} 
			if(isJson){
				//json请求不需要设置userName;
				navigate = false;
			}
		}
		if(navigate){
			//添加设置导航栏的监听器，不直接设置，有可能有的navigate.userId是在action中设置的
			//由于struts中无法在action执行后，修改result中的值
			invocation.addPreResultListener(new UserNamePreResult(userIdName,userNameName));
		}
		String result = invocation.invoke();
		
		return result;
		
		
	}

	/**
	 * @return the userIdName
	 */
	public String getUserIdName() {
		return userIdName;
	}

	/**
	 * @param userIdName the userIdName to set
	 */
	public void setUserIdName(String userIdName) {
		this.userIdName = userIdName;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

}
