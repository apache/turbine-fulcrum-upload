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


import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.service.ServiceException;

/**
 * <p> This service handles parsing <code>multipart/form-data</code>
 * POST requests and turning them into form fields and uploaded files.
 * This can be either performed automatically by the {@link
 * org.apache.fulcrum.parser.ParameterParser} or manually by an user
 * definded {@link org.apache.turbine.modules.Action}.
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @version $Id$
 */
public interface UploadService

{
    /** Avalon Identifier **/
    String ROLE = UploadService.class.getName();

    /**
     * HTTP header.
     */
    String CONTENT_TYPE = "Content-type";

    /**
     * HTTP header.
     */
    String CONTENT_DISPOSITION = "Content-disposition";

    /**
     * HTTP header base type.
     */
    String MULTIPART = "multipart";

    /**
     * HTTP header base type modifier.
     */
    String FORM_DATA = "form-data";

    /**
     * HTTP header base type modifier.
     */
    String MIXED = "mixed";

    /**
     * HTTP header.
     */
    String MULTIPART_FORM_DATA =
        MULTIPART + '/' + FORM_DATA;

    /**
     * HTTP header.
     */
    String MULTIPART_MIXED = MULTIPART + '/' + MIXED;

    /**
     * The request parameter name for overriding 'repository' property
     * (path).
     */
    String REPOSITORY_PARAMETER = "path";

    /**
     * The key in UploadService properties in
     * TurbineResources.properties 'repository' property.
     */
    String REPOSITORY_KEY = "repository";

    /**
     * <p> The default value of 'repository' property (.).  This is
     * the directory where uploaded files will get stored temporarily.
     * Note that "."  is whatever the servlet container chooses to be
     * it's 'current directory'.
     */
    String REPOSITORY_DEFAULT = ".";

    /**
     * w The key in UploadService properties in
     * service configuration 'sizeMax' property.
     */
    String SIZE_MAX_KEY = "sizeMax";

    /**
     * <p> The default value of 'sizMax' property (1 megabyte =
     * 1048576 bytes).  This is the maximum size of POST request that
     * will be parsed by the uploader.  If you need to set specific
     * limits for your users, set this property to the largest limit
     * value, and use an action + no auto upload to enforce limits.
     *
     */
    int SIZE_MAX_DEFAULT = 1048576;

    /**
     * The key in UploadService properties in
     * TurbineResources.properties 'sizeThreshold' property.
     */
    String SIZE_THRESHOLD_KEY = "sizeThreshold";

    /**
     * <p> The default value of 'sizeThreshold' property (10
     * kilobytes = 10240 bytes).  This is the maximum size of a POST
     * request that will have it's components stored temporarily in
     * memory, instead of disk.
     */
    int SIZE_THRESHOLD_DEFAULT = 10240;

    /**
     * The key in UploadService properties in
     * TurbineResources.properties 'headerEncoding' property.
     */
    String HEADER_ENCODING_KEY = "headerEncoding";    

    /**
     * <p> The default value of 'headerEncoding' property (.).  
     * The value has been decided by copying from DiskFileItem class
     */
    String HEADER_ENCODING_DEFAULT = "ISO-8859-1";
    
    /**
     * <p>Parses a <a href="http://rf.cx/rfc1867.html">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.</p>
     *
     * @param req The servlet request to be parsed.
     * @exception ServiceException Problems reading/parsing the
     * request or storing the uploaded file(s).
     */
    List parseRequest(HttpServletRequest req)
        throws ServiceException;

    /**
     * <p>Parses a <a href="http://rf.cx/rfc1867.html">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.</p>
     *
     * @param req The servlet request to be parsed.
     * @param path The location where the files should be stored.
     * @exception ServiceException Problems reading/parsing the
     * request or storing the uploaded file(s).
     */
    List parseRequest(HttpServletRequest req, String path)
        throws ServiceException;

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
    List parseRequest(HttpServletRequest req, int sizeThreshold,
        int sizeMax, String path)
        throws ServiceException;

    /**
     * <p> Retrieves the value of <code>size.max</code> property of the
     * {@link org.apache.fulcrum.upload.UploadService}.
     *
     * @return The maximum upload size.
     */
    long getSizeMax();

    /**
     * <p> Retrieves the value of <code>size.threshold</code> property of
     * {@link org.apache.fulcrum.upload.UploadService}.
     *
     * @return The threshold beyond which files are written directly to disk.
     */
    long getSizeThreshold();

    /**
     * <p> Retrieves the value of the <code>repository</code> property of
     * {@link org.apache.fulcrum.upload.UploadService}.
     *
     * @return The repository.
     */
    String getRepository();
    
    /**
     * <p> Retrieves the value of the <code>headerEncoding</code> property of
     * {@link org.apache.fulcrum.upload.UploadService}.
     *
     * @return Returns the headerEncoding.
     */
    String getHeaderEncoding();
}
