package onlyoffice.integration.ui;

import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;

@Component(
	immediate = true,
	property = {
			"com.liferay.portlet.add-default-resource=true",
			"com.liferay.portlet.display-category=category.hidden",
			"com.liferay.portlet.header-portlet-css=/css/main.css",
			"com.liferay.portlet.instanceable=true",
			"javax.portlet.display-name=OnlyOffice Edit",
			"javax.portlet.init-param.template-path=/",
			"javax.portlet.init-param.view-template=/edit.jsp",
			"javax.portlet.security-role-ref=power-user,user",
			"javax.portlet.resource-bundle=content.Language",
			"javax.portlet.version=3.0"
	},
	service = Portlet.class
)
public class EditActionPortlet extends MVCPortlet {
}