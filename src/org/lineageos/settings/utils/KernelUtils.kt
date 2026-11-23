/*
 * Copyright (C) 2026 FEBRIAN RAHMAD CAHYA
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.utils

/**
 * This Garnet Parts build supports the 6.6 kernel only.
 *
 * Every sysfs node used by the app - qcom-battery, thermal_message, KGSL
 * devfreq and the cpufreq policy0/policy4 directories - is the one exported by
 * the 6.6 vendor drivers in android_kernel_xiaomi_garnet_6.6. Older kernels
 * expose overlapping node names with different semantics, so saved settings
 * must not be applied to them.
 */
object KernelUtils {

    /** True when the running kernel release starts with 6.6. */
    fun isKernel66(): Boolean =
        System.getProperty("os.version", "").trim().startsWith("6.6")
}
