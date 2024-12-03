<%--
 *
 * (c) Copyright Ascensio System SIA 2024
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
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="com.liferay.document.library.kernel.exception.NoSuchFileEntryException" %>
<%@ page import="com.liferay.document.library.kernel.exception.FileExtensionException" %>

<liferay-theme:include page="portal_normal.jsp" />

<liferay-ui:error-header />

<liferay-ui:error-principal />

<liferay-ui:error exception="<%= NoSuchFileEntryException.class %>" message="the-entry-could-not-be-found" />

<liferay-ui:error exception="<%= FileExtensionException.class %>" message="please-enter-a-file-with-a-valid-extension-x" />
