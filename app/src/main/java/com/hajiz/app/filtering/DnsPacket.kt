package com.hajiz.app.filtering

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DnsQuestion(
    val transactionId: ByteArray,
    val payload: ByteArray,
    val questionName: String?,
    val sourceAddress: ByteArray,
    val destinationAddress: ByteArray,
    val sourcePort: Int,
    val destinationPort: Int,
)

object DnsPacket {
    fun parseIpv4Udp(packet: ByteArray): DnsQuestion? {
        if (packet.size < 28 || (packet[0].toInt() ushr 4) != 4) return null
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (headerLength < 20 || packet.size < headerLength + 8) return null
        if (packet[9].toInt() and 0xFF != 17) return null
        val udpStart = headerLength
        val sourcePort = unsignedShort(packet, udpStart)
        val destinationPort = unsignedShort(packet, udpStart + 2)
        if (destinationPort != 53) return null
        val udpLength = unsignedShort(packet, udpStart + 4)
        val payloadStart = udpStart + 8
        val payloadEnd = minOf(packet.size, udpStart + udpLength)
        if (payloadEnd <= payloadStart + 12) return null
        val payload = packet.copyOfRange(payloadStart, payloadEnd)
        val questionName = readQuestionName(payload)
        return DnsQuestion(
            transactionId = payload.copyOfRange(0, 2),
            payload = payload,
            questionName = questionName,
            sourceAddress = packet.copyOfRange(12, 16),
            destinationAddress = packet.copyOfRange(16, 20),
            sourcePort = sourcePort,
            destinationPort = destinationPort,
        )
    }

    fun nxdomain(question: DnsQuestion): ByteArray {
        val response = question.payload.copyOf()
        response[2] = (response[2].toInt() or 0x80).toByte()
        response[3] = (response[3].toInt() or 0x03).toByte()
        response[4] = 0
        response[5] = 0
        response[6] = 0
        response[7] = 0
        return wrapIpv4Udp(
            payload = response,
            sourceAddress = question.destinationAddress,
            destinationAddress = question.sourceAddress,
            sourcePort = question.destinationPort,
            destinationPort = question.sourcePort,
        )
    }

    fun wrapIpv4Udp(
        payload: ByteArray,
        sourceAddress: ByteArray,
        destinationAddress: ByteArray,
        sourcePort: Int,
        destinationPort: Int,
    ): ByteArray {
        val udpLength = payload.size + 8
        val packet = ByteArray(20 + udpLength)
        packet[0] = 0x45
        ByteBuffer.wrap(packet, 2, 2).order(ByteOrder.BIG_ENDIAN).putShort(packet.size.toShort())
        packet[8] = 64
        packet[9] = 17
        sourceAddress.copyInto(packet, 12)
        destinationAddress.copyInto(packet, 16)
        ByteBuffer.wrap(packet, 20, 2).putShort(sourcePort.toShort())
        ByteBuffer.wrap(packet, 22, 2).putShort(destinationPort.toShort())
        ByteBuffer.wrap(packet, 24, 2).putShort(udpLength.toShort())
        payload.copyInto(packet, 28)
        val pseudoHeader = ByteArray(12 + udpLength)
        sourceAddress.copyInto(pseudoHeader, 0)
        destinationAddress.copyInto(pseudoHeader, 4)
        pseudoHeader[9] = 17
        ByteBuffer.wrap(pseudoHeader, 10, 2).putShort(udpLength.toShort())
        packet.copyInto(pseudoHeader, 12, 20, packet.size)
        val udpChecksum = checksum(pseudoHeader, 0, pseudoHeader.size)
        ByteBuffer.wrap(packet, 26, 2).putShort(udpChecksum.toShort())
        ByteBuffer.wrap(packet, 10, 2).putShort(checksum(packet, 0, 20).toShort())
        return packet
    }

    private fun readQuestionName(payload: ByteArray): String? {
        if (payload.size < 13) return null
        var index = 12
        val labels = mutableListOf<String>()
        while (index < payload.size) {
            val length = payload[index].toInt() and 0xFF
            index++
            if (length == 0) break
            if (length > 63 || index + length > payload.size) return null
            labels += payload.copyOfRange(index, index + length).toString(Charsets.US_ASCII)
            index += length
        }
        return labels.joinToString(".").takeIf { it.isNotEmpty() }
    }

    private fun unsignedShort(bytes: ByteArray, index: Int): Int =
        ((bytes[index].toInt() and 0xFF) shl 8) or (bytes[index + 1].toInt() and 0xFF)

    private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var index = offset
        val end = minOf(bytes.size, offset + length)
        while (index + 1 < end) {
            sum += ((bytes[index].toInt() and 0xFF) shl 8) or (bytes[index + 1].toInt() and 0xFF)
            index += 2
        }
        if (index < end) sum += (bytes[index].toInt() and 0xFF) shl 8
        while (sum ushr 16 != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
        return sum.inv().toInt() and 0xFFFF
    }
}