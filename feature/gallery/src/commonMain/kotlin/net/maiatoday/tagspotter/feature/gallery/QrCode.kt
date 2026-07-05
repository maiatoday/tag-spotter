package net.maiatoday.tagspotter.feature.gallery

import kotlin.math.absoluteValue

object QrMath {
    val EXP = IntArray(256)
    val LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP[i] = x
            LOG[x] = i
            x = (x shl 1) xor if (x and 0x80 != 0) 285 else 0
        }
        EXP[255] = EXP[0]
    }

    fun mul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return EXP[(LOG[a] + LOG[b]) % 255]
    }
}

class QrCodeEncoder {

    private fun getGeneratorPolynomial(count: Int): IntArray {
        var g = intArrayOf(1)
        for (i in 0 until count) {
            val next = IntArray(g.size + 1)
            val alpha = QrMath.EXP[i]
            for (j in g.indices) {
                next[j] = next[j] xor QrMath.mul(g[j], alpha)
                next[j + 1] = next[j + 1] xor g[j]
            }
            g = next
        }
        return g
    }

    private fun calculateEcc(data: ByteArray, eccCount: Int): ByteArray {
        val gen = getGeneratorPolynomial(eccCount)
        val result = IntArray(data.size + eccCount)
        for (i in data.indices) {
            result[i] = data[i].toInt() and 0xFF
        }
        for (i in data.indices) {
            val factor = result[i]
            if (factor != 0) {
                for (j in 0 until eccCount) {
                    result[i + 1 + j] = result[i + 1 + j] xor QrMath.mul(gen[eccCount - 1 - j], factor)
                }
            }
        }
        val ecc = ByteArray(eccCount)
        for (i in 0 until eccCount) {
            ecc[i] = result[data.size + i].toByte()
        }
        return ecc
    }

