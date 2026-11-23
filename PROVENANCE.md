# Provenance — XiaomiParts for garnet 6.6

## Source

- Repository: <https://github.com/xiaomi-sm7435-garnet/packages_apps_XiaomiParts>
- Branch: `17`
- Branch tip used: `f4d8d19b01f7f6e9c59c1d4fc6ba082fd5d1a750`
  (`garnet: parts: Make it more Expressive`, Alhaidar Latif, 2026-11-03)
- `garnet-6.6` is created directly from `upstream/17`. No upstream commit was
  squashed, reordered or rewritten; original authors, copyrights and trailers
  are intact.
- `origin` = this fork, `upstream` = `xiaomi-sm7435-garnet`.

## Kernel 6.6 adaptations

Authored locally as `FEBRIAN RAHMAD CAHYA`, because they target our own 6.6
kernel rather than the community 5.10 tree:

- Removed the GPU `force_no_nap` toggle from `GpuManagerUtils.kt`,
  `GpuManagerViewModel.kt`, `GpuManagerScreen.kt`, `ApplyOnBootReceiver.kt`,
  `res/xml/gpu_manager_settings.xml` and `res/values/strings.xml`. In 6.6 KGSL
  (`qcom/opensource/graphics-kernel/kgsl_pwrctrl.c`) the node is deprecated:
  `force_no_nap_show()` always returns `0` and `force_no_nap_store()` returns
  `-EOPNOTSUPP`.
- Added `src/org/lineageos/settings/utils/KernelUtils.kt`, which reports
  whether `System.getProperty("os.version")` starts with `6.6`.
- `ApplyOnBootReceiver` now skips applying saved kernel/GPU settings on a
  non-6.6 kernel, so overlapping sysfs node names on older kernels cannot be
  written with 6.6 semantics.

## Verified 6.6 node compatibility

All sysfs paths used by the app exist in `android_kernel_xiaomi_garnet_6.6`
branch `garnet-6.6`:

- `input_suspend`, `sport_mode`, `fastchg_mode`, `pd_verifed` —
  `drivers/power/supply/qti_battery_charger_xiaomi.c`
- `sconfig`, `charger_temp` — `drivers/thermal/mi_thermal_interface.c`
  (`thermal_message` class)
- KGSL GPU nodes — `qcom/opensource/graphics-kernel` in
  `android_kernel_xiaomi_sm7435-modules`
- `policy0` / `policy4` cpufreq directories — `qcom,freq-domain` in
  `qcom/parrot.dtsi` (four little + four big CPUs)

The 6.6 thermal driver accepts any integer for `sconfig`, so the profile
values `0, 4, 8, 9, 11, 12, 19, 20, 21` remain valid.

## Charging subsystem integration

Authored locally as `FEBRIAN RAHMAD CAHYA`:

- Replaced `ChargeUtils`, `ChargeBoostUtils` and `ChargeBoostService` with
  `charging/ChargingRepository.kt` and `charging/ChargingService.kt`.
  Bypass and boost are now mutually exclusive, both have Quick Settings tiles,
  the boost profile is re-applied at 0/2/5/12/25 s after plug-in and cleared on
  unplug, and the temperature guard has a 42°C/40°C hysteresis.
- `ChargingStatusCard` reports the negotiated `quick_charge_type`,
  `fastchg_mode`, adapter ID, battery temperature and running kernel release.
- The app prefers `/sys/class/qcom-battery/garnet_charge_status`, a read-only
  attribute added to `qti_battery_charger_xiaomi.c` in our kernel, and falls
  back to the individual nodes when it is absent.
- Charging state is restored on boot only from the single boot receiver and
  only when `KernelUtils.isKernel66()` is true.

Nodes were cross-checked against `qti_battery_charger_xiaomi.c` on branch
`garnet-6.6`; `quick_charge_type` values follow the driver enum
`NORMAL/FAST/FLASH/TURBE/SUPER/MAX`.

## Build limitation

No APK was produced in this environment: there is no AOSP checkout, Java or
Kotlin toolchain here. Verification was static (source review, sysfs node
cross-check against the kernel/modules trees, `git diff --check`).

## License

Apache-2.0, as declared by the upstream source files.
