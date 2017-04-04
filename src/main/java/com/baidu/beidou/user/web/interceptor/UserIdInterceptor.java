/**
 * 
 */
package com.baidu.beidou.user.web.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * @author zengyunfeng
 * @version 1.2.0
 * 
 */
public class UserIdInterceptor extends AbstractInterceptor {

	private static final Log log = LogFactory
			.getLog(UserIdInterceptor.class);
	
	private String userIdName = "userId";
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {		
		
		ValueStack stack = invocation.getStack();
		Integer userId = (Integer) stack.findValue(userIdName);
		
		if(userId == null){
			return BeidouCoreConstant.NO_AUTH_JSON;
		}	
		
		stack.setValue("navigate.userId", userId, false);
		
		
		//传入id列表
		List<Integer> userids = null;
		ActionContext context = ActionContext.getContext();
		userids = (List<Integer>)context.get(UserConstant.USER_PRIVILEGE_KEY);
		if(userids == null){
			userids = new ArrayList<Integer>(1);
		}
		userids.add(userId);
		context.put(UserConstant.USER_PRIVILEGE_KEY, userids);
		
		return invocation.invoke();
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

}
