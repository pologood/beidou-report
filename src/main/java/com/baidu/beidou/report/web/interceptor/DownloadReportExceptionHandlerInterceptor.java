package com.baidu.beidou.report.web.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.util.LogUtils;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * <p>ClassName:ReportExceptionHandlerInterceptor
 * <p>Function: 报告下载异常处理
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-18
 */
public class DownloadReportExceptionHandlerInterceptor extends AbstractInterceptor {

	/** 回调方法名 */
	private String callback = "__callbackName__";
	/** 存放返回前端的字符串的变量名 */
	private String content = "content";
	private Log log = LogFactory.getLog(getClass());
	
	public String intercept(ActionInvocation invocation) throws Exception {
		try {
			return invocation.invoke();
		} catch (Exception e) {
			LogUtils.error(log, e);
			String msg = "系统错误，请联系管理员或者稍后重试";
			ValueStack stack = invocation.getStack();
			if (e instanceof BusinessException) {
				msg = e.getMessage();
			}
			String html = buildErrorResponse(stack.findString(callback), msg);
			stack.setValue(content, html);
			return Action.ERROR;
		}
	}
	
	/**
	 * buildErrorResponse:生成异常状态信息
	 *
	 * @param callback
	 * @param msg
	 * @return      
	 * @since 
	*/
	protected String buildErrorResponse(String callback, String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<head></head><body>");
		sb.append("<script type=\"text/javascript\">");
		sb.append("parent." + callback + "('" + msg + "')");
		sb.append("</script></body></html>");
		sb.append("");
		return sb.toString();
				
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
}
