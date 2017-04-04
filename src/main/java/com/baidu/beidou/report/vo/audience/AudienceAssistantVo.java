package com.baidu.beidou.report.vo.audience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class AudienceAssistantVo {
	
	/**一级VOID**/
	private int selfId;
	
	/**VO对象*/
	private BaseAudienceVo selfVo;
	
	/**如果是一级VO，此为其下的二级VO列表*/
	private List<BaseAudienceVo> childVos = new ArrayList<BaseAudienceVo>();
	
	/**
	 * 二级VO的排序方向
	 */
	private int childOrient = 1;
	
	public AudienceAssistantVo(int selfId){
		this.selfId = selfId;
	}
	
	class CproBaseAudienceVoComparator implements Comparator<BaseAudienceVo>  {	
		int order;
		public CproBaseAudienceVoComparator(int order) {
			this.order = order;
		}
		public int compare(BaseAudienceVo o1, BaseAudienceVo o2) {
			return order * (o1.getId() - o2.getId());
		}
	}
	
	@Override
	public int hashCode(){
		return selfId;
	}
	
	public int getSelfId() {
		return selfId;
	}

	public void setSelfId(int selfId) {
		this.selfId = selfId;
	}

	public BaseAudienceVo getSelfVo() {
		return selfVo;
	}

	public void setSelfVo(BaseAudienceVo selfVo) {
		this.selfVo = selfVo;
	}

	public int getChildOrient() {
		return childOrient;
	}

	public void setChildOrient(int childOrient) {
		this.childOrient = childOrient;
	}

	
	public List<BaseAudienceVo> getChildVos() {
		return childVos;
	}

	public void setChildVos(List<BaseAudienceVo> childVos) {
		this.childVos = childVos;
	}

	/**
	 * 添加二级vo
	 * @param second
	 */
	public void addSecondVo(BaseAudienceVo second){
		if(childVos == null){
			childVos = new ArrayList<BaseAudienceVo>();
		}
		if(second != null){
			childVos.add(second);
		}
	}
	
	/**
	 * 获得全部VO，一级VO放在List的第一位置
	 * @return
	 */
	public List<BaseAudienceVo> getAllViewItems(){
		int b = childVos == null ? 0 : childVos.size();
		List<BaseAudienceVo> result = new ArrayList<BaseAudienceVo>(1 + b);
		result.add(selfVo);
		if(!CollectionUtils.isEmpty(childVos)){
			Collections.sort(childVos, new CproBaseAudienceVoComparator(childOrient));
			if(b != 0){
				result.addAll(childVos);
			}
		}
		return result;
	}
}
