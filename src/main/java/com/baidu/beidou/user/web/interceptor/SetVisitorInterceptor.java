/**
 * SetVisitorInterceptor.java
 * 2008-6-30
 */
package com.baidu.beidou.user.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.web.aware.VisitorAware;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * 
 */
public class SetVisitorInterceptor extends AbstractInterceptor {

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
		Visitor loginer = (Visitor) session
				.getAttribute(UserWebConstant.USER_KEY);

		// 设置userid
		Object action = arg0.getAction();
		
		if(action instanceof VisitorAware){
			if(loginer == null){
				return UserWebConstant.PAGE_LOGIN;
			}
			VisitorAware sma = (VisitorAware) action;
			sma.setVisitor(loginer);
		}
		return arg0.invoke();
	}

}
