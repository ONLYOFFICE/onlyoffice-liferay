/**
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
 */

package com.onlyoffice.liferay.docs.utils;

import javax.portlet.RenderRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;

@Component(
    service = OnlyOfficeEditorUtils.class
)
public class OnlyOfficeEditorUtils {

    public String getConfig(Long fileEntryId, RenderRequest req) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            FileEntry fileEntry = _DLAppService.getFileEntry(fileEntryId);
            FileVersion fileVersion = fileEntry.getLatestFileVersion();
            User user = PortalUtil.getUser(req);

            PermissionChecker checker = _permissionFactory.create(user);

            if (!fileEntry.containsPermission(checker, ActionKeys.VIEW)) {
                throw new PrincipalException.MustHavePermission(
                        user.getUserId(), ActionKeys.VIEW);
            }

            Config config = _configService.createConfig(String.valueOf(fileVersion.getFileVersionId()), Mode.EDIT, Type.DESKTOP);

            config.getEditorConfig().setLang(LocaleUtil.fromLanguageId(LanguageUtil.getLanguageId(req)).toLanguageTag());

            return mapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getConfigPreview(Long fileEntryId, String version, RenderRequest req) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            FileEntry fileEntry = _DLAppService.getFileEntry(fileEntryId);
            FileVersion fileVersion = Validator.isNull(version) ? fileEntry.getLatestFileVersion() : fileEntry.getFileVersion(version);
            User user = PortalUtil.getUser(req);

            PermissionChecker checker = _permissionFactory.create(user);

            if (!fileEntry.containsPermission(checker, ActionKeys.VIEW)) {
                throw new PrincipalException.MustHavePermission(
                        user.getUserId(), ActionKeys.VIEW);
            }

            Config config = _configService.createConfig(String.valueOf(fileVersion.getFileVersionId()), Mode.VIEW, Type.EMBEDDED);

            String title = Validator.isNull(version)
                    ? fileVersion.getFileName()
                    : String.format("%s (%s %s)",
                        fileVersion.getFileName(),
                        LanguageUtil.get(req.getLocale(), "version"),
                            fileVersion.getVersion()
                        );

            config.getEditorConfig().setLang(LocaleUtil.fromLanguageId(LanguageUtil.getLanguageId(req)).toLanguageTag());

            config.getDocument().setTitle(title);

            return mapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Reference
    private PermissionCheckerFactory _permissionFactory;

    @Reference
    private DLAppService _DLAppService;

    @Reference
    private ConfigService _configService;

}
