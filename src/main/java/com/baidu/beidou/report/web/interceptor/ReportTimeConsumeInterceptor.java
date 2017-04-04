package com.baidu.beidou.report.web.interceptor;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.web.action.BeidouReportActionSupport;
import com.baidu.beidou.util.DateUtils;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * <p>ClassName:ReportTimeConsumeInterceptor
 * <p>Function: 用于记录报表Action的执行时间差
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-4-12
 */
public class ReportTimeConsumeInterceptor extends AbstractInterceptor {

	private Log logger = LogFactory.getLog("reportActionStat") ;
	
	protected interface ActionType {
		int UNKNOWN = 0;   //其他未知
		int LIST = 1;      //列表功能，含列表和下载，如planList，downloadPlanList
		int FLASH = 2;     //FLash图表，如planSum
		int DETAIL = 3;     //细分报告，含分日和分网站，如**ByDay,**BySite
	}; 
	
	public String intercept(ActionInvocation invocation) throws Exception {
		Object obj = invocation.getAction();
		if( obj instanceof BeidouReportActionSupport ) {

			long time = System.currentTimeMillis();
			BeidouReportActionSupport action = (BeidouReportActionSupport)obj;
			String result;
			try {
				result = invocation.invoke();

				time = System.currentTimeMillis() - time;//in millSec
				
				if (action.getUserId() != null
						&& action.getQp() != null ) {
					generateLog(time, action, null);
				}
				return result;
			} catch (Exception e) {

				time = System.currentTimeMillis() - time;//in millSec
				
				if (action.getUserId() != null
						&& action.getQp() != null ) {
					generateLog(time, action, e);
				}
				throw e;
			}
		} else {
			
			return invocation.invoke();
		}
		
	}

	protected void generateLog(long time, BeidouReportActionSupport reportAction, Exception e) {

		QueryParameter qp = reportAction.getQp();
		AbsJsonObject json = reportAction.getJsonObject();
		if(json == null) {
			json = new AbsJsonObject();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(DateUtils.formatDate(new Date(), "yyyyMMddHHmmss"));
		sb.append("\t");
		sb.append(reportAction.getUserId());
		sb.append("\t");
		sb.append(qp.getLevel());
		sb.append("\t");
		sb.append(qp.getTab());
		sb.append("\t");
		sb.append(time);
		sb.append("\t");
		
		Object obj = json.getData().get("totalPage");
		if (obj != null) {
			sb.append(obj);
		} else {
			sb.append("");
		}
		sb.append("\t");
		obj = json.getData().get("cache");
		if (obj != null) {
			sb.append(obj);
		} else {
			sb.append("");
		}
		sb.append("\t");
		
		obj = json.getData().get("list");
		if (obj != null && (obj instanceof Collection) ) {
			Collection result = (Collection)obj;
			sb.append(result.size());
		} else {
			sb.append("");
		}
		sb.append("\t");
		sb.append(guessReportType(ServletActionContext.getActionMapping().getName()));
		sb.append("\t");
		if (e != null) {
			sb.append(e.getClass().getName());
		} else {
			sb.append("");
		}
		
		logger.info(sb.toString());
	}
	
	
	/**
	 * guessReportType:根据ACTION名字生成报告类型
	 *
	 * @param actionName action名
	 * @return  待统计报告类型    
	*/
	protected int guessReportType(String actionName) {
		if(StringUtils.isEmpty(actionName)) {
			return ActionType.UNKNOWN;
		} 
		if (actionName.endsWith("List")) {
			return ActionType.LIST;
		}
		if (actionName.endsWith("Sum")) {
			return ActionType.FLASH;
		}
		if (actionName.contains("By")) {
			return ActionType.DETAIL;
		}

		return ActionType.UNKNOWN;
	}

}
