package com.baidu.beidou.util.web.result;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.dispatcher.StrutsResultSupport;

import com.opensymphony.xwork2.ActionInvocation;

public class ContentTypedResult extends StrutsResultSupport {
	
	protected String contentParameter = "content";
	protected String contentType = "application/x-javascript";
	
	@Override
	protected void doExecute(String finalLocation, ActionInvocation invocation)
			throws Exception {
		HttpServletResponse response = (HttpServletResponse) 
										invocation.getInvocationContext().get(HTTP_RESPONSE);
		response.setContentType(contentType);
		response.setCharacterEncoding("UTF-8"); 
		response.setHeader("Cache-Control", "no-cache"); 
		
		PrintWriter out = response.getWriter();
		String result = (String) invocation.getStack().findValue(contentParameter);
		out.println(result);
		out.flush();
		out.close();
	}

	public String getContentParameter() {
		return contentParameter;
	}


	public void setContentParameter(String contentParameter) {
		this.contentParameter = contentParameter;
	}


	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
