package com.baidu.beidou.util.web.action;


import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.opensymphony.xwork2.ActionSupport;

/**
 * 统一返回错误提示信息
 * add by guojichun since beidou2.0.0
 */
public class JsonError extends ActionSupport {
	
	private static final long serialVersionUID = 13465324523452L;
		
	private AbsJsonObject jsonObject;
	private Integer status;
	private String messageText;
	
	public String execute() throws Exception {
		if(null == status){
			status=0;
		}
		if(null == messageText){
			messageText="";
		}
		jsonObject = new AbsJsonObject();
		jsonObject.setStatus(status);
		String message=getText(messageText);
		jsonObject.setGlobalMsg(message);
		return SUCCESS;
		
    }
	
	public AbsJsonObject getJsonObject() {
		return jsonObject;
	}
	public void setJsonObject(AbsJsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = StringUtils.filterXSSChars(messageText);
		//利用正则表达式去除ongl表达式,修补struts2远程执行命令的漏洞
		this.messageText = this.messageText.replaceAll("%\\{.*\\}", "");
		this.messageText = this.messageText.replaceAll("\\$\\{.*\\}", ""); //add by wangcohngjie since 2012.12.18
	}
	
	
}
