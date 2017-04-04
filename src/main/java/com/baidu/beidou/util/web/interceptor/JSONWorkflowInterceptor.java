/*
 * $Id: JSONWorkflowInterceptor.java,v 1.9 2010/04/15 07:49:58 zengyf Exp $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.beidou.util.web.interceptor;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;

/**
 * <p>Serializes validation and action errors into JSON. This interceptor does not
 * perform any validation, so it must follow the 'validation' interceptor on the stack.
 * </p>
 * 
 * <p>This stack (defined in struts-default.xml) shows how to use this interceptor with the
 * 'validation' interceptor</p>
 * <pre>
 * &lt;interceptor-stack name="jsonValidationWorkflowStack"&gt;
 *      &lt;interceptor-ref name="basicStack"/&gt;
 *      &lt;interceptor-ref name="validation"&gt;
 *            &lt;param name="excludeMethods"&gt;input,back,cancel&lt;/param&gt;
 *      &lt;/interceptor-ref&gt;
 *      &lt;interceptor-ref name="jsonValidation"/&gt;
 *      &lt;interceptor-ref name="workflow"/&gt;
 * &lt;/interceptor-stack&gt;
 * </pre>
 * <p>If 'validationFailedStatus' is set it will be used as the Response status
 * when validation fails.</p>
 * 
 * <p>If the request has a parameter 'struts.validateOnly' execution will return after 
 * validation (action won't be executed).</p>
 * 
 * <p>A request parameter named 'enableJSONValidation' must be set to 'true' to
 * use this interceptor</p>
 */
public class JSONWorkflowInterceptor extends MethodFilterInterceptor {
	
    private static final long serialVersionUID = -3791754612923730709L;

	private static final Log LOG = LogFactory.getLog(JSONWorkflowInterceptor.class);
    
    private boolean enabled = false;
    
    static char[] hex = "0123456789ABCDEF".toCharArray();
    
    private String inputResultName = Action.INPUT;
    
    /**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
    /**
	 * Set the <code>inputResultName</code> (result name to be returned when 
	 * a action / field error is found registered). Default to {@link Action#INPUT}
	 * 
	 * @param inputResultName
	 */
	public void setInputResultName(String inputResultName) {
		this.inputResultName = inputResultName;
	}

    @Override
    protected String doIntercept(ActionInvocation invocation) throws Exception {
    	
//    	if(!enabled){
//    		return invocation.invoke();
//    	}

    	HttpServletResponse response = ServletActionContext.getResponse();
        //HttpServletRequest request = ServletActionContext.getRequest();
    	 ActionContext context = ActionContext.getContext();
    	 
//    	只要当前的配置或者context中有一个为json，则表示当前流程为json流程
    	boolean jsoned = enabled;
    	if(!jsoned){
        	Boolean isJson = (Boolean)context.get(BeidouConstant.INTERCEPTOR_JSON_NAME);
        	if(isJson == null){
        		jsoned = false;
        	}else{
        		jsoned = isJson;
        	}
        }
        
        //把当前的请求是否为json请求存入context
        context.put(BeidouConstant.INTERCEPTOR_JSON_NAME, jsoned);
    	
        Object action = invocation.getAction();
        
        if (action instanceof ValidationAware) {
            // generate json
            ValidationAware validationAware = (ValidationAware) action;
            if (validationAware.hasErrors()) {
            	//如果需要json处理，则返回json格式，否则返回到input页面
            	if(jsoned){
            		response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().print(buildResponse(validationAware));                
                    return Action.NONE;
            	}else{
            		/*if (action instanceof BeidouActionSupport){
            			BeidouActionSupport ba = (BeidouActionSupport) action;
            			if (ba.getContd()){
            				return invocation.invoke();            				
            			}
            		}*/
            		return inputResultName;
            	}
            	
            }else {
            	try {
        			return invocation.invoke();
        		} catch (Exception e) {
        			if (jsoned) {
        				LogUtils.error(LOG, e.getMessage(), e);
        				return BeidouCoreConstant.SYSTEM_ERROR_JSON;
        			} else {
        				return Action.ERROR;
        			}
        		}
            }
        }else {
        	try {
    			return invocation.invoke();
    		} catch (Exception e) {
    			LogUtils.error(LOG, e.getMessage(), e);
    			if (jsoned) {
    				if (e instanceof BusinessException) {
                		response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().print(buildResponse(e.getMessage(), BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER));                
                        return Action.NONE;
    				}
    					
    				return BeidouCoreConstant.SYSTEM_ERROR_JSON;
    			} else {
    				throw e;
    			}
    		}
        }
    }
	protected String buildResponse(String error) {

		return buildResponse(error, BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER) ;
	}
    
