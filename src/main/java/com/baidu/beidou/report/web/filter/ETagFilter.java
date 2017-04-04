package com.baidu.beidou.report.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A servlet filter to generate and check ETags in HTTP responses.
 * 
 * An ETag is calculated for the servlet's output. If its value matches the
 * value provided in the request's "If-None-Match" header, a 304 Not Modified
 * response is returned; otherwise, the value is added to the response's "ETag"
 * header.
 * 
 * Note that when this filter is applied, the response body cannot be streamed.
 */
public class ETagFilter implements Filter {
	
	private static final Log LOG = LogFactory.getLog(ETagFilter.class);

	private boolean disabled = false;
	
	public void init(FilterConfig filterConfig) {
		String paramValue = filterConfig.getInitParameter("disabled");
		if (paramValue != null && paramValue.trim().equalsIgnoreCase("true")) {
			disabled = true;
			LOG.info("Etag Filter is disabled");
		}
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (
			!disabled
				&&
			request instanceof HttpServletRequest 
				&&
			response instanceof HttpServletResponse
			) {
			
			ETaggingHttpResponse taggingResponse = createTaggingResponse(request, response);
			try {
				chain.doFilter(request, taggingResponse);
			} finally {
				// Write to the output even if there was an exception, as it
				// would have done without this filter.
				taggingResponse.writeToOutput();
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	protected ETaggingHttpResponse createTaggingResponse(ServletRequest request, ServletResponse response) {
		return new ETaggingHttpResponse((HttpServletRequest) request,(HttpServletResponse) response);
	}
}
