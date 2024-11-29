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

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.liferay.document.library.kernel.exception.FileNameException" %>
<%@ page import="com.liferay.portal.kernel.security.auth.PrincipalException" %>
<%@ page import="com.liferay.portal.kernel.portlet.LiferayWindowState"%>
<%@ page import="com.liferay.portal.kernel.util.ResourceBundleUtil" %>
<%@ page import="com.onlyoffice.liferay.docs.constants.PortletKeys" %>

<%@ page import="java.util.ResourceBundle" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />

<%
String redirect = ParamUtil.getString(request, "redirect");
long folderId = ParamUtil.getLong(request, "folderId");
long fileEntryId = ParamUtil.getLong(request, "fileEntryId");

ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(locale, getClass());
String headerTitle = LanguageUtil.get(resourceBundle, "onlyoffice-context-create-name");

portletDisplay.setShowBackIcon(true);
portletDisplay.setURLBack(redirect);
renderResponse.setTitle(headerTitle);
%>

<liferay-portlet:actionURL name="/document_library/create_onlyoffice" var="createFileEntryURL">
	<liferay-portlet:param name="mvcRenderCommandName" value="/document_library/create_onlyoffice" />
</liferay-portlet:actionURL>

<div class="container-fluid-1280">
	<aui:form action="<%= createFileEntryURL %>" cssClass="lfr-dynamic-form" method="post" id="fmCreate" name="fmCreate">
		<aui:input name="folderId" type="hidden" value="<%= folderId %>" />
		<aui:input name="redirectUrl" type="hidden" value="<%= redirect %>" />
		<div class="lfr-form-content">
			<liferay-ui:error exception="<%= Exception.class %>">
				<liferay-ui:message key="an-unexpected-error-occurred" />
			</liferay-ui:error>
			<liferay-ui:error exception="<%= FileNameException.class %>">
				<liferay-ui:message key="please-enter-a-file-with-a-valid-file-name" />
			</liferay-ui:error>
			<liferay-ui:error exception="<%= PrincipalException.MustHavePermission.class %>">
				<liferay-ui:message key="you-do-not-have-the-required-permissions" />
			</liferay-ui:error>
			<div class="hide" id="<portlet:namespace />error-messages"></div>
			<aui:fieldset-group markupView="lexicon">
				<aui:fieldset>
					<aui:select label="onlyoffice-context-create-format" id="type" name="type">
						<aui:option label="onlyoffice-context-create-type-docx" value="docx" />
						<aui:option label="onlyoffice-context-create-type-xlsx" value="xlsx" />
						<aui:option label="onlyoffice-context-create-type-pptx" value="pptx" />
						<aui:option label="onlyoffice-context-create-type-pdf" value="pdf" />
					</aui:select>
					<aui:input name="title" type="text"  required="true" showRequiredLabel="true" />
					<aui:input name="description" type="textarea" />
				</aui:fieldset>
			</aui:fieldset-group>
		</div>

		<aui:button-row>
			<aui:button name="saveButton" type="submit" />
			<aui:button href="<%= redirect %>" type="cancel" />
		</aui:button-row>
	</aui:form>
</div>

<c:if test="<%= (fileEntryId != 0) %>">	
	<liferay-portlet:renderURL portletName="<%= PortletKeys.EDITOR %>" var="editURL" windowState="<%= LiferayWindowState.EXCLUSIVE.toString() %>">
		<portlet:param name="fileEntryId" value="<%= String.valueOf(fileEntryId) %>" />
	</liferay-portlet:renderURL>
	<aui:script>
		document.location.href = "<%= editURL %>";
	</aui:script>
</c:if>
