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

package com.onlyoffice.liferay.docs.ui;

import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.kernel.exception.FileNameException;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.onlyoffice.manager.document.DocumentManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;


@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY,
		"javax.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY_ADMIN,
		"mvc.command.name=/document_library/create_onlyoffice"
	},
	service = {MVCActionCommand.class}
)
public class CreateMVCActionCommand extends BaseMVCActionCommand {
	private static final Log log = LogFactoryUtil.getLog(CreateMVCActionCommand.class);

	@Reference
	private DLAppService dlAppService;
	@Reference
	private DocumentManager documentManager;

	@Override
	protected void doProcessAction(final ActionRequest actionRequest, final ActionResponse actionResponse)
			throws Exception {
		long folderId = ParamUtil.getLong(actionRequest, "folderId");
		String type = ParamUtil.getString(actionRequest, "type");
		String title = ParamUtil.getString(actionRequest, "title");
		String description = ParamUtil.getString(actionRequest, "description");
		String redirect = ParamUtil.getString(actionRequest, "redirectUrl");

		InputStream streamSourceFile = null;

		try {
			actionResponse.setRenderParameter("folderId", String.valueOf(folderId));
			actionResponse.setRenderParameter("redirect", redirect);

			ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
			Long repositoryId = themeDisplay.getScopeGroupId();

			Locale locale = themeDisplay.getLocale();

			streamSourceFile = documentManager.getNewBlankFile(type, locale);

			File sourceFile = FileUtil.createTempFile(streamSourceFile);
			String mimeType = MimeTypesUtil.getContentType(sourceFile);

			String uniqueFileName = DLUtil.getUniqueFileName(repositoryId, folderId, title + "." + type);

			ServiceContext serviceContext = ServiceContextFactory.getInstance(
					DLFileEntry.class.getName(),
					actionRequest
			);

			FileEntry newFile = dlAppService.addFileEntry(repositoryId, folderId, uniqueFileName, mimeType,
					uniqueFileName, description, (String) null, sourceFile,serviceContext);

			actionResponse.setRenderParameter("fileEntryId", String.valueOf(newFile.getFileEntryId()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			if (e instanceof FileNameException || e instanceof PrincipalException.MustHavePermission) {
				SessionErrors.add(actionRequest, e.getClass());
			} else {
				SessionErrors.add(actionRequest, Exception.class);
			}
			return;
		} finally {
			streamSourceFile.close();
		}
	}
}
