package com.baidu.beidou.report.vo.audience;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.util.ReportFieldFormatter;

public class BaseAudienceVo {
	/**
	 * 受众分析数据项
	 */
	protected Integer id;//对象ID
	protected String name;//名字
	protected Integer parentId;//父节点ID	
	protected Integer orderId;//用来排序的orderid
	
	protected long srchuv;//展现受众
	protected long clkuv;//点击受众
	protected long transuv;//转化受众
	
	protected Double srchuvp;//展现受众占比
	protected Double clkuvp;//点击受众占比
	protected Double transuvp;//转化受众占比
	protected Double uvavgp;//互联网用户平均占比
	
	protected Integer srchuvdd;//转化受众区分度
	protected Integer clkuvdd;//转化受众区分度
	protected Integer transuvdd;//转化受众区分度

	protected DecimalFormat df3 = new DecimalFormat("#.###");//3位小数
	
	public BaseAudienceVo(){
		fillZeroStat();
	}
	
	public void fillZeroStat() {
		this.srchuv = 0;
		this.clkuv = 0;
		this.transuv = 0;
		
		this.srchuvp = 0d;
		this.clkuvp = 0d;
		this.transuvp = 0d;
		this.uvavgp = 0d;
		
		this.srchuvdd = 0;
		this.clkuvdd = 0;
		this.transuvdd = 0;
	}
	
	/**
	 * 若某字段为null，相应属性值设为0/-1
	 * 
	 * 从mysql改用storage之后，若除数为0，则ctr、acp、cpm的计算值为0，需要调整为-1
	 * modified by yanjie at 20090709
	 */
	public void fillAudienceRecord(Map<String, Object> record){
		if (CollectionUtils.isEmpty(record)){
			this.fillZeroStat();
		} else {
			Object rd = null;	

			rd = record.get(ReportConstants.SRCHUV);
			if (rd != null){
				this.srchuv = Long.parseLong(rd.toString());
			} else {
				this.srchuv = 0;
			}
			record.put(ReportConstants.SRCHUV, this.srchuv);
			
			rd = record.get(ReportConstants.CLKUV);
			if (rd != null){
				this.clkuv = Long.parseLong(rd.toString());
			} else {
				this.clkuv = 0;
			}
			record.put(ReportConstants.CLKUV, this.clkuv);
			
			rd = record.get(ReportConstants.TRANSUV);
			if (rd != null){
				this.transuv = Long.parseLong(rd.toString());
			} else {
				this.transuv = 0;
			}
			record.put(ReportConstants.TRANSUV, this.transuv);
			

			rd = record.get(ReportConstants.SRCH_UV_PROP);
			if (rd != null){
				this.setSrchuvp(Double.parseDouble(df3.format(rd)));
			}else{
				this.setSrchuvp(0d);
			}
		

			rd = record.get(ReportConstants.CLK_UV_PROP);
			if (rd != null){
				this.setClkuvp(Double.parseDouble(df3.format(rd)));
			}else{
				this.setClkuvp(0d);
			}
			
			rd = record.get(ReportConstants.TRANS_UV_PROP);
			if (rd != null){
				this.setTransuvp(Double.parseDouble(df3.format(rd)));
			}else{
				this.setTransuvp(0d);
			}
			

			rd = record.get(ReportConstants.UV_AVG_PROP);
			if (rd != null){
				this.setUvavgp(Double.parseDouble(df3.format(rd)));
			}else{
				this.setUvavgp(0d);
			}
			
			rd = record.get(ReportConstants.SRCH_UV_DD);
			if (rd != null){
				this.srchuvdd = Integer.parseInt(rd.toString());
			} else {
				this.srchuvdd = 0;
			}
			record.put(ReportConstants.SRCH_UV_DD, this.srchuvdd);
			
			rd = record.get(ReportConstants.CLK_UV_DD);
			if (rd != null){
				this.clkuvdd = Integer.parseInt(rd.toString());
			} else {
				this.clkuvdd = 0;
			}
			record.put(ReportConstants.CLK_UV_DD, this.clkuvdd);
			
			rd = record.get(ReportConstants.TRANS_UV_DD);
			if (rd != null){
				this.transuvdd = Integer.parseInt(rd.toString());
			} else {
				this.transuvdd = 0;
			}
			record.put(ReportConstants.TRANS_UV_DD, this.transuvdd);	
		}
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getParentId() {
		return parentId;
	}
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}
	
	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public long getSrchuv() {
		return srchuv;
	}
	public void setSrchuv(long srchuv) {
		this.srchuv = srchuv;
	}
	public long getClkuv() {
		return clkuv;
	}
	public void setClkuv(long clkuv) {
		this.clkuv = clkuv;
	}
	public long getTransuv() {
		return transuv;
	}
	public void setTransuv(long transuv) {
		this.transuv = transuv;
	}
	
	public Double getSrchuvp() {
		return srchuvp;
	}

	public void setSrchuvp(Double srchuvp) {
		this.srchuvp = srchuvp;
	}

	public Double getClkuvp() {
		return clkuvp;
	}

	public void setClkuvp(Double clkuvp) {
		this.clkuvp = clkuvp;
	}

	public Double getTransuvp() {
		return transuvp;
	}

	public void setTransuvp(Double transuvp) {
		this.transuvp = transuvp;
	}

	public Integer getSrchuvdd() {
		return srchuvdd;
	}

	public void setSrchuvdd(Integer srchuvdd) {
		this.srchuvdd = srchuvdd;
	}

	public Integer getClkuvdd() {
		return clkuvdd;
	}

	public void setClkuvdd(Integer clkuvdd) {
		this.clkuvdd = clkuvdd;
	}

	public Integer getTransuvdd() {
		return transuvdd;
	}

	public void setTransuvdd(Integer transuvdd) {
		this.transuvdd = transuvdd;
	}

	public Double getUvavgp() {
		return uvavgp;
	}

	public void setUvavgp(Double uvavgp) {
		this.uvavgp = uvavgp;
	}	
}
