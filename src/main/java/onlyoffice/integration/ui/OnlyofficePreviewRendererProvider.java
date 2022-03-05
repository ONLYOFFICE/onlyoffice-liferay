package onlyoffice.integration.ui;

import com.liferay.document.library.preview.DLPreviewRenderer;
import com.liferay.document.library.preview.DLPreviewRendererProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import onlyoffice.integration.config.OnlyOfficeConfigManager;

@Component(
        property = {
            "service.ranking:Integer=100"
        },
        service = DLPreviewRendererProvider.class
)
public class OnlyofficePreviewRendererProvider implements DLPreviewRendererProvider {

    @Override
    public Set<String> getMimeTypes() {
        return new HashSet<>(Arrays.asList(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "text/csv",
            "text/rtf",
            "text/plain",
            "application/pdf"
        ));
    }

    @Override
    public DLPreviewRenderer getPreviewDLPreviewRenderer(FileVersion fileVersion) {	
        if (!_config.webPreview()) {
            return null;
        }

        return (request, response) -> {
            RequestDispatcher requestDispatcher = this.servletContext.getRequestDispatcher("/preview.jsp");

            request.setAttribute("fileId", fileVersion.getFileEntryId());

            requestDispatcher.include(request, response);
        };
    }

    @Override
    public DLPreviewRenderer getThumbnailDLPreviewRenderer(FileVersion fileVersion) {
        // TODO Auto-generated method stub
        return null;
    }

    @Reference(target = "(osgi.web.symbolicname=onlyoffice.integration.web)")
    protected ServletContext servletContext;

    @Reference
    private OnlyOfficeConfigManager _config;

    private static final Log _log = LogFactoryUtil.getLog(OnlyofficePreviewRendererProvider.class);
}
