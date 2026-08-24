package com.xingheyuzhuan.shiguangschedule.tool

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import school_index.Adapter
import school_index.School
import school_index.SchoolIndex

class ResourceInitializerManagerTest {
    @Test
    fun legacyTimestampedBundledPrimaryIsReplaced() {
        val installed = index(
            version = "TIME_20260824051407_920",
            school("GLOBAL_TOOLS", "GENERAL_TOOL_01"),
            school("SEU", "SEU_01")
        )
        val builtIn = index(
            version = "",
            school("GLOBAL_TOOLS", "GENERAL_TOOL_01"),
            school("SEU", "SEU_01")
        )

        assertTrue(shouldReplaceLegacyBundledPrimary(installed, builtIn))
    }

    @Test
    fun downloadedOrCustomPrimaryIsPreserved() {
        val builtIn = index(version = "", school("SEU", "SEU_01"))
        val downloaded = index(
            version = "TIME_20260824042255_851",
            school("SEU", "SEU_01"),
            school("REMOTE", "REMOTE_01")
        )
        val alreadyMigrated = index(version = "", school("SEU", "SEU_01"))

        assertFalse(shouldReplaceLegacyBundledPrimary(downloaded, builtIn))
        assertFalse(shouldReplaceLegacyBundledPrimary(alreadyMigrated, builtIn))
    }

    private fun index(version: String, vararg schools: School) = SchoolIndex(
        protocol_version = 2,
        version_id = version,
        schools = schools.toList()
    )

    private fun school(id: String, vararg adapterIds: String) = School(
        id = id,
        name = id,
        initial = id.first().toString(),
        resource_folder = id,
        adapters = adapterIds.map { adapterId ->
            Adapter(adapter_id = adapterId, adapter_name = adapterId, asset_js_path = "$adapterId.js")
        }
    )
}
