<%--
 *
 * (c) Copyright Ascensio System SIA 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 --%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>

<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.document.library.kernel.service.DLAppLocalServiceUtil" %>
<%@ page import="com.liferay.portal.kernel.repository.model.FileVersion" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.util.ResourceBundleUtil" %>

<%@ page import="java.util.ResourceBundle" %>

<%@ page import="org.osgi.framework.BundleContext" %>
<%@ page import="org.osgi.framework.FrameworkUtil" %>

<%@ page import="onlyoffice.integration.OnlyOfficeUtils" %>


<portlet:defineObjects />

<%
    BundleContext bc = FrameworkUtil.getBundle(OnlyOfficeUtils.class).getBundleContext();
    ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(locale, getClass());

    Long fileVersionId = ParamUtil.getLong(renderRequest, "fileId");
    FileVersion file = DLAppLocalServiceUtil.getFileVersion(fileVersionId);
    OnlyOfficeUtils utils = bc.getService(bc.getServiceReference(OnlyOfficeUtils.class));
%>

<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>
    <title><%= file.getFileName() %> - <%= LanguageUtil.get(resourceBundle, "onlyoffice-edit-title") %></title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/main.css" />
    <script id="scriptApi" type="text/javascript" src="<%= utils.getDocServerUrl() %>OfficeWeb/apps/api/documents/api.js"></script>
</head>

<body>
    <div>
        <div id="placeholder"></div>
    </div>
    <script>
    var config = JSON.parse('<%= utils.getDocumentConfig(file, renderRequest) %>');
    new DocsAPI.DocEditor("placeholder", config);
    </script>
</body>
</html>