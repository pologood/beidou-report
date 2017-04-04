package com.baidu.beidou.util.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class FirekylinPageInterceptor extends AbstractInterceptor{

	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		HttpServletRequest request=ServletActionContext.getRequest();
		HttpSession session=request.getSession();
		
		ActionContext context = ActionContext.getContext();
		context.put("basePath", request.getScheme() + "://"
				+ request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath() + "/");
		context.put("servletPath", request.getServletPath());
		Visitor visitor = ((Visitor)session.getAttribute(UserWebConstant.USER_KEY));
		String username = null;
		String[] auths = new String[0];
		if(visitor != null){
			username = visitor.getUsername();
			auths = visitor.getAuths().toArray(auths);
		}
		context.put("username", username);
		context.put("auths", auths);
		
		return arg0.invoke();
	}

}
