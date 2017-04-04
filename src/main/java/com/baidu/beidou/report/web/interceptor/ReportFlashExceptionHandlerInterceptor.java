package com.baidu.beidou.report.web.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * <p>ClassName:ReportFlashExceptionHandlerInterceptor
 * <p>Function: Flash数据生成异常处理
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-18
 */
public class ReportFlashExceptionHandlerInterceptor extends AbstractInterceptor {

	/** Stack中的jsonObject名 */
	private String jsonObjectName = "jsonObject";
	private Log log = LogFactory.getLog(getClass());
	
	public String intercept(ActionInvocation invocation) throws Exception {
		try{
			return invocation.invoke();
		} catch (Exception e){
			AbsJsonObject jsonObject = (AbsJsonObject)invocation.getStack().findValue(jsonObjectName);
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
			jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
			jsonObject.addData("xml", "");
			return Action.SUCCESS;
		}
	}

	public String getJsonObjectName() {
		return jsonObjectName;
	}

	public void setJsonObjectName(String jsonObjectName) {
		this.jsonObjectName = jsonObjectName;
	}
	
}
