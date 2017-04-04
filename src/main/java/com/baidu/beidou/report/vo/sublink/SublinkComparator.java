package com.baidu.beidou.report.vo.sublink;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;

import com.baidu.beidou.olap.vo.SublinkViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.util.StringUtils;

/**
 * SublinkViewItem比较器
 * 
 * @author zhangxichuan
 */
public class SublinkComparator implements Comparator<SublinkViewItem> {

    int order;
    String col;

    /**
     * SublinkComparator含参构造函数
     * 
     * @param col
     *            比较的字段名
     * @param order
     */
    public SublinkComparator(String col, int order) {
        this.col = col;
        this.order = order;
    }

    private int compareByString(String v1, String v2) {
        if (StringUtils.isEmpty(v1)) {
            return -1 * order;
        }
        if (StringUtils.isEmpty(v2)) {
            return order;
        }
        Collator collator = Collator.getInstance(java.util.Locale.CHINA);
        return order * collator.compare(v1, v2);
    }

    @Override
    public int compare(SublinkViewItem o1, SublinkViewItem o2) {
        if (o1 == null) {
            return -1 * order;
        }
        if (o2 == null) {
            return 1 * order;
        }
        if (ReportWebConstants.SUBLINK_COLUMN_SUBLINKNAME.equals(col)) {
            return compareByString(o1.getSublinkName(), o2.getSublinkName());
        }
        if (ReportWebConstants.SUBLINK_COLUMN_SUBLINKURL.equals(col)) {
            return compareByString(o1.getSublinkUrl(), o2.getSublinkUrl());
        }
        if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
            return compareByString(o1.getPlanName(), o2.getPlanName());
        }
        if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
            return compareByString(o1.getGroupName(), o2.getGroupName());
        }
        if (ReportConstants.SRCHS.equals(col)) {
            if ((o1.getSrchs() - o2.getSrchs()) == 0) {
                return o1.getSublinkId().intValue() - o2.getSublinkId().intValue();
            } else {
                return order * (int) (o1.getSrchs() - o2.getSrchs());
            }
        }

        if (ReportConstants.CLKS.equals(col)) {
            return order * (int) (o1.getClks() - o2.getClks());
        }

        if (ReportConstants.COST.equals(col)) {
            if (o1.getCost() > o2.getCost()) {
                return order * 1;
            } else if (o1.getCost() < o2.getCost()) {
                return order * -1;
            } else {
                return 0;
            }

        }

        if (ReportConstants.CTR.equals(col)) {
            BigDecimal b1 = o1.getCtr();
            BigDecimal b2 = o2.getCtr();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.ACP.equals(col)) {
            BigDecimal b1 = o1.getAcp();
            BigDecimal b2 = o2.getAcp();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.CPM.equals(col)) {
            BigDecimal b1 = o1.getCpm();
            BigDecimal b2 = o2.getCpm();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.SRCHUV.equals(col)) {
            return order * (int) (o1.getSrchuv() - o2.getSrchuv());
        }

        if (ReportConstants.CLKUV.equals(col)) {
            return order * (int) (o1.getClkuv() - o2.getClkuv());
        }

        if (ReportConstants.SRSUR.equals(col)) {
            BigDecimal b1 = o1.getSrsur();
            BigDecimal b2 = o2.getSrsur();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.CUSUR.equals(col)) {
            BigDecimal b1 = o1.getCusur();
            BigDecimal b2 = o2.getCusur();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.COCUR.equals(col)) {
            BigDecimal b1 = o1.getCocur();
            BigDecimal b2 = o2.getCocur();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.RES_TIME_STR.equals(col)) {
            return order * (int) (o1.getResTimeStr().compareTo(o2.getResTimeStr()));
        }

        if (ReportConstants.ARRIVAL_RATE.equals(col)) {
            BigDecimal b1 = o1.getArrivalRate();
            BigDecimal b2 = o2.getArrivalRate();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.HOP_RATE.equals(col)) {
            BigDecimal b1 = o1.getHopRate();
            BigDecimal b2 = o2.getHopRate();
            if (b1 == null) {
                return -1 * order;
            }
            if (b2 == null) {
                return order;
            }
            return order * b1.compareTo(b2);
        }

        if (ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
            return order * (int) (o1.getDirectTrans() - o2.getDirectTrans());
        }

        if (ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
            return order * (int) (o1.getIndirectTrans() - o2.getIndirectTrans());
        }

        return order;
    }
}
