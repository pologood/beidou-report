package com.baidu.beidou.user.web.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.util.ThreadContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

public class UserIdContextInterceport extends AbstractInterceptor {

	private static final long serialVersionUID = -1127041543271544343L;

	private static final Log log = LogFactory
			.getLog(UserIdContextInterceport.class);
	
	private String userIdName = "userId";
	
	
	/**
	 * 请求中如果有userId参数，就把这个参数缓存到threadcontext中，供后面分库使用
	 * 这个interceptor需要放到beidoustack中
	 */
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		
		HttpServletRequest request = ServletActionContext.getRequest();
		String str_userid = request.getParameter(userIdName);
		Integer userId = null;
		if(str_userid != null){
			userId = Integer.valueOf(str_userid);
		}
		if(userId == null){
			ValueStack stack = invocation.getStack();
			userId = (Integer) stack.findValue(userIdName);
		}
		if(userId != null){
			ThreadContext.putUserId(userId);
		}else{
			log.info("ACTION NAME:"+invocation.getInvocationContext().getName());
		}
		
		return invocation.invoke();
	}
	
	public String getUserIdName() {
		return userIdName;
	}

	public void setUserIdName(String userIdName) {
		this.userIdName = userIdName;
	}

}
