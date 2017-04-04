/*
 * $Id: PrivilegeWorkflowInterceptor.java,v 1.9 2010/04/15 08:20:18 zengyf Exp $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.beidou.user.web.interceptor;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;

/**
 * 
 */
public class PrivilegeWorkflowInterceptor extends MethodFilterInterceptor {
	private static final Log LOG = LogFactory
		.getLog(PrivilegeWorkflowInterceptor.class);

	private boolean enabled = true;
	private boolean jsonEnabled = false;
	private final static char[] hex = "0123456789ABCDEF".toCharArray();

	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {
		if (!enabled) {
			return invocation.invoke();
		}
		HttpServletResponse response = ServletActionContext.getResponse();
		ActionContext context = ActionContext.getContext();

		// 只要当前的配置或者context中有一个为json，则表示当前流程为json流程
		boolean jsoned = jsonEnabled;
		if (!jsoned) {
			Boolean isJson = (Boolean) context
					.get(BeidouConstant.INTERCEPTOR_JSON_NAME);
			if (isJson == null) {
				jsoned = false;
			} else {
				jsoned = isJson;
			}
		}

		// 把当前的请求是否为json请求存入context
		context.put(BeidouConstant.INTERCEPTOR_JSON_NAME, jsoned);

		Object error = context.get(UserConstant.NOTLOGIN_ERROR_KEY);
		if (error != null) {
			if (jsoned) {
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().print(buildResponse(error));
				return Action.NONE;
			} else {
				return UserWebConstant.PAGE_LOGIN;
			}
		}

		error = context.get(UserConstant.PRIVILEGE_ERROR_KEY);
		if (error != null) {
			if (jsoned) {
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().print(buildResponse(error));
				return Action.NONE;
			} else {
				return BeidouCoreConstant.NO_AUTH_JSON;
			}
		}

		try {
			return invocation.invoke();
		} catch (Exception e) {			
			if (jsoned) { 
				if (e instanceof BusinessException) {
            		response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().print(buildResponse(e.getMessage(), BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER));            
                    LOG.warn( e.getMessage());
                    return Action.NONE;
				}
				printErrMessage( e);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().print(buildResponse("操作失败"));
				return Action.NONE;
			} else {
				throw e;
			}
		}
	}

    private void printErrMessage(Throwable e) {
        if (e == null) return;
        Throwable baseCause = e;
        while (baseCause.getCause() != null) {
            baseCause = baseCause.getCause();
        }
        //"o"=origin, "d"=direct, "e"=exception, "m"=message
        String errMsg = "WorkFlow error," +
                "oe=[" + baseCause.getClass().getCanonicalName() 
                + "], om=[" + baseCause.getMessage() 
                + "], de=[" + e.getClass().getCanonicalName() 
                + "], dm=[" + e.getMessage() + "]";
        LOG.error(errMsg, e);
    }
	protected String buildResponse(Object error) {

		return buildResponse(error, BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER) ;
	}
    
    /**
	 * @return JSON string that contains the errors and field errors
	 * modify by guojichun since beidou2.0.0
	 */
	protected String buildResponse(Object error, int failCode) {
		StringBuilder sb = new StringBuilder();
		sb.append(" { ");
		sb.append("\"status\":" + failCode + ",");
		List<Object> errorMsg = new ArrayList<Object>();
		errorMsg.add(error);
		if (errorMsg.isEmpty()) {
			// remove trailing comma, IE creates an empty object, duh
			sb.deleteCharAt(sb.length() - 1);
		} else {
			sb.append("\"msg\":");
			sb.append(buildArray(errorMsg));
		}
		sb.append(",\"statusInfo\":");
		sb.append("{\""+BeidouCoreConstant.MESSAGE_GLOBAL+"\":\"");
   	  	sb.append(error);
   	  	sb.append("\"}");
   	  	sb.append(",\"data\":{}");
		sb.append("} ");
		return sb.toString();
		/*
		 * response should be something like: { "status": 1, msg:["the_golbal_error"],"statusInfo": {"global":"the_golbal_error"},"data":{} }
		 */
	}

	private String buildArray(Collection<Object> values) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Object value : values) {
			sb.append("\"");
			sb.append(escapeJSON(value));
			sb.append("\",");
		}
		if (!values.isEmpty()) {
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append("]");
		return sb.toString();
	}

	private String escapeJSON(Object obj) {
		StringBuilder sb = new StringBuilder();

		CharacterIterator it = new StringCharacterIterator(obj.toString());

		for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
			if (c == '"') {
				sb.append("\\\"");
			} else if (c == '\\') {
				sb.append("\\\\");
			} else if (c == '/') {
				sb.append("\\/");
			} else if (c == '\b') {
				sb.append("\\b");
			} else if (c == '\f') {
				sb.append("\\f");
			} else if (c == '\n') {
				sb.append("\\n");
			} else if (c == '\r') {
				sb.append("\\r");
			} else if (c == '\t') {
				sb.append("\\t");
			} else if (Character.isISOControl(c)) {
				sb.append(unicode(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Represent as unicode
	 * 
	 * @param c
	 *            character to be encoded
	 */
	private String unicode(char c) {
		StringBuilder sb = new StringBuilder();
		sb.append("\\u");

		int n = c;

		for (int i = 0; i < 4; ++i) {
			int digit = (n & 0xf000) >> 12;

			sb.append(hex[digit]);
			n <<= 4;
		}
		return sb.toString();
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the jsonEnabled
	 */
	public boolean isJsonEnabled() {
		return jsonEnabled;
	}

	/**
	 * @param jsonEnabled
	 *            the jsonEnabled to set
	 */
	public void setJsonEnabled(boolean jsonEnabled) {
		this.jsonEnabled = jsonEnabled;
	}

}
