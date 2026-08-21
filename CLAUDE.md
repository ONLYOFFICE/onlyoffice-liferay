# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A single OSGi bundle (`com.onlyoffice.liferay-docs`) that plugs ONLYOFFICE Docs into Liferay's Documents and Media. It is a Maven project despite what the README says (`gradle build` in README is stale — the project migrated to Maven in 3.0.0).

## Commands

```bash
mvn clean package          # build → target/liferay-docs-<version>.jar (this is the deployable OSGi bundle)
mvn checkstyle:check       # style gate; also runs in `mvn verify` and fails the build on any warning
mvn verify                 # package + checkstyle + deploys the jar into ./bundles/osgi/modules
```

There are no tests in this repository — no test sources, no test harness. Do not report "tests pass"; verification is limited to compilation, Checkstyle, and manual deployment.

Local manual verification:

```bash
mvn verify                             # puts the jar in bundles/osgi/modules
docker compose -f docker/docker-compose.yml up   # mounts that dir as /opt/liferay/deploy
```

`liferay/portal` image tag in `docker/docker-compose.yml` matches the branch's target Liferay version.

Build specifics: `maven-compiler-plugin` targets `release 8` (source must stay Java 8-compatible), CI builds with JDK 21. Runtime deps are copied to `target/lib` and embedded into the bundle via `-includeresource` in `bnd.bnd`; anything added as a `runtime`-scope dependency lands inside the jar, so keep Liferay/OSGi/portlet APIs `provided`.

## Multi-branch layout — read this before editing

One source tree per supported Liferay platform. `release.json` maps a platform name to the branch it is built from, and both `artifact.yml` and `release.yml` fan out over that file, so a release produces one jar per entry:

| release.json name | branch |
|---|---|
| CE-7.2 | *(none — built from the default branch, i.e. this line of development)* |
| CE-7.3 / CE-7.4 | `feature/portal-7.3` / `feature/portal-7.4` |
| DXP-7.2 / 7.3 / 2024 / 2025 / 2026 | `feature/dxp-7.2` … `feature/dxp-2026` |

Each branch pins its own `<liferay.bom.version>` and, importantly, **carries genuinely different Java code** — Liferay's `DLPreviewRendererProvider`, display-context, and portlet APIs changed across versions, so e.g. `feature/dxp-2026` has diverged substantially from `develop` (~500 lines). A change made here does not exist on the other branches; porting is a manual, per-branch cherry-pick. Work happens on `develop`; `master` is the release branch.

Release flow: bump the version in `pom.xml` **and** `bnd.bnd` (`Bundle-Version`), add a `## <version>` section to `CHANGELOG.md`. Pushing to `master` makes `create-tag.yml` read the first version number out of `CHANGELOG.md` and push a `v<version>` tag, which triggers the matrix build + GitHub release.

## Architecture

The integration logic lives in `com.onlyoffice:docs-integration-sdk` (shared across all ONLYOFFICE connectors). This bundle's job is to **implement the SDK's abstract managers/services against Liferay APIs** and register each as an OSGi service. That is the single most important thing to understand: `src/main/java/.../sdk/` holds subclasses of the SDK's `Default*` classes, and everything else consumes them by `@Reference` on the SDK interface, never on the impl.

Because DS field injection can't feed a superclass constructor, every `sdk/` impl uses the same pattern: `super(null, …)` in the constructor plus `@Reference(service = X.class, unbind = "-")` setters that forward to `super.setX(...)`. Follow it when adding a new impl.

| SDK contract | Impl | Liferay-side responsibility |
|---|---|---|
| `SettingsManager` | `sdk/managers/SettingsManagerImpl` | maps `SettingsConstants` keys onto the OSGi config annotation; read-only (`setSetting` is a no-op) |
| `DocumentManager` | `sdk/managers/DocumentManagerImpl` | document key — the editing key stored in the lock when locked, else `uuid_modifiedDate` |
| `UrlManager` | `sdk/managers/UrlManagerImpl` | builds `/o/onlyoffice-docs/download/{groupId}/{uuid}`, `/callback/...`, and the DL go-back URL |
| `JwtManager` | `sdk/managers/JwtManagerImpl` | JWT signing/verification from configured secret |
| `DocumentServerClient` | `sdk/client/DocumentServerClientImpl` | Apache HttpClient client, settings applied in `@Activate` |
| `ConfigService` | `sdk/service/ConfigServiceImpl` | editor `Permissions` from Liferay permission checks + lock state; `User` from the Liferay user |
| `CallbackService` | `sdk/service/CallbackServiceImpl` | the whole save/lock lifecycle (below) |
| `ConvertService` | `sdk/service/ConvertServiceImpl` | thin wiring only |

### Locking is the core invariant

`utils/EditorLockManager` overloads Liferay's DL checkout lock to carry editor state: the lock **owner** string is `onlyoffice-docs.{"editingKey":"<uuid>_<random16>"}` (`EditingMeta` serialized as JSON behind a prefix). Everything derives from that:

