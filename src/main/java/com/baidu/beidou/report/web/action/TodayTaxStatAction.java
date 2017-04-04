package com.baidu.beidou.report.web.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.olap.service.TaxAndCostService;
import com.baidu.beidou.olap.vo.TaxAndCostItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.stat.util.StatUtils;
import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.web.aware.VisitorAware;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.DateUtils;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.opensymphony.xwork2.ActionSupport;

/**
 * 获取账户当日税费数据
 * 
 * @author linan
 *
 */
public class TodayTaxStatAction extends ActionSupport implements VisitorAware{
	
	private static final long serialVersionUID = -6969917734824497867L;
	
	private static Log log = LogFactory.getLog(TodayTaxStatAction.class);
	
	public static final String SYSTEM_ERROR_JSON = BeidouCoreConstant.SYSTEM_ERROR_JSON;
	
	private Visitor visitor;
	private Integer userId;
	private AbsJsonObject jsonObject;

	@Resource(name="taxAndCostServiceImpl")
	private TaxAndCostService taxCostService;
	
	/**
	 * 获取用户账户数据，包括今日消费和税费数据
	 * 
	 * @return
	 */
	public String execute() {
		jsonObject=new AbsJsonObject();
		if (this.userId == null) {
			log.error("parameter is null");
			return SYSTEM_ERROR_JSON;
		}
		
		List<Integer> userIds=new ArrayList<Integer>();
		userIds.add(this.userId);
		
		List<TaxAndCostItem> statData = taxCostService.queryTaxData(
				userIds, 
				DateUtils.getDateCeil(new Date()).getTime(),
				DateUtils.getDateFloor(new Date()).getTime(),
				ReportConstants.TU_NONE);
		
		if(!statData.isEmpty()){
			for(TaxAndCostItem taxAndCostItem : statData){
				Long cost = new Long((long)taxAndCostItem.getCost()) ; // unit cent
				Long tax = new Long((long)taxAndCostItem.getTax()); // unit cent
				jsonObject.addData("todayCost", StatUtils.cent2yuan(cost));
				jsonObject.addData("todayTax", StatUtils.cent2yuan(tax));
			}
		}else{
			jsonObject.addData("todayCost", "-");
			jsonObject.addData("todayTax", "-");
		}
		return SUCCESS;
	}

	public Visitor getVisitor() {
		return visitor;
	}

	public void setVisitor(Visitor visitor) {
		this.visitor = visitor;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public AbsJsonObject getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(AbsJsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

}
