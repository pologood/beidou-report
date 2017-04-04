package com.baidu.beidou.report.web.interceptor;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.cprounit.service.CproUnitMgr;
import com.baidu.beidou.cprounit.vo.UnitInfoView;
import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * <p>ClassName:ReportListPrivilegeInterceptor
 * <p>Function: 用于列表页的权限判断
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-18
 */
public class ReportListPrivilegeInterceptor extends AbstractInterceptor {

	private transient CproGroupMgr groupMgr;
	private transient CproPlanMgr planMgr;
	private transient CproUnitMgr unitMgr;

	/** 前端传递的待查看的创意ID名 */
	private String unitIdName = "qp.unitId";
	/** 前端传递的待查看的组ID名 */
	private String groupIdName = "qp.groupId";
	/** 前端传递的待查看的计划ID名 */
	private String planIdName = "qp.planId";
	/** 前端传递的userID名 */
	private String userIdName = "userId";
	

	public String intercept(ActionInvocation invocation) throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		Visitor loginer = (Visitor) session.getAttribute(UserWebConstant.USER_KEY);
		if (loginer == null) {
			return invocation.invoke();
		}

		List<Integer> userids = null;
		ActionContext context = ActionContext.getContext();
		userids = (List<Integer>) context.get(UserConstant.USER_PRIVILEGE_KEY);
		if (userids == null) {
			userids = new ArrayList<Integer>();
		}

		BeanFactory factory = WebApplicationContextUtils
				.getWebApplicationContext(ServletActionContext
						.getServletContext());
		
		ValueStack stack = invocation.getStack();

		//unitId,groupId,planId,userId之间是只用判断一个
		
		boolean isUserIdSet = false;//当前是否已经设置琮userId了，如果已经设置过则后续就不设置了
		
		//判断unitId
		Long unitId = (Long)stack.findValue(unitIdName);
		if (unitId != null) {
			if (unitMgr == null) {
				//unitMgr = (CproUnitMgr) factory.getBean("cproUnitMgr");
				unitMgr = (CproUnitMgr) factory.getBean("unitMgr");
			}
			Integer userId = (Integer)stack.findValue(userIdName);

			UnitInfoView unit = null;
			if(userId != null){
				unit = unitMgr.findUnitById(userId, unitId);
			}
			if(unit != null) {
				userids.add(unit.getUserid());
				isUserIdSet = true;
			}
		}

		//判断group
		Integer groupId = (Integer)stack.findValue(groupIdName);
		if (!isUserIdSet && groupId != null) {
			if (groupMgr == null) {
				groupMgr = (CproGroupMgr) factory.getBean("cproGroupMgr");
			}
			CproGroup group = groupMgr.findCproGroupById(groupId);
			
			if(group != null) {
				userids.add(group.getUserId());
				isUserIdSet = true;
			}
		}

		//判断plan
		Integer planId = (Integer)stack.findValue(planIdName);
		if (!isUserIdSet && planId != null) {
			if (planMgr == null) {
				planMgr = (CproPlanMgr) factory.getBean("cproPlanMgr");
			}
			CproPlan plan = planMgr.findCproPlanById(planId);
			
			if(plan != null) {
				userids.add(plan.getUserId());
				isUserIdSet = true;
			}
		}
		
		//判断userId
		Integer userId = (Integer)stack.findValue(userIdName);
		if (!isUserIdSet && userId != null) {
			userids.add(userId);
			isUserIdSet = true;
		}
		
		context.put(UserConstant.USER_PRIVILEGE_KEY, userids);
		return invocation.invoke();
	}

	public String getUnitIdName() {
		return unitIdName;
	}

	public void setUnitIdName(String unitIdName) {
		this.unitIdName = unitIdName;
	}

	public String getGroupIdName() {
		return groupIdName;
	}

	public void setGroupIdName(String groupIdName) {
		this.groupIdName = groupIdName;
	}

	public String getPlanIdName() {
		return planIdName;
	}

	public void setPlanIdName(String planIdName) {
		this.planIdName = planIdName;
	}

	public String getUserIdName() {
		return userIdName;
	}

	public void setUserIdName(String userIdName) {
		this.userIdName = userIdName;
	}

}
