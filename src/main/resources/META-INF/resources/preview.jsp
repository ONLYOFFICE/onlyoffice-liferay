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
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>
 
 
<%@ page import="com.liferay.document.library.kernel.service.DLAppLocalServiceUtil" %>
<%@ page import="com.liferay.portal.kernel.repository.model.FileEntry" %>

<%@ page import="org.osgi.framework.BundleContext" %>
<%@ page import="org.osgi.framework.FrameworkUtil" %>

<%@ page import="onlyoffice.integration.OnlyOfficeUtils" %>
 
<liferay-theme:defineObjects />

<portlet:defineObjects />
 
 
<%
    BundleContext bc = FrameworkUtil.getBundle(OnlyOfficeUtils.class).getBundleContext();

    Long fileEntryId = (Long)request.getAttribute("fileId");
    FileEntry fileEntry = DLAppLocalServiceUtil.getFileEntry(fileEntryId);
    OnlyOfficeUtils utils = bc.getService(bc.getServiceReference(OnlyOfficeUtils.class));
%>

<liferay-util:html-top>
    <script id="scriptApi" type="text/javascript" src="<%= utils.getDocServerUrl() %>OfficeWeb/apps/api/documents/api.js"></script>
</liferay-util:html-top>


<div class="preview-file" style="height: 75vh">
    <div id="placeholder"></div>

    <script>
        var config = JSON.parse('<%= utils.getDocumentConfig(fileEntryId, true, renderRequest) %>');
        new DocsAPI.DocEditor("placeholder", config);
    </script>
</div>