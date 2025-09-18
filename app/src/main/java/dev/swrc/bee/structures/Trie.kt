package dev.swrc.bee.structures

import android.content.Context
import androidx.annotation.RawRes
import java.io.BufferedReader
import java.io.InputStreamReader


class Trie {
    internal class TrieNode {
        var children: MutableMap<Char, TrieNode> = HashMap()
        var isWordEnd: Boolean = false
    }

    private val root = TrieNode()

    fun insert(word: String) {
        var node = root
        for (ch in word.toCharArray()) {
            node = node.children.computeIfAbsent(ch) { c: Char? -> TrieNode() }
        }
        node.isWordEnd = true
    }

    fun contains(word: String): Boolean {
        var node: TrieNode? = root
        for (ch in word.toCharArray()) {
            node = node!!.children[ch]
            if (node == null) return false
        }
        return node!!.isWordEnd
    }

    fun startsWith(prefix: String): Boolean {
        var node: TrieNode? = root
        for (ch in prefix.toCharArray()) {
            node = node!!.children[ch]
            if (node == null) return false
        }
        return true
    }

    fun loadDictionary(context: Context, @RawRes resId: Int, letters: String) {
        println("Loaded letters: $letters")
        context.resources.openRawResource(resId).use { `in` ->
            BufferedReader(InputStreamReader(`in`)).use { reader ->
                var line: String
                while ((reader.readLine().also { line = it }) != null) {
                    line = line.lowercase().trim { it <= ' ' }
                    if (line.isNotEmpty() && line.length > 3
                        && (line.all { it in letters.lowercase().toSet() })
                        && line.contains(letters.last().lowercase())) {
                        insert(line)
                    }
                }
            }
        }
    }

    fun printAllWords() {
        fun dfs(node: TrieNode, prefix: String) {
            if (node.isWordEnd) {
                println(prefix)
            }
            for ((ch, child) in node.children) {
                dfs(child, prefix + ch)
            }
        }

        dfs(root, "")
    }

    fun computePoints(word: String, letters: String): Int {
        var points: Int
        if(word.length == 4) return 1
        else points = word.length

        if(word.toSet().containsAll(letters.toSet())) {
            points += 7
        }

        return points
    }

    fun scoreAllWords(letters: String): Int {
        var totalScore = 0

        fun dfs(node: TrieNode, prefix: String) {
            if (node.isWordEnd) {
                val score = computePoints(prefix, letters)
                println("Added score: $prefix → $score")
                totalScore += score
            }
            for ((ch, child) in node.children) {
                dfs(child, prefix + ch)
            }
        }

        dfs(root, "")
        return totalScore
    }

    fun size(): Int {
        var count = 0
        fun dfs(node: TrieNode) {
            if (node.isWordEnd) count++
            for (child in node.children.values) {
                dfs(child)
            }
        }

        dfs(root)
        return count
    }
}