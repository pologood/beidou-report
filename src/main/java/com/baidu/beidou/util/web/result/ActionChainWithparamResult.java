package com.baidu.beidou.util.web.result;

import java.util.Map;

import ognl.OgnlException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.xwork2.ActionChainResult;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.ognl.OgnlUtil;

public class ActionChainWithparamResult extends ActionChainResult {
	private static final Log log = LogFactory.getLog(ActionChainWithparamResult.class);

	public void execute(ActionInvocation invocation) throws Exception {
		Object action = invocation.getAction();
		Map contextMap = invocation.getInvocationContext().getContextMap();
		String resultCode = invocation.getResultCode();
		ResultConfig resultConfig = (ResultConfig) invocation.getProxy().getConfig().getResults().get(resultCode);
		Map params = resultConfig.getParams();
		ActionContext ac = ActionContext.getContext();
        Container cont = ac.getContainer();
        OgnlUtil ognlUtil = cont.getInstance(OgnlUtil.class);
		for (Object key : params.keySet()){
			try{
				ognlUtil.setProperty(key.toString(), params.get(key), action, contextMap, false);
			}catch(Exception e){
				Throwable reason = e.getCause();
				if (reason instanceof OgnlException){
					log.debug(e.getMessage(), reason);
				}else{
					throw e;
				}
			}
		}
		super.execute(invocation);
	}
	
}
