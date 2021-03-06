package onlyoffice.integration.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StreamUtil;

import onlyoffice.integration.OnlyOfficeHasher;
import onlyoffice.integration.OnlyOfficeJWT;

@Component(
    immediate = true,
    property = {
        "osgi.http.whiteboard.context.path=/",
        "osgi.http.whiteboard.servlet.pattern=/onlyoffice/doc/*"
    },
    service = Servlet.class
)
public class OnlyOfficeDocumentApi extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        String key = ParamUtil.getString(request, "key");
        Long fileVersionId = _hasher.validate(key);

        if (fileVersionId <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        FileVersion file;
        try {
            file = DLAppLocalServiceUtil.getFileVersion(fileVersionId);

            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"");
            response.setHeader("Content-Length", Long.toString(file.getSize()));
            response.setContentType(file.getMimeType());

            StreamUtil.transfer(file.getContentStream(false), response.getOutputStream());
        } catch (PortalException e) {
            _log.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        String error = null;

        String key = ParamUtil.getString(request, "key");
        Long fileVersionId = _hasher.validate(key);

        if (fileVersionId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            error = "Wrong fileVersionId";
        } else {
            try {
                FileEntry file = _dlApp.getFileVersion(fileVersionId).getFileEntry();

                String body = getBody(request.getInputStream());
                if (body.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    error = "Empty body";
                } else {
                    JSONObject jsonObj = new JSONObject(body);

                    if (_jwt.isEnabled()) {
                        jsonObj = _jwt.validate(jsonObj, request);
                    }

                    processData(file, jsonObj, request);
                }
            } catch (Exception ex) {
                _log.error("Unable to process ONLYOFFICE response: " + ex.getMessage(), ex);
                error = ex.getMessage();
            }
        }

        try {
            JSONObject respBody = new JSONObject();
            if (error != null) {
                response.setStatus(500);
                respBody.put("error", 1);
                respBody.put("message", error);
            } else {
                respBody.put("error", 0);
            }

            response.getWriter().write(respBody.toString(2));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private String getBody(InputStream is) {
        try {
            Scanner s = new Scanner(is);
            s.useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            s.close();
            is.close();

            return result;
        } catch (IOException e) {
            _log.error(e.getMessage(), e);
        }
        return "";
    }
    
    private void processData(FileEntry file, JSONObject body, HttpServletRequest request)
        throws JSONException, PortalException, Exception {

        Long fileId = file.getFileEntryId();
        Long userId = (long) -1;

        if (body.has("users")) {
            JSONArray users = body.getJSONArray("users");
            userId = users.getLong(0);
        }

        switch(body.getInt("status")) {
            case 0:
                _log.error("ONLYOFFICE has reported that no doc with the specified key can be found");
                    if (file.isSupportsLocking()) {
                        _dlFile.unlockFileEntry(fileId);
                    }
                break;
            case 1:
                if (file.isSupportsLocking()) {
                    if (file.getLock() != null) {
                        _log.info("Document already locked, another user has entered/exited");
                    } else {
                        _log.info("Document opened for editing, locking document");
                        _dlFile.lockFileEntry(userId, fileId);
                    }
                }
                break;
            case 2:
                _log.info("Document updated, changing content");
                if (file.isSupportsLocking()) {
                    _log.info("Unlocking document");
                    _dlFile.unlockFileEntry(fileId);
                }
                updateFile(file, userId, body.getString("url"), request);
                break;
            case 3:
                _log.error("ONLYOFFICE has reported that saving the document has failed, unlocking document");
                if (file.isSupportsLocking()) {
                    _dlFile.unlockFileEntry(fileId);
                }
                break;
            case 4:
                _log.info("No document updates, unlocking document");
                if (file.isSupportsLocking()) {
                    _dlFile.unlockFileEntry(fileId);
                }
                break;
        }
    }

    private void updateFile(FileEntry file, Long userId, String url, HttpServletRequest request) throws Exception {
        _log.info("Trying to download file from URL: " + url);

        try {
            URLConnection con = new URL(url).openConnection();
            InputStream in = con.getInputStream();
            ServiceContext serviceContext = ServiceContextFactory.getInstance(OnlyOfficeDocumentApi.class.getName(), request);

            _dlApp.updateFileEntry(userId, file.getFileEntryId(), file.getFileName(), file.getMimeType(), 
                    file.getTitle(), file.getDescription(), "ONLYOFFICE Edit", 
                    DLVersionNumberIncrease.MINOR, in, con.getContentLength(), serviceContext);

            _log.info("Document saved.");

        } catch (Exception e) {
            String msg = "Couldn't download or save file: " + e.getMessage();
            _log.error(msg, e);
            throw new Exception(msg);
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Log _log = LogFactoryUtil.getLog(OnlyOfficeDocumentApi.class);

    @Reference
    private OnlyOfficeJWT _jwt;

    @Reference
    private OnlyOfficeHasher _hasher;

    @Reference
    private DLAppLocalService _dlApp;

    @Reference
    private DLFileEntryLocalService _dlFile;
}