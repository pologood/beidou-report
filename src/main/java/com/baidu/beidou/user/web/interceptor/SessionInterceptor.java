/**
 * LoginInterceptor.java
 * 2008-6-26
 */
package com.baidu.beidou.user.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.util.loginvalidate.MemcacheConfigure;
import com.baidu.beidou.user.util.loginvalidate.MemcacheSessionInstance;
import com.baidu.beidou.user.util.loginvalidate.ShifenLoginValidator;
import com.baidu.beidou.util.HttpUtils;
import com.baidu.beidou.util.memcache.BeidouMemcacheClient;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * @author zengyunfeng
 * 
 */
public class SessionInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = -6863089304411513562L;

	private BeidouMemcacheClient memClient = null;

	private static final Log LOG = LogFactory.getLog(SessionInterceptor.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		if (memClient == null) {
			memClient = MemcacheSessionInstance.getInstance();
		}
		ShifenLoginValidator validator = ShifenLoginValidator.getInstance();
		HttpServletRequest request = ServletActionContext.getRequest();
		
		Visitor visitor = null;
		HttpSession session = request.getSession();
		visitor = (Visitor)session.getAttribute(UserWebConstant.USER_KEY);
		String ip = HttpUtils.getHttpForwardIp(request);
		if(visitor ==null){
				// 本地没有，查看memcache中是否有该用户的登录信息
			String baiduId = com.baidu.beidou.util.CookieUtils.getCookieValue(request, MemcacheConfigure.MEMCOOKIE_NAME);
			if(baiduId != null){
				baiduId = baiduId+UserWebConstant.USER_KEY;
				visitor = (Visitor)memClient.memcacheRandomGet(baiduId);
				if(visitor != null){
					validator.updateSession(request, session, visitor, ip);
				}
			}
		}else{
			// 更新visitor和memcache中的失效时间
			validator.updateSession(request, session, visitor, ip);
		}
		
		
		return arg0.invoke();
	}	

	
}
