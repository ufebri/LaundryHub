package com.raylabs.laundryhub.e2e

import androidx.test.platform.app.InstrumentationRegistry

internal object LaundryHubE2eConfig {
    private const val ARG_MUTATING = "laundryhub.e2e.mutating"
    private const val ARG_TARGET = "laundryhub.e2e.target"
    private const val TARGET_SANDBOX = "sandbox"

    val mutatingSandboxEnabled: Boolean
        get() {
            val args = InstrumentationRegistry.getArguments()
            val mutating = args.getString(ARG_MUTATING).equals("true", ignoreCase = true)
            val target = args.getString(ARG_TARGET).equals(TARGET_SANDBOX, ignoreCase = true)
            return mutating && target
        }

    const val mutatingSandboxMessage: String =
        "Requires -e laundryhub.e2e.mutating true -e laundryhub.e2e.target sandbox"
}