    fun encode(text: String): Array<BooleanArray> {
        // We use QR Code Version 4, ECC-L.
        // Dimension = 33x33.
        // Data capacity: 80 data codewords, 20 ECC codewords.
        val size = 33
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // Encode payload
        val textBytes = text.encodeToByteArray()
        val dataBytes = ByteArray(80)
        
        // Byte mode indicator: 0100 -> 4 bits
        // Count: 8 bits
        // Data: textBytes
        val bitBuffer = ArrayList<Boolean>()
        // Byte Mode indicator (0100)
        bitBuffer.add(false); bitBuffer.add(true); bitBuffer.add(false); bitBuffer.add(false)
        // Count (8 bits)
        val len = textBytes.size.coerceAtMost(255)
        for (i in 7 downTo 0) {
            bitBuffer.add(((len shr i) and 1) != 0)
        }
        // Data bits
        for (byte in textBytes) {
            val b = byte.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitBuffer.add(((b shr i) and 1) != 0)
            }
        }
        // Terminator (up to 4 bits of 0s)
        val termCount = (80 * 8 - bitBuffer.size).coerceIn(0, 4)
        repeat(termCount) { bitBuffer.add(false) }
        
        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(false)
        }

        // Pad with 0xEC and 0x11
        var toggle = true
        while (bitBuffer.size < 80 * 8) {
            val pad = if (toggle) 0xEC else 0x11
            toggle = !toggle
            for (i in 7 downTo 0) {
                bitBuffer.add(((pad shr i) and 1) != 0)
            }
        }

        // Pack bits to bytes
        for (i in 0 until 80) {
            var b = 0
            for (j in 0 until 8) {
                b = b shl 1
                if (bitBuffer[i * 8 + j]) b = b or 1
            }
            dataBytes[i] = b.toByte()
        }

        // Calculate Reed-Solomon ECC
        val eccBytes = calculateEcc(dataBytes, 20)

        // Combine into complete message sequence (80 data + 20 ECC = 100 bytes)
        val allBytes = ByteArray(100)
        dataBytes.copyInto(allBytes, 0)
        eccBytes.copyInto(allBytes, 80)

        // Draw static patterns
        // Finder patterns
        drawFinder(matrix, reserved, 3, 3)
        drawFinder(matrix, reserved, 29, 3)
        drawFinder(matrix, reserved, 3, 29)

        // Alignment pattern for Version 4 (center at 26, 26)
        drawAlignment(matrix, reserved, 26, 26)

        // Timing patterns
        for (i in 0 until size) {
            if (!reserved[6][i]) {
                matrix[6][i] = i % 2 == 0
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = i % 2 == 0
                reserved[i][6] = true
            }
        }

        // Dark module
        matrix[25][8] = true
        reserved[25][8] = true

        // Draw Format Info (ECC-L, Mask 0)
        // Format bits before masking: 111011111000100 (15 bits)
        // Masked with 101010000010010 -> 010001111010110
        val formatBits = booleanArrayOf(
            false, true, false, false, false, true, true, true,
            true, false, true, false, true, true, false
        )
        // Place format bits:
        // Bits 0..5 to (8,0)..(8,5)
        // Bit 6 to (8,7)
        // Bit 7 to (8,8)
        // Bit 8 to (7,8)
        // Bits 9..14 to (5,8)..(0,8)
        val formatCoords = arrayOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5, 8 to 7, 8 to 8,
            7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8
        )
        for (i in 0..14) {
            val (r, c) = formatCoords[i]
            matrix[r][c] = formatBits[i]
            reserved[r][c] = true
        }

        // Symmetrical format bits
        // Bits 0..7 to (32,8)..(25,8)
        // Bits 8..14 to (8,26)..(8,32)
        for (i in 0..7) {
            val r = size - 1 - i
            matrix[r][8] = formatBits[i]
            reserved[r][8] = true
        }
        for (i in 8..14) {
            val c = size - 15 + i
            matrix[8][c] = formatBits[i]
            reserved[8][c] = true
        }

        // Reserved format areas around top-right & bottom-left finders
        for (i in 0..7) {
            reserved[size - 1 - i][8] = true
            reserved[8][size - 1 - i] = true
        }

        // Place Data and ECC bits in zigzag pattern
        var bitIndex = 0
        var right = size - 1
        var upwards = true
        while (right > 0) {
            if (right == 6) right-- // skip timing pattern column
            for (y in 0 until size) {
                val row = if (upwards) size - 1 - y else y
                for (col in right downTo right - 1) {
                    if (!reserved[row][col]) {
                        var bit = false
                        if (bitIndex < 100 * 8) {
                            val byteIdx = bitIndex / 8
                            val bitIdx = 7 - (bitIndex % 8)
                            bit = ((allBytes[byteIdx].toInt() shr bitIdx) and 1) != 0
                            bitIndex++
                        }
                        // Apply Mask 0: (row + col) % 2 == 0
                        if ((row + col) % 2 == 0) {
                            bit = !bit
                        }
                        matrix[row][col] = bit
                    }
                }
            }
            upwards = !upwards
            right -= 2
        }

        return matrix
    }

    private fun drawFinder(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, cx: Int, cy: Int) {
        for (dy in -3..3) {
            for (dx in -3..3) {
                val px = cx + dx
                val py = cy + dy
                if (px in 0..32 && py in 0..32) {
                    val active = maxOf(dx.absoluteValue, dy.absoluteValue) != 1
                    matrix[py][px] = active
                    reserved[py][px] = true
                }
            }
        }
        // White border around finders
        for (dy in -4..4) {
            for (dx in -4..4) {
                val px = cx + dx
                val py = cy + dy
                if (px in 0..32 && py in 0..32) {
                    reserved[py][px] = true
                }
            }
        }
    }

    private fun drawAlignment(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, cx: Int, cy: Int) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val px = cx + dx
                val py = cy + dy
                if (px in 0..32 && py in 0..32 && !reserved[py][px]) {
                    val active = maxOf(dx.absoluteValue, dy.absoluteValue) != 1
                    matrix[py][px] = active
                    reserved[py][px] = true
                }
            }
        }
    }
}
