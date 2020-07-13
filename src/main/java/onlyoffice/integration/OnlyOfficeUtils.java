package onlyoffice.integration;

import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import onlyoffice.integration.config.OnlyOfficeConfigManager;

@Component(
    service = OnlyOfficeUtils.class
)
public class OnlyOfficeUtils {
    public String getDocServerUrl() {
        return fixUrl(_config.getDocUrl());
    }

    public String getDocServerInnnerUrl() {
        return fixUrl(_config.getDocInnerUrlOrDefault(_config.getDocUrl()));
    }

    public String getLiferayUrl(PortletRequest req) {
        return fixUrl(_config.getLiferayUrlOrDefault(PortalUtil.getPortalURL(req)));
    }

    public boolean isEditable(String ext) {
        if (".docx.xlsx.pptx".indexOf(ext) != -1) return true;
        return false;
    }

    public boolean isViewable(String ext) {
        if (".odt.doc.ods.xls.odp.ppt.txt.pdf".indexOf(ext) != -1) return true;
        return isEditable(ext);
    }

    public String getDocumentConfig(FileVersion file, RenderRequest req) {
        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();

        try {
            String ext = file.getExtension();
            User user = PortalUtil.getUser(req);
            Long fileVersionId = file.getFileVersionId();
            boolean edit = isEditable(ext);
            String url = getFileUrl(req, fileVersionId);

            responseJson.put("type", "desktop");
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            responseJson.put("documentType", getDocType(ext));
            responseJson.put("document", documentObject);
            documentObject.put("title", file.getFileName());
            documentObject.put("url", url);
            documentObject.put("fileType", ext);
            documentObject.put("key", Long.toString(fileVersionId));
            documentObject.put("permissions", permObject);
            permObject.put("edit", edit);

            responseJson.put("editorConfig", editorConfigObject);
            editorConfigObject.put("lang", LocaleUtil.fromLanguageId(LanguageUtil.getLanguageId(req)).toLanguageTag());
            editorConfigObject.put("mode", edit ? "edit" : "view");
            if (edit) {
                editorConfigObject.put("callbackUrl", url);
            }
            editorConfigObject.put("user", userObject);
            userObject.put("id", Long.toString(user.getUserId()));

            userObject.put("firstname", user.getFirstName());
            userObject.put("lastname", user.getLastName());
            userObject.put("name", user.getFullName());

            if (_jwt.isEnabled()) {
                responseJson.put("token", _jwt.createToken(responseJson));
            }
        } catch (Exception e) {
            _log.error(e.getMessage(), e);
        }

        return responseJson.toString().replace("'", "\\'");
    }

    public String getFileUrl(PortletRequest req, Long id) {
        return getLiferayUrl(req) + "o/onlyoffice/doc?key=" + _hasher.getHash(id);
    }

    private String fixUrl(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private String getDocType(String ext) {
        if (".doc.docx.docm.dot.dotx.dotm.odt.fodt.ott.rtf.txt.html.htm.mht.pdf.djvu.fb2.epub.xps".indexOf(ext) != -1) return "text";
        if (".xls.xlsx.xlsm.xlt.xltx.xltm.ods.fods.ots.csv".indexOf(ext) != -1) return "spreadsheet";
        if (".pps.ppsx.ppsm.ppt.pptx.pptm.pot.potx.potm.odp.fodp.otp".indexOf(ext) != -1) return "presentation";
        return null;
    }

    @Reference
    private OnlyOfficeJWT _jwt;

    @Reference
    private OnlyOfficeHasher _hasher;

    @Reference
    private OnlyOfficeConfigManager _config;

    private static final Log _log = LogFactoryUtil.getLog(
            OnlyOfficeUtils.class);
}
