package com.baidu.beidou.report.exception;

/**
 * @author hanxu03
 *
 */
public class RealtimeStatException extends RuntimeException {

	private static final long serialVersionUID = -7285690199360511093L;

	public RealtimeStatException(String msg) {
        super(msg);
    }
	
	public RealtimeStatException(String msg, Throwable e) {
        super(msg, e);
    }
}
