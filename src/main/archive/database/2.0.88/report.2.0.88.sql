USE beidoureport;
SET names utf8;

/**
 * 修改stat_user_yest表结构
 */
ALTER TABLE `stat_user_yest`
ADD COLUMN `last_day_growth` int(10) NULL DEFAULT 0 COMMENT '消费环比增幅，计算为:(昨日消费/前日消费-1)*10000',
ADD COLUMN `last_week_day_growth` int(10) NULL DEFAULT 0 COMMENT '消费同比增幅，计算为:(昨日消费/上周同日消费-1)*10000';

CREATE TABLE `stat_user_20130225` AS SELECT * FROM `stat_user_yest`;