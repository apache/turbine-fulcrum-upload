package org.apache.fulcrum.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
/**
 * UploadServiceTest
 *
 * @author <a href="epugh@upstate.com">Eric Pugh</a>
 * @version $Id$
 */
public class UploadServiceTest extends BaseUnit5Test
{
    private UploadService uploadService = null;


    @BeforeEach
    public void setUp() throws Exception
    {
        try
        {
            uploadService = (UploadService) this.lookup(UploadService.ROLE);
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    /**
     * Simple test that verify an object can be created and deleted.
     * @throws Exception
     */
    @Test
    public void testRepositoryExists() throws Exception
    {
        File f = new File(uploadService.getRepository());
        assertTrue(f.exists());
    }
    
    // This is Apache Commons, but we want to make be really sure it runs clean in Fulcrum
    @Test
    public void testUploadEncoding() throws Exception {
        HttpServletRequest request = getMockRequest();
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=boundary");
        when(request.getContentLength()).thenReturn(-1);// -1
        when(request.getMethod()).thenReturn("post");
        String testData= "Überfülle=\r\nf";
        //override default settings
        requestFormData( request, testData );
        assertTrue(uploadService.isMultipart( request ));
        List<FileItem> fil = uploadService.parseRequest( request );
        assertNotNull(fil);
        assertTrue( fil.size() >0);
        FileItem fi = fil.get( 0 );
        System.out.println( fi.getString() );
        assertEquals(15,fi.getSize());
        // default is ISO-8859-1
        assertTrue( fi.getString(StandardCharsets.UTF_8).startsWith( "Überfülle" ), "data string:'" +fi.getString(StandardCharsets.UTF_8) +"' not as expected");
        
        //reset inputstream
        requestFormData( request, testData);
        FileItemInputIterator fii = uploadService.getItemIterator( request );
        assertNotNull(fii);
        assertTrue( fii.hasNext());
        assertNotNull(fii.next());
    }

    private void requestFormData( HttpServletRequest request, String data)
        throws IOException
    {
        String example ="--boundary\r\n"
            + "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"12345678.txt\"\r\n"
            + "Content-Type: text/plain\r\n"
//            + "Content-Transfer-Encoding: UTF-8\r\n"
            + "\r\n"
            + data 
            + "\r\n--boundary--\r\n";
        final ByteArrayInputStream is = new ByteArrayInputStream (example.getBytes());// "UTF-8"
        when(request.getInputStream()).thenReturn(new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return is.read();
                }

				@Override
				public boolean isFinished() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean isReady() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public void setReadListener(ReadListener readListener) {
					// TODO Auto-generated method stub
					
				}
            });
    }
    
    protected Map<String,Object> attributes = new HashMap<String,Object>();
    protected int maxInactiveInterval = 0;
    // from Turbine org.apache.turbine.test.BaseTestCase, should be later in Fulcrum Testcontainer BaseUnit4Test
    protected HttpServletRequest getMockRequest()
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                String key = (String) invocation.getArguments()[0];
                return attributes.get(key);
            }
        }).when(session).getAttribute(anyString());

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                String key = (String) invocation.getArguments()[0];
                Object value = invocation.getArguments()[1];
                attributes.put(key, value);
                return null;
            }
        }).when(session).setAttribute(anyString(), any());

        when(session.getMaxInactiveInterval()).thenReturn(maxInactiveInterval);

        doAnswer(new Answer<Integer>()
        {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable
            {
                return Integer.valueOf(maxInactiveInterval);
            }
        }).when(session).getMaxInactiveInterval();

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Integer value = (Integer) invocation.getArguments()[0];
                maxInactiveInterval = value.intValue();
                return null;
            }
        }).when(session).setMaxInactiveInterval(anyInt());

        when(session.isNew()).thenReturn(true);
        when(request.getSession()).thenReturn(session);

        when(request.getServerName()).thenReturn("bob");
        when(request.getProtocol()).thenReturn("http");
        when(request.getScheme()).thenReturn("scheme");
        when(request.getPathInfo()).thenReturn("damn");
        when(request.getServletPath()).thenReturn("damn2");
        when(request.getContextPath()).thenReturn("wow");
        when(request.getContentType()).thenReturn("html/text");

        when(request.getCharacterEncoding()).thenReturn("UTF-8");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getLocale()).thenReturn(Locale.US);

        when(request.getHeader("Content-type")).thenReturn("html/text");
        when(request.getHeader("Accept-Language")).thenReturn("en-US");

        Vector<String> v = new Vector<String>();
        when(request.getParameterNames()).thenReturn(v.elements());
        return request;
    }
}
