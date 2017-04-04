/**
 * 2009-5-7
 * zengyunfeng
 * @version 1.1.3
 */
package com.baidu.beidou.user.web.action;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.web.aware.VisitorAware;
import com.baidu.beidou.util.UserActionLogUtils;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.opensymphony.xwork2.ActionSupport;

/**
 * 用户行为记录
 */
public class UserActionLogAction extends ActionSupport implements VisitorAware{

	private static final long serialVersionUID = -1197078401414370402L;
	private String useraction = null;
	private String targetObj = null;	//记录该行为针对的数据对象
	private Visitor visitor=null;
	private AbsJsonObject jsonObject = null;
	
	public String execute() throws Exception {
		UserActionLogUtils.info(visitor, targetObj, useraction);
		jsonObject = new AbsJsonObject();
		return SUCCESS;
	}
	
	
	/**
	 * @return the useraction
	 */
	public String getUseraction() {
		return useraction;
	}
	/**
	 * @param useraction the useraction to set
	 */
	public void setUseraction(String useraction) {
		this.useraction = useraction;
	}


	/**
	 * @return the visitor
	 */
	public Visitor getVisitor() {
		return visitor;
	}


	/**
	 * @param visitor the visitor to set
	 */
	public void setVisitor(Visitor visitor) {
		this.visitor = visitor;
	}


	/**
	 * @return the jsonObject
	 */
	public AbsJsonObject getJsonObject() {
		return jsonObject;
	}


	/**
	 * @param jsonObject the jsonObject to set
	 */
	public void setJsonObject(AbsJsonObject absJsonObject) {
		this.jsonObject = absJsonObject;
	}


	/**
	 * @return the targetObj
	 */
	public String getTargetObj() {
		return targetObj;
	}


	/**
	 * @param targetObj the targetObj to set
	 */
	public void setTargetObj(String targetObj) {
		this.targetObj = targetObj;
	}
}
