package onlyoffice.integration.ui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.display.context.DLDisplayContextFactory;
import com.liferay.document.library.display.context.DLEditFileEntryDisplayContext;
import com.liferay.document.library.display.context.DLViewFileVersionDisplayContext;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileShortcut;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;

import onlyoffice.integration.OnlyOfficeConvertUtils;
import onlyoffice.integration.OnlyOfficeUtils;

@Component(immediate = true, service = DLDisplayContextFactory.class)
public class EditMenuContextFactory
    implements DLDisplayContextFactory {

    public DLEditFileEntryDisplayContext getDLEditFileEntryDisplayContext(
        DLEditFileEntryDisplayContext parentDLEditFileEntryDisplayContext,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        DLFileEntryType dlFileEntryType) {

        return parentDLEditFileEntryDisplayContext;
    }

    public DLEditFileEntryDisplayContext getDLEditFileEntryDisplayContext(
        DLEditFileEntryDisplayContext parentDLEditFileEntryDisplayContext,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, FileEntry fileEntry) {

        return parentDLEditFileEntryDisplayContext;
    }

    public DLViewFileVersionDisplayContext getDLViewFileVersionDisplayContext(
        DLViewFileVersionDisplayContext parentDLViewFileVersionDisplayContext,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, FileShortcut fileShortcut) {

        return parentDLViewFileVersionDisplayContext;
    }

    public DLViewFileVersionDisplayContext getDLViewFileVersionDisplayContext(
        DLViewFileVersionDisplayContext parentDLViewFileVersionDisplayContext,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, FileVersion fileVersion) {

        return new EditMenuContext(
            parentDLViewFileVersionDisplayContext.getUuid(),
            parentDLViewFileVersionDisplayContext, httpServletRequest,
            httpServletResponse, fileVersion, _utils, _convertUtils, _permissionFactory);
    }

    @Reference
    private OnlyOfficeConvertUtils _convertUtils;

    @Reference
    private OnlyOfficeUtils _utils;

    @Reference
    private PermissionCheckerFactory _permissionFactory;
}