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

package com.onlyoffice.liferay.docs.sdk.service;

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.liferay.docs.utils.SecurityUtils;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Action;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.text.MessageFormat;
import java.util.List;

@Component(
        service = CallbackService.class
)
public class CallbackServiceImpl extends DefaultCallbackService {
    private static final Log log = LogFactoryUtil.getLog(CallbackServiceImpl.class);

    @Reference
    private DLAppService dlAppService;
    @Reference
    private UrlManager urlManager;
    @Reference
    private FileEntryUtils fileEntryUtils;

    public CallbackServiceImpl() {
        super(null, null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Reference(service = JwtManager.class, unbind = "-")
    public void setJwtManager(final JwtManager jwtManager) {
        super.setJwtManager(jwtManager);
    }

    public void handlerEditing(final Callback callback, final String fileId) throws Exception {
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws Exception {
                    FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );

                    if (!fileEntryUtils.isLockedInEditor(fileEntry)) {
                        throw new RuntimeException(
                                MessageFormat.format(
                                        "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                        String.valueOf(fileEntry.getFileEntryId())
                                )
                        );
                    }

                    switch (action.getType()) {
                        case CONNECTED:
                            handlerConnecting(callback, fileEntry, userId);
                            break;
                        case DISCONNECTED:
                            handlerDisconnecting(callback, fileEntry, userId);
                            break;
                        default:
                    }
                    return null;
                }
            }, userId);
        }
    }

    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        log.info("Document updated, changing content");
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            final FileEntry fileEntry = SecurityUtils.runAs(new SecurityUtils.RunAsWork<FileEntry>() {
                public FileEntry doWork() throws PortalException {
                    FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );

                    if (!fileEntryUtils.isLockedInEditor(fileEntry)) {
                        throw new RuntimeException(
                                MessageFormat.format(
                                        "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                        String.valueOf(fileEntry.getFileEntryId())
                                )
                        );
                    }

                    return fileEntry;
                }
            }, userId);

            userId = fileEntry.getLock().getUserId();

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws Exception {
                    String fileUrl = urlManager.replaceToInnerDocumentServerUrl(callback.getUrl());
                    fileEntryUtils.updateFileEntryFromUrl(
                            fileEntry,
                            fileUrl,
                            DLVersionNumberIncrease.MAJOR
                    );

                    dlAppService.checkInFileEntry(
                            fileEntry.getFileEntryId(),
                            DLVersionNumberIncrease.MAJOR,
                            "ONLYOFFICE Edit",
                            ServiceContextThreadLocal.getServiceContext()
                    );
                    return null;
                }
            }, userId);
        }

        log.info("Document saved.");
    }

    public void handlerClosed(final Callback callback, final String fileId) throws Exception {
        log.info("No document updates, unlocking document");
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            final FileEntry fileEntry = SecurityUtils.runAs(new SecurityUtils.RunAsWork<FileEntry>() {
                public FileEntry doWork() throws PortalException {
                    FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );

                    if (!fileEntryUtils.isLockedInEditor(fileEntry)) {
                        throw new RuntimeException(
                                MessageFormat.format(
                                        "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                        String.valueOf(fileEntry.getFileEntryId())
                                )
                        );
                    }

                    return fileEntry;
                }
            }, userId);

            Lock lock = fileEntry.getLock();

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws PortalException {
                    dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
                    return null;
                }
            }, lock.getUserId());
        }
    }

    public void handlerForcesave(final Callback callback, final String fileId) throws Exception {
        if (!getSettingsManager().getSettingBoolean("customization.forcesave", false)) {
            log.info("Forcesave is disabled, ignoring forcesave request");
            return;
        }

        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            final FileEntry fileEntry = SecurityUtils.runAs(new SecurityUtils.RunAsWork<FileEntry>() {
                public FileEntry doWork() throws PortalException {
                    FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );

                    if (!fileEntryUtils.isLockedInEditor(fileEntry)) {
                        throw new RuntimeException(
                                MessageFormat.format(
                                        "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                        String.valueOf(fileEntry.getFileEntryId())
                                )
                        );
                    }

                    return fileEntry;
                }
            }, userId);

            Lock lock = fileEntry.getLock();

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws Exception {
                    String fileUrl = urlManager.replaceToInnerDocumentServerUrl(callback.getUrl());
                    fileEntryUtils.updateFileEntryFromUrl(
                            fileEntry,
                            fileUrl,
                            DLVersionNumberIncrease.MINOR
                    );

                    dlAppService.checkInFileEntry(
                            fileEntry.getFileEntryId(),
                            DLVersionNumberIncrease.MINOR,
                            "ONLYOFFICE Edit",
                            ServiceContextThreadLocal.getServiceContext()
                    );

                    FileEntry checkOutFileEntry = dlAppService.checkOutFileEntry(
                            fileEntry.getFileEntryId(),
                            lock.getOwner(),
                            0,
                            ServiceContextThreadLocal.getServiceContext()
                    );
                    dlAppService.refreshFileEntryLock(
                            checkOutFileEntry.getLock().getUuid(),
                            checkOutFileEntry.getLock().getCompanyId(),
                            0
                    );
                    return null;
                }
            }, lock.getUserId());
        }

        log.info("Document saved (forcesave).");
    }

    private void handlerConnecting(final Callback callback, final FileEntry fileEntry, final Long userId)
            throws Exception {
        Lock lock = fileEntry.getLock();

        if (lock.getExpirationTime() > 0) {
            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws PortalException {
                    dlAppService.refreshFileEntryLock(
                            lock.getUuid(),
                            lock.getCompanyId(),
                            0
                    );
                    return null;
                }
            }, lock.getUserId());
        }
    }

    private void handlerDisconnecting(final Callback callback, final FileEntry fileEntry, final long userId)
            throws Exception {
        Lock lock = fileEntry.getLock();
        List<String> users = callback.getUsers();

        if (!users.contains(String.valueOf(userId)) && lock.getUserId() == userId) {
            dlAppService.cancelCheckOut(fileEntry.getFileEntryId());

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws PortalException {
                    FileEntry checkOutFileEntry = dlAppService.checkOutFileEntry(
                            fileEntry.getFileEntryId(),
                            lock.getOwner(),
                            0,
                            ServiceContextThreadLocal.getServiceContext()
                    );
                    dlAppService.refreshFileEntryLock(
                            checkOutFileEntry.getLock().getUuid(),
                            checkOutFileEntry.getLock().getCompanyId(),
                            0
                    );
                    return null;
                }
            }, Long.parseLong(users.get(0)));
        }
    }
}
