package com.baidu.beidou.report.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class for generating and managing ETags for improved caching.
 * 
 * Objects of this class have two modes: batching mode and streaming mode.
 * 
 * In batching mode, the response body is stored in a buffer and, at the end,
 * its ETag is calculated and everything is written to the output at once.
 * 
 * In streaming mode, however, the response body is output as it's received from
 * the servlet, and no ETag is calculated.
 */
public class ETaggingHttpResponse extends HttpServletResponseWrapper {
	
	private static final Log LOG = LogFactory.getLog(ETaggingHttpResponse.class);

	public static final String RESPONSE_HEADER = "ETag";
	public static final String REQUEST_HEADER = "If-None-Match";
	
	public static final String HTTP_10_RESP_HEADER = "Last-Modified";
	public static final String HTTP_10_REQ_HEADER = "If-Modified-Since";

	private final HttpServletRequest request;
	private final BufferServletOutputStream stream;
	private ServletOutputStream originalStream;
	private PrintWriter writer;
	private boolean batching;

	public ETaggingHttpResponse(HttpServletRequest request,
			HttpServletResponse response) {
		super(response);
		this.request = request;
		this.stream = new BufferServletOutputStream();
		this.writer = null;
		this.batching = true;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (originalStream == null) {
			originalStream = getResponse().getOutputStream();
		}
		if (isCommitted()) {
			batching = false;
		}
		return stream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			writer = new PrintWriter(new OutputStreamWriter(getOutputStream(),getCharacterEncoding()));
		}
		return writer;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The response object is also switched to streaming mode.
	 */
	@Override
	public void flushBuffer() throws IOException {
		writeToOutput();
		getResponse().flushBuffer();
		batching = false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The response object is switched to batching mode if the response has not
	 * been committed yet.
	 */
	@Override
	public void reset() {
		super.reset();
		writer = null;
		stream.reset();
		batching = !isCommitted();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The response object is switched to batching mode if the response has not
	 * been committed yet.
	 */
	@Override
	public void resetBuffer() {
		super.resetBuffer();
		writer = null;
		stream.reset();
		batching = !isCommitted();
	}

	/**
	 * Outputs the response body.
	 * 
	 * In batching mode, it outputs the full contents of the buffer with its
	 * corresponding ETag, or a NOT_MODIFIED response if the ETag matches the
	 * request's "If-None-Match" header.
	 * 
	 * In streaming mode, output is only generated if the buffer is not empty;
	 * in that case, the buffer is flushed to the output.
	 * 
	 * @throws IOException
	 *             If there was a problem writing to the output.
	 */
	void writeToOutput() throws IOException {
		if (writer != null) {
			writer.flush();
		}
		byte[] bytes = stream.getBuffer().toByteArray();
		if (batching) {
			String etag = stream.getContentHash();
			String reqEtag = request.getHeader(REQUEST_HEADER);
			((HttpServletResponse) getResponse()).setHeader(RESPONSE_HEADER,etag);
			if (etag.equals(reqEtag)) {
				emitETagMatchedResult();
			} else {
				emitFullResponseBody(bytes);
			}
		} else if (bytes.length != 0) {
			originalStream.write(bytes);
			stream.getBuffer().reset();
		}
	}

	private void emitETagMatchedResult() {
		((HttpServletResponse) getResponse()).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		getResponse().setContentLength(0);
	}

	private void emitFullResponseBody(byte[] bytes) throws IOException {
		letHttp10NotUseCache();
		((HttpServletResponse) getResponse()).setHeader("Cache-Control", "pre-check=2, post-check=1");
		getResponse().setContentLength(bytes.length);
		getResponse().getOutputStream().write(bytes);
	}
	
	private void letHttp10NotUseCache(){
		Calendar cal = Calendar.getInstance();
	    cal.set(Calendar.MILLISECOND, 0);
	    Date lastModified = cal.getTime();
		((HttpServletResponse) getResponse()).setDateHeader(HTTP_10_RESP_HEADER, lastModified.getTime());
	}

	/**
	 * A ServletOutputStream that stores the data in a byte array buffer.
	 */
	class BufferServletOutputStream extends ServletOutputStream {

		private static final int BUFFER_INITIAL_CAPACITY = 16384;

		private ByteArrayOutputStream buffer = new ByteArrayOutputStream(
				BUFFER_INITIAL_CAPACITY);

		@Override
		public void write(int b) throws IOException {
			if (batching) {
				buffer.write(b);
			} else {
				originalStream.write(b);
			}
		}

		public ByteArrayOutputStream getBuffer() {

			return buffer;
		}

		public void reset() {
			buffer.reset();
		}

		public String getContentHash() {
			try {
				buffer.flush();
			} catch (IOException e) {
				// should never happen
				LOG.error(e);
			}
			long before = System.currentTimeMillis();
			byte[] bytes = buffer.toByteArray();
			String md5 = DigestUtils.md5Hex(bytes);
			long after = System.currentTimeMillis();
			LOG.info("compute md5: " + bytes.length + " bytes\t" + (after - before) + "ms\t" + request.getRequestURI());
			return md5;
		}
	}
}
