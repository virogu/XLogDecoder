package com.virogu.decoder

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement

object ECDHUtils {

    init {
        //Security.addProvider(BouncyCastleProvider())
    }

    @Throws(Exception::class)
    fun loadPublicKey(data: ByteArray?): PublicKey {
        val params: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val pubKey = ECPublicKeySpec(
            params.curve.decodePoint(data), params
        )
        val kf = KeyFactory.getInstance("ECDH", "BC")
        return kf.generatePublic(pubKey)
    }

    @Throws(Exception::class)
    fun loadPrivateKey(data: ByteArray?): PrivateKey {
        val params: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val prvKey = ECPrivateKeySpec(BigInteger(1, data), params)
        val kf = KeyFactory.getInstance("ECDH", "BC")
        return kf.generatePrivate(prvKey)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getECDHKey(pubKey: ByteArray?, prvKey: ByteArray?): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH", "BC")
        val prvk = loadPrivateKey(prvKey)
        val pubk = loadPublicKey(pubKey)
        ka.init(prvk)
        ka.doPhase(pubk, true)
        //System.out.println(name + bytesToHex(secret));
        return ka.generateSecret()
    }
}