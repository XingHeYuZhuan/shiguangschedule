package com.xingheyuzhuan.shiguangschedule.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import school_index.Adapter
import school_index.School
import school_index.SchoolIndex

class SchoolRepositoryTest {
    @Test
    fun mergeSchoolIndexesKeepsRemoteEntriesAndOverlaysBuiltIns() {
        val primary = SchoolIndex(
            protocol_version = 2,
            version_id = "REMOTE_VERSION",
            schools = listOf(
                school("GLOBAL_TOOLS", "远程工具", adapter("REMOTE_TOOL", "远程"), adapter("SHARED", "远程版本")),
                school("REMOTE", "远程学校", adapter("REMOTE_01", "远程适配"))
            )
        )
        val builtIn = SchoolIndex(
            protocol_version = 2,
            version_id = "BUILTIN_VERSION",
            schools = listOf(
                school("GLOBAL_TOOLS", "内置工具", adapter("SHARED", "内置版本"), adapter("LOCAL_TOOL", "内置")),
                school("SEU", "东南大学", adapter("SEU_01", "东南大学课表"))
            )
        )

        val merged = mergeSchoolIndexes(primary, builtIn)

        assertEquals("REMOTE_VERSION", merged.version_id)
        assertEquals(listOf("GLOBAL_TOOLS", "REMOTE", "SEU"), merged.schools.map { it.id })
        assertEquals("内置工具", merged.schools.first().name)
        assertEquals(
            listOf("REMOTE_TOOL", "SHARED", "LOCAL_TOOL"),
            merged.schools.first().adapters.map { it.adapter_id }
        )
        assertEquals("内置版本", merged.schools.first().adapters[1].adapter_name)
    }

    private fun school(id: String, name: String, vararg adapters: Adapter) = School(
        id = id,
        name = name,
        initial = id.first().toString(),
        resource_folder = id,
        adapters = adapters.toList()
    )

    private fun adapter(id: String, name: String) = Adapter(
        adapter_id = id,
        adapter_name = name,
        asset_js_path = "$id.js"
    )
}
