/**
 * 
 */
package com.baidu.beidou.user.web.interceptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.service.UserMgr;
import com.baidu.beidou.util.BeanUtils;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.TextProviderFactory;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * 用户数据权限过滤
 * @author yangyun
 * @time 2009-12-16
 * @version 1.2.8
 */
public class UserPrivilegeInterceptor extends AbstractInterceptor implements
		LocaleProvider {

	private static final Log LOG = LogFactory
			.getLog(UserPrivilegeInterceptor.class);

	private final transient TextProvider textProvider = new TextProviderFactory()
			.createInstance(getClass(), this);


	

	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		
		
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		ActionContext context = ActionContext.getContext();
		Visitor loginer = (Visitor) session.getAttribute(UserWebConstant.USER_KEY);
		if (loginer == null) {
			LOG.error("errors.login.noSession");
			context.put(UserConstant.NOTLOGIN_ERROR_KEY, textProvider
					.getText("errors.login.noSession"));
			return invocation.invoke();
		}
		
		//获得要操作的客户ID列表
		List<Integer> userids = getOperatedUserIdListForCheckingPrivilege();
		if (CollectionUtils.isEmpty(userids)) {
			return invocation.invoke();
		}
		String privilege=(String)(context.get(UserConstant.PRIVILEGE_KEY));
		UserMgr userMgr=(UserMgr)BeanUtils.getBean("userMgr");
		
		//userids去重
		Set<Integer> uniqUserIds = new HashSet<Integer>(userids);
		userids = new ArrayList<Integer>(uniqUserIds);
		
		for (Integer uid : userids) {
			
			if(!userMgr.hasDataPrivilege(privilege,loginer.getUserid(),uid)){
				LOG.warn(loginer.toString()+" has no  privilege to "+uid);
				context.put(UserConstant.PRIVILEGE_ERROR_KEY, textProvider
						.getText("errors.noprivilege"));
				return invocation.invoke();
			}
		}
		
		return invocation.invoke();
	}

	private List<Integer> getOperatedUserIdListForCheckingPrivilege() {
		HttpServletRequest request = ServletActionContext.getRequest();
		ActionContext context = ActionContext.getContext();
		List<Integer> userids = (List<Integer>) context.get(UserConstant.USER_PRIVILEGE_KEY);
		if (userids == null) {
			userids = new ArrayList<Integer>();
			String userId = request.getParameter("userId");
			if (userId == null){
				userId = request.getParameter("navigate.userId");
			}
			userids.add(Integer.parseInt(userId));
		}
		return userids;
	}
	
	public Locale getLocale() {
		return ActionContext.getContext().getLocale();
	}

	

}
