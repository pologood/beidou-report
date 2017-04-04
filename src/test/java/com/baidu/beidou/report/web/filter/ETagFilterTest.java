package com.baidu.beidou.report.web.filter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.junit.Before;
import org.junit.Test;

import com.baidu.beidou.test.BaseJMockTest;

public class ETagFilterTest  extends BaseJMockTest{
    
    private String encoding = "utf-8";
	private byte[] jsonBytes = null;
	private String md5 = null;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private OutputStream output = new ServletOutputStream() {
		@Override
		public void write(int b) throws IOException {
			baos.write(b);
		}
	};

	private Action responseToRequest = new Action() {
		public Object invoke(Invocation invocation) throws Throwable {
			HttpServletResponse response = (HttpServletResponse) invocation.getParameter(1);
			response.getOutputStream().write(jsonBytes);
			return null;
		}
		public void describeTo(Description desc) {
		}
	};

    @Before
    public void beforeEach() throws UnsupportedEncodingException{
		baos.reset();
		jsonBytes = "{\"data\":{\"kt\":1,\"qt\":1},\"msg\":[],\"status\":0,\"statusInfo\":{}}".getBytes(encoding);
		md5 = DigestUtils.md5Hex(jsonBytes);
    }

	@Test
	public final void testOnlyApplyToHttpRequestAndHttpResponse() throws IOException, ServletException {
		
		final ServletRequest request = context.mock(ServletRequest.class);
		final ServletResponse response = context.mock(ServletResponse.class);
		final FilterChain chain = context.mock(FilterChain.class);
		
		context.checking(new Expectations() {{
			oneOf(chain).doFilter(request, response);
		}});
		
		ETagFilter filter = new ETagFilter();
		filter.doFilter(request, response, chain);
	}

	@Test
	public final void testMatchedEtag() throws IOException, ServletException {

		final HttpServletRequest request = context.mock(HttpServletRequest.class);
		final HttpServletResponse response = context.mock(HttpServletResponse.class);
		final FilterChain chain = context.mock(FilterChain.class);
		
		context.checking(new Expectations() {{
			allowing(response).getWriter();
			allowing(response).getOutputStream(); will(returnValue(output));
			allowing(response).isCommitted() ;will(returnValue(false));
			allowing(response).getCharacterEncoding(); will(returnValue(encoding));
			allowing(request).getHeader("If-None-Match"); will(returnValue(md5));
			allowing(request).getRequestURI();
			oneOf(response).setStatus(304);
			oneOf(response).setContentLength(0);
			oneOf(chain).doFilter(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class))); will(responseToRequest);
			oneOf(response).setHeader("ETag", md5);
			
		}});
		
		
		ETagFilter filter = new ETagFilter();
		filter.doFilter(request, response, chain);
		assertThat(baos.toByteArray().length,is(0));
		
	}

	@Test
	public final void testNotMatchedEtag() throws IOException, ServletException {
		
		final HttpServletRequest request = context.mock(HttpServletRequest.class);
		final HttpServletResponse response = context.mock(HttpServletResponse.class);
		final FilterChain chain = context.mock(FilterChain.class);
		
		context.checking(new Expectations() {{
			allowing(response).getWriter();
			allowing(response).getOutputStream(); will(returnValue(output));
			allowing(response).isCommitted() ;will(returnValue(false));
			allowing(response).getCharacterEncoding(); will(returnValue(encoding));
			allowing(request).getHeader("If-None-Match"); will(returnValue("anyString"));
			allowing(request).getRequestURI();
			oneOf(response).setHeader("Cache-Control", "pre-check=2, post-check=1");
			oneOf(response).setContentLength(jsonBytes.length);
			oneOf(response).setDateHeader(with(any(String.class)),with(any(long.class)));
			oneOf(chain).doFilter(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class))); will(responseToRequest);
			oneOf(response).setHeader("ETag", md5);
			
		}});
		
		ETagFilter filter = new ETagFilter();
		filter.doFilter(request, response, chain);
		assertThat(baos.toByteArray(),is(jsonBytes));
		
	}
	


	@Test
	public final void testFlushed() throws IOException, ServletException {
		
		final HttpServletRequest request = context.mock(HttpServletRequest.class);
		final HttpServletResponse response = context.mock(HttpServletResponse.class);
		final FilterChain chain = context.mock(FilterChain.class);
		
		context.checking(new Expectations() {{
			allowing(response).getOutputStream(); will(returnValue(output));
			allowing(response).isCommitted() ;will(returnValue(true));
			allowing(response).getCharacterEncoding(); will(returnValue(encoding));
			oneOf(chain).doFilter(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class))); will(responseToRequest);
		}});
		
		ETagFilter filter = new ETagFilter();
		filter.doFilter(request, response, chain);
		assertThat(baos.toByteArray(),is(jsonBytes));
		
	}
	
	
}
