package com.baidu.beidou.report.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprounit.constant.CproUnitConstant;
import com.baidu.beidou.cprounit.service.CproUnitMgr;
import com.baidu.beidou.cprounit.service.impl.UbmcTmpUrlHandlerFactory;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportCproUnitDao;
import com.baidu.beidou.report.dao.vo.FrontBackendUnitStateMapping;
import com.baidu.beidou.report.dao.vo.UnitQueryParameter;
import com.baidu.beidou.report.service.ReportCproUnitMgr;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.comparator.ViewStateOrderComparator;
import com.baidu.beidou.olap.vo.UnitViewItem;
import com.baidu.beidou.util.sidriver.bo.SiProductBiz.ProductTemplateResponse.ProductTemplate;
import com.baidu.beidou.util.sidriver.bo.SiProductBiz.ProductTemplateType;
import com.baidu.beidou.util.sidriver.service.SmartIdeaService;

/**
 * <p>
 * ClassName:ReportCproUnitMgrImpl
 * <p>
 * Function: 推广组报表服务
 * 
 * @author <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created 2011-3-5
 */
public class ReportCproUnitMgrImpl implements ReportCproUnitMgr {

    private static Log logger = LogFactory.getLog(ReportCproUnitMgrImpl.class);

    private ReportCproUnitDao reportCproUnitDao;
    private CproUnitMgr unitMgr;
    private SmartIdeaService smartIdeaService;

