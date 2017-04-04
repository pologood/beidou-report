CREATE DATABASE IF NOT EXISTS `beidoureport` /*!40100 DEFAULT CHARACTER SET utf8 */;

use beidoureport;

CREATE TABLE `stat_user_yest` (
  `userid` int(10) NOT NULL,
  `srchs` bigint(20) NOT NULL,
  `clks` int(11) NOT NULL,
  `cost` int(11) NOT NULL,
  UNIQUE KEY `userid` (`userid`),
  KEY `cost` (`cost`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `stat_plan_yest` (
  `userid` int(10) NOT NULL,
  `planid` int(10) NOT NULL,
  `srchs` bigint(20) NOT NULL,
  `clks` int(11) NOT NULL,
  `cost` int(11) NOT NULL,
  UNIQUE KEY `planid` (`planid`),
  KEY `userid` (`userid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `stat_group_yest` (
  `userid` int(10) NOT NULL,
  `planid` int(10) NOT NULL,
  `groupid` int(10) NOT NULL,
  `srchs` bigint(20) NOT NULL,
  `clks` int(11) NOT NULL,
  `cost` int(11) NOT NULL,
  UNIQUE KEY `groupid` (`groupid`),
  KEY `userid` (`userid`),
  KEY `planid` (`planid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `stat_unit_yest` (
  `userid` int(10) NOT NULL,
  `planid` int(10) NOT NULL,
  `groupid` int(10) NOT NULL,
  `unitid` bigint(20) NOT NULL,
  `srchs` bigint(20) NOT NULL,
  `clks` int(11) NOT NULL,
  `cost` int(11) NOT NULL,
  UNIQUE KEY `unitid` (`unitid`),
  KEY `userid` (`userid`),
  KEY `planid` (`planid`),
  KEY `groupid` (`groupid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `stat_user_all` (
  `userid` int(10) NOT NULL,
  `srchs` bigint(20) NOT NULL,
  `clks` int(11) NOT NULL,
  `cost` int(11) NOT NULL,
  UNIQUE KEY `userid` (`userid`),
  KEY `cost` (`cost`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `sysnvtab` (
  `name` varchar(64) collate utf8_bin NOT NULL,
  `value` text collate utf8_bin,
  UNIQUE KEY `name` (`name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;