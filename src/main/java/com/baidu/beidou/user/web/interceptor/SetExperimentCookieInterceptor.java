package com.baidu.beidou.user.web.interceptor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.system.constant.ExperimentConfCache;
import com.baidu.beidou.system.vo.ExpUserGroup;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * ClassName: SetExperimentCookieInterceptor <p>
 * 
 * Function: 根据userId来切流量作转发，进行指定用户的请求由小流量实验平台集群处理  <br>
 * 			 该拦截器位于所有拦截器栈到最前面，也就是说来了请求先看是不是指定到对到集群（小流量或者全流量）处理的，如果不是那么做二次转发。
 *
 * @version 1.0
 * @author zhangxu
 * @since cpweb326 小流量实验平台搭建
 * 	 
 */
public class SetExperimentCookieInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = -6823019374413513061L;
	
	private static final Log LOG = LogFactory.getLog(SetExperimentCookieInterceptor.class);
	
	/**
	 * 重定向到结果名（Struts result name）
	 */
	private static final String REDIRECT_RESULT_NAME = "expRedirect";
	
	/**
	 * 放在Struts valuestack中用于重定向到url
	 */
	private static final String REDIRECT_URL_IN_VALUESTACK = "expRedirectUrl";

	/**
	 * 小流量配置cookie到key
	 */
	private static final String COOKIE_KEY = "B_D_BEIDOU_EXP_K";
	
	/**
	 * 小流量配置cookie到key
	 */
	private static final String USERID_CONST = "userId";
	
	/**
	 * 小流量配置cookie的domain
	 */
	private static final String DOMAIN_CONST = ".beidou.baidu.com";
	
	/**
	 * 最长url长度，因为转发的url会拼接GET和POST参数，如果转发的url长度太长，会造成HTTP服务器发送响应失败，抑或是浏览器无法处理这么长的GET请求，
	 * 因此如果长度太长到话就忽略小流量转发策略。
	 */
	private static final int MAX_GET_URL_LENGTH = 512;
	
	/**
	 * 拼装重发url时对于参数值到编码方式
	 */
	private static final String PARAMETER_VALUE_ENCODING = "utf-8";

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		
		ValueStack stack = invocation.getStack();

		String paramUserId = null;
		
		try {
			
			// 如果发现小流量cookie，先判断是不是强制不小流量的，如果是那么清除cookie，重新发送请求；
			//    如果uri匹配忽略uri前缀，直接由接收该请求到服务器处理，忽略来其他检查步骤
			if(hasCookie(request))
			{
				String uri = request.getRequestURI();
				for(String forbidenUri : ExperimentConfCache.FORBIDDEN_EXP_URI_PREFIX_LIST){
					if(uri.startsWith(forbidenUri)){
						LOG.info("expriment cookie detected but uri=" + forbidenUri + " should NOT be exprimented");
						removeCookie(request, response);
						String expRedirectUrl = reconstructURL(request);
						stack.set(REDIRECT_URL_IN_VALUESTACK, expRedirectUrl);
						return REDIRECT_RESULT_NAME;
					}
				}
				for(String ignoreUri : ExperimentConfCache.IGONORE_IF_EXP_URI_PREFIX_LIST){
					if(uri.startsWith(ignoreUri)){
						LOG.info("expriment cookie detected but uri=" + ignoreUri + " ignore check experiment cookie");
						return invocation.invoke();
					}
				}
			}
			
			paramUserId = request.getParameter("userId");
			
			// 当url参数中没有userId时，如果被小流量来，根据配置来处理是否需要清除cookie重发请求；
			// 当url中有userId时，取得该userId对应小流量标识cookie值（这里到标识包含小流量版本号），如果该cookie值为空，那么该用户显然
			//    是不应该被小流量到，此时清除cookie重发请求；如果cookie有值，检测下是不是正确到cookie，防止cookie欺骗或者小流量已下线，
			//    如果cookie不对，打上正确到cookie重发请求，否则就继续下面到拦截器处理。
			if (StringUtils.isEmpty(paramUserId)) 
			{
				if(hasCookie(request))
				{
					if(ExperimentConfCache.IF_EXP_REDIRECT_WHEN_URI_NOT_USERID){
						removeCookie(request, response);
						String expRedirectUrl = reconstructURL(request);
						if(expRedirectUrl.length() > MAX_GET_URL_LENGTH){
							LOG.info("expriment cookie detected but no userId found, but redirect url is too long so just continue, url=[" + expRedirectUrl + "]");
							return invocation.invoke();
						}
						stack.set(REDIRECT_URL_IN_VALUESTACK, expRedirectUrl);
						LOG.info("expriment cookie detected but no userId found, so remove cookie and re-send url [" + expRedirectUrl + "]");
						return REDIRECT_RESULT_NAME;
					}
				}
			} 
			else 
			{
				int userId = Integer.parseInt(paramUserId);
				String expVersion = getExpVersion(userId);
				if (expVersion != null) 
				{
					String cookie = getCookie(request);
					if (cookie == null || !cookie.equalsIgnoreCase(expVersion)) 
					{
						setCookie(response, expVersion);
						String expRedirectUrl = reconstructURL(request, userId);
						if(expRedirectUrl.length() > MAX_GET_URL_LENGTH){
							LOG.info("expriment cookie not match for userId=" + userId + ", but redirect url is too long so just continue, do not update cookie value to " + expVersion + " and re-send request url [" + expRedirectUrl + "]");
							return invocation.invoke();
						}
						stack.set(REDIRECT_URL_IN_VALUESTACK, expRedirectUrl);
						LOG.info("expriment cookie not match for userId=" + userId + ", so update cookie value to " + expVersion + " and re-send request url [" + expRedirectUrl + "]");
						return REDIRECT_RESULT_NAME; // here exit the below process
					}
					LOG.info("expriment cookie=" + expVersion + " match for userId=" + userId);
				} 
				else 
				{
					if(hasCookie(request))
					{
						removeCookie(request, response);
						String expRedirectUrl = reconstructURL(request, userId);
						if(expRedirectUrl.length() > MAX_GET_URL_LENGTH){
							LOG.info("expriment cookie detected but userId" + userId + " does not contain in expriment pool, but redirect url is too long so just continue, do not remove cookie and re-send request url [" + expRedirectUrl + "]");
							return invocation.invoke();
						}
						stack.set(REDIRECT_URL_IN_VALUESTACK, expRedirectUrl);
						LOG.info("expriment cookie detected but userId" + userId + " does not contain in expriment pool, so remove cookie and re-send request url [" + expRedirectUrl + "]");
						return REDIRECT_RESULT_NAME; // here exit the below process
					}
				}
			}
		} catch (NumberFormatException e) {
			LOG.error("userId=" + paramUserId + " cannot be parsed");
		} catch (Exception e) {
			LOG.error("Exception occurred while setting expriment cookie", e);
		}
		
		return invocation.invoke();
		
	}
	
	/**
	 * 获取某个user到小流量标识cookie值，如果该用户不在小流量用户池中，那么返回null
	 * 
	 * @param userId
	 * @return
	 */
	private String getExpVersion(int userId){
		for(ExpUserGroup expUserGroup : ExperimentConfCache.USERGROUP_LIST){
			String expVersion = expUserGroup.getExpVersion();
			for(Integer expUserId : expUserGroup.getUserIdSet()){
				if(expUserId == userId){
					return expVersion;
				}
			}
		}
		return null;
	}

	/**
	 * 获取request中以小流量标识为Key到cookie value
	 * 
	 * @param httpReq
	 * @return 小流量标识的cookie值
	 */
	public String getCookie(HttpServletRequest httpReq) {
		Cookie[] cookies = httpReq.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (COOKIE_KEY.equals(cookies[i].getName())) {
					return cookies[i].getValue();
				}
			}
		}
		return null;
	}

	/**
	 * 在response中设置小流量标识的key到value为expCookie
	 * 
	 * @param httpRes
	 * @param expCookie 小流量标识到cookie值
	 */
	public void setCookie(HttpServletResponse httpRes, String expCookie) {
		Cookie cookie = new Cookie(COOKIE_KEY, expCookie);
		cookie.setPath("/");
		cookie.setDomain(DOMAIN_CONST);
		httpRes.addCookie(cookie);
	}

	/**
	 * 清除小流量标识到cookie
	 * 
	 * @param httpReq
	 * @param httpRes
	 */
	public void removeCookie(HttpServletRequest httpReq,
			HttpServletResponse httpRes) {
		Cookie[] cookies = httpReq.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				String expCookieKey = cookies[i].getName();
				// 查找小流量cookie标志并清除cookie
				if (expCookieKey.equals(COOKIE_KEY)) {
					cookies[i].setValue(null);
					cookies[i].setMaxAge(0); 
					cookies[i].setPath("/");
					cookies[i].setDomain(DOMAIN_CONST);
					httpRes.addCookie(cookies[i]);
				}
			}
		}
	}
	
	/**
	 * 判断cookie是否存在小流量标识
	 * 
	 * @param httpReq
	 * @return 如果存在返回true，不存在返回false
	 */
	public boolean hasCookie(HttpServletRequest httpReq) {
		Cookie[] cookies = httpReq.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				String expCookieKey = cookies[i].getName();
				// 查找小流量cookie标志
				if (expCookieKey.equals(COOKIE_KEY)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 重构发送请求（带userId）
	 * 
	 * @param httpReq
	 * @param userId 
	 * @return 发送请求url
	 */
	public String reconstructURL(HttpServletRequest httpReq, int userId) {
		StringBuilder result = new StringBuilder();
		result.append(httpReq.getRequestURL() + "?");
		//String queryStr = httpReq.getQueryString();
		String parameterName = null;
		String[] parameterValues = null;
		Enumeration<String> e = httpReq.getParameterNames();
		int paramNum = 0;
		while(e.hasMoreElements())
		{
			parameterName = e.nextElement();
			parameterValues = httpReq.getParameterValues(parameterName);
			//System.out.println(parameterName + "=" + parameterValues);
			for(String parameterValue : parameterValues){
				try {
					result.append(parameterName + "=" + URLEncoder.encode(parameterValue,PARAMETER_VALUE_ENCODING) + "&");
				} catch (UnsupportedEncodingException ex ){
					LOG.error("error when parsing request params", ex);
				}
			}
			paramNum++;
		}
		String returnUrl = result.toString();
		if(!StringUtils.isEmpty(returnUrl)){
			if(!returnUrl.contains(USERID_CONST + "=")){
				returnUrl += USERID_CONST + "=" + userId;
			} else {
				returnUrl = returnUrl.substring(0, returnUrl.length() - 1);
			}
		} else {
			returnUrl = returnUrl.substring(0, returnUrl.length() - 1);
		}
		return returnUrl;
	}
	
	/**
	 * 重构发送请求
	 * 
	 * @param httpReq
	 * @return
	 */
	public String reconstructURL(HttpServletRequest httpReq) {
		StringBuilder result = new StringBuilder();
		result.append(httpReq.getRequestURL() + "?");
		//String queryStr = httpReq.getQueryString();
		String parameterName = null;
		String[] parameterValues = null;
		Enumeration e = httpReq.getParameterNames();
		int paramNum = 0;
		while(e.hasMoreElements())
		{
			parameterName = (String)e.nextElement();
			parameterValues = httpReq.getParameterValues(parameterName);
			//System.out.println(parameterName + "=" + parameterValues);
			for(String parameterValue : parameterValues){
				try {
					result.append(parameterName + "=" + URLEncoder.encode(parameterValue,PARAMETER_VALUE_ENCODING) + "&");
				} catch (UnsupportedEncodingException ex ){
					LOG.error("error when parsing request params", ex);
				}
			}
			paramNum++;
		}
		String returnUrl = result.toString();
		returnUrl = returnUrl.substring(0, returnUrl.length() - 1);
		return returnUrl;
	}

	
	

}
