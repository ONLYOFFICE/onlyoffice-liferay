# ONLYOFFICE app for Liferay

This app enables users to edit office documents from [Liferay](https://www.liferay.com/) using ONLYOFFICE Docs packaged as Document Server - [Community or Enterprise Edition](#onlyoffice-docs-editions).

## Features

The app allows to:

* Create and edit text documents, spreadsheets, and presentations.
* Share documents with other users.
* Co-edit documents in real-time: use two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

## Supported formats: 

**For viewing:**

* **WORD**: DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, MHTML, ODT, OTT, RTF, STW, SXW, TXT, WPS, WPT, XML
* **CELL**: CSV, ET, ETT, FODS, ODS, OTS, SXC, XLS, XLSB, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE**: DPS, DPT, FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SXI
* **PDF**: DJVU, DOCXF, OFORM, OXPS, PDF, XPS

**For editing:**

* **WORD**: DOCM, DOCX, DOTM, DOTX
* **CELL**: XLSM, XLSX, XLTM, XLTX
* **SLIDE**: POTM, POTX, PPSM, PPSX, PPTM, PPTX
* **PDF**: PDF

**For converting to Office Open XML formats:**

* **WORD:** DOC, DOCM, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, MHTML, ODT, OTT, RTF, STW, SXW, WPS, WPT, XML
* **CELL:** ET, ETT, FODS, ODS, OTS, SXC, XLS, XLSB, XLSM, XLT, XLTM, XLTX
* **SLIDE:** DPS, DPT, FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, SXI
* **PDF**: DOCXF, OXPS, PDF, XPS

## Installing ONLYOFFICE Docs

You will need an instance of ONLYOFFICE Docs (Document Server) that is resolvable and connectable both from Liferay and any end clients. ONLYOFFICE Document Server must also be able to POST to Liferay directly.

You can install free Community version of ONLYOFFICE Docs or scalable Enterprise Edition with pro features.

To install free Community version, use [Docker](https://github.com/onlyoffice/Docker-DocumentServer) (recommended) or follow [these instructions](https://helpcenter.onlyoffice.com/installation/docs-community-install-ubuntu.aspx) for Debian, Ubuntu, or derivatives.  

To install Enterprise Edition, follow instructions [here](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx).

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

## Installing ONLYOFFICE app for Liferay

Either install it from [Liferay Marketplace](https://web.liferay.com/marketplace/-/mp/application/171169174) or if you're building the app by yourself simply put compiled .jar file from `build\libs` folder to `/opt/liferay/deploy`. Liferay will install it automatically.

## Configuring ONLYOFFICE app for Liferay

In order to configure it you must navigate to *System Settings* `(Control Panel -> Configuration -> System Settings)`. In *Platform* section click on *Connectors* category and select ONLYOFFICE.

Starting from version 7.2, JWT is enabled by default and the secret key is generated automatically to restrict the access to ONLYOFFICE Docs and for security reasons and data integrity. 
Specify your own **Secret key** on the Liferay **System Settings** page. 
In the ONLYOFFICE Docs [config file](https://api.onlyoffice.com/editors/signature/), specify the same secret key and enable the validation.

## Compiling ONLYOFFICE app for Liferay

Simply run `gradle build`. Output .jar will be placed inside `build/libs` directory.

## How it works

The ONLYOFFICE integration follows the API documented [here](https://api.onlyoffice.com/editors/basic):

* User navigates to a *Documents and Media* section within Liferay and selects the `Edit in ONLYOFFICE` action.
* Liferay prepares a JSON object for the Document Server with the following properties:
  * **url**: the URL that ONLYOFFICE Document Server uses to download the document,
  * **callbackUrl**: the URL that ONLYOFFICE Document Server informs about status of the document editing;
  * **key**: the fileVersionId to instruct ONLYOFFICE Document Server whether to download the document again or not;
  * **title**: the document Title (name).
* The client browser makes a request for the javascript library from ONLYOFFICE Document Server and sends ONLYOFFICE Document Server the docEditor configuration with the above properties.
* Then ONLYOFFICE Document Server downloads the document from Liferay and the user begins editing.
* ONLYOFFICE Document Server sends a POST request to the `callback` URL to inform Liferay that a user is editing the document.
* Liferay locks the document, but still allows other users with write access the ability to collaborate in real time with ONLYOFFICE Document Server by leaving the Action present.
* When all users and client browsers are done with editing, they close the editing window.
* After 10 seconds of inactivity, ONLYOFFICE Document Server sends a POST to the `callback` URL letting Liferay know that the clients have finished editing the document and closed it.
* Liferay downloads the new version of the document, replacing the old one.

## ONLYOFFICE Docs editions 

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

* Community Edition (`onlyoffice-documentserver` package)
* Enterprise Edition (`onlyoffice-documentserver-ee` package)

The table below will help you make the right choice.

| Pricing and licensing | Community Edition | Enterprise Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubLiferay#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubLiferay#docs-enterprise)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/docs-enterprise-prices.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubLiferay)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Enterprise Edition** |
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/installation/docs-community-index.aspx) | [Help Center](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | One year support included |
| Premium support | [Contact us](mailto:sales@onlyoffice.com) | [Contact us](mailto:sales@onlyoffice.com) |
| **Services** | **Community Edition** | **Enterprise Edition** |
| Conversion Service                | + | + |
| Document Builder Service          | + | + |
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                       | + | + |
| Dark theme                             | + | + |
| 125%, 150%, 175%, 200% scaling         | + | + |
| White Label                            | - | - |
| Integrated test example (node.js)      | + | + |
| Mobile web editors                     | - | +* |
| **Plugins & Macros** | **Community Edition** | **Enterprise Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Enterprise Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | + | + | 
| Editing Content control         | + | + | 
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Mail Merge                      | + | + |
| Comparing Documents             | + | + |
| **Spreadsheet Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | + | + |
| Data validation           | + | + |
| Conditional formatting          | + | + |
| Sparklines                   | + | + |
| Sheet Views                     | + | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| **Form creator features** | **Community Edition** | **Enterprise Edition** |
| Adding form fields           | + | + |
| Form preview                    | + | + |
| Saving as PDF                   | + | + |
| **Working with PDF**      | **Community Edition** | **Enterprise Edition** |
| Text annotations (highlight, underline, cross out) | + | + |
| Comments                        | + | + |
| Freehand drawings               | + | + |
| Form filling                    | + | + |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubLiferay#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubLiferay#docs-enterprise)  |

\* If supported by DMS.