    public List<UnitViewItem> findCproUnitViewItem(QueryParameter qp, boolean shouldPagable) {

        if (qp == null || qp.getUserId() == null) {
            logger.error("queryparameter or userId must not be null when execute the query");
            return new ArrayList<UnitViewItem>();
        }
        UnitQueryParameter queryParam = makeUnitQueryParameter(qp, shouldPagable);
        List<UnitViewItem> result = reportCproUnitDao.findCproUnitReportInfo(queryParam);

        fillSmartIdeaSrcInfo(result, qp.getUserId());

        // 填充临时URL
        this.fillTmpUrl(result);

        if (shouldPagable && !CollectionUtils.isEmpty(result)
                && ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())) {

            // 如果按状态排序则在内存中排。
            Collections.sort(result, new ViewStateOrderComparator(qp.getOrder()));
        }
        return result;
    }

    /**
     * 填充智能创意信息
     * 
     * @param result 数据库查询结果
     * @param userId
     */
    private void fillSmartIdeaSrcInfo(List<UnitViewItem> result, Integer userId) {
        if (CollectionUtils.isEmpty(result)) {
            return;
        }

        boolean hasSmart = false;
        for (UnitViewItem unitViewItem : result) {
            if (unitViewItem.getIsSmart() == CproGroupConstant.IS_SMART_TRUE) {
                hasSmart = true;
                break;
            }
        }

        // for smart idea fill src/height/width info
        if (hasSmart) {
            try {
                Map<Integer, ProductTemplate> templateMap = new HashMap<Integer, ProductTemplate>();
                try {
                    // construct template map
                    List<ProductTemplate> defaultTemplateList =
                            smartIdeaService.getTemplateInfoByType(ProductTemplateType.SYS_DEFAULT, userId);
                    List<ProductTemplate> customTemplateList =
                            smartIdeaService.getTemplateInfoByType(ProductTemplateType.USER_DEFINE, userId);
                    for (ProductTemplate productTemplate : defaultTemplateList) {
                        templateMap.put(productTemplate.getId(), productTemplate);
                    }
                    for (ProductTemplate productTemplate : customTemplateList) {
                        templateMap.put(productTemplate.getId(), productTemplate);
                    }
                } catch (Exception e) {
                    logger.error("Error occurred when visit nova api for smartidea!");
                }

                if (CollectionUtils.isEmpty(templateMap.keySet())) {
                    logger.error("userId=" + userId + " get null from nova api!");
                }

                for (UnitViewItem unitViewItem : result) {
                    if (unitViewItem.getIsSmart() == CproGroupConstant.IS_SMART_TRUE) {
                        Integer templateId = unitViewItem.getTemplateId();

                        unitViewItem.setSiHeight(unitViewItem.getHeight());
                        unitViewItem.setSiWidth(unitViewItem.getWidth());

                        // cannot find template so continue, this should never happen
                        if (templateId == null) {
                            logger.error("Template id=" + templateId + ", userId=" + userId + " is null!");
                            unitViewItem.setHeight(ReportWebConstants.SMARTIDEA_DEFAULT_THUMBNAIL_HEIGHT);
                            unitViewItem.setWidth(ReportWebConstants.SMARTIDEA_DEFAULT_THUMBNAIL_WIDTH);
                            continue;
                        }

                        // cannot find template, this might happen
                        ProductTemplate productTemplate = templateMap.get(templateId);
                        if (productTemplate == null) {
                            logger.error("Template id=" + templateId + ", userId=" + userId
                                    + " cannot find from nova api!");
                            unitViewItem.setHeight(ReportWebConstants.SMARTIDEA_DEFAULT_THUMBNAIL_HEIGHT);
                            unitViewItem.setWidth(ReportWebConstants.SMARTIDEA_DEFAULT_THUMBNAIL_WIDTH);
                        } else {
                            // set unit thumbnail info
                            if (productTemplate.getThumbnail() != null
                                    && StringUtils.isNotEmpty(productTemplate.getThumbnail().getUrl())) {
                                unitViewItem.setSrc(productTemplate.getThumbnail().getUrl());
                                unitViewItem.setHeight(productTemplate.getThumbnail().getHeight());
                                unitViewItem.setWidth(productTemplate.getThumbnail().getWidth());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error occurred when fill smart idea unit src/height/widht info ", e);
            }
        }

    }

    private void fillTmpUrl(List<UnitViewItem> list) {
        List<UnitViewItem> voList = new ArrayList<UnitViewItem>();
        for (UnitViewItem item : list) {
            if (item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_PICTURE
                    || item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_FLASH
                    || (item.getWuliaoType() == CproUnitConstant.MATERIAL_TYPE_LITERAL_WITH_ICON && item.getIsSmart()
                            .equals(CproUnitConstant.IS_SMART_FALSE))) {
                voList.add(item);
            }
        }
        unitMgr.fillTmpUrlBatch(voList, UbmcTmpUrlHandlerFactory.getHandler(UnitViewItem.class));
    }

    public int countCproUnitViewItem(QueryParameter qp) {
        if (qp == null || qp.getUserId() == null) {
            logger.error("queryparameter or userId must not be null when execute the query");
            return 0;
        }
        UnitQueryParameter queryParam = makeUnitQueryParameter(qp, false);
        return reportCproUnitDao.countCproUnitReportInfo(queryParam);
    }

    protected UnitQueryParameter makeUnitQueryParameter(QueryParameter qp, boolean shouldPagable) {

        UnitQueryParameter queryParam = new UnitQueryParameter();

        queryParam.setUserId(qp.getUserId());// userId不可能为空
        queryParam.setPlanId(qp.getPlanId());// planId可能为空
        queryParam.setGroupId(qp.getGroupId());
        queryParam.setName(qp.getKeyword());
        queryParam.setPromotionType(qp.getPromotionType());

        Integer[] groupTypes = qp.getDisplayType();

        if (!ArrayUtils.isEmpty(groupTypes)) {
            boolean hasFix = false;// 是否有固定
            boolean hasFlow = false;// 是否有悬浮
            boolean hasFilm = false;// 是否有贴片

            for (Integer groupType : groupTypes) {
                if (CproGroupConstant.GROUP_TYPE_FIXED == groupType) {
                    hasFix = true;
                    continue;
                }
                if (CproGroupConstant.GROUP_TYPE_FLOW == groupType) {
                    hasFlow = true;
                    continue;
                }
                if (CproGroupConstant.GROUP_TYPE_FILM == groupType) {
                    hasFilm = true;
                    continue;
                }
            }
            if (hasFix == hasFlow && hasFix == hasFilm) {
                // 如果三个都有或者都没有则不用按groupType过滤
            } else {
                queryParam.setGroupType(qp.getDisplayType());
            }
        }
        Integer[] wuliaoTypes = qp.getWuliaoType();

        if (!ArrayUtils.isEmpty(wuliaoTypes)) {
            queryParam.setWuliaoType(wuliaoTypes);
        }

        FrontBackendUnitStateMapping stateMapping = (FrontBackendUnitStateMapping) qp.getStateMapping();
        if (stateMapping != null && stateMapping.needFilterByState()) {
            // unit状态
            queryParam.setState(stateMapping.getUnitState());
            queryParam.setStateInclude(stateMapping.isIncludeUnitState());

            // group的状态
            queryParam.setGroupState(stateMapping.getGroupState());
            queryParam.setIncludeGroupState(stateMapping.isIncludeGroupState());

            // plan的状态
            queryParam.setIncludePlanState(stateMapping.isIncludePlanState());// 采用in | not in
            queryParam.setBudgetOver(stateMapping.isHasBudgetOver());// 添加已下线状态
            queryParam.setPlanState(stateMapping.getPlanStateWithoutBudgetOver());
        }

        if (!CollectionUtils.isEmpty(qp.getIds())) {
            queryParam.setIds(qp.getIds());
        }

        // 如果需要分页则设置分页信息
        // 通常来说，如果进行了分页的话就不按state和统计字段进行排序（记录条件大于1W的情况）
        if (shouldPagable) {
            int page = (qp.getPage() == null || qp.getPage() < 0) ? 0 : qp.getPage();
            int pageSize =
                    (qp.getPageSize() == null || qp.getPageSize() < 0) ? ReportConstants.PAGE_SIZE : qp.getPageSize();
            queryParam.setPage(page);
            queryParam.setPageSize(pageSize);
        }

        // 需要分页且非按统计字段和状态排序则通过sql的order by来排序（因为DB中的state值与需求的排序规则不匹配）
        // 说明：由于不分页的话是由前端直接来排序和分页的。
        if (shouldPagable && !ReportConstants.isStatField(qp.getOrderBy())
                && !ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())) {

            // 如果按照状态来排序的话由于在DB中不方便，因此由内存来排序
            queryParam.setSortColumn(ReportWebConstants.UNIT_FRONT_BACKEND_PARAMNAME_MAPPING.get(qp.getOrderBy()));
            queryParam.setSortOrder(qp.getOrder());
        }

        // 增加设置物料尺寸过滤条件
        if (qp.getUnitScale() != null) {
            String scale = qp.getUnitScale();
            String[] scaleArray = scale.split("\\*");
            if (scaleArray.length == 2 && scaleArray[0] != null && scaleArray[1] != null) {
                queryParam.setWidth(scaleArray[0]);
                queryParam.setHeight(scaleArray[1]);
            }
        }

        return queryParam;
    }

    public List<Long> findAllCproUnitIdsByQuery(QueryParameter qp, boolean shouldPagable) {

        if (qp == null || qp.getUserId() == null) {
            logger.error("queryparameter or userId must not be null when execute the query");
            return new ArrayList<Long>(0);
        }
        UnitQueryParameter queryParam = makeUnitQueryParameter(qp, false);
        return reportCproUnitDao.findAllCproUnitIds(queryParam);
    }

    public Map<Long, String> findCproUnitAuditInfo(Collection<Long> unitIds, int userId) {

        return reportCproUnitDao.findCproUnitAuditInfo(unitIds, userId);
    }

    public ReportCproUnitDao getReportCproUnitDao() {
        return reportCproUnitDao;
    }

    public void setReportCproUnitDao(ReportCproUnitDao reportCproUnitDao) {
        this.reportCproUnitDao = reportCproUnitDao;
    }

    public CproUnitMgr getUnitMgr() {
        return unitMgr;
    }

    public void setUnitMgr(CproUnitMgr unitMgr) {
        this.unitMgr = unitMgr;
    }

    public SmartIdeaService getSmartIdeaService() {
        return smartIdeaService;
    }

    public void setSmartIdeaService(SmartIdeaService smartIdeaService) {
        this.smartIdeaService = smartIdeaService;
    }

}