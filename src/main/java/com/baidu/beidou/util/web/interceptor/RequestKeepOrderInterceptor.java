package com.baidu.beidou.util.web.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.unbiz.redis.RedisCacheManager;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * 根据推广组id判断设置所属的用户
 * 
 * @author tangweihan
 * @fileName RequestKeepOrderInterceptor.java
 * @dateTime 2014-01-21 15:46
 */
public class RequestKeepOrderInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory
			.getLog(RequestKeepOrderInterceptor.class);

	private RedisCacheManager redisCacheMgr;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com
	 * .opensymphony.xwork2.ActionInvocation)
	 */
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {

		// ValueStack stack = invocation.getStack();
		ActionContext context = ActionContext.getContext();
		// Integer userId = (Integer) stack.findValue("userId");

		HttpServletRequest request = ServletActionContext.getRequest();
		String sUserId = request.getParameter("userId");
		Integer userId = Integer.valueOf(sUserId);

		if (userId == null) {
			LOG.info("用户ID不能为空");
			context.put(UserConstant.PRIVILEGE_ERROR_KEY, "用户ID不能为空");
			return invocation.invoke();
		}

		//获取用户名和Action Name
		String key = sUserId + context.getName();
		LOG.debug("lock key=" + key);
		// 设置锁，失效时间为10s
		Object obj = redisCacheMgr.getSet(key, 10, 1);

		if(obj != null) {
			LOG.warn("[token=" + key + "]短时间内重复提交同一请求，" + obj);
			context.put(UserConstant.PRIVILEGE_ERROR_KEY,"[token=" + key + "]短时间内重复提交同一请求，" + obj);
			return invocation.invoke();
		}

		// action正常执行
		String result = invocation.invoke();
		// action执行完后清锁
		redisCacheMgr.remove(key);
		return result;
	}

	public RedisCacheManager getRedisCacheMgr() {
		return redisCacheMgr;
	}

	public void setRedisCacheMgr(RedisCacheManager redisCacheMgr) {
		this.redisCacheMgr = redisCacheMgr;
	}

}
