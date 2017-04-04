package com.baidu.beidou.report.vo.group;

import java.util.Set;

import com.baidu.beidou.olap.vo.GroupViewItem;

public class ExtGroupViewItem extends GroupViewItem {
	
	private int isAllSite;
	private Set<Integer> siteList;
	private Set<Integer> siteTradeList;
	
	public int getIsAllSite() {
		return isAllSite;
	}
	public void setIsAllSite(int isAllSite) {
		this.isAllSite = isAllSite;
	}
	public Set<Integer> getSiteList() {
		return siteList;
	}
	public void setSiteList(Set<Integer> siteList) {
		this.siteList = siteList;
	}
	public Set<Integer> getSiteTradeList() {
		return siteTradeList;
	}
	public void setSiteTradeList(Set<Integer> siteTradeList) {
		this.siteTradeList = siteTradeList;
	}

}
