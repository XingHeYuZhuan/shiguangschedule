package com.xingheyuzhuan.shiguangschedule.tool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.NSObject
import platform.posix.memcpy

private class IosFileManager(
    private val callbacks: () -> FileManagerCallbacks
) : FileManager {
    private var imagePickerDelegate: IosImagePickerDelegate? = null
    private var importPickerDelegate: IosDocumentImportDelegate? = null
    private var exportPickerDelegate: IosDocumentExportDelegate? = null

    override fun pickImage() {
        val viewController = currentViewController() ?: run {
            callbacks().onImagePicked?.invoke(null)
            return
        }

        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter()
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration)
        val delegate = IosImagePickerDelegate(
            onComplete = { image ->
                callbacks().onImagePicked?.invoke(image)
                imagePickerDelegate = null
            }
        )

        imagePickerDelegate = delegate
        picker.delegate = delegate
        viewController.presentViewController(picker, animated = true, completion = null)
    }

    override fun importFile(allowedExtensions: List<String>) {
        val viewController = currentViewController() ?: run {
            callbacks().onFileImported?.invoke(null, null)
            return
        }

        val documentTypes = allowedExtensions
            .mapNotNull { extensionToType(it) }
            .ifEmpty { listOf(UTTypeData) }
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = documentTypes)
        val delegate = IosDocumentImportDelegate(
            onComplete = { bytes, fileName ->
                callbacks().onFileImported?.invoke(bytes, fileName)
                importPickerDelegate = null
            }
        )

        importPickerDelegate = delegate
        picker.setDelegate(delegate)
        viewController.presentViewController(picker, animated = true, completion = null)
    }

    override fun exportFile(defaultFileName: String, bytes: ByteArray) {
        val viewController = currentViewController() ?: run {
            callbacks().onFileExported?.invoke(false)
            return
        }

        val fileUrl = writeTemporaryFile(defaultFileName, bytes) ?: run {
            callbacks().onFileExported?.invoke(false)
            return
        }
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(fileUrl))
        val delegate = IosDocumentExportDelegate(
            onComplete = { success ->
                callbacks().onFileExported?.invoke(success)
                exportPickerDelegate = null
            }
        )

        exportPickerDelegate = delegate
        picker.setDelegate(delegate)
        viewController.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberFileManager(callbacks: FileManagerCallbacks): FileManager {
    val currentCallbacks = rememberUpdatedState(callbacks)
    return remember { IosFileManager { currentCallbacks.value } }
}

private class IosImagePickerDelegate(
    private val onComplete: (androidx.compose.ui.graphics.ImageBitmap?) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: run {
            onComplete(null)
            return
        }
        val provider = result.itemProvider
        provider.loadFirstImageData { data ->
            onComplete(data?.toImageBitmap())
        }
    }
}

private class IosDocumentImportDelegate(
    private val onComplete: (ByteArray?, String?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: run {
            onComplete(null, null)
            return
        }
        val hasAccess = url.startAccessingSecurityScopedResource()
        try {
            val path = url.path
            val data = path?.let { NSFileManager.defaultManager.contentsAtPath(it) }
            onComplete(data?.toByteArray(), url.lastPathComponent)
        } finally {
            if (hasAccess) url.stopAccessingSecurityScopedResource()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onComplete(null, null)
    }
}

private class IosDocumentExportDelegate(
    private val onComplete: (Boolean) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onComplete(true)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onComplete(false)
    }
}

private fun NSItemProvider.loadFirstImageData(onComplete: (NSData?) -> Unit) {
    val typeIdentifier = registeredTypeIdentifiers.firstOrNull() as? String ?: run {
        onComplete(null)
        return
    }
    loadDataRepresentationForTypeIdentifier(typeIdentifier) { data: NSData?, _: NSError? ->
        runOnMain { onComplete(data) }
    }
}

private fun runOnMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) {
        block()
    }
}

private fun currentViewController(): UIViewController? {
    val windowScene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { it is UIWindowScene } as? UIWindowScene
    val root = windowScene?.windows
        .orEmpty()
        .mapNotNull { it as? UIWindow }
        .firstOrNull { it.isKeyWindow() }
        ?.rootViewController

    return root?.topPresentedViewController()
}

private fun UIViewController.topPresentedViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

private fun extensionToType(extension: String): UTType? =
    UTType.typeWithFilenameExtension(extension.lowercase().trimStart('.'))

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return bytes
}

private fun NSData.toImageBitmap(): androidx.compose.ui.graphics.ImageBitmap? =
    runCatching { Image.makeFromEncoded(toByteArray()).toComposeImageBitmap() }.getOrNull()

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun writeTemporaryFile(fileName: String, bytes: ByteArray): NSURL? {
    val safeFileName = fileName.ifBlank { "export.dat" }
    val path = NSTemporaryDirectory() + safeFileName
    val url = NSURL.fileURLWithPath(path)
    val data = if (bytes.isEmpty()) {
        NSData()
    } else {
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }

    return if (NSFileManager.defaultManager.createFileAtPath(path, data, attributes = null)) url else null
}
