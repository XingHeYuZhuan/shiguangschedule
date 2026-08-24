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

                if (!forceOverwrite && fileSystem.exists(targetRepoDir / "index")) {
                    installBuiltInOverlay(zipFileSystem)
                    return@runCatching
                }

                if (fileSystem.exists(targetRepoDir)) {
                    fileSystem.deleteRecursively(targetRepoDir)
                }
                fileSystem.createDirectories(targetRepoDir)

                unzipDirectory(zipFileSystem, "/".toPath(), targetRepoDir)
            } finally {
                fileSystem.delete(tempZipFile)
            }
        }
    }

    /**
     * 升级安装时保留在线仓库，只覆盖随当前 App 发布的内置索引及其适配脚本。
     */
    private fun installBuiltInOverlay(zipFileSystem: FileSystem) {
        val bundledIndexPath = "/index/builtin_school_index.pb".toPath()
        if (!zipFileSystem.exists(bundledIndexPath)) return

        val targetIndexPath = targetRepoDir / "index" / "builtin_school_index.pb"
        copyZipFile(zipFileSystem, bundledIndexPath, targetIndexPath)

        val builtInIndex = zipFileSystem.source(bundledIndexPath).use { source ->
            SchoolIndex.ADAPTER.decode(source.buffer())
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
