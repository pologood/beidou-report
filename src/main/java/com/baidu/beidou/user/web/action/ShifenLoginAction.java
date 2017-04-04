/**
 * 2008-6-30  上午01:56:02 
 * 
 */
package com.baidu.beidou.user.web.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.util.loginvalidate.ShifenLoginValidator;
import com.baidu.beidou.user.util.loginvalidate.UCResponseState;
import com.baidu.beidou.util.Base64Coder;
import com.opensymphony.xwork2.Action;

/**
 * @author zengyunfeng
 * 
 */
public class ShifenLoginAction {

	private static final Log LOG = LogFactory.getLog(ShifenLoginAction.class);

	private int requestId = -1;
	
	private String fromurl;//from url
	
	/**
	 * 根据登录的用户角色
	 * 
	 * @return
	 */
	public String login() {
		
		String ChooseUser = "chooseUser";
		ShifenLoginValidator validator = ShifenLoginValidator.getInstance();
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		
		// 判断session中是否有登录用户信息
		Integer loginUserId = validator.isLogin(request, response,true);
		
		if (UCResponseState.STATE_NOT_LOGIN_HAS_REDIRECT == loginUserId){
			return null;//由UC实现登陆不成功的跳转，beidou不再关系，1.2.2
		}
		
		if (UCResponseState.STATE_NOT_LOGIN_NOT_REDIRECT.equals(loginUserId)){
			return UserWebConstant.PAGE_LOGIN;
		}
		
		if (UCResponseState.STATE_UNEXPECTED_EXCEPTION.equals(loginUserId)) {
			return Action.ERROR;
		}		
		
		HttpSession session = request.getSession();
		Object sessionPerson = session.getAttribute(UserWebConstant.USER_KEY);
		
		Visitor sessionVisitor = null;
		if (sessionPerson instanceof Visitor) { // session中有该用户的登录信息
			sessionVisitor = (Visitor) sessionPerson;
		}		
		if(sessionVisitor == null){	//session中没有用户信息，直接到dispatch
			
			if (!StringUtils.isEmpty(this.fromurl)) {
				this.fromurl = this.fromurl.replaceAll(" ", "+");//特殊处理，“+”属于base64字符，但被浏览器处理成空格
				this.fromurl = Base64Coder.decodeString(fromurl);
				return "back";
			}

			return "index";
		}

		if(requestId <= 0){	// 没有请求ID，使用loginName
			if(! loginUserId.equals(sessionVisitor.getUserid())){
				//之前已经有了登录用户的数据,跳转到用户提示页
				return ChooseUser;
			}
		}else{	//有请求ID,使用requestId
			if(requestId != sessionVisitor.getUserid()){
				//之前已经有了登录用户的数据,跳转到用户提示页
				return ChooseUser;
			}
		}
		
		if (!StringUtils.isEmpty(this.fromurl)) {
			this.fromurl = this.fromurl.replaceAll(" ", "+");//特殊处理，“+”属于base64字符，但被浏览器处理成空格
			this.fromurl = Base64Coder.decodeString(fromurl);
			return "back";
		}

		return "index";
	}

	/**
	 * @return the requestId
	 */
	public int getRequestId() {
		return requestId;
	}

	/**
	 * @param requestId the requestId to set
	 */
	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	/**
	 * @return the fromurl
	 */
	public String getFromurl() {
		return fromurl;
	}

	/**
	 * @param fromurl the fromurl to set
	 */
	public void setFromurl(String fromurl) {
		this.fromurl = fromurl;
	}


}
