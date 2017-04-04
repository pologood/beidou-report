/**
 * 2008-6-30  上午01:56:02 
 * 
 */
package com.baidu.beidou.user.web.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.ROLE_CODE;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.web.aware.VisitorAware;
import com.baidu.beidou.util.LogUtils;

/**
 * @author zengyunfeng
 * 
 */
public class DispatchAction implements VisitorAware {

	private static final Log LOG = LogFactory.getLog(DispatchAction.class);
	private Visitor visitor;

	/**
	 * 根据登录的用户角色
	 * 
	 * @return
	 */
	public String dispatch() {
		if(visitor==null){
			return UserWebConstant.PAGE_LOGIN;
		}
		String result = "";
		
		//必须考虑顺序
		if(visitor.getRoles().contains(UserConstant.USER_ROLE_AUDITER)){
			result = "auditer";
		}else if(visitor.getRoles().contains(UserConstant.USER_ROLE_SALER_SUPER)||
				visitor.getRoles().contains(UserConstant.USER_ROLE_CLIENT_ADMIN)){
			result = "supersaler";
		}else if(visitor.getRoles().contains(UserConstant.USER_ROLE_SALER_FIRST)
				||visitor.getRoles().contains(UserConstant.USER_ROLE_SALER_SECOND)){
			result = "saler";
		}else if(visitor.getRoles().contains(UserConstant.USER_ROLE_CUSTOMER_HEAVY)
				||visitor.getRoles().contains(UserConstant.USER_ROLE_CUSTOMER_NORMAL)
				||visitor.getRoles().contains(UserConstant.USER_ROLE_CUSTOMER_VIP)){
			result = "normal";
		}else {
			LogUtils.fatal(LOG, "error role["+visitor.getRoles()+"] login!");
			result = UserWebConstant.PAGE_LOGIN;
		}
			
		return result;
	}

	/**
	 * @return the visitor
	 */
	public Visitor getVisitor() {
		return visitor;
	}

	/**
	 * @param visitor
	 *            the visitor to set
	 */
	public void setVisitor(Visitor visitor) {
		this.visitor = visitor;
	}

}
