/**
 * 2009-12-15 上午11:29:41
 * @author zengyunfeng
 */
package com.baidu.beidou.user.util.loginvalidate;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.util.memcache.BeidouMemcacheClient;

/**
 * @author zengyunfeng
 *
 */
public class MemcacheSessionInstance extends BeidouMemcacheClient{
	private static final Log LOG = LogFactory.getLog(MemcacheSessionInstance.class);
//	private final BeidouMemcacheClient memClient;
	private static volatile MemcacheSessionInstance instance = null;

	private MemcacheSessionInstance() throws IOException {
		super(MemcacheConfigure.MASTER_MEMCACHE_SERVER, MemcacheConfigure.SLAVE_MEMCACHE_SERVER,
				MemcacheConfigure.op_queue_len, MemcacheConfigure.read_buffer_size, MemcacheConfigure.operation_timeout);
	}
	
	public static MemcacheSessionInstance getInstance(){
		if(instance == null){
			synchronized (MemcacheSessionInstance.class) {
				if (instance == null) {
					try {
						instance = new MemcacheSessionInstance();
					} catch (IOException e) {
						LOG.fatal(e.getMessage(), e);
					}
				}
			}
		}
		return instance;
	}
	
	
	
	


}
