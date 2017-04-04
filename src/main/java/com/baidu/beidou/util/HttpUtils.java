/**
 * 2010-3-22 上午11:53:15
 * @author zengyunfeng
 * @modified by yangyun  2010-4-23 
 */
package com.baidu.beidou.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @author zengyunfeng
 * 获取HTTP相关的属性
 */
public class HttpUtils {

	private static final String forwardHeadName = "X-Forwarded-For";
	public static String getHttpForwardIp(HttpServletRequest request){
		if(request == null){
			return null;
		}
		String ip=request.getHeader(forwardHeadName);
		if(ip==null){
			return request.getRemoteAddr(); 
		}else{
			return ip;
		}
		
	}
	
	/**
	 * 获取浏览器信息
	 * @author yangyun
	 * @version 1.2.18
	 * @param request
	 * @return
	 */
	public static String getHttpBrowser(HttpServletRequest request){
		if(request == null){
			return null;
		}
		return request.getHeader("User-Agent");
	}
	
	/**
	 * 获得整个url,如http://www.weamea.com/music/index.jsp?id=4342
	 * @author yangyun
	 * @version 1.2.18
	 * @param request
	 * @return
	 */
	public static String getFullURL(HttpServletRequest request){
		if(request == null){
			return null;
		}
		String url=request.getRequestURL().toString();
		String QueryString=request.getQueryString();
		if((QueryString!=null)&&(QueryString.length()>0))
			url=url+"?"+QueryString;
		return url;
	} 
	
	
}	