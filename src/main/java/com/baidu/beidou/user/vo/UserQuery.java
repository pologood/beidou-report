package com.baidu.beidou.user.vo;


import static com.baidu.beidou.util.FilterUtils.addBetweenCondition;
import static com.baidu.beidou.util.FilterUtils.addBetweenParameter;
import static com.baidu.beidou.util.FilterUtils.addIntCondition;
import static com.baidu.beidou.util.FilterUtils.addIntParameter;
import static com.baidu.beidou.util.FilterUtils.addListANDCondition;
import static com.baidu.beidou.util.FilterUtils.addListParameter;
import static com.baidu.beidou.util.FilterUtils.addOrderCondition;
import static com.baidu.beidou.util.FilterUtils.addStringCondition;
import static com.baidu.beidou.util.FilterUtils.addStringEqualParameter;
import static com.baidu.beidou.util.FilterUtils.addStringLikeParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * 针对用户的query
 * 
 * @author Administrator
 *
 */
public class UserQuery {
	public UserQuery(int userStatus, int queryBy, String queryWord) {
		super();
		this.userStatus = userStatus;
		this.queryBy = queryBy;
		this.queryWord = queryWord;
	}

	/**
	 * 用户状态
	 */
	private int userStatus;
	/**
	 * 筛选方式
	 */
	private int queryBy;
	/**
	 * 检索词
	 */
	private String queryWord;
	public int getUserStatus() {
		return userStatus;
	}

	public void setUserStatus(int userStatus) {
		this.userStatus = userStatus;
	}

	public int getQueryBy() {
		return queryBy;
	}

	public void setQueryBy(int queryBy) {
		this.queryBy = queryBy;
	}

	public String getQueryWord() {
		return queryWord;
	}

	public void setQueryWord(String queryWord) {
		this.queryWord = queryWord;
	}

	private final List<Object> param = new ArrayList<Object>();

	public String getFilter() {

		StringBuilder result = new StringBuilder();
		param.clear();
		
		
		if(this.userStatus!=0){
			addIntCondition(result, "u.state=?", this.userStatus);
			addIntParameter(param, this.userStatus);
		}

		if(!this.queryWord.equals("")){
			String word=this.queryWord;
			if(this.queryBy==1){
				int userid=Integer.parseInt(word);
				addIntCondition(result, "u.userid=?", userid);
				addIntParameter(param, userid);
			}else if(this.queryBy==2){
				addStringCondition(result, "u.username=? ", word);
				addStringLikeParameter(param, word);
			}else if(this.queryBy==2){
				addStringCondition(result, "u.auditManager=? ", word);
				addStringLikeParameter(param, word);
			}
		}		

		return result.toString();
	}

	public List<Object> getParam() {
		return param;
	}
}
