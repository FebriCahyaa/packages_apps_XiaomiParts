# XiaomiParts — garnet, kernel 6.6 only

Garnet Parts for Xiaomi garnet / kera (SM7435, 4 KiB pages), built for the
Linux **6.6** kernel in `FebriCahyaa/android_kernel_xiaomi_garnet_6.6`.

This branch supports **kernel 6.6 only**. Saved kernel/GPU settings are not
applied when the running kernel release does not start with `6.6`, and the
GPU toggle for `force_no_nap` is removed because 6.6 KGSL marks it deprecated
(the node reads back `0` and writes fail with `EOPNOTSUPP`).

## Stable base

- Upstream: <https://github.com/xiaomi-sm7435-garnet/packages_apps_XiaomiParts>
- Upstream branch: `17`
- Base of this branch: `f4d8d19b01f7f6e9c59c1d4fc6ba082fd5d1a750`
  (`garnet: parts: Make it more Expressive`)
- Full upstream history, authors, `Change-Id` and `Signed-off-by` trailers are
  preserved. Local changes are integration commits authored by
  `FEBRIAN RAHMAD CAHYA`.

## Companion repositories

| Repository | Branch | Purpose |
|---|---|---|
| `FebriCahyaa/android_kernel_xiaomi_garnet_6.6` | `garnet-6.6` | Kernel (AOSP `android15-6.6-lts`, 6.6.162, release `6.6.162-ZairenOX-Oz1B`) |
| `FebriCahyaa/android_kernel_xiaomi_sm7435-modules` | `garnet-6.6` | Out-of-tree modules (KGSL, display, audio, camera, WLAN) |
| `FebriCahyaa/android_kernel_xiaomi_sm7435-devicetrees` | `garnet-6.6` | Garnet DTS/DTSI |

## Charging subsystem (this branch)

Bypass charging and fast charging now share one source of truth in
`src/org/lineageos/settings/charging/ChargingRepository.kt`:

- **Bypass charging** writes `qcom-battery/input_suspend`. It can be restored on
  boot and has a dedicated Quick Settings tile.
- **Fast charge boost** writes `qcom-battery/sport_mode` and
  `thermal_message/charger_temp` with the same retries and 42°C/40°C
  temperature guard as HyperOS, plus a Quick Settings tile.
- The two are mutually exclusive: enabling bypass clears the boost nodes, and
  enabling boost releases bypass. This avoids asking the charger for more
  current while its input is suspended.
- `ChargingService` is registered once, applies the profile at 0/2/5/12/25 s
  after plug-in, and clears the boost nodes on unplug.
- Status comes from `quick_charge_type`, `fastchg_mode`, `adapter_id`,
  `input_suspend`, `sport_mode` and the optional `garnet_charge_status` node
  added to our kernel driver.
- Experimental `pd_verifed` nudge remains opt-in.

## Features and their 6.6 kernel backing

| Feature | Sysfs node | Provided by |
|---|---|---|
| Bypass charging | `/sys/class/qcom-battery/input_suspend` | `drivers/power/supply/qti_battery_charger_xiaomi.c` |
| Charge boost | `/sys/class/qcom-battery/sport_mode`, `fastchg_mode`, `pd_verifed`, `/sys/class/thermal/thermal_message/charger_temp` | `qti_battery_charger_xiaomi.c`, `drivers/thermal/mi_thermal_interface.c` |
| Thermal profiles | `/sys/class/thermal/thermal_message/sconfig` | `drivers/thermal/mi_thermal_interface.c` |
| Core control | `/sys/devices/system/cpu/cpuN/online` | CPU hotplug core |
| CPU manager | `/sys/devices/system/cpu/cpufreq/policy{0,4}/...` | `qcom-cpufreq-hw`; `policy0` = little cluster, `policy4` = big cluster on SM7435 |
| GPU manager | `/sys/class/kgsl/kgsl-3d0/...` | `qcom/opensource/graphics-kernel` in the modules repo |

## Building

Check this repository into an AOSP tree (for example
`packages/apps/XiaomiParts`) and build with:

```sh
m XiaomiParts
```

The module is a privileged `system_ext` app signed with the platform key and
uses hidden APIs, so it must be part of the platform build. Make sure the
device tree does not also provide a second `XiaomiParts` module (some garnet
device trees embed one under `parts/`).

All startup work goes through `GarnetBootReceiver`, which refuses to apply any
saved setting when `System.getProperty("os.version")` is not a 6.6 release.

```xml
<!-- local_manifest example -->
<project path="packages/apps/XiaomiParts"
         name="FebriCahyaa/packages_apps_XiaomiParts"
         remote="febri" revision="garnet-6.6" />
```

## Sync with upstream

```sh
git fetch upstream
git rebase upstream/17
```

`origin` points to this fork; `upstream` points to `xiaomi-sm7435-garnet`.
Reapply the 6.6 adaptation commits after a rebase.

See `PROVENANCE.md` for source and compatibility details.
