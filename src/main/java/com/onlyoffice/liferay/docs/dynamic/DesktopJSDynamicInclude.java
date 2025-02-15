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

package com.onlyoffice.liferay.docs.dynamic;

import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.servlet.taglib.BaseDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(
        immediate = true,
        service = DynamicInclude.class
)
public class DesktopJSDynamicInclude extends BaseDynamicInclude {
    @Reference
    private Portal portal;
    @Reference(target = "(osgi.web.symbolicname=com.onlyoffice.liferay-docs)")
    private ServletContext servletContext;

    @Override
    public void include(final HttpServletRequest request, final HttpServletResponse response, final String key)
            throws IOException {

        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent.contains("AscDesktopEditor")) {
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

            StringBundler sb = new StringBundler();

            sb.append("<script src=\"");
            sb.append(themeDisplay.getPortalURL());
            sb.append(portal.getPathProxy());
            sb.append(servletContext.getContextPath());
            sb.append("/js/desktop.js\" ");
            sb.append("type= \"text/javascript\">");
            sb.append("</script>");

            PrintWriter printWriter = response.getWriter();
            printWriter.println(sb.toString());
        }
    }

    @Override
    public void register(final DynamicIncludeRegistry dynamicIncludeRegistry) {
        dynamicIncludeRegistry.register(
                "/html/common/themes/top_head.jsp#post"
        );
    }
}
