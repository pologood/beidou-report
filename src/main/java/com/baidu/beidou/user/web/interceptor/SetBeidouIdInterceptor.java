/**
 * SetUserIdInterceptor.java
 * 2008-6-30
 */
package com.baidu.beidou.user.web.interceptor;


/**
 * @author zengyunfeng
 * 
 */
//public class SetBeidouIdInterceptor extends AbstractInterceptor {
//
//	private static final long serialVersionUID = -3226655247666853048L;
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see com.opensymphony.xwork2.interceptor.AbstractInterceptor#intercept(com.opensymphony.xwork2.ActionInvocation)
//	 */
//	@Override
//	public String intercept(ActionInvocation arg0) throws Exception {
//		HttpServletRequest request = ServletActionContext.getRequest();
//		HttpSession session = request.getSession();
//		Visitor loginer = (Visitor) session.getAttribute(UserWebConstant.USER_KEY);
//
//		// 设置id
//		Object action = arg0.getAction();
//		if (action instanceof BeidouIdAware) {
//			if (loginer == null) {
//				return UserWebConstant.PAGE_LOGIN;
//			}
//			if (loginer.hasPrivilege(UserConstant.USER_ROLE_CUSTOMER_NORMAL)) {
//				BeidouIdAware sma = (BeidouIdAware) action;
//				sma.setBeidouId(loginer.getId());
//			}
//		}
//
//		return arg0.invoke();
//	}
//
//}
