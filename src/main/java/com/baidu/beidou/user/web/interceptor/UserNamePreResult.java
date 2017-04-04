/**
 * 
 */
package com.baidu.beidou.user.web.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.service.UserMgr;
import com.baidu.beidou.util.BeanUtils;
import com.baidu.beidou.util.ThreadContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.PreResultListener;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * 设置导航栏 的用户名
 * @author zengyunfeng
 * @version 1.3.16 
 * 
 */
public class UserNamePreResult implements PreResultListener {

	private static final Log log = LogFactory
			.getLog(UserNamePreResult.class);
	
	private String userIdName = "navigate.userId";
	private String userNameName = "navigate.userName";
	private transient UserMgr userMgr = null;
	
	
	public UserNamePreResult(String userIdName, String userNameName) {
		super();
		this.userIdName = userIdName;
		this.userNameName = userNameName;
	}


	public UserNamePreResult() {
		super();
	}


	public void beforeResult(ActionInvocation invocation, String resultCode) {
		intercept(invocation);
	}
	

	public void intercept(ActionInvocation invocation) {		
		if (userMgr == null) {
			userMgr = (UserMgr)BeanUtils.getBean("userMgr");
		}
		ValueStack stack = invocation.getStack();
		try{
			Integer userId = (Integer) stack.findValue(userIdName);
			
			if(userId == null){
				return ;
			}	
			
			Visitor visitor = ThreadContext.getSessionVisitor();
			if(visitor==null){
				return ;
			}
			//判断是否与登陆用户是否有权限
			//空字符串，默认为管理类权限
			if(!userMgr.hasDataPrivilege("", visitor.getUserid(), userId)){
				return ;
			}
			User user = userMgr.findUserBySFid(userId);
			if(user == null){
				return ;
			}
			String userName = user.getUsername();
			stack.setValue(userNameName, userName, false);
		}catch (Exception e) {
			log.warn(e.getMessage(),e);
		}
		
	}

	/**
	 * @return the userIdName
	 */
	public String getUserIdName() {
		return userIdName;
	}

	/**
	 * @param userIdName the userIdName to set
	 */
	public void setUserIdName(String userIdName) {
		this.userIdName = userIdName;
	}

}
