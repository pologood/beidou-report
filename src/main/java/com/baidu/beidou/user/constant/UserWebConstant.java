package com.baidu.beidou.user.constant;

public class UserWebConstant {

	/**
	 * 登录页面
	 */
	public static final String PAGE_LOGIN = "login";
	/**
	 * HttpSession中存放登录用户的key
	 */
	public static final String USER_KEY = "UserConstant_VISITOR";
	/**
	 * memcache中存放token的前缀
	 */
	public static final String TOKEN_KEY = "UserConstant_TOKEN";

	/**
	 * 客户
	 */
	public static final int USER_TYPE_CUSTOMER = 0;
	
	/**
	 * 客服
	 */
	public static final int USER_TYPE_MANAGER = 1;
}
