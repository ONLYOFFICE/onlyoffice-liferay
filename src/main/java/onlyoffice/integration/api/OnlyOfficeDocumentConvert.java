package onlyoffice.integration.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import onlyoffice.integration.OnlyOfficeConvertUtils;

@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.security-role-ref=power-user,user",
		"javax.portlet.version=3.0"
	},
	service = Portlet.class
)
public class OnlyOfficeDocumentConvert extends MVCPortlet {
	
	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
		Long versionId = ParamUtil.getLong(request , "fileId");
		String key = ParamUtil.getString(request , "key");
		String fn = ParamUtil.getString(request , "fileName");

		response.setContentType("application/json");
		PrintWriter writer = response.getWriter();
		
		try {		
			FileVersion file = DLAppLocalServiceUtil.getFileVersion(versionId);
		
			JSONObject json = _convert.convert(request, file, key);
			
			if (json.getBoolean("endConvert")) {
				savefile(request, file, json.getString("fileUrl"), fn);
			}
			
			writer.write(json.toString());
		} catch (Exception ex) {
			_log.error(ex.getMessage());
			writer.write("{\"error\":\"" + ex.getMessage() + "\"}");
		}
	}
	
	
	private void savefile(ResourceRequest request, FileVersion file, String url, String filename) throws Exception {
		User user = PortalUtil.getUser(request);
		
    	_log.info("Trying to download file from URL: " + url);
    	
    	URLConnection con = new URL(url).openConnection();
        InputStream in = con.getInputStream();
        ServiceContext serviceContext = ServiceContextFactory.getInstance(OnlyOfficeDocumentConvert.class.getName(), request);
        
        _dlApp.addFileEntry(user.getUserId(), file.getRepositoryId(), file.getFileEntry().getFolderId(), filename,
        		_convert.getMimeType(file.getExtension()), filename, file.getDescription(), "ONLYOFFICE Convert",
        		in, con.getContentLength(), serviceContext);
        
        _log.info("Document saved.");
	}

	@Reference
	private DLAppLocalService _dlApp;
	
	@Reference
	OnlyOfficeConvertUtils _convert;
	
	private static final Log _log = LogFactoryUtil.getLog(
			OnlyOfficeDocumentConvert.class);
}