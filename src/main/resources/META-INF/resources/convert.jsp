<%--
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
 --%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>

<portlet:defineObjects />

<div style="padding: 20px 10px;">
    <span id="ooConvertText">
        <liferay-ui:message
            key="onlyoffice-convert-process"
            arguments='<%=  new Object[] {request.getAttribute("fileName"), request.getAttribute("newFileName")} %>'
        />
    </span>
    <div class="progress">
        <span id="ooProgressThumb" style="padding: 5px;" class="progress-bar"></span>
    </div>

    <script type="text/javascript">
    (function() {
        var text = document.getElementById("ooConvertText");
        var thumb = document.getElementById("ooProgressThumb");

        const onSuccess = () => {
            text.innerHTML = '<%= LanguageUtil.get(request, "onlyoffice-convert-done") %>';
            window.top.location.reload();
        }

        const onError = (message) => {
            text.innerHTML = '<%= LanguageUtil.get(request, "onlyoffice-convert-error") %>' + message;
        }

        const convert = (fileEntryId, version, time) => {
            fetch("/o/onlyoffice-docs/convert",
                {
                    headers: {
                      'Accept': 'application/json',
                      'Content-Type': 'application/json'
                    },
                    method: "POST",
                    body: JSON.stringify({
                        fileEntryId: fileEntryId,
                        version: version,
                        time: time,
                    })
            })
            .then(async (response) => {
                const data = await response.json();
                if (!response.ok) {
                     onError(data.error);
                     return;
                }

                if (data.percent != null) {
                    thumb.style.flex = data.percent / 100;
                    thumb.innerHTML = data.percent + "%";
                }

                if (!data.endConvert) {
                    setTimeout(() => convert(fileEntryId, version, time), 1000);
                } else {
                    onSuccess();
                }
            }).catch(e => {
                console.error('Error converting fileEntry:', error);
            })
        }

        convert(
            '<%= request.getAttribute("fileEntryId") %>',
            '<%= request.getAttribute("version") %>',
            Date.now(),
        );
    })();
    </script>
</div>