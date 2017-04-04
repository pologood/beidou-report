package com.baidu.beidou.user.util.loginvalidate;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.cprounit.exception.ScheduleException;
import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.cache.UserInfoCache;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.util.BeidouConfig;
import com.baidu.beidou.util.HttpUtils;
import com.baidu.beidou.util.ThreadContext;
import com.baidu.beidou.util.memcache.BeidouMemcacheClient;
import com.baidu.uc.CasClient;
import com.baidu.uc.NotLoginException;
import com.baidu.uc.protocol.CasCheckResponse;
import com.baidu.uc.svr.CasInfo;
import com.opensymphony.xwork2.ActionContext;

/**
 * @author zengyunfeng
 * 
 */
public final class ShifenLoginValidator {

	private static final Log LOG = LogFactory
			.getLog(ShifenLoginValidator.class);
	private static final Log TEST_LOG = LogFactory.getLog("test");

	private static volatile ShifenLoginValidator validator = null;
	private final BeidouMemcacheClient memClient;

	private ShifenLoginValidator() throws IOException {
		memClient = MemcacheSessionInstance.getInstance();
	}

	public static ShifenLoginValidator getInstance() {
		if(validator == null){
			synchronized (ShifenLoginValidator.class) {
				if (validator == null) {
					try {
						validator = new ShifenLoginValidator();
					} catch (IOException e) {
						LOG.fatal(e.getMessage(), e);
					}
				}
			}
		}
		return validator;
	}

	public void beidouLogout(HttpServletRequest request){
		if(request == null){
			return ;
		}
		String baiduId = com.baidu.beidou.util.CookieUtils.getCookieValue(request, MemcacheConfigure.MEMCOOKIE_NAME);
		if(baiduId != null){
			baiduId = baiduId+UserWebConstant.USER_KEY;
			memClient.memcacheRandomDelete(baiduId);
		}
		ServletActionContext.getRequest().getSession().invalidate();
	}
	
	/**
	 * 添加是否需要自动重定向参数
	 * 
	 * @param request
	 * @param response
	 * @param autoRedirect
	 * @author guojichun
	 * @since 2.0.0
	 * @return下午02:17:25
	 */
	public Integer isLogin(HttpServletRequest request, HttpServletResponse response,boolean autoRedirect) {
		Integer userid = null;
		userid= isUcLogin(request, response,autoRedirect);
		if(!UCResponseState.isLoginUserId(userid)){
			return userid;
		}
		Visitor visitor = null;
		HttpSession session = request.getSession();
		visitor = (Visitor)session.getAttribute(UserWebConstant.USER_KEY);
		String ip = HttpUtils.getHttpForwardIp(request);
		if(visitor ==null){
			// 本地没有，查看memcache中是否有该用户的登录信息
			String baiduId = com.baidu.beidou.util.CookieUtils.getCookieValue(request, MemcacheConfigure.MEMCOOKIE_NAME);
			if(baiduId == null){
				return userid;
			}
			baiduId = baiduId+UserWebConstant.USER_KEY;
			visitor = (Visitor)memClient.memcacheRandomGet(baiduId);
			if(visitor != null){
				updateSession(request, session, visitor, ip);
			}
		}else{
			// 更新visitor和memcache中的失效时间
			updateSession(request, session, visitor, ip);
		}
		
		return userid;
	}
	

	/**
	 * 根据登录用户判断是否可以操作当前的请求用户
	 * 
	 * @param session
	 * @param loginName:
	 *            登录的用户名
	 * @param requestName：请求的用户名
	 * @param ip
	 * @return
	 */
	public void updateSession(HttpServletRequest request, HttpSession session, Visitor loginPerson,
			String ip) {
		if(request == null || session == null){
			return ;
		}
		loginPerson.setIp(ip);
		session.setAttribute(UserWebConstant.USER_KEY, loginPerson);
		Map strutsSession = com.opensymphony.xwork2.ActionContext.getContext().getSession();
		if(strutsSession != null){
			strutsSession.put(UserWebConstant.USER_KEY, loginPerson);
		}
		String baiduId = com.baidu.beidou.util.CookieUtils.getCookieValue(request, MemcacheConfigure.MEMCOOKIE_NAME);
		if(baiduId == null){
			return ;
		}
		baiduId = baiduId+UserWebConstant.USER_KEY;
		memClient.memcacheRandomSet(baiduId, loginPerson,MemcacheConfigure.expire);
		ThreadContext.putVisitor(loginPerson);
	}
	
