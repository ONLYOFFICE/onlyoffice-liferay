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

<%@ page import="java.util.ResourceBundle" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />

<%
String redirect = ParamUtil.getString(request, "redirect");

boolean portletTitleBasedNavigation = GetterUtil.getBoolean(portletConfig.getInitParameter("portlet-title-based-navigation"));

ResourceBundle resourceBundle = ResourceBundle.getBundle("content/Language", themeDisplay.getLocale());
String headerTitle = LanguageUtil.get(resourceBundle, "onlyoffice-context-action-create");

if (portletTitleBasedNavigation) {
	portletDisplay.setShowBackIcon(true);
	portletDisplay.setURLBack(redirect);

	renderResponse.setTitle(headerTitle);
}

long folderId = ParamUtil.getLong(request, "folderId");

String labelFormat = LanguageUtil.get(resourceBundle, "onlyoffice-context-section-format");

String docx = LanguageUtil.get(resourceBundle, "onlyoffice-context-type-docx");
String xlsx = LanguageUtil.get(resourceBundle, "onlyoffice-context-type-xlsx");
String pptx = LanguageUtil.get(resourceBundle, "onlyoffice-context-type-pptx");
%>

<liferay-portlet:actionURL name="/document_library/create_onlyoffice" var="createFileEntryURL">
	<liferay-portlet:param name="mvcRenderCommandName" value="/document_library/create_onlyoffice" />
</liferay-portlet:actionURL>

<div class="container-fluid-1280">
	<aui:form action="<%= createFileEntryURL %>" cssClass="lfr-dynamic-form" method="post" id="fmCreate" name="fmCreate" onSubmit='<%= "event.preventDefault(); " + renderResponse.getNamespace() + "createFile();" %>' >
		<aui:input name="folderId" type="hidden" value="<%= folderId %>" />
		<div class="lfr-form-content">
			<div class="hide" id="<portlet:namespace />error-messages"></div>
			<aui:fieldset-group markupView="lexicon">
				<aui:fieldset>
					<aui:select label="<%= labelFormat %>" id="type" name="type">
						<aui:option value="docx"><%= docx %></aui:option>
						<aui:option value="xlsx"><%= xlsx %></aui:option>
						<aui:option value="pptx"><%= pptx %></aui:option>
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

<script>
	function <portlet:namespace />createFile() {

		var url = AUI().one("#<portlet:namespace/>fmCreate").get("action");

		var folderId = "<%= folderId %>";
		var type = AUI().one("#<portlet:namespace/>type").get("value");
		var title = AUI().one("#<portlet:namespace/>title").get("value");
		var description = AUI().one("#<portlet:namespace/>description").get("value");

		$.ajax({
			url: url,
			dataType: "json",
			type: "post",
			data: Liferay.Util.ns('<portlet:namespace />', {
				folderId: folderId,
				type: type,
				title: title,
				description: description
			}),
			error: function () {
				<portlet:namespace />showErrorMessage("<%= LanguageUtil.get(resourceBundle, "onlyoffice-context-create-exceptionUndefined") %>");
			},
			complete: function (data) {
				var responseText = data.responseText;
				var response = $.parseJSON(responseText);
				var exception = response.exception;
				if (!exception) {
					window.open(response.editUrl);
					document.location.href = "<%= redirect %>";
				} else {
					if (exception.indexOf("FileNameException") != -1) {
						<portlet:namespace />showErrorMessage("<%= LanguageUtil.get(resourceBundle, "onlyoffice-context-create-exceptionFileName") %>");
					} else if (exception.indexOf("MustHavePermission") != -1) {
						<portlet:namespace />showErrorMessage("<%= LanguageUtil.get(resourceBundle, "onlyoffice-context-create-exceptionPermission") %>");
					} else {
						<portlet:namespace />showErrorMessage("<%= LanguageUtil.get(resourceBundle, "onlyoffice-context-create-exceptionUndefined") %>");
					}
				}
			}
		});
	}

	function <portlet:namespace />showErrorMessage (message) {
		var messageContainer = AUI().one("#<portlet:namespace />error-messages");
		messageContainer.addClass("alert alert-dismissible alert-danger");
		messageContainer.setAttribute("role", "alert");
		messageContainer.html("<strong class='lead'>Error:</strong>" + message);

		messageContainer.show();
	}
</script>
