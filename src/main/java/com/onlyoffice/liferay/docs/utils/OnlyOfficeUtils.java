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

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTableConstants;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;

import org.osgi.service.component.annotations.Component;

@Component(
    service = OnlyOfficeUtils.class
)
public class OnlyOfficeUtils {
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

}
