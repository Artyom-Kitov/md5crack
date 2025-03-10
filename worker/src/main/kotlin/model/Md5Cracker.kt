package ru.nsu.dsi.md5.model

import org.paukov.combinatorics.CombinatoricsFactory
import org.paukov.combinatorics.ICombinatoricsVector
import java.security.MessageDigest
import kotlin.math.pow

class Md5Cracker(
    private val alphabet: Set<Char>,
    private val target: String,
) {
    @OptIn(ExperimentalStdlibApi::class)
    private val targetBytes = target.hexToByteArray()

    fun crack(partNum: Int, partCount: Int, maxLen: Int): List<String> {
        val result = mutableListOf<String>()
        for (len in 1..maxLen) {
            val range = getRange(partNum, partCount, len, alphabet.size)
            result.addAll(generateForRange(range, len))
        }
        return result
    }

    private fun generateForRange(range: IntRange, len: Int): List<String> {
        val vector = CombinatoricsFactory.createVector(alphabet)
        val generator = CombinatoricsFactory.createPermutationWithRepetitionGenerator(vector, len)
            .iterator()
            .skip(range.first)
        val count = range.last - range.first + 1
        val resultForLen = mutableListOf<String>()
        repeat(count) {
            if (generator.hasNext()) {
                val word = generator.next().asString()
                val hash = word.md5()
                if (hash.contentEquals(targetBytes)) {
                    resultForLen.add(word)
                }
            }
        }
        return resultForLen
    }

    private fun ICombinatoricsVector<Char>.asString(): String {
        val builder = StringBuilder()
        this.vector.forEach { builder.append(it) }
        return builder.toString()
    }

    private fun String.md5() = MessageDigest.getInstance("MD5")
        .digest(this.toByteArray())

    private fun <T> MutableIterator<T>.skip(start: Int): MutableIterator<T> {
        for (i in 0..<start) {
            if (this.hasNext()) {
                this.next()
            } else {
                return this
            }
        }
        return this
    }

    private fun getRange(partNum: Int, partCount: Int, wordLen: Int, charsCount: Int): IntRange {
        val words = charsCount.toDouble().pow(wordLen.toDouble()).toInt()
        val remainder = words % partCount
        val step = words / partCount
        var start = 0
        var end = step + if (partNum < remainder) 1 else 0
        for (i in 0..<partNum) {
            start += step + if (i < remainder) 1 else 0
            end += step + if (i < remainder) 1 else 0
        }
        return start..<end
    }
}