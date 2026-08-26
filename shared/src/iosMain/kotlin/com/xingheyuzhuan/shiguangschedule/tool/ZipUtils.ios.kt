package com.xingheyuzhuan.shiguangschedule.tool

import okio.Buffer

/**
 * iOS Zip writer used by local backup export.
 *
 * Kotlin/Native does not provide java.util.zip, so this writes a standards-compliant
 * ZIP archive with stored entries. Okio's ZipFileSystem can read it back on import.
 */
actual object ZipUtils {
    actual fun createZip(entries: Map<String, ByteArray>): ByteArray {
        if (entries.isEmpty()) return ByteArray(0)

        val output = Buffer()
        val centralDirectory = Buffer()
        var entryCount = 0

        entries.forEach { (fileName, bytes) ->
            val nameBytes = fileName.encodeToByteArray()
            require(nameBytes.size <= UShort.MAX_VALUE.toInt()) { "ZIP entry name is too long: $fileName" }

            val offset = output.size
            val crc32 = crc32(bytes)
            val size = bytes.size.toLong()
            require(size <= UInt.MAX_VALUE.toLong()) { "ZIP entry is too large: $fileName" }

            output.writeIntLe(LOCAL_FILE_HEADER_SIGNATURE)
            output.writeShortLe(VERSION_NEEDED)
            output.writeShortLe(GENERAL_PURPOSE_UTF8_FLAG)
            output.writeShortLe(STORE_METHOD)
            output.writeShortLe(DEFAULT_DOS_TIME)
            output.writeShortLe(DEFAULT_DOS_DATE)
            output.writeIntLe(crc32.toInt())
            output.writeIntLe(size.toInt())
            output.writeIntLe(size.toInt())
            output.writeShortLe(nameBytes.size)
            output.writeShortLe(0)
            output.write(nameBytes)
            output.write(bytes)

            centralDirectory.writeIntLe(CENTRAL_DIRECTORY_HEADER_SIGNATURE)
            centralDirectory.writeShortLe(VERSION_MADE_BY)
            centralDirectory.writeShortLe(VERSION_NEEDED)
            centralDirectory.writeShortLe(GENERAL_PURPOSE_UTF8_FLAG)
            centralDirectory.writeShortLe(STORE_METHOD)
            centralDirectory.writeShortLe(DEFAULT_DOS_TIME)
            centralDirectory.writeShortLe(DEFAULT_DOS_DATE)
            centralDirectory.writeIntLe(crc32.toInt())
            centralDirectory.writeIntLe(size.toInt())
            centralDirectory.writeIntLe(size.toInt())
            centralDirectory.writeShortLe(nameBytes.size)
            centralDirectory.writeShortLe(0)
            centralDirectory.writeShortLe(0)
            centralDirectory.writeShortLe(0)
            centralDirectory.writeShortLe(0)
            centralDirectory.writeIntLe(0)
            centralDirectory.writeIntLe(offset.toInt())
            centralDirectory.write(nameBytes)

            entryCount++
        }

        val centralDirectoryOffset = output.size
        val centralDirectorySize = centralDirectory.size
        require(entryCount <= UShort.MAX_VALUE.toInt()) { "ZIP contains too many entries" }
        require(centralDirectoryOffset <= UInt.MAX_VALUE.toLong()) { "ZIP archive is too large" }
        require(centralDirectorySize <= UInt.MAX_VALUE.toLong()) { "ZIP central directory is too large" }

        output.writeAll(centralDirectory)
        output.writeIntLe(END_OF_CENTRAL_DIRECTORY_SIGNATURE)
        output.writeShortLe(0)
        output.writeShortLe(0)
        output.writeShortLe(entryCount)
        output.writeShortLe(entryCount)
        output.writeIntLe(centralDirectorySize.toInt())
        output.writeIntLe(centralDirectoryOffset.toInt())
        output.writeShortLe(0)

        return output.readByteArray()
    }
}

private const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50
private const val CENTRAL_DIRECTORY_HEADER_SIGNATURE = 0x02014b50
private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
private const val VERSION_NEEDED = 20
private const val VERSION_MADE_BY = 20
private const val GENERAL_PURPOSE_UTF8_FLAG = 0x0800
private const val STORE_METHOD = 0
private const val DEFAULT_DOS_TIME = 0
private const val DEFAULT_DOS_DATE = 0
private const val CRC32_POLYNOMIAL = 0xedb88320u

private fun crc32(bytes: ByteArray): UInt {
    var crc = 0xffffffffu
    bytes.forEach { byte ->
        crc = crc xor byte.toUByte().toUInt()
        repeat(8) {
            crc = if ((crc and 1u) != 0u) {
                (crc shr 1) xor CRC32_POLYNOMIAL
            } else {
                crc shr 1
            }
        }
    }
    return crc xor 0xffffffffu
}
