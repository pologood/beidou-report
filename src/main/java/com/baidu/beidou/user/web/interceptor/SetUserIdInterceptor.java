/**
 * SetUserIdInterceptor.java
 * 2008-6-30
 */
package com.baidu.beidou.user.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.ROLE_CODE;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.web.aware.UserIdAware;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * 
 */
public class SetUserIdInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = -3226655247666853048L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		Visitor loginer = (Visitor) session.getAttribute(UserWebConstant.USER_KEY);

		// 设置userid
		Object action = arg0.getAction();
		if (action instanceof UserIdAware) {
			if (loginer == null) {
				return UserWebConstant.PAGE_LOGIN;
			}
			if (loginer.hasPrivilege(UserConstant.USER_ROLE_CUSTOMER_NORMAL)) {
				UserIdAware sma = (UserIdAware) action;
				sma.setUserId(loginer.getUserid());
			}
		}

		return arg0.invoke();
	}

}
