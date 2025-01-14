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

package com.onlyoffice.liferay.docs.api;

import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.PortalSessionThreadLocal;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringBundler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component(
        immediate = true,
        property = {
                "osgi.http.whiteboard.context.path=/",
                "osgi.http.whiteboard.servlet.pattern=/onlyoffice/desktop/login/*"
        },
        service = Servlet.class
)
public class OnlyOfficeDesktopLogin extends HttpServlet {
    public static final String DOCUMENT_LIBRARY_ADMIN =
            "/group/guest/~/control_panel/manage?p_p_id=com_liferay_document_library_web_portlet_DLAdminPortlet";

    private static final long serialVersionUID = 1L;

    @Reference
    private Portal portal;
    @Reference
    private UserLocalService userLocalService;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        try {
            HttpSession httpSession = request.getSession();

            if (PortalSessionThreadLocal.getHttpSession() == null) {
                PortalSessionThreadLocal.setHttpSession(httpSession);
            }

            User user = portal.getUser(request);

            if (user == null) {
                String userIdString = (String) httpSession.getAttribute("j_username");
                String password = (String) httpSession.getAttribute("j_password");

                if (userIdString != null && password != null) {
                    long userId = GetterUtil.getLong(userIdString);

                    user = userLocalService.getUser(userId);
                }
            }

            if (user == null) {
                response.sendRedirect(
                    StringBundler.concat(
                        portal.getPathMain(), "/portal/login?redirect=",
                        DOCUMENT_LIBRARY_ADMIN
                    )
                );
            } else {
                response.sendRedirect(DOCUMENT_LIBRARY_ADMIN);
            }
        } catch (Exception exception) {
            portal.sendError(exception, request, response);
        }
    }
}
