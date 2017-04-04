package com.baidu.beidou.report.web.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.report.vo.QueryParameter;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * <p>ClassName:ReportParamDigestInterceptor
 * <p>Function: 解析请求参数，在下载的时候只能将所有的参数写成
 * __PostIframeParamsName__=qp.tab%3D1%26qp.level%3Daccount的形式
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-18
 */
public class ReportParamDigestInterceptor extends AbstractInterceptor {

	/** 请求参数的Key */
	private String paramStringKey = "__PostIframeParamsName__";
	/** 查询参数前缀 */
	private String queryParamPrefix = "qp";
	
	public String intercept(ActionInvocation invocation) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		String params = request.getParameter(paramStringKey);
		ValueStack stack = invocation.getStack();
		if(!StringUtils.isEmpty(params)) {
		//设置参数
			if (stack.findValue(queryParamPrefix) == null) {
				stack.setValue(queryParamPrefix, new QueryParameter());
			}
			String[] array = params.split("&");
			for(String ss : array){
				String[] inner = ss.split("=");
				if(inner != null && inner.length == 2 ){
					stack.setValue(inner[0], inner[1], false);
				}
			}
		}
		return invocation.invoke();
	}

	public String getParamStringKey() {
		return paramStringKey;
	}

	public void setParamStringKey(String paramStringKey) {
		this.paramStringKey = paramStringKey;
	}

	public String getQueryParamPrefix() {
		return queryParamPrefix;
	}

	public void setQueryParamPrefix(String queryParamPrefix) {
		this.queryParamPrefix = queryParamPrefix;
	}

}
