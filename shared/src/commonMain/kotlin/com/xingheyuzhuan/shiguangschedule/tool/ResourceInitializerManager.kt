package com.xingheyuzhuan.shiguangschedule.tool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import school_index.SchoolIndex
import shiguangschedule.shared.generated.resources.Res

/**
 * 旧版直接构建曾把仅含内置学校的索引写成 TIME_* 主索引，导致它可能比远程仓库
 * 更新，从而永久阻断第一次同步。仅当学校/适配器布局与当前内置索引完全一致时迁移，
 * 避免覆盖已经下载过的完整或自定义仓库索引。
 */
internal fun shouldReplaceLegacyBundledPrimary(
    installed: SchoolIndex,
    builtIn: SchoolIndex
): Boolean {
    if (!installed.version_id.startsWith("TIME_") || builtIn.version_id.isNotBlank()) return false

    fun SchoolIndex.adapterLayout(): Map<String, Set<String>> = schools.associate { school ->
        school.id to school.adapters.map { it.adapter_id }.toSet()
    }

    return installed.adapterLayout() == builtIn.adapterLayout()
}

/**
 * 直接从源码构建的安装包只有 school_index.pb，没有 Release 工作流额外生成的
 * builtin_school_index.pb。首次解压时补一份不可被在线更新覆盖的内置索引，确保
 * 后续 GitUpdater 替换主索引后仍能叠加随 App 发布的适配器。
 */
internal fun ensureBuiltInIndexInstalled(
    fileSystem: FileSystem,
    indexDirectory: Path
) {
    val primaryPath = indexDirectory / "school_index.pb"
    val builtInPath = indexDirectory / "builtin_school_index.pb"
    if (!fileSystem.exists(builtInPath) && fileSystem.exists(primaryPath)) {
        fileSystem.copy(primaryPath, builtInPath)
    }
}

/**
 * 资源与缓存初始化管理器。
 *
 * 负责应用启动时的静态离线资源解压校验，以及过期的分享与下载临时缓存清理。
 */
