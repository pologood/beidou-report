package com.baidu.beidou.report.vo.site;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.olap.vo.TradeViewItem;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.StatInfo;

public class TradeAssistant  {
	private int selfTradeId;
	
	private TradeViewItem selfTrade;
	private Map<Integer, TradeViewItem> childTrades = null;
	
	private boolean isFirstTrade;
	
	private boolean selfFiltered;
	
	public TradeAssistant(int selfTradeId, boolean isFirstTrade, boolean forFlash){
		this.selfTradeId = selfTradeId;
		this.isFirstTrade = isFirstTrade;
	}

	
	public StatInfo getSumData(){
		if(selfTrade != null){
			return selfTrade;
		}else{
			StatInfo info = new StatInfo();
			if(childTrades != null){
				for(TradeViewItem vo : childTrades.values()){
					info.setSrchs(vo.getSrchs() + info.getSrchs());
					info.setClks(vo.getClks() + info.getClks());
					info.setCost(vo.getCost() + info.getCost());
				}
			}
			return info;
		}
	}
	
	@Override
	public int hashCode(){
		return selfTradeId;
	}
	
	public void filter(QueryParameter qp){
		if(ReportWebConstants.filter(qp, selfTrade)){
			this.selfFiltered = true;
		}
	}

	public void addSecondTrade(TradeViewItem second){
		if(childTrades == null){
			childTrades = new HashMap<Integer, TradeViewItem>();
		}
		if(second != null && second.getSecondTradeId() != 0){
			childTrades.put(second.getSecondTradeId(), second);
		}
	}
	
	public TradeViewItem getSecondTrade(int secondTradeId){
		if(childTrades == null){
			return null;
		}
		return childTrades.get(secondTradeId);
	}

	public TradeViewItem getSelfTrade() {
		return selfTrade;
	}

	public void setSelfTrade(TradeViewItem firstTrade) {
		this.selfTrade = firstTrade;
	}
	
	public List<TradeViewItem> getTradeViewItem(){
		if(this.isAllFiltered()){
			return new ArrayList<TradeViewItem>(0);
		}
		int b = childTrades == null ? 0 : childTrades.size();
		List<TradeViewItem> result = new ArrayList<TradeViewItem>(1 + b);
		result.add(selfTrade);		
		if(b != 0){
			result.addAll(childTrades.values());
		}
		return result;
	}
	
	public void ensureStatFields(){
		if(selfTrade != null){
			if(selfTrade.isHasFillStatInfo()){
				selfTrade.generateExtentionFields();
			}else{
				selfTrade.fillZeroStat();
			}
		}
		if(childTrades != null){
			for(TradeViewItem second : childTrades.values()){
				if(second.isHasFillStatInfo()){
					second.generateExtentionFields();
				}else{
					second.fillZeroStat();
				}
			}
		}
	}

	public boolean isAllFiltered() {
		return this.selfFiltered;
	}

	public boolean isFirstTrade() {
		return isFirstTrade;
	}

	public int getSelfTradeId() {
		return selfTradeId;
	}
}
