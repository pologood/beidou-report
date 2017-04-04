/**
 * 2009-12-3 下午07:26:19
 * @author zengyunfeng
 */
package com.baidu.beidou.util.web.interceptor;

import java.util.Map;

import org.apache.struts2.interceptor.TokenInterceptor;

import com.baidu.beidou.util.memcache.MemcacheTokenHelper;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;

/**
 * @author zengyunfeng
 *
 */
public class MemcacheTokenInterceptor extends TokenInterceptor {
	protected String doIntercept(ActionInvocation invocation) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Intercepting invocation to check for valid transaction token.");
        }
        Map session = ActionContext.getContext().getSession();

        synchronized (session) {
            if (!MemcacheTokenHelper.getInstance().validToken()) {
                return handleInvalidToken(invocation);
            }

            return handleValidToken(invocation);
        }
    }
}
