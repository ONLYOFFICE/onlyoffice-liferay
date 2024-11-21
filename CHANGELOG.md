# Change Log

##
## Added
- editing pdf
- shardkey parameter

## Changed
- create pdf instead docxf
- delete create oform from docxf
- com.onlyofficce:docs-integration-sdk:1.3.0
- address of the convert service, /converter instead /ConvertService.ashx
- default token lifetime is 5 minutes
- default empty file templates

## 2.4.0
## Added
- compatible with Liferay CE 7.4 GA44 - GA73
- compatible with Liferay DXP 7.4 U44 - U73

## 2.3.0
## Added
- Chinese (Traditional, Taiwan), Basque (Spain) and Malay (Malaysia) empty file templates
- new route to open in desktop app
- setting jwt header on configuration page


## 2.2.0
## Added
- preview of the documents
- keep intermediate versions when editing (forcesave)
- Galician empty file templates

## Changed
- checkin/checkout instead of lock/unlock
- fixed deployment error for portal-7.4 [#24](https://github.com/ONLYOFFICE/onlyoffice-liferay/issues/24)

## 2.1.0
## Added
- support docxf and oform formats
- create blank docxf from creation menu
- create oform from docxf from document manager
- "save as" in editor

## 2.0.0
## Added
- Ability to create documents
- Desktop mode
- goback from editor
- DE, ES, FR, IT, PT_BR, RU translations

## Changed
- apache license

## 1.1.0
## Fixed
- Fixed an issue with collaborative editing
- Fixed an issue when only admin user could trigger convertation
- Fixed an issue with user permissions

## 1.0.0
## Added
- Edit option for DOCX, XLSX, PPTX.
- View and convert options for ODT, ODS, ODP, DOC, XLS, PPT.
- Configuration page
- JWT support