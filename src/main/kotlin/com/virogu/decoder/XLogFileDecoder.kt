package com.virogu.decoder

import com.virogu.decoder.CommonUtils.bytesToHexString
import com.virogu.decoder.CommonUtils.decompress
import com.virogu.decoder.CommonUtils.hexStringToByteArray
import com.virogu.decoder.CommonUtils.hexStringToBytes
import com.virogu.decoder.CommonUtils.teaDecrypt
import com.virogu.decoder.ECDHUtils.getECDHKey
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class RetData(var startpos: Int, var lastseq: Int)

@Suppress("unused", "UNUSED_VARIABLE")
object XLogFileDecoder {
    private var MAGIC_NO_COMPRESS_START: Byte = 0x03
    private var MAGIC_NO_COMPRESS_START1: Byte = 0x06
    private var MAGIC_NO_COMPRESS_NO_CRYPT_START: Byte = 0x08
    private var MAGIC_COMPRESS_START: Byte = 0x04
    private var MAGIC_COMPRESS_START1: Byte = 0x05
    private var MAGIC_COMPRESS_START2: Byte = 0x07
    private var MAGIC_COMPRESS_NO_CRYPT_START: Byte = 0x09
    private var MAGIC_END: Byte = 0x00
    private var PRI_KEY = "145aa7717bf9745b91e9569b80bbf1eedaa6cc6cd0e26317d810e35710f44cf8"
    private var PUB_KEY =
        "572d1e2710ae5fbca54c76a382fdd44050b3a675cb2bf39feebe85ef63d947aff0fa4943f1112e8b6af34bebebbaefa1a0aae055d9259b89a1858f7cc9af9df1"
    private var BYTE_PRIV_KEY = hexStringToBytes(PRI_KEY)

