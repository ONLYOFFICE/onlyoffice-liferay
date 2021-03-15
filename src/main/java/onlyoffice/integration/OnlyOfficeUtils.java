/**
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
 */

package onlyoffice.integration;

import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import java.util.Arrays;
import java.util.List;

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

    public String getLiferayUrl(HttpServletRequest req) {
        return fixUrl(_config.getLiferayUrlOrDefault(PortalUtil.getPortalURL(req)));
    }

    private List<String> editableExtensions = Arrays.asList("docx", "xlsx", "pptx");
    public boolean isEditable(String ext) {
        return editableExtensions.contains(trimDot(ext));
    }

    private List<String> viewableExtensions = Arrays.asList("odt", "doc", "ods", "xls", "odp", "ppt", "csv", "rtf", "txt", "pdf");
    public boolean isViewable(String ext) {
        return viewableExtensions.contains(trimDot(ext)) || isEditable(ext);
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

            PermissionChecker checker = _permissionFactory.create(PortalUtil.getUser(req));
            FileEntry fe = file.getFileEntry();
            boolean editPerm = fe.containsPermission(checker, ActionKeys.UPDATE);
            if (!fe.containsPermission(checker, ActionKeys.VIEW)) {
                throw new Exception("User don't have read rights");
            }

            boolean edit = isEditable(ext) && editPerm;
            String url = getFileUrl(PortalUtil.getHttpServletRequest(req), fileVersionId);

            responseJson.put("type", "desktop");
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            responseJson.put("documentType", getDocType(ext));
            responseJson.put("document", documentObject);
            documentObject.put("title", file.getFileName());
            documentObject.put("url", url);
            documentObject.put("fileType", ext);
            documentObject.put("key", file.getUuid() + "_" + file.getVersion().hashCode());
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

    public String getFileUrl(HttpServletRequest request, Long id) {
        return getLiferayUrl(request) + "o/onlyoffice/doc?key=" + _hasher.getHash(id);
    }

    private String trimDot(String ext) {
        if (ext.startsWith(".")) {
            return ext.substring(1);
        }
        return ext;
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

    @Reference
    private DLFileEntryLocalService _dlFile;

    @Reference
    private PermissionCheckerFactory _permissionFactory;

    private static final Log _log = LogFactoryUtil.getLog(
            OnlyOfficeUtils.class);
}