@Suppress("unused")
@Single(createdAtStart = true)
class ResourceInitializerManager(
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path,
    @Named("CacheDir") private val cacheDir: Path
) {
    private val targetRepoDir: Path = filesDir / "repo"
    private val shareTempDir: Path = cacheDir / "share_temp"

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeOfflineRepo()
            clearTempCaches()
        }
    }

    /**
     * 校验并解压内置离线适配仓库。
     *
     * @param forceOverwrite 是否强制清除并重新解压覆盖现有本地仓库
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun initializeOfflineRepo(forceOverwrite: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val zipBytes = Res.readBytes("files/offline_schools.zip")
            val tempZipFile = filesDir / "temp_offline_schools.zip"

            fileSystem.write(tempZipFile) {
                write(zipBytes)
            }

            try {
                val zipFileSystem = fileSystem.openZip(tempZipFile)

                if (!forceOverwrite && fileSystem.exists(targetRepoDir / "index" / "school_index.pb")) {
                    installBuiltInOverlay(zipFileSystem)
                    return@runCatching
                }

                if (fileSystem.exists(targetRepoDir)) {
                    fileSystem.deleteRecursively(targetRepoDir)
                }
                fileSystem.createDirectories(targetRepoDir)

                unzipDirectory(zipFileSystem, "/".toPath(), targetRepoDir)
                ensureBuiltInIndexInstalled(fileSystem, targetRepoDir / "index")
            } finally {
                fileSystem.delete(tempZipFile)
            }
        }
    }

    /**
     * 升级安装时保留已下载的仓库，只覆盖当前 App 内置的索引和适配脚本。
     * Release 包使用显式的 builtin 索引；本地 Debug 包可直接使用包内主索引作为内置叠加层。
     */
    private fun installBuiltInOverlay(zipFileSystem: FileSystem) {
        val explicitBuiltInPath = "/index/builtin_school_index.pb".toPath()
        val bundledPrimaryPath = "/index/school_index.pb".toPath()
        val bundledIndexPath = when {
            zipFileSystem.exists(explicitBuiltInPath) -> explicitBuiltInPath
            zipFileSystem.exists(bundledPrimaryPath) -> bundledPrimaryPath
            else -> return
        }

        val targetIndexPath = targetRepoDir / "index" / "builtin_school_index.pb"
        copyZipFile(zipFileSystem, bundledIndexPath, targetIndexPath)

        val builtInIndex = zipFileSystem.source(bundledIndexPath).use { source ->
            SchoolIndex.ADAPTER.decode(source.buffer())
        }

        // Release 包包含显式 builtin 索引，主索引来自远程仓库，不需要迁移。
        // 直接构建只有 bundledPrimaryPath，需要修复旧版带 TIME_* 的精简主索引。
        if (bundledIndexPath == bundledPrimaryPath) {
            migrateLegacyBundledPrimary(zipFileSystem, bundledPrimaryPath, builtInIndex)
        }

        val resourceRoot = "/schools/resources".toPath()

        builtInIndex.schools.forEach { school ->
            school.adapters.forEach { adapter ->
                val relativePath = "${school.resource_folder}/${adapter.asset_js_path}"
                    .replace('\\', '/')
                if (relativePath.startsWith('/') || relativePath.split('/').any { it == ".." }) {
                    throw IllegalArgumentException("Illegal built-in resource path: $relativePath")
                }

                val sourcePath = resourceRoot / school.resource_folder / adapter.asset_js_path
                if (!zipFileSystem.exists(sourcePath)) {
                    throw IllegalStateException("Built-in adapter resource not found: $sourcePath")
                }

                val targetPath = targetRepoDir / "schools" / "resources" /
                    school.resource_folder / adapter.asset_js_path
                copyZipFile(zipFileSystem, sourcePath, targetPath)
            }
        }
    }

    private fun migrateLegacyBundledPrimary(
        zipFileSystem: FileSystem,
        bundledPrimaryPath: Path,
        builtInIndex: SchoolIndex
    ) {
        val installedPrimaryPath = targetRepoDir / "index" / "school_index.pb"
        val installedIndex = runCatching {
            fileSystem.source(installedPrimaryPath).use { source ->
                SchoolIndex.ADAPTER.decode(source.buffer())
            }
        }.getOrNull() ?: return

        if (shouldReplaceLegacyBundledPrimary(installedIndex, builtInIndex)) {
            copyZipFile(zipFileSystem, bundledPrimaryPath, installedPrimaryPath)
        }
    }

    private fun copyZipFile(zipFileSystem: FileSystem, sourcePath: Path, targetPath: Path) {
        targetPath.parent?.let { fileSystem.createDirectories(it) }
        zipFileSystem.source(sourcePath).use { source ->
            fileSystem.sink(targetPath).buffer().use { sink ->
                sink.writeAll(source)
            }
        }
    }

    /**
     * 清理应用生成的临时文件与过期缓存目录。
     */
    suspend fun clearTempCaches(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (fileSystem.exists(shareTempDir)) {
                fileSystem.deleteRecursively(shareTempDir)
            }

            val tempSchoolsRepo = cacheDir / "temp_schools_repo"
            val tempIndexRepo = cacheDir / "temp_index_repo"
            if (fileSystem.exists(tempSchoolsRepo)) fileSystem.deleteRecursively(tempSchoolsRepo)
            if (fileSystem.exists(tempIndexRepo)) fileSystem.deleteRecursively(tempIndexRepo)
        }
    }

    /**
     * 递归解压 Zip 虚拟文件系统中的目录与文件。
     */
    private fun unzipDirectory(
        zipFileSystem: FileSystem,
        currentZipPath: Path,
        targetDir: Path
    ) {
        val entries = zipFileSystem.list(currentZipPath)
        for (entry in entries) {
            val destinationPath = targetDir / entry.name

            if (!destinationPath.toString().startsWith(targetDir.toString())) {
                throw IllegalArgumentException("Illegal zip path: ${entry.name}")
            }

            val metadata = zipFileSystem.metadata(entry)

            if (metadata.isDirectory) {
                fileSystem.createDirectories(destinationPath)
                unzipDirectory(zipFileSystem, entry, destinationPath)
            } else {
                copyZipFile(zipFileSystem, entry, destinationPath)
            }
        }
    }
}
