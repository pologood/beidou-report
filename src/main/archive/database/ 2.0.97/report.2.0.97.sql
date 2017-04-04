USE beidoureport;
SET names utf8;

CREATE TABLE `realtime_stat_user` (
  `userid` int(10) NOT NULL,
  `cost` int(11) NOT NULL,
  UNIQUE KEY `userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;