/**
 * ShifenToBeidouInterceptor.java
 * 2008-6-26
 */
package com.baidu.beidou.user.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.exception.UserStateDisableException;
import com.baidu.beidou.user.service.UserMgr;
import com.baidu.beidou.user.util.loginvalidate.ShifenLoginValidator;
import com.baidu.beidou.user.util.loginvalidate.UCResponseState;
import com.baidu.beidou.util.HttpUtils;
import com.baidu.beidou.util.LogUtils;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * 
 */
public class ShifenToBeidouInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = -6863089304411513562L;

	private transient UserMgr userMgr;

	private static final Log LOG = LogFactory.getLog(ShifenToBeidouInterceptor.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		String LOGIN = UserWebConstant.PAGE_LOGIN;
		if (userMgr == null) {
			BeanFactory factory = WebApplicationContextUtils
					.getWebApplicationContext(ServletActionContext
							.getServletContext());
			userMgr = (UserMgr) factory.getBean("userMgr");
		}
		ShifenLoginValidator validator = ShifenLoginValidator.getInstance();
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		
		// 判断session中是否有登录用户信息
		Integer loginUserId = validator.isLogin(request, response,true);
		
		if (UCResponseState.STATE_NOT_LOGIN_HAS_REDIRECT == loginUserId){
			return null;//由UC实现登陆不成功的跳转，beidou不再关系，1.2.2
		}
		
		if (UCResponseState.STATE_NOT_LOGIN_NOT_REDIRECT.equals(loginUserId)){
			return UserWebConstant.PAGE_LOGIN;
		}
		
		if (UCResponseState.STATE_UNEXPECTED_EXCEPTION.equals(loginUserId)) {
			return Action.ERROR;
		}
		
		//LOG.info("visiter user id:" + loginUserId);
		String ip = HttpUtils.getHttpForwardIp(request);
		HttpSession session = request.getSession();
		Object sessionPerson = session.getAttribute(UserWebConstant.USER_KEY);

		// 获得请求的用户ID，如果用户ID<0，则表示操作登录用户
		int requestId = -1;

		String paramId = request.getParameter("requestId");
		if (!StringUtils.isEmpty(paramId)) {
			try {
				requestId = Integer.parseInt(paramId); // 获得请求的用户
			} catch (NumberFormatException e) {
				LogUtils.error(LOG, "param requestId="+paramId+" is not number", e);
			}
		}
		

		Visitor sessionVisitor = null;
		Visitor requestVisitor = null;

		if (sessionPerson instanceof Visitor) { // session中有该用户的登录信息
			sessionVisitor = (Visitor) sessionPerson;
		}

		if (requestId < 0) { // 没有请求ID，使用loginName
			
			if(sessionVisitor == null
					|| ! loginUserId.equals(sessionVisitor.getUserid())){ 
				// session中无登录用户的数据，
				try{
					requestVisitor = userMgr.getUserByUserId(loginUserId);
				}catch(UserStateDisableException e){
					LOG.error(e.getMessage()+" login denied");
					return LOGIN;
				}
				if (requestVisitor != null) {
					ShifenLoginValidator.getInstance().updateSession(request, session, requestVisitor, ip);
				} else {
					LOG
							.warn("user["
									+ loginUserId
									+ "] has login, but can't get his request user[userid="
									+ loginUserId + "] information");
					return LOGIN;
				}
			}
		} else { // 有请求ID
			
			if(sessionVisitor == null || requestId != sessionVisitor.getUserid()){	// session中无请求用户的数据，
				try{
					requestVisitor = userMgr.getUserByUserId(requestId);
				}catch(UserStateDisableException e){
					LOG.error(e.getMessage()+" login denied");
					return LOGIN;
				}
				if (requestVisitor != null) {
					if((! loginUserId.equals(requestId))){
						// 请求ID既不是登录用户
						return LOGIN;
					}
					ShifenLoginValidator.getInstance().updateSession(request, session, requestVisitor, ip);
				} else {
					LOG
							.error("user["
									+ loginUserId
									+ "] has login, but can't get his request user[userid="
									+ loginUserId + "] information");
					return LOGIN;
				}
			}
		}
		
		//记录用户登陆信息，add by zp，version：1.2.0
		sessionPerson = session.getAttribute(UserWebConstant.USER_KEY);
		if (sessionPerson instanceof Visitor) { // session中有该用户的登录信息
			sessionVisitor = (Visitor) sessionPerson;
			
			LOG.info("Login User Info:" + sessionVisitor.toString());
		}

		return arg0.invoke();
	}
	

}
