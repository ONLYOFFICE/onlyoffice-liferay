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
		var text = jQuery("#ooConvertText");
		var thumb = jQuery("#ooProgressThumb");
		var timeOut = null;

		function onError(error) {
			text.text('<%= errorText %> ' + error);
		}
		
		function _callAjax() {
		    var url = '<%= apiurl %>';
		    jQuery.ajax({
			    type : "POST",
			    url : url,
			    cache: false,
			    success: function(data) {	
			    	if (data.error) {
			    		onError(data.error);
			    		return;
			    	}
			    	
			    	if (data.percent != null) {
			    		var perc = data.percent / 100;
			    		if (perc > 0) {
			    			thumb.css({flex: data.percent / 100});
			    		}
			    		thumb.text(data.percent + "%");
			    	}
			    	
			    	if (!data.endConvert) {
			    		setTimeout(_callAjax, 1000);
			    	} else {
			    		text.text('<%= doneText %>');
			    		window.top.location.reload();
			    	}
			    },
			    error: onError
		  	});
		}
	
		_callAjax();
	})();
	</script>
</div>