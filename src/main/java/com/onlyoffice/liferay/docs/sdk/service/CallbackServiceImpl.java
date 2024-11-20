/**
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
 */

package com.onlyoffice.liferay.docs.sdk.service;

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Action;
import com.onlyoffice.model.documenteditor.callback.action.Type;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.onlyoffice.liferay.docs.utils.OnlyOfficeUtils;


@Component(
    service = CallbackService.class
)
public class CallbackServiceImpl extends DefaultCallbackService {

    public CallbackServiceImpl() {
        super(null, null);
    }

    public void handlerEditing(final Callback callback, final String fileId) throws Exception {
        Long userId = (long) -1;

        if (callback.getUsers() != null && !callback.getUsers().isEmpty()) {
            List<String> users = callback.getUsers();
            userId = Long.parseLong(users.get(0));
        }

        if (callback.getActions() != null) {
            List<Action> actions = callback.getActions();
            if (actions.size() > 0) {
                Action action = actions.get(0);
                if (Type.CONNECTED.equals(action.getType())) {
                    FileEntry fileEntry = _dlApp.getFileEntry(Long.parseLong(fileId));

                    if (!fileEntry.isCheckedOut()) {
                        setUserThreadLocal(userId);
                        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

                        if (_OOUtils.getCollaborativeEditingKey(fileEntry) == null) {
                            String key = callback.getKey();
                            _OOUtils.setCollaborativeEditingKey(fileEntry, key);
                        }

                        _dlAppService.checkOutFileEntry(fileEntry.getFileEntryId(), serviceContext);

                        _log.info("Document opened for editing, locking document");
                    } else {
                        _log.info("Document already locked, another user has entered");
                    }
                }
            }
        }
    }

    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        _log.info("Document updated, changing content");

        Long userId = (long) -1;

        if (callback.getUsers() != null && !callback.getUsers().isEmpty()) {
            List<String> users = callback.getUsers();
            userId = Long.parseLong(users.get(0));
        }

        FileEntry fileEntry = _dlApp.getFileEntry(Long.parseLong(fileId));
        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
        String url = urlManager.replaceToInnerDocumentServerUrl(callback.getUrl());

        checkLockFileEntry(fileEntry, userId, serviceContext);

        updateFile(fileEntry, userId, url, DLVersionNumberIncrease.MAJOR, serviceContext);

        fileEntry = DLAppLocalServiceUtil.getFileEntry(fileEntry.getFileEntryId());

        _OOUtils.setCollaborativeEditingKey(fileEntry, null);

        _log.info("Document saved.");
    }

    public void handlerClosed(final Callback callback, final String fileId) throws Exception {
        _log.info("No document updates, unlocking document");

        FileEntry fileEntry = _dlApp.getFileEntry(Long.parseLong(fileId));

        if (fileEntry.isCheckedOut()) {
            setUserThreadLocal(fileEntry.getLock().getUserId());
            _dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
        }
        _OOUtils.setCollaborativeEditingKey(fileEntry, null);
    }

    public void handlerForcesave(final Callback callback, final String fileId) throws Exception {
        if (getSettingsManager().getSettingBoolean("customization.forcesave", false)) {
            Long userId = (long) -1;

            if (callback.getUsers() != null && !callback.getUsers().isEmpty()) {
                List<String> users = callback.getUsers();
                userId = Long.parseLong(users.get(0));
            }

            FileEntry fileEntry = _dlApp.getFileEntry(Long.parseLong(fileId));
            ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
            String url = urlManager.replaceToInnerDocumentServerUrl(callback.getUrl());

            checkLockFileEntry(fileEntry, userId, serviceContext);

            updateFile(fileEntry, userId, url, DLVersionNumberIncrease.MINOR, serviceContext);

            fileEntry = _dlApp.getFileEntry(fileEntry.getFileEntryId());

            _dlAppService.checkOutFileEntry(fileEntry.getFileEntryId(), serviceContext);

            _log.info("Document saved (forcesave).");
        } else {
            _log.info("Forcesave is disabled, ignoring forcesave request");
        }
    }

    private void updateFile(FileEntry fileEntry, Long userId, String url, DLVersionNumberIncrease dlVersionNumberIncrease,
        ServiceContext serviceContext) throws Exception {
        _log.info("Trying to download file from URL: " + url);

        try {
            URLConnection con = new URL(url).openConnection();
            InputStream in = con.getInputStream();

            _dlApp.updateFileEntry(userId, fileEntry.getFileEntryId(), fileEntry.getFileName(), fileEntry.getMimeType(),
                    fileEntry.getTitle(), fileEntry.getDescription(), "ONLYOFFICE Edit",dlVersionNumberIncrease, in,
                    con.getContentLength(), serviceContext);

            _dlAppService.checkInFileEntry(fileEntry.getFileEntryId(), dlVersionNumberIncrease, "ONLYOFFICE Edit", serviceContext);
        } catch (Exception e) {
            String msg = "Couldn't download or save file: " + e.getMessage();
            _log.error(msg, e);
            throw e;
        }
    }

    private void checkLockFileEntry(FileEntry fileEntry, Long userId, ServiceContext serviceContext) throws PortalException {
        if (fileEntry.isCheckedOut() && userId.longValue() != fileEntry.getLock().getUserId()) {
            setUserThreadLocal(fileEntry.getLock().getUserId());
            _dlAppService.cancelCheckOut(fileEntry.getFileEntryId());

            setUserThreadLocal(userId);
        } else {
            setUserThreadLocal(userId);
        }
    }

    private void setUserThreadLocal (Long userId) throws PortalException {
        User user = UserLocalServiceUtil.getUser(userId);

        PermissionChecker permissionChecker;
        permissionChecker = PermissionCheckerFactoryUtil.create(user);

        PrincipalThreadLocal.setName(userId);
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
    }

    private static final Log _log = LogFactoryUtil.getLog(CallbackServiceImpl.class);

    @Reference
    private DLAppService _dlAppService;

    @Reference
    private DLAppLocalService _dlApp;

    @Reference
    private OnlyOfficeUtils _OOUtils;

    @Reference
    private UrlManager urlManager;

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(
            SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Reference(service = JwtManager.class, unbind = "-")
    public void setJwtManager(
            JwtManager jwtManager) {
        super.setJwtManager(jwtManager);
    }
}
