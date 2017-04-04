
CREATE TABLE  if not exists `bd_996_dt_stat` (
  `userid` bigint(20) default '0',
  `planid` bigint(20) default '0',
  `groupid` bigint(20) default '0',
  `genderid` bigint(20) default '0',
  `srchs` bigint(20) default '0',
  `clks` bigint(20) default '0',
  `cost` bigint(20) default '0',
  `stat_time` bigint(20) default '0'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE  if not exists `bd_997_it_stat` (
  `userid` bigint(20) default '0',
  `planid` bigint(20) default '0',
  `groupid` bigint(20) default '0',
  `iid` bigint(20) default '0',
  `itid` bigint(20) default '0',
  `srchs` bigint(20) default '0',
  `clks` bigint(20) default '0',
  `cost` bigint(20) default '0',
  `stat_time` bigint(20) default '0'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE  if not exists `bd_995_keyword_stat` (
  `userid` bigint(20) default '0',
  `planid` bigint(20) default '0',
  `groupid` bigint(20) default '0',
  `keywordid` bigint(20) default '0',
  `wordid` bigint(20) default '0',
  `type` bigint(20) default '0',
  `srchs` bigint(20) default '0',
  `clks` bigint(20) default '0',
  `cost` bigint(20) default '0',
  `stat_time` bigint(20) default '0'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;