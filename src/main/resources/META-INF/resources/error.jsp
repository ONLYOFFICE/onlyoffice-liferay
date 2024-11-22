<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="com.liferay.document.library.kernel.exception.NoSuchFileEntryException" %>
<%@ page import="com.liferay.document.library.kernel.exception.FileExtensionException" %>

<liferay-theme:include page="portal_normal.jsp" />

<liferay-ui:error-header />

<liferay-ui:error-principal />

<liferay-ui:error exception="<%= NoSuchFileEntryException.class %>" message="the-entry-could-not-be-found" />

<liferay-ui:error exception="<%= FileExtensionException.class %>" message="please-enter-a-file-with-a-valid-extension-x" />