	/**
	 * 添加是否需要自动重定向参数
	 * 
	 * @param request
	 * @param response
	 * @param autoRedirect
	 * @author guojichun
	 * @since 2.0.0
	 * @return下午02:17:25
	 */
	private Integer isUcLogin(HttpServletRequest request,
			HttpServletResponse response,boolean autoRedirect) {

		CasInfo objcasInfo = new CasInfo(MemcacheConfigure
				.getUC_SESSION_SERVERS(), MemcacheConfigure
				.getUC_BEIDOU_APPID(), MemcacheConfigure.getUC_BEIDOU_APPKEY(),
				MemcacheConfigure.getUC_SERVER_TIMEOUT());

		objcasInfo.setCookieDomain(MemcacheConfigure
				.getUC_BEIDOU_COOKIE_DOMAIN());// 必须设置
		objcasInfo.setJumpUrl(MemcacheConfigure.getUC_JUMP_URL());
		objcasInfo.setLoginUrl(MemcacheConfigure.getUC_LOGIN_URL());
		objcasInfo.setAutoRedirect(autoRedirect);
		/**
		 * 构造对象，进行验证
		 */

		CasClient objCc = new CasClient(request, response, objcasInfo);
		CasCheckResponse objCcr = null;

		try {
			objCcr = objCc.validateST();
		} catch (NotLoginException e) {
			LOG.warn(e.getMessage());
			return UCResponseState.STATE_NOT_LOGIN_NOT_REDIRECT;
		} catch (Exception e) {
			// 记录日志
			LOG.error(e.getMessage(), e);

			return UCResponseState.STATE_UNEXPECTED_EXCEPTION;
		}

		if (objCcr == null) {
			LOG.info("exception occured when validateST: CheckResponse is null.");
			return UCResponseState.STATE_NOT_LOGIN_HAS_REDIRECT;
		}

		int loginUserId = Long.valueOf(objCcr.getUcid()).intValue();
		
		// 如果是MCC账户，保存其用户名
		if(objCcr.getReg_appid() == BeidouConfig.MCC_APPID ){
			UserInfoCache.getInstance().setMccUserName(String.valueOf(loginUserId), objCcr.getUsername());
		}
		
		return loginUserId;
	}

	public void clearSession(HttpServletRequest request)
			throws ScheduleException {

		// 设置超时时间
		HttpConnectionManagerParams connectionParams = new HttpConnectionManagerParams();
		connectionParams.setConnectionTimeout(MemcacheConfigure
				.getUC_SERVER_TIMEOUT());
		HttpConnectionManager manager = new SimpleHttpConnectionManager();
		manager.setParams(connectionParams);

		HttpClient client = new HttpClient(manager);

		PostMethod method = new PostMethod(MemcacheConfigure.getUC_LOGOUT_URL());
		try {
			int state = client.executeMethod(method);

			// 重试3次
			for (int i = 0; (i < 3) && (state != HttpStatus.SC_OK); i++) {
				state = client.executeMethod(method);
			}

			if (state != HttpStatus.SC_OK) {
				throw new ScheduleException(
						"logout request, but got response state: " + state);
			}
		} catch (HttpException e) {
			throw new ScheduleException("logout occure Exception", e);
		} catch (IOException e) {
			throw new ScheduleException("logout occure Exception", e);
		} finally {
			method.releaseConnection();
		}
	}
	
	public Integer isLogin(HttpServletRequest request, String baiduId) {
		Integer userid = null;
		Visitor visitor = null;
		
		HttpSession session = request.getSession();
		visitor = (Visitor)session.getAttribute(UserWebConstant.USER_KEY);
		String ip = HttpUtils.getHttpForwardIp(request);
		if (visitor == null){
			// 本地没有，查看memcache中是否有该用户的登录信息
			baiduId = baiduId + UserWebConstant.USER_KEY;
			visitor = (Visitor)memClient.memcacheRandomGet(baiduId);
			if (visitor != null){
				userid = visitor.getUserid();
				updateSession(session, visitor, ip, baiduId);
			}
		}
		else{
			// 更新visitor和memcache中的失效时间
			userid = visitor.getUserid();
			updateSession(session, visitor, ip, baiduId);
		}
		
		return userid;
	}
	
	public void updateSession(HttpSession session, Visitor loginPerson,
			String ip, String baiduId) {
		if (session == null) {
			return ;
		}
		
		loginPerson.setIp(ip);
		session.setAttribute(UserWebConstant.USER_KEY, loginPerson);
		Map strutsSession = ActionContext.getContext().getSession();
		if (strutsSession != null){
			strutsSession.put(UserWebConstant.USER_KEY, loginPerson);
		}
		baiduId = baiduId + UserWebConstant.USER_KEY;
		memClient.memcacheRandomSet(baiduId, loginPerson, MemcacheConfigure.expire);
		ThreadContext.putVisitor(loginPerson);
	}

	/**
	 * @return the memClient
	 */
	public BeidouMemcacheClient getMemClient() {
		return memClient;
	}
}

