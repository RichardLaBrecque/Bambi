/*
 *
 * The CIP4 Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-2025 The International Cooperation for the Integration of 
 * Processes in  Prepress, Press and Postpress (CIP4).  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        The International Cooperation for the Integration of 
 *        Processes in  Prepress, Press and Postpress (www.cip4.org)"
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "CIP4" and "The International Cooperation for the Integration of 
 *    Processes in  Prepress, Press and Postpress" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact info@cip4.org.
 *
 * 5. Products derived from this software may not be called "CIP4",
 *    nor may "CIP4" appear in their name, without prior written
 *    permission of the CIP4 organization
 *
 * Usage of this software in commercial products is subject to restrictions. For
 * details please consult info@cip4.org.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE INTERNATIONAL COOPERATION FOR
 * THE INTEGRATION OF PROCESSES IN PREPRESS, PRESS AND POSTPRESS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the The International Cooperation for the Integration 
 * of Processes in Prepress, Press and Postpress and was
 * originally based on software 
 * copyright (c) 1999-2001, Heidelberger Druckmaschinen AG 
 * copyright (c) 1999-2001, Agfa-Gevaert N.V. 
 *  
 * For more information on The International Cooperation for the 
 * Integration of Processes in  Prepress, Press and Postpress , please see
 * <http://www.cip4.org/>.
 *  
 * 
 */

package org.cip4.bambi.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Enumeration;

import org.cip4.bambi.BambiTestCase;
import org.cip4.bambi.server.BambiServer;
import org.cip4.jdflib.util.CPUTimer;
import org.junit.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Dr. Rainer Prosi, Heidelberger Druckmaschinen AG
 * 
 *         16.02.2009
 */
public class BambiServletTest extends BambiTestCase
{

	public BambiServletTest()
	{
		super();
		wantContainer = true;
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testCreate() throws Exception
	{
		final BambiServer s = new BambiServer();
		assertNotNull(new BambiServlet(s));
	}

	class Enus implements Enumeration<String>
	{

		@Override
		public boolean hasMoreElements()
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String nextElement()
		{
			// TODO Auto-generated method stub
			return "";
		}

	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testDoGetPost() throws Exception
	{
		startContainer();
		final BambiServer s = new BambiServer();
		final BambiServlet bs = new BambiServlet(s);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer(""));
		when(req.getHeaderNames()).thenReturn(new Enus());
		// the mocked response has no output stream, so writing the response fails
		final HttpServletResponse resp = mock(HttpServletResponse.class);
		assertTrue(bs.doGetPost(req, resp, true));
		verify(resp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	@Test
	public void testDoGetPostNoErrorAfterCommit() throws Exception
	{
		startContainer();
		final BambiServlet bs = new BambiServlet(new BambiServer());
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer(""));
		when(req.getHeaderNames()).thenReturn(new Enus());
		final HttpServletResponse resp = mock(HttpServletResponse.class);
		when(resp.isCommitted()).thenReturn(true);
		assertTrue(bs.doGetPost(req, resp, true));
		verify(resp, never()).sendError(anyInt());
	}

	/**
	 * the root device work timer must be stopped even if reading the request fails
	 */
	@Test
	public void testDoGetPostEndsWorkOnIOException() throws Exception
	{
		startContainer();
		final BambiServlet bs = new BambiServlet(new BambiServer());
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getInputStream()).thenThrow(new IOException("broken stream"));

		bs.doGetPost(req, mock(HttpServletResponse.class), true);

		assertFalse("work timer still running", isRunning(BambiContainer.getInstance().getRootDev().getDeviceTimer(false)));
	}

	/**
	 * CPUTimer.start() is a no-op on a running timer, so a running timer does not count another start
	 */
	private boolean isRunning(final CPUTimer timer)
	{
		final long starts = timer.getNumStarts();
		timer.start();
		final boolean running = timer.getNumStarts() == starts;
		if (!running)
		{
			timer.stop();
		}
		return running;
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 *
	 */
	@Test
	public void testAuthMessage() throws Exception
	{
		startContainer();
		final BambiServer s = new BambiServer();
		final BambiServlet bs = new BambiServlet(s);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		assertEquals("BASIC", bs.getAuthMessage(req).substring(0, 5));
		when(req.getMethod()).thenReturn("Post");
		assertEquals("Not authenticated", bs.getAuthMessage(req));
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testUnAuth() throws Exception
	{
		startContainer();
		final BambiServer s = new BambiServer();
		final BambiServlet bs = new BambiServlet(s);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		assertTrue(bs.unAuthenticated(req, mock(HttpServletResponse.class)));
		when(req.getMethod()).thenReturn("Post");
		assertTrue(bs.unAuthenticated(req, mock(HttpServletResponse.class)));
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testAuthResp() throws Exception
	{
		startContainer();
		final BambiServer s = spy(new BambiServer());
		when(s.isAuthenticated(any(), any())).thenReturn(false);
		final BambiServlet bs = new BambiServlet(s);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer("https"));
		when(req.getHeaderNames()).thenReturn(new Enus());
		assertTrue(bs.doGetPost(req, mock(HttpServletResponse.class), true));
		assertTrue(bs.doGetPost(req, mock(HttpServletResponse.class), false));
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testDoGet() throws Exception
	{
		startContainer();
		final BambiServer s = new BambiServer();
		final BambiServlet bs = new BambiServlet(s);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer("https"));
		when(req.getHeaderNames()).thenReturn(new Enus());
		bs.doGet(req, mock(HttpServletResponse.class));
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testDoPost() throws Exception
	{
		startContainer();
		final BambiServer s = new BambiServer();
		final BambiServlet bs = new BambiServlet(s);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer("http"));
		when(req.getHeaderNames()).thenReturn(new Enus());
		bs.doPost(req, mock(HttpServletResponse.class));
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws Exception
	 * 
	 */
	@Test
	public void testCreateContainer() throws Exception
	{
		final BambiServer s = new BambiServer();
		assertNotNull(new BambiServlet(s));
		assertNotNull(BambiContainer.getInstance());
	}

	/**
	 * @throws BambiException
	 * 
	 * 
	 */
	@Test
	public void testParseEnv() throws BambiException
	{
		System.setProperty("foo", "/a/b/c");
		assertEquals(BambiServlet.parseEnv("%foo/bar"), "/a/b/c/bar");
		assertEquals("/bar", BambiServlet.parseEnv("/bar"));
		assertEquals(null, BambiServlet.parseEnv(null));
	}
}
