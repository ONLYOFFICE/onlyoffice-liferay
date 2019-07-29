<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.document.library.kernel.service.DLAppLocalServiceUtil" %>
<%@ page import="com.liferay.portal.kernel.repository.model.FileVersion" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>

<%@ page import="org.osgi.framework.BundleContext" %>
<%@ page import="org.osgi.framework.FrameworkUtil" %>

<%@ page import="onlyoffice.integration.OnlyOfficeUtils" %>


<portlet:defineObjects />

<%
	BundleContext bc = FrameworkUtil.getBundle(OnlyOfficeUtils.class).getBundleContext();

	Long fileVersionId = ParamUtil.getLong(renderRequest, "fileId");
	FileVersion file = DLAppLocalServiceUtil.getFileVersion(fileVersionId);
	OnlyOfficeUtils utils = bc.getService(bc.getServiceReference(OnlyOfficeUtils.class));
%>

<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>
    <title><%= file.getFileName() %> - <%= LanguageUtil.get(request, "onlyoffice-edit-title") %></title>
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