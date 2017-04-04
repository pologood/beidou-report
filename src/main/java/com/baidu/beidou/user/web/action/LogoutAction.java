/**
 * 2008-6-30  上午01:56:02 
 * 
 */
package com.baidu.beidou.user.web.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.util.loginvalidate.MemcacheConfigure;
import com.baidu.beidou.user.util.loginvalidate.ShifenLoginValidator;
import com.opensymphony.xwork2.Action;

/**
 * @author zengyunfeng
 *
 */
public class LogoutAction {
	
	private static final Log LOG = LogFactory.getLog(LogoutAction.class);
	
	String ucLogoutUrl;
	
	public String goToShifen(){
		ShifenLoginValidator.getInstance().beidouLogout(ServletActionContext.getRequest());
		return Action.SUCCESS;
	}
	
	public String logout(){
		
		//清空beidou本地session
		ShifenLoginValidator.getInstance().beidouLogout(ServletActionContext.getRequest());
		//beidou本地的退出页面
		StringBuilder beidouLogoutUrl = new StringBuilder();
		beidouLogoutUrl.append(ServletActionContext.getRequest().getScheme());
		beidouLogoutUrl.append("://");
		beidouLogoutUrl.append(ServletActionContext.getRequest().getServerName());
		beidouLogoutUrl.append(":");
		beidouLogoutUrl.append(ServletActionContext.getRequest().getServerPort());
		beidouLogoutUrl.append(ServletActionContext.getRequest().getContextPath());
		beidouLogoutUrl.append("/logout.jsp");
		
		try {
			ucLogoutUrl = MemcacheConfigure.getUC_LOGOUT_URL() + "&u=" + URLEncoder.encode(beidouLogoutUrl.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error(e.getMessage(), e);
		}		
						
		return Action.SUCCESS;
	}

	public String getUcLogoutUrl() {
		return ucLogoutUrl;
	}

	public void setUcLogoutUrl(String ucLogoutUrl) {
		this.ucLogoutUrl = ucLogoutUrl;
	}	
}
