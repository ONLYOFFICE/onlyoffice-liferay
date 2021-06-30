var data = {
    displayName: decodeURI(JSON.parse(`"${Liferay.ThemeDisplay.getUserName()}"`)),
    email: Liferay.ThemeDisplay.getUserEmailAddress(),
    domain: Liferay.ThemeDisplay.getPortalURL(),
    provider: "liferay",
    userId: Liferay.ThemeDisplay.getUserId()
};

window.AscDesktopEditor.execCommand("portal:login", JSON.stringify(data));