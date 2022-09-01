package com.virogu.decoder

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

object CommonUtils {
    private fun charToByte(c: Char): Byte {
        return "0123456789ABCDEF".indexOf(c).toByte()
    }

    @JvmStatic
    fun hexStringToBytes(hex: String?): ByteArray? {
        var hexString = hex
        if (hexString == null || hexString == "") {
            return null
        }
        hexString = hexString.uppercase(Locale.getDefault())
        val length = hexString.length / 2
        val hexChars = hexString.toCharArray()
        val d = ByteArray(length)
        for (i in 0 until length) {
            val pos = i * 2
            d[i] = (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
        }
        return d
    }

    private fun containsHexPrefix(input: String): Boolean {
        return input.length > 1 && input[0] == '0' && input[1] == 'x'
    }

    private fun cleanHexPrefix(input: String): String {
        return if (containsHexPrefix(input)) {
            input.substring(2)
        } else {
            input
        }
    }

    @JvmStatic
    fun hexStringToByteArray(input: String): ByteArray {
        val cleanInput = cleanHexPrefix(input)
        val len = cleanInput.length
        if (len == 0) {
            return byteArrayOf()
        }
        val data: ByteArray
        val startIdx: Int
        if (len % 2 != 0) {
            data = ByteArray(len / 2 + 1)
            data[0] = (cleanInput[0].digitToIntOrNull(16) ?: -1).toByte()
            startIdx = 1
        } else {
            data = ByteArray(len / 2)
            startIdx = 0
        }
        var i = startIdx
        while (i < len) {
            data[(i + 1) / 2] =
                (((cleanInput[i].digitToIntOrNull(16) ?: (-1 shl 4)) + (cleanInput[i + 1].digitToIntOrNull(16)
                    ?: -1))).toByte()
            i += 2
        }
        return data
    }

    @JvmStatic
    fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("")
        if (src == null || src.isEmpty()) {
            return null
        }
        for (i in src.indices) {
            val v = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        return stringBuilder.toString()
    }

    private fun longToArray(x: Long, y: Long): ByteArray {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(x.toInt())
        buffer.putInt(y.toInt())
        return buffer.array()
    }

    private fun bytesToInt(b: ByteArray, offset: Int): Int {
        return b[offset].toInt() and 0xFF or (b[offset + 1].toInt() and 0xFF shl 8) or (b[offset + 2].toInt() and 0xFF shl 16) or (b[offset + 3].toInt() and 0xFF shl 24)
    }

    private fun teaDecipher(byte_array: ByteArray, k: ByteArray): ByteArray {
        val op = 0xffffffffL
        val delta = 0x9E3779B9L
        var s = delta shl 4 and op
        var v0 = bytesToInt(byte_array, 0).toLong() and 0x0FFFFFFFFL
        var v1 = bytesToInt(byte_array, 4).toLong() and 0x0FFFFFFFFL
        val k1 = bytesToInt(k, 0).toLong() and 0x0FFFFFFFFL
        val k2 = bytesToInt(k, 4).toLong() and 0x0FFFFFFFFL
        val k3 = bytesToInt(k, 8).toLong() and 0x0FFFFFFFFL
        val k4 = bytesToInt(k, 12).toLong() and 0x0FFFFFFFFL
        var cnt = 16
        while (cnt > 0) {
            v1 = v1 - ((v0 shl 4) + k3 xor v0 + s xor (v0 shr 5) + k4) and op
            v0 = v0 - ((v1 shl 4) + k1 xor v1 + s xor (v1 shr 5) + k2) and op
            s = s - delta and op
            cnt--
        }
        return longToArray(v0, v1)
    }

    @JvmStatic
    fun teaDecrypt(v: ByteArray, k: ByteArray): ByteArray {
        val num = v.size shr 3 shl 3
        val ret = ByteBuffer.allocate(v.size).order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i < num) {
            val sv = ByteArray(8)
            ByteBuffer.wrap(v, i, 8)[sv]
            val x = teaDecipher(sv, k)
            ret.put(x)
            i += 8
        }
        val remain = ByteArray(v.size - num)
        ByteBuffer.wrap(v, num, v.size - num)[remain]
        ret.put(remain)
        return ret.array()
    }

    @JvmStatic
    fun decompress(encData: ByteArray): ByteArray? {
        try {
            val bos = ByteArrayOutputStream()
            val zos = InflaterOutputStream(bos, Inflater(true))
            zos.write(encData)
            zos.close()
            return bos.toByteArray()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}