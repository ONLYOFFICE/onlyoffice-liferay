<%--
 *
 * (c) Copyright Ascensio System SIA 2023
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
<%@ page import="com.liferay.portal.kernel.repository.model.FileEntry" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.util.ResourceBundleUtil" %>
<%@ page import="com.liferay.portal.kernel.servlet.HttpHeaders" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="com.liferay.portal.kernel.util.HtmlUtil" %>

<%@ page import="java.util.ResourceBundle" %>

<%@ page import="org.osgi.framework.BundleContext" %>
<%@ page import="org.osgi.framework.FrameworkUtil" %>

<%@ page import="com.onlyoffice.liferay.docs.utils.OnlyOfficeEditorUtils" %>
<%@ page import="com.onlyoffice.liferay.docs.permission.OnlyOfficePermissionUtils" %>
<%@ page import="com.onlyoffice.manager.url.UrlManager" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />

<%
    BundleContext bc = FrameworkUtil.getBundle(OnlyOfficeEditorUtils.class).getBundleContext();
    ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(locale, getClass());

    Long fileEntryId = ParamUtil.getLong(renderRequest, "fileId");
    FileEntry fileEntry = DLAppLocalServiceUtil.getFileEntry(fileEntryId);
    OnlyOfficeEditorUtils editorUtils = bc.getService(bc.getServiceReference(OnlyOfficeEditorUtils.class));
    UrlManager urlManger = bc.getService(bc.getServiceReference(UrlManager.class));
%>

<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>
    <title><%= fileEntry.getFileName() %> - <%= LanguageUtil.get(resourceBundle, "onlyoffice-edit-title") %></title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/main.css" />
    <script id="scriptApi" type="text/javascript" src="<%= urlManger.getDocumentServerApiUrl() %>"></script>

    <% if (request.getHeader(HttpHeaders.USER_AGENT).contains("AscDesktopEditor")) { %>
        <script type="text/javascript">
            var Liferay = Liferay || {};
            Liferay.ThemeDisplay = Liferay.ThemeDisplay || {
                getUserId: function () {
                    return "<%= themeDisplay.getUserId() %>";
                },
                getUserName: function () {
                    return "<%= themeDisplay.getUser().getFullName() %>";
                },
                getUserEmailAddress: function () {
                    return "<%= themeDisplay.getUser().getEmailAddress() %>";
                },
                getPortalURL: function () {
                    return "<%= themeDisplay.getPortalURL() %>";
                },
                isSignedIn: function () {
                    return <%= themeDisplay.isSignedIn() %>;
                }
            };
        </script>

        <script src="<%= HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, application.getContextPath() + "/js/desktop.js")) %>" type="text/javascript"></script>
    <% } %>
</head>

<body>
    <div>
        <div id="placeholder"></div>
    </div>
    <script>
    var config = JSON.parse('<%= editorUtils.getConfig(fileEntryId, renderRequest) %>');

        var onRequestSaveAs = function (event) {
            var url = event.data.url;
            var fileType = event.data.fileType ? event.data.fileType : event.data.title.split(".").pop();

            var request = new XMLHttpRequest();
            request.open("POST", '<%= PortalUtil.getPortalURL(request) + "/o/onlyoffice/api?type=save-as" %>', true);
            request.send(JSON.stringify({
                url: url,
                fileType: fileType,
                fileEntryId: "<%= fileEntryId %>"
            }));

            request.onreadystatechange = function() {
                if (request.readyState != 4) return;
                if (request.status == 200) {
                    var response = JSON.parse(request.response);
                    docEditor.showMessage("<%= LanguageUtil.get(request, "onlyoffice-save-as-success")%>" + " " + response.fileName);
                } else if (request.status == 403) {
                    docEditor.showMessage("<%= LanguageUtil.get(request, "onlyoffice-save-as-error-forbidden")%>");
                } else {
                    docEditor.showMessage("<%= LanguageUtil.get(request, "onlyoffice-save-as-error-unknown")%>");
                }
            }
        }

        config.events = {};

        <% if (OnlyOfficePermissionUtils.saveAs(fileEntry, themeDisplay.getUser())) { %>
            config.events.onRequestSaveAs = onRequestSaveAs;
        <% } %>

        var connectEditor = function () {
            if (typeof DocsAPI === "undefined") {
                alert("<%= LanguageUtil.get(request, "onlyoffice-editor-docs-api-undefined")%>");
                return;
            }

            if ((config.document.fileType === "docxf" || config.document.fileType === "oform")
                && DocsAPI.DocEditor.version().split(".")[0] < 7) {
                alert("<%= LanguageUtil.get(request, "onlyoffice-editor-froms-error-version")%>");
                window.location.href = config.editorConfig.customization.goback.url;
                return;
            }

            return new DocsAPI.DocEditor("placeholder", config);
        }

        var docEditor = connectEditor();
    </script>
</body>
</html>