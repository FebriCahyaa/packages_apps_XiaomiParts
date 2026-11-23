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

## Kernel-6.6 hardening (P0)

Authored locally as `FEBRIAN RAHMAD CAHYA`:

- Replaced `BootCompletedReceiver`, `Startup` and `ApplyOnBootReceiver` with
  `GarnetBootReceiver`. Boot work is gated by `KernelUtils.isKernel66()` and
  runs on `Dispatchers.IO`. LOCKED_BOOT starts services, restores charging mode
  and overrides HDR types; BOOT_COMPLETED restores saturation, CPU cores and
  CPU/GPU settings.
- `ThermalService` and `RefreshService` now use `START_NOT_STICKY` and register
  their listeners once in `onCreate()`, so disabling thermal monitoring stays
  disabled and restarts cannot duplicate receivers.
- `KernelManagerUtils` no longer defaults to the absent `walt` governor or the
  msm8996 `691200` frequency. Defaults now come from each cluster's
  `scaling_available_governors` / `scaling_available_frequencies`, with
  `schedutil` as the fallback, and writes are clamped to the available range.
- Core control derives its core count from `/sys/devices/system/cpu/possible`,
  keeps at least two little and one big core online, persists the chosen state
  and restores it on boot.
- Saturation settings share `SaturationController` between the UI and boot
  restore instead of duplicating the SurfaceFlinger transaction code.
- The two internal services are no longer exported, and `targetSdkVersion` is
  35.

## Performance presets and diagnostics

Authored locally as `FEBRIAN RAHMAD CAHYA`:

- `PerformanceManager` applies battery/balanced/performance presets using only
  frequencies and governors exposed by the running 6.6 kernel. Each preset is
  saved into the existing CPU/GPU preferences and enables their apply-on-boot
  flags, so `GarnetBootReceiver` restores it without duplicate sysfs code.
- `PerformanceTileService` cycles the three presets from Quick Settings.
- `GpuSafety` suspends the GPU force-clock/bus/rail flags while the temperature
  guard is holding boost back and restores them after cooldown. Disabling the
  guard now clears the hot state instead of leaving hidden restrictions.
- `KernelInfoActivity` reports the running release, board, CPU governor, GPU
  model and which charging nodes are present, including the optional
  `garnet_charge_status` attribute from our kernel.

## Main-thread I/O cleanup

Authored locally as `FEBRIAN RAHMAD CAHYA`:

- Added `refreshIo`, `setBypassIo`, `setBoostIo`, `setForceAuthIo`,
  `setTempGuardIo` and `setRestoreOnBootIo` to `ChargingRepository`. Screens,
  Quick Settings tiles and the performance tile write sysfs on
  `Dispatchers.IO` and only update Compose/tile state back on the main thread.
- `ChargingService` keeps its retry timer on the main `Handler` but performs
  every node read/write in a cancelable IO scope. Unplugging now also restores
  GPU force-on flags that were suspended by the temperature guard.

## Reality Display Engine and Unified Memory Manager

Authored locally as `FEBRIAN RAHMAD CAHYA`:

- `display/RealityDisplayEngine.kt` applies five picture presets across
  SurfaceFlinger saturation, `peak_refresh_rate`, `min_refresh_rate` and HDR
  output. It reads `garnet_caps` and `dynamic_fps` from the Xiaomi DSI display
  module so unsupported effects are reported instead of assumed.
- `memory/UnifiedMemoryManager.kt` implements Tier 0-2 and Tier 4 of the UMM
  contract in `android_kernel_xiaomi_garnet_6.6/UMM.md`: it reports physical
  memory, LZ4 zram, VM reclaim and dmabuf usage, and applies reclaim profiles
  through `/proc/sys/vm`. Tier 3 (zram writeback to UFS 2.2) is read-only here
  and configured by the kernel's manual `garnet_umm_setup.sh`.
- Both features expose a Quick Settings tile and are restored from
  `GarnetBootReceiver` only on a 6.6 kernel.
- `ui/components/GarnetStatusCard.kt` is now shared by the charging, display,
  memory and performance pages. `docs/UI_AUDIT.md` records the remaining legacy
  screens.

## CPU and GPU clock optimisation

Authored locally as `FEBRIAN RAHMAD CAHYA`:

- `performance/GpuClockController.kt` reads `gpu_available_frequencies` and maps
  each preset to a devfreq min/max range. It never writes a frequency outside
  the driver table, keeps independent GPU bus votes on, leaves clock forcing
  off, and refuses the performance floor above 70 °C GPU temperature.
- `performance/PerformanceManager.kt` keeps `schedutil` on both CPU clusters.
  The performance profile raises the CPU floor to the 60th percentile of each
  advertised table instead of pinning the static `performance` governor, which
  would hold the top OPP while idle.
- `GpuManagerUtils.setGovernor()` is now an intentional no-op:
  `msm-adreno-tz` and `gpubw_mon` are `DEVFREQ_GOV_FLAG_IMMUTABLE` in this 6.6
  KGSL tree. `GpuManagerViewModel` no longer writes sysfs on the main thread.
- `performance/GpuStatusCard.kt` shows the live GPU clock, range, governor,
  power-level count and temperature so the user can see what the kernel
  actually permits.

UMM is our project policy name; it is not an upstream Linux/Android standard.
The app does not claim otherwise.

## Build limitation

No APK was produced in this environment: there is no AOSP checkout, Java or
Kotlin toolchain here. Verification was static (source review, sysfs node
cross-check against the kernel/modules trees, `git diff --check`).

## License

Apache-2.0, as declared by the upstream source files.
