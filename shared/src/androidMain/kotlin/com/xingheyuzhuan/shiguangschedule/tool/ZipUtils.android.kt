package com.xingheyuzhuan.shiguangschedule.tool

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual object ZipUtils {
    actual fun createZip(entries: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos.buffered()).use { zos ->
            entries.forEach { (filename, bytes) ->
                zos.putNextEntry(ZipEntry(filename))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}