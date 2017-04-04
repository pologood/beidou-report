package com.baidu.beidou.report.vo.region;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.olap.vo.RegionViewItem;
import com.baidu.beidou.util.StringUtils;

public class RegionAssistant {
	
	/**一级地域ID**/
	private int selfRegionId;
	
	/**地域对象*/
	private RegionViewItem selfRegion;
	
	/**如果是一级地域，此为其下的二级地域列表*/
	private List<RegionViewItem> childRegions = new ArrayList<RegionViewItem>();
	
	/**
	 * 地域类型（排序）：1是一级地域，2是二级兴趣
	 */
	private int regionType = 1;
	
	/**
	 * 二级地域的排序方向
	 */
	private int childOrient = 1;
	
	public RegionAssistant(int selfRegionId){
		this.selfRegionId = selfRegionId;
	}
	
	class CproRegionViewItemComparator implements Comparator<RegionViewItem>  {	
		int order;
		public CproRegionViewItemComparator(int order) {
			this.order = order;
		}
		
		private int compareByString(String v1, String v2) {
			Collator collator=Collator.getInstance(java.util.Locale.CHINA);
			if(StringUtils.isEmpty(v1)) {
				return -1 * order;
			}
			if(StringUtils.isEmpty(v2)) {
				return order;
			}		
			return order * collator.compare(v1,v2);
		}
		
		public int compare(RegionViewItem o1, RegionViewItem o2) {
			if(o1.getRegionId() == 0){//“未知”排在最后
				return order * 1;
			}else{
				return compareByString(o1.getRegionName(), o2.getRegionName());
			}
		}
	}
	
	@Override
	public int hashCode(){
		return selfRegionId;
	}
	
	public void addSecondRegion(RegionViewItem second){
		if(childRegions == null){
			childRegions = new ArrayList<RegionViewItem>();
		}
		if(second != null){
			childRegions.add(second);
		}
	}
	
	public RegionViewItem getSecondRegion(int secondRegionId){
		if(childRegions == null){
			return null;
		}
		return childRegions.get(secondRegionId);
	}

	public RegionViewItem getSelfRegion() {
		return selfRegion;
	}

	public void setSelfRegion(RegionViewItem firstRegion) {
		this.selfRegion = firstRegion;
	}
	
	
	/**
	 * 获得全部兴趣VO，一节兴趣点和兴趣组合放在List的第一位置
	 * @return
	 */
	public List<RegionViewItem> getRegionViewItem(){
		int b = childRegions == null ? 0 : childRegions.size();
		List<RegionViewItem> result = new ArrayList<RegionViewItem>(1 + b);
		result.add(selfRegion);
		if(!CollectionUtils.isEmpty(childRegions)){
			Collections.sort(childRegions, new CproRegionViewItemComparator(childOrient));
			if(b != 0){
				result.addAll(childRegions);
			}
		}
		return result;
	}
	
	/**
	 * 计算统计数据和扩展数据，有子节点的计算统计数据和，没有子节点的即本身数据
	 */
	public void ensureStatFields(){
		if(childRegions != null){
			selfRegion.setChildCount(childRegions.size());
			for(RegionViewItem second : childRegions){
				second.generateExtentionFields();
				selfRegion.setSrchs(second.getSrchs()+selfRegion.getSrchs());
				selfRegion.setCost(second.getCost()+selfRegion.getCost());
				selfRegion.setClks(second.getClks()+selfRegion.getClks());
//				selfRegion.setSrchuv(second.getSrchuv()+selfRegion.getSrchuv());
//				selfRegion.setClkuv(second.getClkuv()+selfRegion.getClkuv());
//				selfRegion.setTransuv(second.getTransuv()+selfRegion.getTransuv());
				selfRegion.setHolmesClks(second.getHolmesClks()+selfRegion.getHolmesClks());
				selfRegion.setArrivalCnt(second.getArrivalCnt()+selfRegion.getArrivalCnt());
				selfRegion.setEffectArrCnt(second.getEffectArrCnt()+selfRegion.getEffectArrCnt());
				selfRegion.setHopCnt(second.getHopCnt()+selfRegion.getHopCnt());
				selfRegion.setResTime(second.getResTime()+selfRegion.getResTime());
			}
		}
		selfRegion.generateExtentionFields();
	}
	
	/********************getters and setters***********************/
	
	public int getSelfRegionId() {
		return selfRegionId;
	}
	
	public void setSelfRegionId(int selfRegionId) {
		this.selfRegionId = selfRegionId;
	}

	public int getRegionType() {
		return regionType;
	}

	public void setRegionType(int regionType) {
		this.regionType = regionType;
	}

	public int getChildOrient() {
		return childOrient;
	}

	public void setChildOrient(int childOrient) {
		this.childOrient = childOrient;
	}

	public List<RegionViewItem> getChildRegions() {
		return childRegions;
	}

	public void setChildRegions(List<RegionViewItem> childRegions) {
		this.childRegions = childRegions;
	}	
}
