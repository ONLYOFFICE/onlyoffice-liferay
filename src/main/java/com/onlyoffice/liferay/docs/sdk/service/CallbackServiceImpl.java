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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.liferay.docs.utils.OnlyOfficeUtils;
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
    private OnlyOfficeUtils utils;
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

            FileEntry fileEntry = SecurityUtils.runAs(new SecurityUtils.RunAsWork<FileEntry>() {
                public FileEntry doWork() throws PortalException {
                    return dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );
                }
            }, userId);

            switch (action.getType()) {
                case CONNECTED:
                    handlerConnecting(callback, fileEntry.getFileEntryId(), userId);
                    break;
                case DISCONNECTED:
                    handlerDisconnecting(callback, fileEntry.getFileEntryId(), userId);
                    break;
                default:
            }
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
                    return dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );
                }
            }, userId);

            if (!fileEntry.isCheckedOut() || utils.getCollaborativeEditingKey(fileEntry) == null) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            userId = fileEntry.getLock().getUserId();

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws Exception {
                    String fileUrl = urlManager.replaceToInnerDocumentServerUrl(callback.getUrl());
                    FileEntry updatedFileEntry = fileEntryUtils.updateFileEntryFromUrl(
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

                    FileEntry checkInFileEntry = dlAppService.getFileEntry(fileEntry.getFileEntryId());
                    utils.setCollaborativeEditingKey(checkInFileEntry, null);
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
                    return dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );
                }
            }, userId);

            if (!fileEntry.isCheckedOut() || utils.getCollaborativeEditingKey(fileEntry) == null) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            userId = fileEntry.getLock().getUserId();

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws PortalException {
                    dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
                    utils.setCollaborativeEditingKey(fileEntry, null);
                    return null;
                }
            }, userId);
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
                    return dlAppService.getFileEntryByUuidAndGroupId(
                            fileEntryKeys.getUuid(),
                            fileEntryKeys.getGroupId()
                    );
                }
            }, userId);

            if (!fileEntry.isCheckedOut() || utils.getCollaborativeEditingKey(fileEntry) == null) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws Exception {
                    String fileUrl = urlManager.replaceToInnerDocumentServerUrl(callback.getUrl());
                    FileEntry updatedFileEntry = fileEntryUtils.updateFileEntryFromUrl(
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

                    dlAppService.checkOutFileEntry(
                            fileEntry.getFileEntryId(),
                            ServiceContextThreadLocal.getServiceContext()
                    );
                    return null;
                }
            }, userId);
        }

        log.info("Document saved (forcesave).");
    }

    private void handlerConnecting(final Callback callback, final long fileEntryId, final Long userId)
            throws Exception {
        SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
            public Void doWork() throws PortalException {
                FileEntry fileEntry = dlAppService.getFileEntry(fileEntryId);

                if (fileEntry.isCheckedOut() && utils.getCollaborativeEditingKey(fileEntry) == null) {
                    throw new RuntimeException(
                            MessageFormat.format(
                                    "FileEntry with ID ({0}) is locked",
                                    String.valueOf(fileEntry.getFileEntryId())
                            )
                    );
                }

                if (fileEntry.isCheckedOut() && utils.getCollaborativeEditingKey(fileEntry) != null) {
                    log.info(
                            MessageFormat.format(
                            "FileEntry with ID ({0}) already locked in ONLYOFFICE Docs Editor",
                                    String.valueOf(fileEntry.getFileEntryId())
                            )
                    );
                    return null;
                }

                String key = callback.getKey();
                utils.setCollaborativeEditingKey(fileEntry, key);

                dlAppService.checkOutFileEntry(
                        fileEntry.getFileEntryId(),
                        ServiceContextThreadLocal.getServiceContext()
                );
                log.info(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );

                return null;
            }
        }, userId);
    }

    private void handlerDisconnecting(final Callback callback, final long fileEntryId, final Long userId)
            throws Exception {
        SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
            public Void doWork() throws PortalException {
                FileEntry fileEntry = dlAppService.getFileEntry(fileEntryId);

                if (!fileEntry.isCheckedOut() || utils.getCollaborativeEditingKey(fileEntry) == null) {
                    throw new RuntimeException(
                            MessageFormat.format(
                                    "FileEntry with ID ({0}) not locked in ONLYOFFICE Docs Editor",
                                    String.valueOf(fileEntry.getFileEntryId())
                            )
                    );
                }
                return null;
            }
        }, userId);

        List<String> users = callback.getUsers();
        if (!users.contains(String.valueOf(userId))) {
            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws PortalException {
                    dlAppService.cancelCheckOut(fileEntryId);
                    return null;
                }
            }, userId);

            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() throws PortalException {
                    dlAppService.checkOutFileEntry(fileEntryId, ServiceContextThreadLocal.getServiceContext());
                    return null;
                }
            }, Long.parseLong(users.get(0)));
        }
    }
}
