/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
import com.liferay.portal.kernel.portlet.LiferayPortletConfig;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderConstants;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.PortalUtil;
import com.onlyoffice.liferay.docs.portlet.ResourceBundlePortletConfigWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(
        immediate = true,
        property = {
                "javax.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY,
                "javax.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY_ADMIN,
                "mvc.command.name=/document_library/create_onlyoffice"
        },
        service = MVCRenderCommand.class
)
public class CreateMVCRenderCommand implements MVCRenderCommand {
    @Reference(target = "(osgi.web.symbolicname=com.onlyoffice.liferay-docs)")
    private ServletContext servletContext;

    @Override
    public String render(final RenderRequest renderRequest, final RenderResponse renderResponse)
            throws PortletException {
        RequestDispatcher requestDispatcher = this.servletContext.getRequestDispatcher("/create.jsp");

        LiferayPortletConfig portletConfig = (LiferayPortletConfig) renderRequest.getAttribute(
                JavaConstants.JAVAX_PORTLET_CONFIG);

        PortletConfig resourceBundlePortletConfigWrapper = new ResourceBundlePortletConfigWrapper(portletConfig);
        renderRequest.setAttribute(JavaConstants.JAVAX_PORTLET_CONFIG, resourceBundlePortletConfigWrapper);

        try {
            HttpServletRequest request = PortalUtil.getHttpServletRequest(renderRequest);
            HttpServletResponse response = PortalUtil.getHttpServletResponse(renderResponse);
            requestDispatcher.include(request, response);
            requestDispatcher.toString();
        } catch (Exception e) {
            throw new PortletException(e.getMessage(), e);
        }

        return MVCRenderConstants.MVC_PATH_VALUE_SKIP_DISPATCH;
    }
}