    private fun isGoodLogBuffer(_buffer: ByteArray, _offset: Int, count: Int): Boolean {
        if (_offset == _buffer.size) return true
        val cryptKeyLen: Int
        val magicStart = _buffer[_offset]
        cryptKeyLen =
            if (MAGIC_NO_COMPRESS_START == magicStart || MAGIC_COMPRESS_START == magicStart || MAGIC_COMPRESS_START1 == magicStart) {
                4
            } else if (MAGIC_COMPRESS_START2 == magicStart || MAGIC_NO_COMPRESS_START1 == magicStart || MAGIC_NO_COMPRESS_NO_CRYPT_START == magicStart || MAGIC_COMPRESS_NO_CRYPT_START == magicStart) {
                64
            } else {
                System.out.printf("_buffer[%d]:%d != MAGIC_NUM_START%n", _offset, _buffer[_offset])
                return false
            }
        val headerLen = 1 + 2 + 1 + 1 + 4 + cryptKeyLen
        if (_offset + headerLen + 1 + 1 > _buffer.size) {
            System.out.printf("offset:%d > len(buffer):%d%n", _offset, _buffer.size)
            return false
        }
        val length =
            ByteBuffer.wrap(_buffer, _offset + headerLen - 4 - cryptKeyLen, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val llength = length.toLong() and 0xffffffffL
        if (_offset + headerLen + llength + 1 > _buffer.size) {
            System.out.printf(
                "log length:%d, end pos %d > len(buffer):%d%n", llength, _offset + headerLen + llength + 1, _buffer.size
            )
            return false
        }
        if (MAGIC_END != _buffer[_offset + headerLen + length]) {
            System.out.printf(
                "log length:%d, buffer[%d]:%d != MAGIC_END%n",
                length,
                _offset + headerLen + length,
                _buffer[_offset + headerLen + length]
            )
            return false
        }
        return if (1 >= count) {
            true
        } else {
            isGoodLogBuffer(_buffer, _offset + headerLen + length + 1, count - 1)
        }
    }

    private fun getLogStartPos(_buffer: ByteArray, _count: Int): Int {
        var offset = 0
        while (offset < _buffer.size) {
            if (MAGIC_NO_COMPRESS_START == _buffer[offset] || MAGIC_NO_COMPRESS_START1 == _buffer[offset] || MAGIC_COMPRESS_START == _buffer[offset] || MAGIC_COMPRESS_START1 == _buffer[offset] || MAGIC_COMPRESS_START2 == _buffer[offset] || MAGIC_COMPRESS_NO_CRYPT_START == _buffer[offset] || MAGIC_NO_COMPRESS_NO_CRYPT_START == _buffer[offset]) {
                if (isGoodLogBuffer(_buffer, offset, _count)) return offset
            }
            offset += 1
        }
        return -1
    }

    @Suppress("KotlinConstantConditions")
    private fun decodeBuffer(_buffer: ByteArray, _offset: Int, lastSeq: Int, _outBuffer: StringBuffer): RetData {
        var offset = _offset
        val retData = RetData(offset, lastSeq)
        if (offset >= _buffer.size) return RetData(-1, lastSeq)
        val ret = isGoodLogBuffer(_buffer, offset, 1)
        var tmpbuffer = ByteArray(_buffer.size - offset)
        if (!ret) {
            System.arraycopy(_buffer, offset, tmpbuffer, 0, tmpbuffer.size)
            val fixPos = getLogStartPos(tmpbuffer, 1)
            offset += if (-1 == fixPos) {
                return RetData(-1, lastSeq)
            } else {
                _outBuffer.append(String.format("[F]decode_log_file.py decode error len=%d, result:%s \n", fixPos, ret))
                fixPos
            }
        }
        val cryptKeyLen: Int = when (val magicStart = _buffer[offset].toInt()) {
            MAGIC_NO_COMPRESS_START.toInt(), MAGIC_COMPRESS_START.toInt(), MAGIC_COMPRESS_START1.toInt() -> {
                4
            }
            MAGIC_COMPRESS_START2.toInt(), MAGIC_NO_COMPRESS_START1.toInt(), MAGIC_NO_COMPRESS_NO_CRYPT_START.toInt(), MAGIC_COMPRESS_NO_CRYPT_START.toInt() -> {
                64
            }
            else -> {
                _outBuffer.append("in DecodeBuffer _buffer[%d]:%d != MAGIC_NUM_START", offset, magicStart)
                return RetData(-1, lastSeq)
            }
        }
        val headerLen = 1 + 2 + 1 + 1 + 4 + cryptKeyLen
        val length =
            ByteBuffer.wrap(_buffer, offset + headerLen - 4 - cryptKeyLen, 4).order(ByteOrder.LITTLE_ENDIAN).int
        tmpbuffer = ByteArray(length)
        val seq = ByteBuffer.wrap(_buffer, offset + headerLen - 4 - cryptKeyLen - 2 - 2, 2)
            .order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0x0FFFF
        val beginHour = Char(
            ByteBuffer.wrap(_buffer, offset + headerLen - 4 - cryptKeyLen - 1 - 1, 1).order(ByteOrder.LITTLE_ENDIAN)
                .get().toUShort()
        )
        val endHour = Char(
            ByteBuffer.wrap(_buffer, offset + headerLen - 4 - cryptKeyLen - 1, 1).order(ByteOrder.LITTLE_ENDIAN).get()
                .toUShort()
        )
        if (seq != 0 && seq != 1 && lastSeq != 0 && seq != (lastSeq + 1)) {
            _outBuffer.append(String.format("[F]decode_log_file.py log seq:%d-%d is missing\n", lastSeq + 1, seq - 1))
        }
        if (seq != 0) {
            retData.lastseq = seq
        }
        System.arraycopy(_buffer, offset + headerLen, tmpbuffer, 0, tmpbuffer.size)
        try {
            when {
                MAGIC_NO_COMPRESS_START1 == _buffer[offset] -> {

                }
                MAGIC_COMPRESS_START2 == _buffer[offset] -> {
                    val bytePubKeyX = ByteArray(32)
                    val bytePubKeyY = ByteArray(32)
                    ByteBuffer.wrap(_buffer, offset + headerLen - cryptKeyLen, cryptKeyLen shr 1)
                        .order(ByteOrder.LITTLE_ENDIAN)[bytePubKeyX]
                    ByteBuffer.wrap(_buffer, offset + headerLen - (cryptKeyLen shr 1), cryptKeyLen shr 1)
                        .order(ByteOrder.LITTLE_ENDIAN)[bytePubKeyY]
                    val pubKeyX = bytesToHexString(bytePubKeyX)
                    val pubKeyY = bytesToHexString(bytePubKeyY)
                    val pubKey = String.format("04%s%s", pubKeyX, pubKeyY)
                    val teaKey = getECDHKey(hexStringToByteArray(pubKey), BYTE_PRIV_KEY)
                    tmpbuffer = teaDecrypt(tmpbuffer, teaKey)
                    tmpbuffer = decompress(tmpbuffer)!!
                }
                MAGIC_COMPRESS_START == _buffer[offset] || MAGIC_COMPRESS_NO_CRYPT_START == _buffer[offset] -> {
                    tmpbuffer = decompress(tmpbuffer)!!
                }
                MAGIC_COMPRESS_START1 == _buffer[offset] -> {
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _outBuffer.append(String.format("[F]decode_log_file.py decompress err, %s\n", e.toString()))
            retData.startpos = offset + headerLen + length + 1
            return retData
        }
        _outBuffer.append(String(tmpbuffer))
        retData.startpos = offset + headerLen + length + 1
        return retData
    }

    private fun parseFileImpl(_file: String, _outfile: String) {
        var fis: FileInputStream? = null
        var dis: DataInputStream? = null
        var os: OutputStream? = null
        var writer: OutputStreamWriter? = null
        var bw: BufferedWriter? = null
        try {
            fis = FileInputStream(_file)
            dis = DataInputStream(fis)
            val buffer = ByteArray(dis.available())
            dis.readFully(buffer)
            val startPos = getLogStartPos(buffer, 2)
            if (-1 == startPos) {
                return
            }
            val outbuffer = StringBuffer()
            var retData = RetData(startPos, 0)
            do {
                //System.out.println(retData.startpos + ":" + retData.lastseq);
                retData = decodeBuffer(buffer, retData.startpos, retData.lastseq, outbuffer)
            } while (-1 != retData.startpos)
            if (outbuffer.isEmpty()) return
            os = FileOutputStream(_outfile)
            writer = OutputStreamWriter(os)
            bw = BufferedWriter(writer)
            bw.write(outbuffer.toString())
            bw.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fis?.close()
                dis?.close()
                os?.close()
                writer?.close()
                bw?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun parseFile(_file: String, _outfile: String): Boolean {
        parseFileImpl(_file, _outfile)
        return File(_outfile).exists()
    }

    fun parseFile(_file: File, _outfile: File): Boolean {
        return parseFile(_file.absolutePath, _outfile.absolutePath)
    }
}