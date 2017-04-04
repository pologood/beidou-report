package com.baidu.beidou.report.vo.comparator;

import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.StatInfo;

public abstract class AbstractComparator {
	
	protected QueryParameter qp;
	protected int order = 1;
	protected AbstractComparator(QueryParameter qp){
		this.qp = qp;
		if(QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
			order = -1;
		}
	}
	
	protected int compareBySrchs(StatInfo o1, StatInfo o2){
		if(o1.getSrchs() > o2.getSrchs()){
			return order;
		}else if(o1.getSrchs() == o2.getSrchs()){
			return 0;
		}else{
			return order * -1;
		}		
	}
	
	protected int compareByClks(StatInfo o1, StatInfo o2){
		if(o1.getClks() > o2.getClks()){
			return order;
		}else if(o1.getClks() == o2.getClks()){
			return 0;
		}else{
			return order * -1;
		}
	}
	
	protected int compareByCost(StatInfo o1, StatInfo o2){
		if(o1.getCost() > o2.getCost()){
			return order;
		}else if(o1.getCost() == o2.getCost()){
			return 0;
		}else{
			return order * -1;
		}
	}
	
	protected int compareByCpm(StatInfo o1, StatInfo o2){
		return order * (o1.getCpm().compareTo(o2.getCpm()));	
	}
	
	protected int compareByAcp(StatInfo o1, StatInfo o2){
		return order * (o1.getAcp().compareTo(o2.getAcp()));
	}
	
	protected int compareByCtr(StatInfo o1, StatInfo o2){
		return order * (o1.getCtr().compareTo(o2.getCtr()));
	}

	public int compareByIndirectTrans(StatInfo o1, StatInfo o2) {
		if(o1.getIndirectTrans() > o2.getIndirectTrans()){
			return order;
		}else if(o1.getIndirectTrans() == o2.getIndirectTrans()){
			return 0;
		}else{
			return order * -1;
		}
	}

	public int compareByDirectTrans(StatInfo o1, StatInfo o2) {
		if(o1.getDirectTrans() > o2.getDirectTrans()){
			return order;
		}else if(o1.getDirectTrans() == o2.getDirectTrans()){
			return 0;
		}else{
			return order * -1;
		}
	}
	
	//增加uv数据和holmes数据的比较 @cpweb-492
	protected int compareBySrchuv(StatInfo o1, StatInfo o2){
		if(o1.getSrchuv() > o2.getSrchuv()){
			return order;
		}else if(o1.getSrchuv() == o2.getSrchuv()){
			return 0;
		}else{
			return order * -1;
		}		
	}
	
	protected int compareByClkuv(StatInfo o1, StatInfo o2){
		if(o1.getClkuv() > o2.getClkuv()){
			return order;
		}else if(o1.getClkuv() == o2.getClkuv()){
			return 0;
		}else{
			return order * -1;
		}
	}
	
	protected int compareBySrsur(StatInfo o1, StatInfo o2){
		return order * (o1.getSrsur().compareTo(o2.getSrsur()));	
	}
	
	protected int compareByCusur(StatInfo o1, StatInfo o2){
		return order * (o1.getCusur().compareTo(o2.getCusur()));
	}
	
	protected int compareByCocur(StatInfo o1, StatInfo o2){
		return order * (o1.getCocur().compareTo(o2.getCocur()));
	}
	
	protected int compareByArrivalRate(StatInfo o1, StatInfo o2){
		return order * (o1.getArrivalRate().compareTo(o2.getArrivalRate()));
	}
	
	protected int compareByHopRate(StatInfo o1, StatInfo o2){
		return order * (o1.getHopRate().compareTo(o2.getHopRate()));
	}
	
	protected int compareByResTime(StatInfo o1, StatInfo o2){
		return o1.getResTimeStr().compareTo(o2.getResTimeStr()) * order;
	}
}
