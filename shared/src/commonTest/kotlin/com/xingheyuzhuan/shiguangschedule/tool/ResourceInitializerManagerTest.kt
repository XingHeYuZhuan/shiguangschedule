package com.xingheyuzhuan.shiguangschedule.tool

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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

    @Test
    fun firstInstallCopiesBundledPrimaryToBuiltInIndex() {
        val fileSystem = FakeFileSystem()
        val indexDirectory = "/repo/index".toPath()
        val primaryBytes = byteArrayOf(1, 2, 3)
        fileSystem.createDirectories(indexDirectory)
        fileSystem.write(indexDirectory / "school_index.pb") { write(primaryBytes) }

        ensureBuiltInIndexInstalled(fileSystem, indexDirectory)

        val installedBytes = fileSystem.read(indexDirectory / "builtin_school_index.pb") {
            readByteArray()
        }
        assertContentEquals(primaryBytes, installedBytes)
    }

    @Test
    fun explicitBuiltInIndexIsNeverReplacedByPrimary() {
        val fileSystem = FakeFileSystem()
        val indexDirectory = "/repo/index".toPath()
        val explicitBuiltInBytes = byteArrayOf(9, 8, 7)
        fileSystem.createDirectories(indexDirectory)
        fileSystem.write(indexDirectory / "school_index.pb") { write(byteArrayOf(1, 2, 3)) }
        fileSystem.write(indexDirectory / "builtin_school_index.pb") { write(explicitBuiltInBytes) }

        ensureBuiltInIndexInstalled(fileSystem, indexDirectory)

        val installedBytes = fileSystem.read(indexDirectory / "builtin_school_index.pb") {
            readByteArray()
        }
        assertContentEquals(explicitBuiltInBytes, installedBytes)
    }

    @Test
    fun bundledAdapterScriptsAreProtectedFromRepositoryUpdates() {
        val builtIn = index(
            version = "",
            schoolWithAssetPath("SEU", "seu_01.js"),
            schoolWithAssetPath("GLOBAL_TOOLS", "nested/tool.js")
        )

        assertEquals(
            setOf("SEU/seu_01.js", "GLOBAL_TOOLS/nested/tool.js"),
            builtInAdapterResourcePaths(builtIn)
        )
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

    private fun schoolWithAssetPath(id: String, assetPath: String) = School(
        id = id,
        name = id,
        initial = id.first().toString(),
        resource_folder = id,
        adapters = listOf(
            Adapter(adapter_id = "${id}_01", adapter_name = id, asset_js_path = assetPath)
        )
    )
}
