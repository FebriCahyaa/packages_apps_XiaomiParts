# Garnet Parts UI audit (Material 3)

Scope: `packages_apps_XiaomiParts`, branch `garnet-6.6`, audited 2026-11-23.

## Findings and status

| Area | Finding | Status |
|---|---|---|
| Boot | Three receivers duplicated startup logic and applied non-6.6 defaults | Fixed: `GarnetBootReceiver` |
| Status cards | Charging, kernel info and new pages each had their own label/value row | Fixed: `GarnetStatusCard` / `GarnetStatusRow` reused by charging, display and memory |
| Previews/legacy XML | Older activities still use platform styles and Java Preference fragments | Open: migrate `RefreshSettingsActivity`, `KernelManagerActivity`, `GpuManagerActivity` |
| Sysfs on main thread | Several legacy click handlers still call `FileUtils` directly | Partly fixed: charging, performance, GPU manager, boot and new pages use `Dispatchers.IO`; kernel manager clicks still open |
| Hardcoded strings | Core control toast messages are English literals in Kotlin | Open: move to `strings.xml` |
| Icons | Mixed `Icons.Filled`, `Icons.Rounded` and drawable resources | Partly fixed: new pages consistently use existing drawables |
| Theme | `ExpressiveTheme` + Material 3 expressive APIs already in use | Keep; new pages follow it |

## Rule for new pages

1. `GarnetTopAppBar` + `GarnetPageHeading` for navigation and title.
2. `GarnetStatusCard` for read-only state; `ModernSetting*` rows for actions.
3. All sysfs/proc access goes through a repository and `Dispatchers.IO`.
4. Every page is gated by `KernelUtils.isKernel66()`.
5. No hardcoded user-facing text; add to `res/values/strings.xml`.

The last two features (Reality Display Engine and Unified Memory Manager) were
built with these rules. The open items above are intentionally left visible so
a later pass can migrate the legacy screens without mixing concerns.
