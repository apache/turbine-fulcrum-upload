package org.apache.fulcrum.upload;


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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * <p> This class is an implementation of {@link UploadService}.
 *
 * <p> Files will be stored in temporary disk storage on in memory,
 * depending on request size, and will be available from the {@link
 * org.apache.fulcrum.util.parser.ParameterParser} as {@link
 * org.apache.fulcrum.upload.FileItem}s.
 *
 * <p>This implementation of {@link UploadService} handles multiple
 * files per single html widget, sent using multipar/mixed encoding
 * type, as specified by RFC 1867.  Use {@link
 * org.apache.fulcrum.util.parser.ParameterParser#getFileItems(String)} to
 * acquire an array of {@link
 * org.apache.fulcrum.upload.FileItem}s associated with given
 * html widget.
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultUploadService
    extends AbstractLogEnabled
    implements UploadService, Initializable, Configurable, Contextualizable
{
    /** A File Item Factory object for the actual uploading */
    private DiskFileItemFactory itemFactory;

    private int sizeThreshold;
    private int sizeMax;

    private String repositoryPath;
    private String headerEncoding;

    /**
     * The application root
     */
    private String applicationRoot;

    /**
     * The maximum allowed upload size
     */
    public long getSizeMax()
    {
        return sizeMax;
    }

    /**
     * The threshold beyond which files are written directly to disk.
     */
    public long getSizeThreshold()
    {
        return itemFactory.getSizeThreshold();
    }

    /**
     * The location used to temporarily store files that are larger
     * than the size threshold.
     */
    public String getRepository()
    {
        return itemFactory.getRepository().getAbsolutePath();
    }

    /**
     * @return Returns the headerEncoding.
     */
    public String getHeaderEncoding() 
    {
        return headerEncoding;
    }
    
    /**
     * <p>Parses a <a href="http://rf.cx/rfc1867.html">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.</p>
     *
     * @param req The servlet request to be parsed.
     * @exception ServiceException Problems reading/parsing the
     * request or storing the uploaded file(s).
     */
    public List parseRequest(HttpServletRequest req)
        throws ServiceException
    {
        return parseRequest(req, this.sizeMax, this.itemFactory);
    }

    /**
     * <p>Parses a <a href="http://rf.cx/rfc1867.html">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.</p>
     *
     * @param req The servlet request to be parsed.
     * @param path The location where the files should be stored.
     * @exception ServiceException Problems reading/parsing the
     * request or storing the uploaded file(s).
     */
    public List parseRequest(HttpServletRequest req, String path)
        throws ServiceException
    {
        return parseRequest(req, this.sizeThreshold, this.sizeMax, path);
    }

    /**
     * <p>Parses a <a href="http://rf.cx/rfc1867.html">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.</p>
     *
     * @param req The servlet request to be parsed.
     * @param sizeThreshold the max size in bytes to be stored in memory
     * @param sizeMax the maximum allowed upload size in bytes
     * @param path The location where the files should be stored.
     * @exception ServiceException Problems reading/parsing the
     * request or storing the uploaded file(s).
     */
    public List parseRequest(HttpServletRequest req, int sizeThreshold,
                                  int sizeMax, String path)
            throws ServiceException
    {
        return parseRequest(req, sizeMax, new DiskFileItemFactory(sizeThreshold, new File(path)));
    }

    /**
     * <p>Parses a <a href="http://rf.cx/rfc1867.html">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.</p>
     *
     * @param req The servlet request to be parsed.
     * @param sizeMax the maximum allowed upload size in bytes
     * @param factory the file item factory to use
     * 
     * @exception ServiceException Problems reading/parsing the
     * request or storing the uploaded file(s).
     */
    private List parseRequest(HttpServletRequest req, int sizeMax, DiskFileItemFactory factory)
            throws ServiceException
    {
        try
        {
            ServletFileUpload fileUpload = new ServletFileUpload(factory);
            fileUpload.setSizeMax(sizeMax);
            fileUpload.setHeaderEncoding(headerEncoding);
            return fileUpload.parseRequest(req);
        }
        catch (FileUploadException e)
        {
            throw new ServiceException(UploadService.ROLE, e.getMessage(), e);
        }
    }

    /**
     * @see org.apache.fulcrum.ServiceBroker#getRealPath(String)
     */
    private String getRealPath(String path)
    {
        String absolutePath = null;
        if (applicationRoot == null)
        {
            absolutePath = new File(path).getAbsolutePath();
        }
        else
        {
            absolutePath = new File(applicationRoot, path).getAbsolutePath();
        }

        return absolutePath;
    }

    // ---------------- Avalon Lifecycle Methods ---------------------
    /**
     * Avalon component lifecycle method
     */
    public void configure(Configuration conf)
    {
        repositoryPath = conf.getAttribute(
                UploadService.REPOSITORY_KEY,
                UploadService.REPOSITORY_DEFAULT);

        headerEncoding = conf.getAttribute(
                UploadService.HEADER_ENCODING_KEY,
                UploadService.HEADER_ENCODING_DEFAULT);
        
        sizeMax = conf.getAttributeAsInteger(
                UploadService.SIZE_MAX_KEY,
                UploadService.SIZE_MAX_DEFAULT);

        sizeThreshold = conf.getAttributeAsInteger(
                UploadService.SIZE_THRESHOLD_KEY,
                UploadService.SIZE_THRESHOLD_DEFAULT);
    }

    /**
     * Initializes the service.
     *
     * This method processes the repository path, to make it relative to the
     * web application root, if necessary
     */
    public void initialize() throws Exception
    {
        // test for the existence of the path within the webapp directory.
        // if it does not exist, assume the path was to be used as is.
        String testPath = getRealPath(repositoryPath);
        File testDir = new File(testPath);
        if ( testDir.exists() )
        {
            repositoryPath = testPath;
        }

        getLogger().debug(
                "Upload Service: REPOSITORY_KEY => " + repositoryPath);

        itemFactory = new DiskFileItemFactory(sizeThreshold, new File(repositoryPath));
    }

    public void contextualize(Context context) throws ContextException
    {
        this.applicationRoot = context.get( "urn:avalon:home" ).toString();
    }
}
