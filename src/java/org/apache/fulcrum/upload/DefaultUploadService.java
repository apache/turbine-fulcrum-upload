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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * This class is an implementation of {@link UploadService}.
 *
 * <p>
 * Files will be stored in temporary disk storage on in memory, depending on request size,
 * and will be available from the
 * <code>org.apache.fulcrum.util.parser.ParameterParser</code> as
 * <code>org.apache.commons.fileupload.FileItem</code> objects.
 *
 * <p>
 * This implementation of {@link UploadService} handles multiple files per single html
 * form, sent using multipart/form-data encoding type, as specified by RFC 1867. Use
 * <code>org.apache.fulcrum.parser.ParameterParser#getFileItems(String)</code> to acquire
 * an array of <code>org.apache.commons.fileupload.FileItem</code> objects associated with
 * given html form.
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultUploadService extends AbstractLogEnabled
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
    @Override
    public long getSizeMax()
    {
        return sizeMax;
    }

    /**
     * The threshold beyond which files are written directly to disk.
     */
    @Override
    public long getSizeThreshold()
    {
        return itemFactory.getThreshold();
    }

    /**
     * The location used to temporarily store files that are larger than the size threshold.
     */
    @Override
    public String getRepository()
    {
        return itemFactory.getRepository().toAbsolutePath().toString();
    }

    /**
     * @return Returns the headerEncoding.
     */
    @Override
    public String getHeaderEncoding()
    {
        return headerEncoding;
    }

    /**
     * <p>
     * Parses a <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant
     * <code>multipart/form-data</code> stream.
     * </p>
     *
     * @param req The servlet request to be parsed.
     * @throws ServiceException Problems reading/parsing the request or storing the uploaded
     *                          file(s).
     */
    @Override
    public List<FileItem> parseRequest(HttpServletRequest req) throws ServiceException
    {
        return parseRequest( req, this.sizeMax, this.itemFactory );
    }

    /**
     * <p>
     * Parses a <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant
     * <code>multipart/form-data</code> stream.
     * </p>
     *
     * @param req  The servlet request to be parsed.
     * @param path The location where the files should be stored.
     * @throws ServiceException Problems reading/parsing the request or storing the uploaded
     *                          file(s).
     */
    @Override
    public List<FileItem> parseRequest(HttpServletRequest req, String path) throws ServiceException
    {
        return parseRequest( req, this.sizeThreshold, this.sizeMax, path );
    }

    /**
     * <p>
     * Parses a <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant
     * <code>multipart/form-data</code> stream.
     * </p>
     *
     * @param req           The servlet request to be parsed.
     * @param sizeThreshold the max size in bytes to be stored in memory
     * @param sizeMax       the maximum allowed upload size in bytes
     * @param path          The location where the files should be stored.
     * @return list of file items
     * @throws ServiceException Problems reading/parsing the request or storing the uploaded
     *                          file(s).
     */
    @Override
    public List<FileItem> parseRequest(HttpServletRequest req, int sizeThreshold, int sizeMax, String path)
            throws ServiceException
    {
        Path buildPath = Paths.get( path );
        return parseRequest( req, sizeMax, DiskFileItemFactory.builder()
                .setPath(buildPath )
                .setBufferSize( sizeThreshold )
                .get()
                );
    }

    /**
     * <p>
     * Parses a <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant
     * <code>multipart/form-data</code> stream.
     * </p>
     *
     * @param req     The servlet request to be parsed.
     * @param sizeMax the maximum allowed upload size in bytes
     * @param factory the file item factory to use
     * @return list of file items
     * @throws ServiceException Problems reading/parsing the request or storing the uploaded
     *                          file(s).
     */
    protected List<FileItem> parseRequest(HttpServletRequest req, int sizeMax, DiskFileItemFactory factory)
            throws ServiceException
    {
        try
        {
            JakartaServletFileUpload fileUpload = new JakartaServletFileUpload<>( factory );
            fileUpload.setSizeMax( sizeMax );
            fileUpload.setHeaderCharset( null );

            if (getHeaderEncoding() != null)
            {
                Charset uploadCharset = getHeaderEncoding().equals( "UTF-8" ) ? StandardCharsets.UTF_8
                        : getHeaderEncoding().startsWith( "ISO-8859" ) ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;
                fileUpload.setHeaderCharset( uploadCharset );
                // fileUpload.setHeaderEncoding(headerEncoding);
            }
            return fileUpload.parseRequest( req );
        } catch (FileUploadException e)
        {
            throw new ServiceException( UploadService.ROLE, e.getMessage(), e );
        }
    }

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a> compliant
     * <code>multipart/form-data</code> stream.
     *
     * @param req The servlet request to be parsed.
     *
     * @return An iterator to instances of <code>FileItemStream</code> parsed from the
     *         request, in the order that they were transmitted.
     *
     * @throws ServiceException if there are problems reading/parsing the request or storing
     *                          files. This may also be a network error while communicating
     *                          with the client or a problem while storing the uploaded
     *                          content.
     */
    @Override
    public FileItemInputIterator getItemIterator(HttpServletRequest req) throws ServiceException
    {
        JakartaServletFileUpload upload = new JakartaServletFileUpload();
        try
        {
            return upload.getItemIterator( req );
        } catch (FileUploadException e)
        {
            throw new ServiceException( UploadService.ROLE, e.getMessage(), e );
        } catch (IOException e)
        {
            throw new ServiceException( UploadService.ROLE, e.getMessage(), e );
        }
    }

    /**
     * Utility method that determines whether the request contains multipart content.
     *
     * @param req The servlet request to be evaluated. Must be non-null.
     *
     * @return <code>true</code> if the request is multipart; <code>false</code> otherwise.
     */
    @Override
    public boolean isMultipart(HttpServletRequest req)
    {
        return JakartaServletFileUpload.isMultipartContent( req );
    }


    /**
     * @see org.apache.fulcrum.ServiceBroker#getRealPath(String)
     */
    private String getRealPath(String path)
    {
        String absolutePath = null;
        if (applicationRoot == null)
        {
            absolutePath = new File( path ).getAbsolutePath();
        } else
        {
            absolutePath = new File( applicationRoot, path ).getAbsolutePath();
        }

        return absolutePath;
    }

    // ---------------- Avalon Lifecycle Methods ---------------------
    /**
     * Avalon component lifecycle method
     */
    @Override
    public void configure(Configuration conf)
    {
        repositoryPath = conf.getAttribute( UploadService.REPOSITORY_KEY, UploadService.REPOSITORY_DEFAULT );

        headerEncoding = conf.getAttribute( UploadService.HEADER_ENCODING_KEY, UploadService.HEADER_ENCODING_DEFAULT );

        sizeMax = conf.getAttributeAsInteger( UploadService.SIZE_MAX_KEY, UploadService.SIZE_MAX_DEFAULT );

        sizeThreshold = conf.getAttributeAsInteger( UploadService.SIZE_THRESHOLD_KEY,
                UploadService.SIZE_THRESHOLD_DEFAULT );
    }

    /**
     * Avalon component lifecycle method
     *
     * Initializes the service.
     *
     * This method processes the repository path, to make it relative to the web application
     * root, if necessary
     */
    @Override
    public void initialize() throws Exception
    {
        // test for the existence of the path within the webapp directory.
        // if it does not exist, assume the path was to be used as is.
        String testPath = getRealPath( repositoryPath );
        File testDir = new File( testPath );
        if (testDir.exists())
        {
            repositoryPath = testPath;
        }

        getLogger().debug( "Upload Service: REPOSITORY_KEY => " + repositoryPath );

        itemFactory = 
                DiskFileItemFactory.builder()
                .setPath(Paths.get( repositoryPath ) )
                .setBufferSize( sizeThreshold )
                .get();
    }

    /**
     * Avalon component lifecycle method
     */
    @Override
    public void contextualize(Context context) throws ContextException
    {
        this.applicationRoot = context.get( "urn:avalon:home" ).toString();
    }
}
