package com.baidu.beidou.report.vo.sublink;

import com.baidu.beidou.report.vo.StatInfo;

/**
 * SublinkViewItem汇总信息
 * 
 * @author zhangxichuan
 * 
 */
public class SublinkViewItemSum extends StatInfo {

    private static final long serialVersionUID = -4456088221396576131L;

    private Integer sublinkCount;

    /**
     * SublinkViewItemSum无参构造方法
     */
    public SublinkViewItemSum() {

    }

    /**
     * SublinkViewItemSum含参构造方法
     * 
     * @param sublinkCount
     *            子链总数
     */
    public SublinkViewItemSum(Integer sublinkCount) {
        this.sublinkCount = sublinkCount;
    }

    /**
     * getter for sublinkCount
     * 
     * @return 子链总数
     */
    public Integer getSublinkCount() {
        return sublinkCount;
    }

    /**
     * setter for sublinkCount 设置子链总数
     * 
     * @param sublinkCount
     */
    public void setSublinkCount(Integer sublinkCount) {
        this.sublinkCount = sublinkCount;
    }

}
