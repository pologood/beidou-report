package com.baidu.beidou.report.vo.site;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.constant.UnionSiteCache;
import com.baidu.beidou.cprogroup.vo.TradeInfo;
import com.baidu.beidou.report.ReportConstants;


public class SiteFlashCache {
	/** 一级域名缓存，key为groupid,value为该groupid对应的展现网站列表集（有可能是从行业转换而来） */
	private Map<Integer, Set<String>> firstDomainCache = new HashMap<Integer, Set<String>>();
	/** 二级域名缓存，key为groupid. */
	private Map<Integer, Set<String>> secondDomainCache = new HashMap<Integer, Set<String>>();
	
	public void insertTrade(Integer groupId, Integer tradeId){
		TradeInfo tradeInfo = UnionSiteCache.tradeInfoCache.getTradeInfoById(tradeId);
		if(tradeInfo == null){
			return;
		}
		List<String> urls = UnionSiteCache.tradeInfoCache.getSiteUrlListByTradeId(tradeId);
		if(urls != null){
			//如果是自有流量，是个二级域
			if(tradeInfo.getViewstat() == CproGroupConstant.TRADE_WHITELIST){
				Set<String> set = secondDomainCache.get(groupId);
				if(set == null){
					set = new HashSet<String>();
					secondDomainCache.put(groupId, set);
				}
				set.addAll(urls);				
			}else{
				Set<String> set = firstDomainCache.get(groupId);
				if(set == null){
					set = new HashSet<String>();
					firstDomainCache.put(groupId, set);
				}
				set.addAll(urls);
			}
		}
	}
	
	public void insertFirstDomain(Integer groupId, String firstDomain){
		Set<String> set = firstDomainCache.get(groupId);
		if(set == null){
			set = new HashSet<String>();
			firstDomainCache.put(groupId, set);
		}
		set.add(firstDomain);
	}
	
	public void insertSecondDomain(Integer groupId, String secondDomain){
		Set<String> set = secondDomainCache.get(groupId);
		if(set == null){
			set = new HashSet<String>();
			secondDomainCache.put(groupId, set);
		}
		set.add(secondDomain);
	}
	
	/**
	 * getFirstDomainGroupIdList:
	 * 获取一级域名对应的groupIds
	 * 
	 * @return      
	 * @since 
	*/
	public List<Integer> getFirstDomainGroupIdList() {
		List<Integer> result = new ArrayList<Integer>();
		if (!MapUtils.isEmpty(firstDomainCache)) {
			result.addAll(firstDomainCache.keySet());
		}
		return result;
	}
	
	public List<Map<String, Object>> filterByFirstDomainSet(List<Map<String, Object>> source) {
		
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		if (!CollectionUtils.isEmpty(source)) {
			
			for (Map<String, Object> item : source) {
				Object obj = item.get(ReportConstants.GROUP);
				if (obj == null) {
					continue;
				}
				Integer groupId = Integer.valueOf(obj.toString());
				
				Set<String> domain = firstDomainCache.get(groupId);
				if(domain == null || CollectionUtils.isEmpty(domain)) {
					continue;
				}
				if(domain.contains(item.get(ReportConstants.MAINSITE))) {
					result.add(item);
				}
			}
		}
		
		return result;
	}

	
	public List<Map<String, Object>> filterBySecondDomainSet(List<Map<String, Object>> source) {
		
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		if (!CollectionUtils.isEmpty(source)) {
			
			for (Map<String, Object> item : source) {
				Object obj = item.get(ReportConstants.GROUP);
				if (obj == null) {
					continue;
				}
				Integer groupId = Integer.valueOf(obj.toString());
				
				Set<String> domain = secondDomainCache.get(groupId);
				if(domain == null || CollectionUtils.isEmpty(domain)) {
					continue;
				}
				if(domain.contains(item.get(ReportConstants.SITE))) {
					result.add(item);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * getSecondDomainGroupIdList:
	 * 获取二级域名对应的groupIds
	 *
	 * @return      
	 * @since 
	*/
	public List<Integer> getSecondDomainGroupIdList() {
		List<Integer> result = new ArrayList<Integer>();
		if (!MapUtils.isEmpty(secondDomainCache)) {
			result.addAll(secondDomainCache.keySet());
		}
		return result;
	}

	public Map<Integer, Set<String>> getFirstDomainCache() {
		return firstDomainCache;
	}

	public void setFirstDomainCache(Map<Integer, Set<String>> firstDomainCache) {
		this.firstDomainCache = firstDomainCache;
	}

	public Map<Integer, Set<String>> getSecondDomainCache() {
		return secondDomainCache;
	}

	public void setSecondDomainCache(Map<Integer, Set<String>> secondDomainCache) {
		this.secondDomainCache = secondDomainCache;
	}
	
	
}
