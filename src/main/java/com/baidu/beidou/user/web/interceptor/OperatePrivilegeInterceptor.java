package com.baidu.beidou.user.web.interceptor;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.service.UserInfoMgr;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.TextProviderFactory;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * 
 * @author yangyun
 * @time 2009-12-15
 */
public class OperatePrivilegeInterceptor extends AbstractInterceptor implements
LocaleProvider{
	
	private static final long serialVersionUID = 3181826001969581713L;

	private static final Log LOG = LogFactory
			.getLog(OperatePrivilegeInterceptor.class);

	private final transient TextProvider textProvider = new TextProviderFactory()
			.createInstance(getClass(), this);
	
	private String privilege;
	
	private UserInfoMgr userInfoMgr;
	
	public String intercept(ActionInvocation arg0) throws Exception {
		
		if(privilege==null){
			return arg0.invoke();
		}else{
			ActionContext context = ActionContext.getContext();
			HttpServletRequest request = ServletActionContext.getRequest();
			Visitor loginer = (Visitor) request.getSession().getAttribute(
					UserWebConstant.USER_KEY);
			if (loginer == null) {
				LOG.error("errors.login.noSession");
				context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider
						.getText("errors.login.noSession"));
				return arg0.invoke();
			}
			
			String[] auths=userInfoMgr.getUserAuths(loginer.getUserid());
			for (String auth : auths) {
				
				if(privilege.equals(auth)){
					context.put(UserConstant.PRIVILEGE_KEY, privilege);
					return arg0.invoke();
				}
			}
			
			// 如果登录者是MCC账户，则权限同被操作用户
			if(loginer.getRoles().contains(UserConstant.USER_ROLE_MCC)){
				String[] userIdArr = (String[])(arg0.getInvocationContext().getParameters().get("userId"));
				if(userIdArr != null && userIdArr.length>0){
					String[] mccAuths=userInfoMgr.getUserAuths(Integer.parseInt(userIdArr[0]));
					for (String auth : mccAuths) {
						
						if(privilege.equals(auth)){
							context.put(UserConstant.PRIVILEGE_KEY, privilege);
							return arg0.invoke();
						}
					}
				}
			}
			
			LOG.warn(loginer.toString()+" has no operate privilege: "+ privilege);
			context.put(UserConstant.PRIVILEGE_ERROR_KEY, textProvider
					.getText("errors.no.operate.privilege"));

			return arg0.invoke();
		}
		
		
	}
	
	public Locale getLocale() {
		return ActionContext.getContext().getLocale();
	}

	public String getPrivilege() {
		return privilege;
	}

	public void setPrivilege(String privilege) {
		this.privilege = privilege;
	}

	public UserInfoMgr getUserInfoMgr() {
		return userInfoMgr;
	}

	public void setUserInfoMgr(UserInfoMgr userInfoMgr) {
		this.userInfoMgr = userInfoMgr;
	}
	
	

}
