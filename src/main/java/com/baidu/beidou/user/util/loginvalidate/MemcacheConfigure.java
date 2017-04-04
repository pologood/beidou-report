/**
 * 2008-6-25  下午11:33:58 
 * 
 */
package com.baidu.beidou.user.util.loginvalidate;

/**
 * @author zengyunfeng
 * 
 */
public class MemcacheConfigure {
	
	private static String UC_SESSION_SERVERS = "";
	private static int UC_BEIDOU_APPID = 3;
	private static int UC_SERVER_TIMEOUT=500;
	private static String UC_BEIDOU_APPKEY = "";
	private static String UC_BEIDOU_COOKIE_DOMAIN = "";
	private static String UC_LOGIN_URL = "";
	private static String UC_LOGOUT_URL = "";
	private static String UC_JUMP_URL = "";
	
	/**
	 * memcache地址
	 * @version 解决用户IP漂移
	 * @author zengyunfeng
	 * @return
	 */
	public static String MASTER_MEMCACHE_SERVER= null;
	
	
	/**
	 * memcache地址
	 * @version 解决用户IP漂移
	 * @author zengyunfeng
	 * @return
	 */
	public static String SLAVE_MEMCACHE_SERVER= null;
	
	
	
	/**
	 * cookie 的key,对应cookie的值用做memcache中key的前缀
	 */
	public static String MEMCOOKIE_NAME = null;
	
	/**
	 * 单位为秒
	 */
	public static int operation_timeout = 1;
	/**
	 * 数据缓冲的大小
	 */
	public static int read_buffer_size = 16384;
	/**
	 * 读写队列
	 */
	public static int op_queue_len = 16384;
	/**
	 * 单位为秒
	 */
	public static int expire = 1440;
	
	
	
	
	public static int getUC_BEIDOU_APPID() {
		return UC_BEIDOU_APPID;
	}
	public void setUC_BEIDOU_APPID(int uc_beidou_appid) {
		UC_BEIDOU_APPID = uc_beidou_appid;
	}
	public static String getUC_BEIDOU_APPKEY() {
		return UC_BEIDOU_APPKEY;
	}
	public void setUC_BEIDOU_APPKEY(String uc_beidou_appkey) {
		UC_BEIDOU_APPKEY = uc_beidou_appkey;
	}
	public static String getUC_BEIDOU_COOKIE_DOMAIN() {
		return UC_BEIDOU_COOKIE_DOMAIN;
	}
	public void setUC_BEIDOU_COOKIE_DOMAIN(String uc_beidou_cookie_domain) {
		UC_BEIDOU_COOKIE_DOMAIN = uc_beidou_cookie_domain;
	}
	public static String getUC_JUMP_URL() {
		return UC_JUMP_URL;
	}
	public void setUC_JUMP_URL(String uc_jump_url) {
		UC_JUMP_URL = uc_jump_url;
	}
	public static String getUC_LOGIN_URL() {
		return UC_LOGIN_URL;
	}
	public void setUC_LOGIN_URL(String uc_login_url) {
		UC_LOGIN_URL = uc_login_url;
	}
	public static String getUC_SESSION_SERVERS() {
		return UC_SESSION_SERVERS;
	}
	public void setUC_SESSION_SERVERS(String uc_session_servers) {
		UC_SESSION_SERVERS = uc_session_servers;
	}
	public static int getUC_SERVER_TIMEOUT() {
		return UC_SERVER_TIMEOUT;
	}
	public void setUC_SERVER_TIMEOUT(int uc_server_timeout) {
		UC_SERVER_TIMEOUT = uc_server_timeout;
	}
	public static String getUC_LOGOUT_URL() {
		return UC_LOGOUT_URL;
	}
	public void setUC_LOGOUT_URL(String uc_logout_url) {
		UC_LOGOUT_URL = uc_logout_url;
	}
	/**
	 * @return the operation_timeout
	 */
	public static int getOperation_timeout() {
		return operation_timeout;
	}
	/**
	 * @param operation_timeout the operation_timeout to set
	 */
	public void setOperation_timeout(int operation_timeout) {
		MemcacheConfigure.operation_timeout = operation_timeout;
	}
	/**
	 * @return the read_buffer_size
	 */
	public static int getRead_buffer_size() {
		return read_buffer_size;
	}
	/**
	 * @param read_buffer_size the read_buffer_size to set
	 */
	public void setRead_buffer_size(int read_buffer_size) {
		MemcacheConfigure.read_buffer_size = read_buffer_size;
	}
	/**
	 * @return the op_queue_len
	 */
	public static int getOp_queue_len() {
		return op_queue_len;
	}
	/**
	 * @param op_queue_len the op_queue_len to set
	 */
	public void setOp_queue_len(int op_queue_len) {
		MemcacheConfigure.op_queue_len = op_queue_len;
	}
	/**
	 * @return the expire
	 */
	public static int getExpire() {
		return expire;
	}
	/**
	 * @param expire the expire to set
	 */
	public void setExpire(int expire) {
		MemcacheConfigure.expire = expire;
	}
	
	/**
	 * @return the mEMCACHE_SERVER
	 */
	public static String getMASTER_MEMCACHE_SERVER() {
		return MASTER_MEMCACHE_SERVER;
	}

	/**
	 * @param memcache_server the mEMCACHE_SERVER to set
	 */
	public void setMASTER_MEMCACHE_SERVER(String memcache_server) {
		MASTER_MEMCACHE_SERVER = memcache_server;
	}
	/**
	 * @return the sLAVE_MEMCACHE_SERVER
	 */
	public static String getSLAVE_MEMCACHE_SERVER() {
		return SLAVE_MEMCACHE_SERVER;
	}
	/**
	 * @param slave_memcache_server the sLAVE_MEMCACHE_SERVER to set
	 */
	public void setSLAVE_MEMCACHE_SERVER(String slave_memcache_server) {
		SLAVE_MEMCACHE_SERVER = slave_memcache_server;
	}
	/**
	 * @return the mEMCOOKIE_NAME
	 */
	public static String getMEMCOOKIE_NAME() {
		return MEMCOOKIE_NAME;
	}
	/**
	 * @param memcookie_name the mEMCOOKIE_NAME to set
	 */
	public void setMEMCOOKIE_NAME(String memcookie_name) {
		MEMCOOKIE_NAME = memcookie_name;
	}
	
}