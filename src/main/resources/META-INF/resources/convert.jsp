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

<%@page import="java.util.Date"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.document.library.kernel.service.DLAppLocalServiceUtil" %>
<%@ page import="com.liferay.portal.kernel.repository.model.FileVersion" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>

<%@ page import="javax.portlet.MutableRenderParameters" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.PortletRequest" %>

<%@ page import="org.json.JSONObject" %>

<%@ page import="org.osgi.framework.BundleContext" %>
<%@ page import="org.osgi.framework.FrameworkUtil" %>

<%@ page import="onlyoffice.integration.OnlyOfficeUtils" %>
<%@ page import="onlyoffice.integration.OnlyOfficeConvertUtils" %>

<portlet:defineObjects />

<%
	BundleContext bc = FrameworkUtil.getBundle(OnlyOfficeUtils.class).getBundleContext();

	Long fileVersionId = ParamUtil.getLong(renderRequest, "fileId");
	FileVersion file = DLAppLocalServiceUtil.getFileVersion(fileVersionId);
	OnlyOfficeUtils utils = bc.getService(bc.getServiceReference(OnlyOfficeUtils.class));
	OnlyOfficeConvertUtils convertUtils = bc.getService(bc.getServiceReference(OnlyOfficeConvertUtils.class));

	String originalFileName = file.getFileName();
	String newFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + "." + convertUtils.convertsTo(file.getExtension()); 
	
	String convertText = String.format(LanguageUtil.get(request, "onlyoffice-convert-process"),
			"<b>" + originalFileName + "</b>", "<b>" + newFileName + "</b>");
	String doneText = LanguageUtil.get(request, "onlyoffice-convert-done");
	String errorText = LanguageUtil.get(request, "onlyoffice-convert-error");

	String apiurl = convertUtils.getConvertUrl(request) + "?key=" + Long.toString(new Date().getTime())
		+ "&fileId=" + Long.toString(fileVersionId)
		+ "&fileName=" + newFileName;
	
%>

<div style="padding: 20px 10px;">
	<span id="ooConvertText"><%= convertText %></span>
	<div class="progress">
		<span id="ooProgressThumb" style="padding: 5px;" class="progress-bar"></span>
	</div>
	
	<script type="text/javascript">
	(function() {
		var text = document.getElementById("ooConvertText");
		var thumb = document.getElementById("ooProgressThumb");
		var timeOut = null;

		function onError(error) {
			text.innerHTML = "<%= errorText %>" + error;
		}
		
		function _callAjax() {
			var url = "<%= apiurl %>";

			var xhr = new XMLHttpRequest();
			xhr.open("POST", url, false);
			xhr.send();

			if (xhr.status != 200) {
				onError( xhr.status + " " + xhr.statusText );
			} else {
				var data = JSON.parse(xhr.responseText);

				if (data.error) {
					onError(data.error);
					return;
				}

				if (data.percent != null) {
					var perc = data.percent / 100;
					if (perc > 0) {
						thumb.style.flex = data.percent / 100;
					}
					thumb.innerHTML = data.percent + "%";
				}

				if (!data.endConvert) {
					setTimeout(_callAjax, 1000);
				} else {
					text.innerHTML = "<%= doneText %>";
					window.top.location.reload();
				}
			}
		}

		_callAjax();
	})();
	</script>
</div>