/**
 * 2008-6-30  上午02:21:02 
 * 
 */
package com.baidu.beidou.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zengyunfeng
 * 
 */
public class CookieUtils {

	public static String getCookieValue(HttpServletRequest request,
			String cookieName) {
		if (cookieName == null || request == null) {
			return null;
		}
		Cookie[] cks = request.getCookies();
		if (cks == null) {
			return null;
		}
		for (Cookie cookie : cks) {
			if (cookieName.equals(cookie.getName()))
				return cookie.getValue();
		}
		return null;
	}

	public static Cookie getCookie(HttpServletRequest request, String cookieName) {
		if (cookieName == null || request == null) {
			return null;
		}
		Cookie[] cks = request.getCookies();
		if (cks == null) {
			return null;
		}
		for (Cookie cookie : cks) {
			if (cookieName.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}
}
