package com.baidu.beidou.report.vo.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.olap.vo.InterestViewItem;

public class InterestAssistant {
	/**一级兴趣或者兴趣组合ID**/
	private int selfInterestId;
	/**一级兴趣或者兴趣组合对象*/
	private InterestViewItem selfInterest;
	/**如果是一级兴趣，此为其下的二级兴趣列表*/
	private List<InterestViewItem> childInterests = null;
	/**
	 * 兴趣类型（排序）：1是抽象人群属性，2是一级兴趣，3是兴趣组合
	 */
	private int interestType = 1;
	/**
	 * 二级兴趣点的排序方向
	 */
	private int childOrient = 1;
	/**
	 * 
	 * 排序ID
	 */
	private int orderId = 0;
	
	public InterestAssistant(int selfInterestId){
		this.selfInterestId = selfInterestId;
	}
	
	class CproInterestViewItemComparator implements Comparator<InterestViewItem>  {	
		int order;
		public CproInterestViewItemComparator(int order) {
			this.order = order;
		}
		public int compare(InterestViewItem o1, InterestViewItem o2) {
			return order * (o1.getOrderId() - o2.getOrderId());
		}
	}
	
	@Override
	public int hashCode(){
		return selfInterestId;
	}
	
	public void addSecondInterest(InterestViewItem second){
		if(childInterests == null){
			childInterests = new ArrayList<InterestViewItem>();
		}
		if(second != null && second.getInterestId() != 0){
			childInterests.add(second);
		}
	}
	
	public InterestViewItem getSecondInterest(int secondInterestId){
		if(childInterests == null){
			return null;
		}
		return childInterests.get(secondInterestId);
	}

	public InterestViewItem getSelfInterest() {
		return selfInterest;
	}

	public void setSelfInterest(InterestViewItem firstInterest) {
		this.selfInterest = firstInterest;
	}
	
	
	/**
	 * 获得全部兴趣VO，一节兴趣点和兴趣组合放在List的第一位置
	 * @return
	 */
	public List<InterestViewItem> getInterestViewItem(){
		int b = childInterests == null ? 0 : childInterests.size();
		List<InterestViewItem> result = new ArrayList<InterestViewItem>(1 + b);
		result.add(selfInterest);
		if(!CollectionUtils.isEmpty(childInterests)){
			Collections.sort(childInterests, new CproInterestViewItemComparator(childOrient));
			if(b != 0){
				result.addAll(childInterests);
			}
		}
		return result;
	}
	
	/**
	 * 计算统计数据和扩展数据，有子节点的计算统计数据和，没有子节点的即本身数据
	 */
	public void ensureStatFields(){
		if(childInterests != null){
			for(InterestViewItem second : childInterests){
				second.generateExtentionFields();
				selfInterest.setSrchs(second.getSrchs() + selfInterest.getSrchs());
				selfInterest.setCost(second.getCost() + selfInterest.getCost());
				selfInterest.setClks(second.getClks() + selfInterest.getClks());
				
				selfInterest.setArrivalCnt(second.getArrivalCnt() + selfInterest.getArrivalCnt());
				selfInterest.setHopCnt(second.getHopCnt() + selfInterest.getHopCnt());
				selfInterest.setResTime(second.getResTime() + selfInterest.getResTime());
				
				selfInterest.setHolmesClks(second.getHolmesClks() + selfInterest.getHolmesClks());
				selfInterest.setEffectArrCnt(second.getEffectArrCnt() + selfInterest.getEffectArrCnt());
			}
		}
		selfInterest.generateExtentionFields();
	}
	
	/********************getters and setters***********************/
	
	public int getSelfInterestId() {
		return selfInterestId;
	}
	
	public void setSelfInterestId(int selfInterestId) {
		this.selfInterestId = selfInterestId;
	}

	public int getInterestType() {
		return interestType;
	}

	public void setInterestType(int interestType) {
		this.interestType = interestType;
	}

	public int getChildOrient() {
		return childOrient;
	}

	public void setChildOrient(int childOrient) {
		this.childOrient = childOrient;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
	
}
