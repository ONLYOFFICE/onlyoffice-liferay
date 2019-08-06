package onlyoffice.integration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileVersion;

@Component(
    service = OnlyOfficeConvertUtils.class
)
public class OnlyOfficeConvertUtils {
	
	public boolean isConvertable(String ext) {
		return convertableDict.containsKey(trimDot(ext));
	}
	
	public String convertsTo(String ext) {
		return convertableDict.getOrDefault(trimDot(ext), null);
	}
	
	public String getMimeType(String ext) {
		return mimeTypes.getOrDefault(trimDot(ext), null);
	}

	@SuppressWarnings("serial")
	public static final Map<String, String> convertableDict = new HashMap<String, String>() {{
		put("odt", "docx");
        put("doc", "docx");
        put("odp", "pptx");
        put("ppt", "pptx");
        put("ods", "xlsx");
        put("xls", "xlsx");
	}};

	@SuppressWarnings("serial")
	public static final Map<String, String> mimeTypes = new HashMap<String, String>() {{
		put("odt", "application/vnd.oasis.opendocument.text");
        put("doc", "application/msword");
        put("odp", "application/vnd.oasis.opendocument.presentation");
        put("ppt", "application/vnd.ms-powerpoint");
        put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        put("xls", "application/vnd.ms-excel");
        put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}};
	
	public JSONObject convert(PortletRequest req, FileVersion file, String key) throws SecurityException, Exception {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
        	Long fileVersionId = file.getFileVersionId();
        	String ext = file.getExtension();
        	
            JSONObject body = new JSONObject();
            body.put("async", true);
            body.put("embeddedfonts", true);
            body.put("filetype", ext);
            body.put("outputtype", convertsTo(ext));
            body.put("key", Long.toString(fileVersionId) + key);
            body.put("url", _utils.getFileUrl(req, fileVersionId));

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            HttpPost request = new HttpPost(_utils.getDocServerUrl() + "ConvertService.ashx");
            request.setEntity(requestEntity);
            request.setHeader("Accept", "application/json");

            if (_jwt.isEnabled()) {
                String token = _jwt.createToken(body);
                JSONObject payloadBody = new JSONObject();
                payloadBody.put("payload", body);
                String headerToken = _jwt.createToken(body);
                body.put("token", token);
                request.setHeader("Authorization", "Bearer " + headerToken);
            }

            _log.debug("Sending POST to Docserver: " + body.toString());
            try(CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();

                if(status != HttpStatus.SC_OK) {
                    throw new HttpException("Docserver returned code " + status);
                } else {
            		InputStream is = response.getEntity().getContent();
            		String content = "";

        	        byte[] buffer = new byte[10240];
        	        for (int length = 0; (length = is.read(buffer)) > 0;) {
        	        	content += new String(buffer, 0, length);
        	        }                    
                    
                    _log.debug("Docserver returned: " + content);
                    JSONObject callBackJson = null;
                    try{
                        callBackJson = new JSONObject(content);
                    } catch (Exception e) {
                        throw new Exception("Couldn't convert JSON from docserver: " + e.getMessage());
                    }

                    return callBackJson;
                }
            }
        }
    }
	
	private String trimDot(String input) {
		return input.startsWith(".") ? input.substring(1) : input; 
	}

	@Reference
	private OnlyOfficeJWT _jwt;

	@Reference
	private OnlyOfficeUtils _utils;
	
	private static final Log _log = LogFactoryUtil.getLog(
			OnlyOfficeConvertUtils.class);
}
