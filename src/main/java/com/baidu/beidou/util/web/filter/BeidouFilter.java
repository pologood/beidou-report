package com.baidu.beidou.util.web.filter;

import static com.baidu.beidou.util.StringUtils.convertString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.dispatcher.ng.filter.StrutsPrepareAndExecuteFilter;

import com.baidu.beidou.util.HttpUtils;
import com.baidu.beidou.util.ThreadContext;
import com.baidu.beidou.util.dao.datasource.DynamicDataSourceInterceptor;
import com.google.common.base.Joiner;
import com.opensymphony.xwork2.config.RuntimeConfiguration;
import com.opensymphony.xwork2.config.entities.ActionConfig;


/**
 * 修改struts默认逻辑，将exclude匹配的url使用servlet直接处理
 * 
 * @author yangyun
 * @version 1.3.0
 * @date 2010.05.13
 */
public class BeidouFilter extends StrutsPrepareAndExecuteFilter {
	private static final Log LOG = LogFactory.getLog(BeidouFilter.class);
    private static final Log _stat_log = LogFactory.getLog("statLog");

	private List<String> excludes = new ArrayList<String>();
	private RuntimeConfiguration runtimeConfig;
	
	private String[] prefixes;
	public static final String DEFAULT_EMPTY_TAG = "-";
	public static final String TIME_COST_KEY = "TIME_COST";
	
    public void init(FilterConfig filterConfig) throws ServletException {
    	this.excludes.addAll( Arrays.asList(filterConfig.getInitParameter("excludeServlets").split(",")));
    	this.prefixes = filterConfig.getInitParameter("prefixes").split(",");
        super.init(filterConfig);
    }
    
	@Override
	protected void postInit(Dispatcher dispatcher, FilterConfig filterConfig) {
		runtimeConfig = dispatcher.getConfigurationManager().getConfiguration().getRuntimeConfiguration();
	}

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.setCharacterEncoding("UTF-8");
        
        String url = request.getServletPath();
        String dangerUrl = "/struts/webconsole.html";
        if (url.contains(dangerUrl)){
            response.sendRedirect(request.getContextPath() + "/noPermission.jsp");
            return;
        }
        
        long start = System.currentTimeMillis();
     