- `isLockedInEditor` / `isLockedNotInEditor` — a lock whose owner lacks the prefix is a *foreign* lock and must block editing.
- `isValidDocumentKey` — the Document Server's `key` must equal the stored `editingKey`; every callback handler rejects a mismatch.
- `dlAppService.checkOutFileEntry` with `expirationTime == 0` only locks for an hour, so `TIMEOUT_INFINITY` locks are followed by an explicit `refreshFileEntryLock`. `TIMEOUT_CONNECTING_EDITOR` (60 s) is the provisional lock taken when the editor page opens, upgraded to infinite on the `CONNECTED` callback.
- There is no "change owner" API, so `changeLockOwner` unlocks and re-locks inside one transaction — needed when the original lock owner disconnects but co-editors remain.

`listener/LockModelListener` closes the loop from the other side: if a Liferay `Lock` is removed by anything *other* than `CallbackServiceImpl` (detected by walking the stack trace — deliberate, not accidental), it sends the Document Server `INFO` then `DROP` commands so open editors are kicked out.

### Request paths

- **REST (JAX-RS whiteboard)** — `controller/OnlyofficeDocsRESTApplication` mounts everything under `/o/onlyoffice-docs` with `liferay.access.control.disable=true` and guest access allowed, so **each controller authenticates for itself**: `CallbackController`/`DownloadController` verify the JWT header from `SettingsManager`, then act as a Liferay user via `SecurityUtils`; `ConvertController`/`FeatureController` authenticate from the servlet session instead.
- **`utils/SecurityUtils`** is how any of that code gets a Liferay identity: `setUserAuthentication` sets `PrincipalThreadLocal` + `PermissionThreadLocal`, and `runAs(work, userId)` does it with restore in a `finally`. Callbacks arrive from the Document Server with no Liferay session, so nothing in the callback path works without it. Permission checks still happen explicitly (`utils/PermissionUtils`, `fileEntry.containsPermission`) — the whiteboard disables Liferay's own access control.
- **Portlets** — `EditorPortlet` (`/edit.jsp`) builds the `Config` via `ConfigService`, serializes it to a request attribute, and takes the provisional lock; `ConvertPortlet` (`/convert.jsp`). Both are `category.hidden` and reached by explicit URL, extending `AbstractDefaultPortlet` → which swaps in `ResourceBundlePortletConfigWrapper` so this bundle's `content/Language*.properties` resolve inside JSPs.
- **DL UI integration** — `ui/EditMenuContextFactory` + `EditMenuContext` add the "Edit in ONLYOFFICE" / convert menu items to file entries; `ui/EditToolbarContributorContext` adds the folder-level "create" item; `ui/CreateMVCRenderCommand` + `CreateMVCActionCommand` implement `mvc.command.name=/document_library/create_onlyoffice` registered against `DLPortletKeys.DOCUMENT_LIBRARY{,_ADMIN}`; `ui/OnlyofficePreviewRendererProviderFactory` registers a `DLPreviewRendererProvider` **programmatically in `@Activate`** (not via `@Component`) because the `content.type` property has to be computed from the SDK's viewable-format list.
- **Desktop-app support** — `api/OnlyOfficeDesktopLogin` and `api/OnlyOfficeStatus` are HTTP-whiteboard servlets under `/onlyoffice/...`; `dynamic/DesktopJSDynamicInclude` injects `js/desktop.js` into the portal `top_head` only when the User-Agent contains `AscDesktopEditor`.

### Configuration

`config/OnlyOfficeConfiguration` is a metatype `@ObjectClassDefinition` surfaced in System Settings → Platform → Connectors (labels localized through `content/Language`). Two components read the same `configurationPid`: `OnlyOfficeConfigManager` (for JSPs/legacy callers) and `SettingsManagerImpl` (for the SDK). Adding a setting means touching the annotation, the `getSetting` switch, and `content/Language*.properties`.

## Conventions enforced by the build

- Every `.java` file must start verbatim with `onlyoffice.header` (Apache 2.0 block, current year). Checkstyle's `Header` module fails otherwise; the year is bumped repo-wide at the start of a year.
- 120-char lines, no trailing whitespace, no tabs, no star imports, `final` on method parameters (`FinalParameters`), `DeclarationOrder` respected. `checkstyle-suppressions.xml` waives only the Javadoc and `DesignForExtension` checks — so Javadoc is optional but everything else is not.
- `checkstyle.header.file` is set in the `standalone` profile (active by default); a build with that profile disabled cannot resolve the header path.
- Lombok is available (`provided`) and used sparingly — `@Data` on DTOs, `@SneakyThrows` where the SDK/Liferay signatures conflict.
- Commits follow Conventional Commits (`build(deps):`, `docs:`, `feat:`, …).
