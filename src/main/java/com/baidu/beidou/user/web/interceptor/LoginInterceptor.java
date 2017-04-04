/**
 * LoginInterceptor.java
 * 2008-6-26
 */
package com.baidu.beidou.user.web.interceptor;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.exception.UserStateDisableException;
import com.baidu.beidou.user.service.UserMgr;
import com.baidu.beidou.user.util.loginvalidate.ShifenLoginValidator;
import com.baidu.beidou.user.util.loginvalidate.UCResponseState;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.HttpUtils;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.TextProviderFactory;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * @author genglei01, modified in 2010-7-5
 * 
 */
public class LoginInterceptor extends AbstractInterceptor implements
		LocaleProvider {

	private static final long serialVersionUID = -6863089304411513562L;
	
	private static final Log LOG = LogFactory.getLog(LoginInterceptor.class);

	private transient UserMgr userMgr = null;

	private final transient TextProvider textProvider = new TextProviderFactory()
			.createInstance(getClass(), this);

	private boolean disable = false;
	
	private boolean jsonEnabled = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		if (disable) {
			return arg0.invoke();
		}
		
		if (userMgr == null) {
			BeanFactory factory = WebApplicationContextUtils
					.getWebApplicationContext(ServletActionContext
							.getServletContext());
			userMgr = (UserMgr) factory.getBean("userMgr");
		}
		
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		ActionContext context = ActionContext.getContext();
		
		ShifenLoginValidator validator = ShifenLoginValidator.getInstance();
		boolean needRedirect =true;
		// 判断session中是否有登录用户信息
		// 如果是json工作流，则在uc校验失败的时候不用uc替我们重定向 add by guojichun since 2.0.0
		if(jsonEnabled){
			needRedirect=false;
		}
		Integer loginUserId = validator.isLogin(request, response,needRedirect);
		
		// 返回null，response已经被redirect
		if (UCResponseState.STATE_NOT_LOGIN_HAS_REDIRECT == loginUserId){
			if(jsonEnabled){
				return BeidouCoreConstant.NO_LOGIN_JSON;
			}
			else{
				context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider.getText("errors.login.noSession"));
				return null;//由UC实现登陆不成功的跳转，beidou不再关系，1.2.2
			}
		}
		
		// 抛出没有登录的异常
		if ( UCResponseState.STATE_NOT_LOGIN_NOT_REDIRECT.equals(loginUserId) ) {
			if(jsonEnabled){
				// 如果是json则直接返回json 没有登录提示 add by guojichun since 2.0.0
				return BeidouCoreConstant.NO_LOGIN_JSON;
			}
			else{
				context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider.getText("errors.login.noSession"));
				return UserWebConstant.PAGE_LOGIN;
			}
		}
		
		// 抛出mcpack等其他异常
		if ( UCResponseState.STATE_UNEXPECTED_EXCEPTION.equals(loginUserId) ){
			if(jsonEnabled){
				return BeidouCoreConstant.SYSTEM_ERROR_JSON;
			}
			else{
				return Action.ERROR;
			}
		}
		
		//LOG.info("visiter user id:" + loginUserId);
		String ip = HttpUtils.getHttpForwardIp(request);
		HttpSession session = request.getSession();
		Object sessionPerson = session.getAttribute(UserWebConstant.USER_KEY);

		Visitor sessionVisitor = null;
		Visitor requestVisitor = null;

		if (sessionPerson instanceof Visitor) { // session中有该用户的登录信息
			sessionVisitor = (Visitor) sessionPerson;
		}

		if (sessionVisitor == null) {
			// session中无数据，以登录用户作为操作账户
			LOG.info("session中无数据，以UC中的userid["+loginUserId+"]登录");
			try{
				requestVisitor = userMgr.getUserByUserId(loginUserId);
				if (requestVisitor != null) {
					ShifenLoginValidator.getInstance().updateSession(request, session, requestVisitor, ip);
				}
				else {
					LOG
							.error("user["
									+ loginUserId
									+ "] has login, but can't get his request user[userid="
									+ loginUserId + "] information");
					context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider.getText("errors.login.noSession"));
				}
			}
			catch(UserStateDisableException e){
				LOG.error(e.getMessage()+" login denied");
				context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider.getText("errors.login.noSession"));
			}
			
		} 
		else if (! loginUserId.equals(sessionVisitor.getUserid())) {
			// session中没有登录用户的数据，使用登录用户作为操作用户
			try {
				LOG.info("session中没有登录用户的数据，以UC中的userid[" + loginUserId + "]登录");
				requestVisitor = userMgr.getUserByUserId(loginUserId);
				if (requestVisitor != null) {
					ShifenLoginValidator.getInstance().updateSession(request, session, requestVisitor, ip);
				} else {
					// 以下日志的级别原为error，logcheck异常日志排查中将级别降为warn
					LOG.warn("user[" + loginUserId + "] has login, but can't get his request user[userid="
							+ loginUserId + "] information");
					context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider.getText("errors.login.noSession"));
				}
			} catch (UserStateDisableException e) {
				LOG.error(e.getMessage() + " login denied");
				context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider.getText("errors.login.noSession"));
			}
				
		}

		return arg0.invoke();
	}	

	
	public Locale getLocale() {
		return ActionContext.getContext().getLocale();
	}

	public boolean isDisable() {
		return disable;
	}

	public void setDisable(boolean disable) {
		this.disable = disable;
	}


	public boolean isJsonEnabled() {
		return jsonEnabled;
	}


	public void setJsonEnabled(boolean jsonEnabled) {
		this.jsonEnabled = jsonEnabled;
	}
	
}