        String target = request.getServletPath() + (request.getPathInfo() == null ?"" : request.getPathInfo());
        if(target!=null){
        	for(String exclude:excludes){
            	if(exclude!=null&&!"".equals(exclude)){
            		if(target.matches(exclude)){
            			RequestDispatcher rdsp = request.getRequestDispatcher(target);
            		    rdsp.forward(req, res);
            		    return ;
            		}
            	}
            }
        }
        
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            for (String prefix : this.prefixes) {
                if (name.startsWith(prefix)) {
                	return ;
                }
            }
        }
        
        String uri = request.getRequestURI();
        boolean isAction = uri.endsWith(".action");
        ActionMapping mapping = null;
        try{
        	if(isAction){
        		ThreadContext.init();
        		analysisActionDebugParam(request);
        		mapping = prepare.findActionMapping(request, response, true);
        	}
        	
        	super.doFilter(req, res, chain);
        } catch(IllegalStateException e) {
        	LOG.warn("IllegalStateException " + e.getMessage());
        } finally {
        	if (isAction) {
        		Map<String, Object> properties = new HashMap<String, Object>();
        		properties.put(TIME_COST_KEY, (System.currentTimeMillis() - start));
        		try {
        			_stat_log.info(getActionLogString(request, (HttpServletResponse)res, mapping, properties));
        		} catch (Exception ex) {
        			ex.printStackTrace();
        		}
	        	ThreadContext.clean();
        	}
        }
    }
    
    private String getActionLogString(HttpServletRequest request, HttpServletResponse response, ActionMapping mapping, Map<String, Object> properties) {
    	ActionConfig config = null;
    	if (mapping != null) {
    		config = runtimeConfig.getActionConfig(mapping.getNamespace(), mapping.getName());
    	}
    	String className = DEFAULT_EMPTY_TAG;
    	String methodName = DEFAULT_EMPTY_TAG;
    	if (config != null) {
    		className = convertString(config.getClassName(), DEFAULT_EMPTY_TAG);
    		methodName = convertString(config.getMethodName(), DEFAULT_EMPTY_TAG);
    	}
    	Integer userId = ThreadContext.getUserId();
    	String ip = HttpUtils.getHttpForwardIp(request);
    	int idx = ip.indexOf(',');
        if (idx > -1) {
        	ip = ip.substring(0, idx);
        }
        String executeRet = ThreadContext.<String>getContext("ACTIONINVOCATION_RESULT_INFO");
    	return Joiner.on("\t").join(
    			convertString(ip, DEFAULT_EMPTY_TAG),									// ip
    			convertString(request.getMethod(), DEFAULT_EMPTY_TAG),					// method
    			request.getRequestURI(),												// requestUri
    			className,																// actionClass
    			methodName,																// actionMethod
    			(userId == null ? DEFAULT_EMPTY_TAG : userId.toString()),				// userid
    			(properties.get(TIME_COST_KEY) == null ? 
	    			  DEFAULT_EMPTY_TAG : properties.get(TIME_COST_KEY).toString()),	// cost(ms)
	    		getFullRequestQueryString(request),										// full querystring
	    		convertString(executeRet, DEFAULT_EMPTY_TAG)							// status
//	    	DEFAULT_EMPTY_TAG,															// serial id
	    		);
    }
    
    private String getFullRequestQueryString(HttpServletRequest request) {
    	Map map = request.getParameterMap();
    	if (map == null || map.size() == 0) {
    		return DEFAULT_EMPTY_TAG;
    	}
    	
    	StringBuilder queryStringBuilder = new StringBuilder();
    	for (Object key : map.keySet()) {
    		if (key == null || !(key instanceof String)) {
    			continue;
    		}
    		
    		Object value = request.getParameter((String)key);
    		if (value == null) {
    			continue;
    		}
    		
    		String valString = null;
    		if (value instanceof String[]) {
    			String[] array = (String[])value;
    			StringBuilder sb = new StringBuilder();
    			for (String s : array) {
    				sb.append(key).append("=").append(s).append("&");
    			}
    			valString = sb.toString();
    			queryStringBuilder.append(valString);
    		} else {
    			valString = value.toString();
    			queryStringBuilder.append(key).append("=").append(valString).append("&");
    		}
    	}
    	
    	if (queryStringBuilder.length() == 0) {
    		return DEFAULT_EMPTY_TAG;
    	}
    	
    	if (queryStringBuilder.length() > 255) {
    		queryStringBuilder.delete(255, queryStringBuilder.length());
    	}
    	
    	return queryStringBuilder.toString();
    }
    
    /**
     * 从前端获取action_debug参数
     * @param request
     */
    private void analysisActionDebugParam(HttpServletRequest request) {
    	String actionDebug = DynamicDataSourceInterceptor.ACTION_DEBUG;
    	Integer threadActionDebug = ThreadContext.getContext(actionDebug);
    	if(threadActionDebug != null){
    		return;
    	}
    	
    	//获取url中的参数
    	String queryString = request.getQueryString();
    	if(queryString == null){
    		return;
    	}
    	
    	//解析url中的action_debug参数
    	String flag = actionDebug + '=';
    	int index = queryString.indexOf(flag);
    	if(index >= 0){
    		
    		int start = index + flag.length();
    		int end = queryString.indexOf('&', start);
    		String sParam = null;
    		if(end < 0){
    			sParam = queryString.substring(start);
    		}else{
    			sParam = queryString.substring(start, end);
    		}
    		
    		try{
    			Integer actionDebugValue = Integer.parseInt(sParam);
    			ThreadContext.putContext(actionDebug, actionDebugValue);
    		}catch(Exception e){
    			LOG.error("error in analysis action_debug", e);
    		}
    	}
	}

    public void destroy() {
        prepare.cleanupDispatcher();
    }
}
