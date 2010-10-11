/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* ResourceXMLConstants.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 26, 2010 11:44:37 AM
* $Id$
*/
package com.dtolabs.shared.resources;

/**
 * ResourceXMLConstants contains constants for parsing Resource XML format.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ResourceXMLConstants {
    /**
     * Public identifier for Project document 1.0 dtd
     */
    public static final String DTD_PROJECT_DOCUMENT_1_0_EN =
        "-//DTO Labs Inc.//DTD Resources Document 1.0//EN";
    /**
     * resources path to project.dtd file
     */
    public static final String PROJECT_DTD_RESOURCE_PATH = "com/dtolabs/shared/resources/project.dtd";
    public static final String PKG_FILETYPE = "filetype";
    public static final String COMMON_DESCRIPTION = "description";
    public static final String TRANFORM_NAME = "name";
    public static final String TRANFORM_DESCRIPTION = "description";
    public static final String TRANFORM_FILETYPE = "filetype";
    public static final String TRANFORM_OUTPUTDIR = "outputdir";
    public static final String TRANFORM_TEMPLATE = "template";
    public static final String TRANFORM_TEMPLATETYPE = "templatetype";
    public static final String TRANFORM_TEMPLATEDIR = "templatedir";
    public static final String TRANFORM_DIRECTION = "direction";
    public static final String TRANFORM_PROXIMITY = "proximity";
    /**
     * Attributes of "transform" entities.
     */
    public static final String[] TRANSFORM_ATTRIBUTES = new String[]{
        TRANFORM_NAME,
        TRANFORM_DESCRIPTION,
        TRANFORM_FILETYPE,
        TRANFORM_OUTPUTDIR,
        TRANFORM_TEMPLATE,
        TRANFORM_TEMPLATETYPE,
        TRANFORM_TEMPLATEDIR,
        TRANFORM_DIRECTION,
        TRANFORM_PROXIMITY
    };
    public static final String COMMON_REPLACE = "replace";
    public static final String COMMON_TYPE = "type";
    public static final String COMMON_NAME = "name";
    public static final String COMMON_TAGS = "tags";
    /**
     * Common attributes of all entity xml nodes
     */
    public static final String[] commonProps = {
        COMMON_DESCRIPTION,
        COMMON_TAGS
    };
    public static final String NODE_HOSTNAME = "hostname";
    public static final String NODE_OS_ARCH = "osArch";
    public static final String NODE_OS_FAMILY = "osFamily";
    public static final String NODE_OS_NAME = "osName";
    public static final String NODE_OS_VERSION = "osVersion";
    public static final String NODE_USERNAME = "username";
    /**
     * attributes of "node" nodes
     */
    public static final String[] nodeProps = {
        NODE_HOSTNAME,
        NODE_OS_ARCH,
        NODE_OS_FAMILY,
        NODE_OS_NAME,
        NODE_OS_VERSION,
        NODE_USERNAME,
    };
    public static final String SETTING_TYPE = "settingType";
    public static final String SETTING_VALUE = "settingValue";
    /**
     * attributes of "setting" nodes
     */
    public static final String[] settingProps = {
        SETTING_TYPE,
        SETTING_VALUE
    };
    public static final String PKG_ARCH = "arch";
    public static final String PKG_BASE = "base";
    public static final String PKG_BUILDTIME = "buildtime";
    public static final String PKG_FILENAME = "filename";
    public static final String PKG_INSTALLROOT = "installroot";
    public static final String PKG_INSTALLRANK = "installrank";
    public static final String PKG_RELEASE = "release";
    public static final String PKG_RELEASETAG = "releasetag";
    public static final String PKG_REPO_URL = "repoUrl";
    public static final String PKG_RESTART = "restart";
    public static final String PKG_VENDOR = "vendor";
    public static final String PKG_VERSION = "version";
    /**
     * attributes of "package" nodes
     */
    public static final String[] packageProps = {
        PKG_ARCH,
        PKG_BASE,
        PKG_BUILDTIME,
        PKG_FILENAME,
        PKG_FILETYPE,
        PKG_INSTALLROOT,
        PKG_INSTALLRANK,
        PKG_RELEASE,
        PKG_RELEASETAG,
        PKG_REPO_URL,
        PKG_RESTART,
        PKG_VENDOR,
        PKG_VERSION
    };
    public static final String DEPLOYMENT_BASEDIR = "basedir";
    public static final String DEPLOYMENT_INSTALL_ROOT = "installRoot";
    public static final String DEPLOYMENT_STARTUPRANK = "startuprank";
    /**
     * attributes of "deployment" nodes
     */
    final public static String[] deploymentProps = {
        DEPLOYMENT_BASEDIR,
        DEPLOYMENT_INSTALL_ROOT,
        DEPLOYMENT_STARTUPRANK
    };
    public static final String NODE_ENTITY_TAG = "node";
    public static final String SETTING_ENTITY_TAG = "setting";
    public static final String PACKAGE_ENTITY_TAG = "package";
    public static final String DEPLOYMENT_ENTITY_TAG = "deployment";
    public static final String TRANSFORM_TAG = "transform";
    public static final String TRANSFORMS_GROUP_TAG = "transforms";

    public static final String RESOURCES_GROUP_TAG = "resources";
    public static final String REFERRERS_GROUP_TAG = "referrers";
    public static final String RESOURCE_REF_TAG = "resource";
    public static final String RESOURCES_REPLACE_PROP = "resources.replace";
    public static final String REFERRERS_REPLACE_PROP = "referrers.replace";
}
