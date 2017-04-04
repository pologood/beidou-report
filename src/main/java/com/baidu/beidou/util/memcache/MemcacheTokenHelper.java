package com.baidu.beidou.util.memcache;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.util.TokenHelper;

import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.util.loginvalidate.MemcacheConfigure;
import com.baidu.beidou.user.util.loginvalidate.MemcacheSessionInstance;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

public class MemcacheTokenHelper {
	private static final Log LOG = LogFactory.getLog(MemcacheTokenHelper.class);
	private BeidouMemcacheClient memClient;
	private static volatile MemcacheTokenHelper helper = null;

	private MemcacheTokenHelper() throws IOException {
		memClient = MemcacheSessionInstance.getInstance();
	}

	public static MemcacheTokenHelper getInstance() {
		if (helper == null) {
			synchronized (MemcacheTokenHelper.class) {
				if (helper == null) {
					try {
						helper = new MemcacheTokenHelper();
					} catch (IOException e) {
						LOG.fatal(e.getMessage(), e);
					}
				}
			}
		}
		return helper;
	}

	private String getMemcacheKey(String tokenName) {
		HttpServletRequest request = ServletActionContext.getRequest();
		String baiduId = com.baidu.beidou.util.CookieUtils.getCookieValue(
				request, MemcacheConfigure.MEMCOOKIE_NAME);
		if (baiduId != null) {
			baiduId = baiduId + UserWebConstant.TOKEN_KEY + tokenName;
			return baiduId;
		} else {
			return null;
		}
	}

	public boolean validToken() {
		String tokenName = TokenHelper.getTokenName();

		if (tokenName == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("no token name found -> Invalid token ");
			return false;
		}

		String token = TokenHelper.getToken(tokenName);

		if (token == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("no token found for token name " + tokenName
						+ " -> Invalid token ");
			return false;
		}

		Map session = ActionContext.getContext().getSession();
		String sessionToken = (String) session.get(tokenName);
		String tokenKey = getMemcacheKey(tokenName);
		if(sessionToken == null && tokenKey != null){
			sessionToken = (String) memClient.memcacheRandomGet(tokenKey);
		}
		if (!token.equals(sessionToken)) {
			// token验证没有通过
			LOG
					.warn(LocalizedTextUtil
							.findText(
									TokenHelper.class,
									"struts.internal.invalid.token",
									ActionContext.getContext()
											.getLocale(),
									"Form token {0} does not match the session token {1}.",
									new Object[] { token, sessionToken }));
			return false;
		}

		// remove the token so it won't be used again
		session.remove(tokenName);
		// 移除memcache中的token
		memClient.memcacheRandomDelete(tokenKey);
		return true;
	}

	public String setToken(String tokenName) {
		Map session = ActionContext.getContext().getSession();
		String token = TokenHelper.generateGUID();
		try {
			session.put(tokenName, token);
			String tokenKey = getMemcacheKey(tokenName);
			if (tokenKey != null) {
				memClient.memcacheRandomSet(tokenKey, token, MemcacheConfigure.expire);
			}
		} catch (IllegalStateException e) {
			// WW-1182 explain to user what the problem is
			String msg = "Error creating HttpSession due response is commited to client. You can use the CreateSessionInterceptor or create the HttpSession from your action before the result is rendered to the client: "
					+ e.getMessage();
			LOG.error(msg, e);
			throw new IllegalArgumentException(msg);
		}

		return token;
	}

}