    /**
	 * @return JSON string that contains the errors and field errors
	 * modify by guojichun since beidou2.0.0
	 */
	protected String buildResponse(String error, int failCode) {
		StringBuilder sb = new StringBuilder();
		sb.append(" { ");
		sb.append("\"status\":" + failCode + ",");
		List<String> errorMsg = new ArrayList<String>();
		errorMsg.add(error);
		if (errorMsg.isEmpty()) {
			// remove trailing comma, IE creates an empty object, duh
			sb.deleteCharAt(sb.length() - 1);
		} else {
			sb.append("\"msg\":");
			sb.append(buildArray(errorMsg));
		}
		sb.append(",\"statusInfo\":");
		sb.append("{\""+BeidouCoreConstant.MESSAGE_GLOBAL+"\":\"");
   	  	sb.append(error);
   	  	sb.append("\"}");
   	  	sb.append(",\"data\":{}");
		sb.append("} ");
		return sb.toString();
		/*
		 * response should be something like: { "status": 1, msg:["the_golbal_error"],"statusInfo": {"global":"the_golbal_error"},"data":{} }
		 */
	}

    /**
     * 将错误信息构造json格式数据
     * modify by guojichun since beidou2.0.0
     */
    protected String buildResponse(ValidationAware validationAware)
      {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        String FieldMsg = "";
        String GlobalMsg = "";
        if (validationAware.hasErrors()){
          sb.append("\"status\":"+BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER+",");     
          
          List<String> errorMsg = new ArrayList<String>();
          //action errors
          if (validationAware.hasActionErrors()) {
              errorMsg.addAll(validationAware.getActionErrors());
          }
          //field errors
          if (validationAware.hasFieldErrors()) {
              Map<String, List<String>> fieldErrors = validationAware.getFieldErrors();
              for (Map.Entry<String, List<String>> fieldError : fieldErrors.entrySet()) {
                  errorMsg.addAll(fieldError.getValue());
              }
          }
          if(errorMsg.size() == 0){
        	  //remove trailing comma, IE creates an empty object, duh
              sb.deleteCharAt(sb.length() - 1);
          }else{
          	sb.append("\"msg\":");
              sb.append(buildArray(errorMsg));
          }
          sb.append(",\"statusInfo\":");
          //有action级别的错误
          if (validationAware.hasActionErrors()) {
        	  Collection<String> GlobalMsgArray = validationAware.getActionErrors();
        	  //只返回第一个action级别的错误
        	  if(GlobalMsgArray.size()>0){
        		  GlobalMsg = GlobalMsg + escapeJSON(GlobalMsgArray.toArray()[0].toString());
        		  //对于fileupload抛出文件大小异常的单独处理，add by dongying since 1.0.7
        		  /*if(GlobalMsg.contains("the request was rejected because its size")){
        			  fileUploadErrorFlag = true;
        			  GlobalMsg = FILESIZE_TOO_LARGE_ERROR;
        	    	  sb.append("{\""+"field"+"\":{\""+FILE_HANDLE+"\":\"");
        	    	  sb.append(GlobalMsg);
        	    	  sb.append("\"}}}");
        	    	  return sb.toString();
        		  }*/
        	  }
          }
          //有field级别的错误
          if (validationAware.hasFieldErrors()){
              Map<String, List<String>> fieldErrors = validationAware.getFieldErrors();
              Set<Entry<String, List<String>>> set = fieldErrors.entrySet();
              
              for(Map.Entry<String, List<String>> entry : set){
              	FieldMsg = FieldMsg + "\""+escapeJSON(entry.getKey().toString())+"\":";      	
              	List<String> value =  entry.getValue();
              	//某个field有多个错误时，只返回第一个非空的错误信息
              	if(value.size()>0){
              		String message="";
              		for(int i=0;i<value.size();i++){
              			if(!StringUtils.isEmpty(value.get(i))){
              				message=value.get(i);
              				break;
              			}
              		}
              		FieldMsg = FieldMsg + "\""+ escapeJSON(message);
              	}
              	FieldMsg = FieldMsg + "\",";
          	 }
          }
          
          if (GlobalMsg != "") {
        	  sb.append("{\""+BeidouCoreConstant.MESSAGE_GLOBAL+"\":\"");
        	  sb.append(GlobalMsg);
        	  sb.append("\"}");
          } 
          if (FieldMsg != ""){	
        	  if (GlobalMsg != ""){
        		  sb.append(",");
        	  }
              sb.append("{\""+BeidouCoreConstant.MESSAGE_FIELD+"\":{");
              sb.append(FieldMsg);
              sb.deleteCharAt(sb.length() - 1);
              sb.append("}}");
          } 
        }
        sb.append(",\"data\":{}");
        sb.append("}");
        return sb.toString();
        
        /*response should be something like:
         {
         	"status":1,
         	"msg":["the_golbal_error","this_field_error","that_field_error"],
	        "statusInfo":{
	              "global": "the_golbal_error",
	              "field": {
	                    "field1": "this_field_error",
	                    "field2": "that_field_error"
	              }
	         },
	         "data":{}
	     }
         */
      }

    private String buildArray(Collection<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (String value : values) {
            sb.append("\"");
            sb.append(escapeJSON(value));
            sb.append("\",");
        }
        if (values.size() > 0)
            sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
    
    private String escapeJSON(Object obj) {
        StringBuilder sb = new StringBuilder();

        CharacterIterator it = new StringCharacterIterator(obj.toString());

        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '/') {
                sb.append("\\/");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (Character.isISOControl(c)) {
                sb.append(unicode(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Represent as unicode
     * @param c character to be encoded
     */
    private String unicode(char c) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\u");

        int n = c;

        for (int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;

            sb.append(hex[digit]);
            n <<= 4;
        }
        return sb.toString();
    }
}
