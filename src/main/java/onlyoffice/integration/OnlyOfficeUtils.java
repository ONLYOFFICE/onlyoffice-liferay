/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

import javax.portlet.MutableRenderParameters;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTableConstants;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

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
        return fixUrl(_config.getDocInnerUrl());
    }

    public String getLiferayUrl(HttpServletRequest req) {
        return fixUrl(_config.getLiferayUrlOrDefault(PortalUtil.getPortalURL(req)));
    }

    public String getSaveAsUrl(HttpServletRequest req) {
        return getLiferayUrl(req) + "o/onlyoffice/api?type=save-as";
    }

    public String replaceDocServerURLToInternal(String url) {
        String innerDocEditorUrl = getDocServerInnnerUrl();
        String publicDocEditorUrl = getDocServerUrl();
        if (!publicDocEditorUrl.equals(innerDocEditorUrl)) {
            url = url.replace(publicDocEditorUrl, innerDocEditorUrl);
        }
        return url;
    }

    private List<String> editableExtensions = Arrays.asList("docx", "xlsx", "pptx", "docxf");
    public boolean isEditable(String ext) {
        return editableExtensions.contains(trimDot(ext));
    }

    private List<String> fillFormExtensions = Arrays.asList("oform");
    public boolean isFillForm(String ext) {
        return fillFormExtensions.contains(trimDot(ext));
    }

    private List<String> viewableExtensions = Arrays.asList("odt", "doc", "ods", "xls", "odp", "ppt", "csv", "rtf", "txt", "pdf");
    public boolean isViewable(String ext) {
        return viewableExtensions.contains(trimDot(ext)) || isEditable(ext) || isFillForm(ext);
    }

    private String getGoBackUrl(RenderRequest request, FileEntry fileEntry) {
        Group group = GroupLocalServiceUtil.fetchGroup(fileEntry.getGroupId());

        PortletURL portletURL = PortalUtil.getControlPanelPortletURL(
            request, group, DLPortletKeys.DOCUMENT_LIBRARY_ADMIN,
            0, 0, PortletRequest.RENDER_PHASE);

        long folderId = fileEntry.getFolderId();

        MutableRenderParameters params = portletURL.getRenderParameters();

        if (folderId == DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
            params.setValue("mvcRenderCommandName", "/document_library/view");
        } else {
            params.setValue("mvcRenderCommandName", "/document_library/view_folder");
            params.setValue("folderId", String.valueOf(folderId));
        }

        return portletURL.toString();
    }

    public String getDocumentConfig(Long fileEntryId, String version, Boolean preview, RenderRequest req) {
        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject customizationObject = new JSONObject();
        JSONObject goBackObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();

        try {
            FileEntry fileEntry = _DLAppService.getFileEntry(fileEntryId);

            boolean versionSpecific = false;

            if (!Validator.isNull(version)) {
                versionSpecific = true;
            }

            FileVersion fileVersion =  versionSpecific ? fileEntry.getFileVersion(version) : fileEntry.getLatestFileVersion();

            String ext = fileVersion.getExtension();
            User user = PortalUtil.getUser(req);

            PermissionChecker checker = _permissionFactory.create(PortalUtil.getUser(req));

            boolean editPerm = fileEntry.containsPermission(checker, ActionKeys.UPDATE);
            if (!fileEntry.containsPermission(checker, ActionKeys.VIEW)) {
                throw new Exception("User don't have read rights");
            }

            boolean edit = (isEditable(ext) || isFillForm(ext)) && editPerm && !preview;
            String url = getFileUrl(PortalUtil.getHttpServletRequest(req), fileVersion.getFileVersionId());

            String title = versionSpecific ? String.format("%s (%s %s)",
                                                            fileVersion.getFileName(),
                                                            LanguageUtil.get(req.getLocale(), "version"),
                                                            fileVersion.getVersion()
                                                        )
                                            : fileVersion.getFileName();

            responseJson.put("type", preview ? "embedded" : "desktop");
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            responseJson.put("documentType", getDocType(ext));
            responseJson.put("document", documentObject);
            documentObject.put("title", title);
            documentObject.put("url", url);
            documentObject.put("fileType", ext);
            documentObject.put("key", getDocKey(fileVersion, versionSpecific));
            documentObject.put("permissions", permObject);
            permObject.put("edit", edit);

            responseJson.put("editorConfig", editorConfigObject);
            editorConfigObject.put("lang", LocaleUtil.fromLanguageId(LanguageUtil.getLanguageId(req)).toLanguageTag());
            editorConfigObject.put("mode", edit ? "edit" : "view");
            editorConfigObject.put("customization", customizationObject);
            customizationObject.put("goback", goBackObject);
            customizationObject.put("forcesave", _config.forceSaveEnabled());
            goBackObject.put("url", getGoBackUrl(req, fileEntry));

            if (edit) {
                editorConfigObject.put("callbackUrl", getFileUrl(PortalUtil.getHttpServletRequest(req), fileEntry.getFileEntryId()));
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
        if (".doc.docx.docm.dot.dotx.dotm.odt.fodt.ott.rtf.txt.html.htm.mht.pdf.djvu.fb2.epub.xps.docxf.oform".indexOf(ext) != -1) return "text";
        if (".xls.xlsx.xlsm.xlt.xltx.xltm.ods.fods.ots.csv".indexOf(ext) != -1) return "spreadsheet";
        if (".pps.ppsx.ppsm.ppt.pptx.pptm.pot.potx.potm.odp.fodp.otp".indexOf(ext) != -1) return "presentation";
        return null;
    }

    private String getDocKey(FileVersion fileVersion, boolean versionSpecific) throws PortalException {
        if (versionSpecific && !fileVersion.getVersion().equals("PWC")) {
            return createDocKey(fileVersion, true);
        } else {
            String key = getCollaborativeEditingKey(fileVersion.getFileEntry());

            if (key != null) {
                return key;
            } else {
                return createDocKey(fileVersion, false);
            }
        }
    }

    private String createDocKey(FileVersion fileVersion, boolean versionSpecific) throws PortalException {
        String key = fileVersion.getFileEntry().getUuid() + "_" + fileVersion.getVersion().hashCode();

        if (versionSpecific) {
            key = key + "_version";
        }

        return key;
    }

    public void setCollaborativeEditingKey(FileEntry fileEntry, String key) throws PortalException {
        ExpandoBridge expandoBridge = fileEntry.getExpandoBridge();

        if (!expandoBridge.hasAttribute("onlyoffice-collaborative-editor-key")) {
            expandoBridge.addAttribute("onlyoffice-collaborative-editor-key", ExpandoColumnConstants.STRING, false);
        }

        if (key == null || key.isEmpty()) {
            ExpandoValueLocalServiceUtil.deleteValue(expandoBridge.getCompanyId(), expandoBridge.getClassName(),
                    ExpandoTableConstants.DEFAULT_TABLE_NAME, "onlyoffice-collaborative-editor-key", expandoBridge.getClassPK());
        } else {
            ExpandoValueLocalServiceUtil.addValue(expandoBridge.getCompanyId(), expandoBridge.getClassName(),
                    ExpandoTableConstants.DEFAULT_TABLE_NAME, "onlyoffice-collaborative-editor-key", expandoBridge.getClassPK(), key);
        }
    }

    public String getCollaborativeEditingKey(FileEntry fileEntry) throws PortalException {
        ExpandoBridge expandoBridge = fileEntry.getExpandoBridge();

        if (expandoBridge.hasAttribute("onlyoffice-collaborative-editor-key")) {
            ExpandoValue value = ExpandoValueLocalServiceUtil.getValue(expandoBridge.getCompanyId(), expandoBridge.getClassName(),
                    ExpandoTableConstants.DEFAULT_TABLE_NAME, "onlyoffice-collaborative-editor-key", expandoBridge.getClassPK());

            if (value != null && !value.getString().isEmpty()) {
                return value.getString();
            }
        }

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

    @Reference
    private DLAppService _DLAppService;

    private static final Log _log = LogFactoryUtil.getLog(
            OnlyOfficeUtils.class);
}
