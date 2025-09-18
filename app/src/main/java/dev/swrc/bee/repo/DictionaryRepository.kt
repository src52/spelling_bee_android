package dev.swrc.bee.repo

import android.content.Context
import androidx.annotation.RawRes
import dev.swrc.bee.network.WordRequest
import dev.swrc.bee.structures.Trie

interface DictionaryRepository {
    fun isValid(word: String): Boolean
    fun scoreAllWords(): Int
    fun print()
    fun size(): Int
}

class PlaceholderDictionaryRepository : DictionaryRepository { //Used for @Preview
    override fun isValid(word: String) = true
    override fun scoreAllWords(): Int { return 0 }
    override fun print() {}
    override fun size(): Int { return 0 }
}

class TrieDictionaryRepository(
    context: Context,
    @RawRes dictionaryResId: Int,
    private val letters: String
) : DictionaryRepository {

    private val trie: Trie = Trie().apply {
        loadDictionary(context, dictionaryResId, letters)
    }

    override fun isValid(word: String): Boolean {
        val key = word.trim().lowercase()
        return trie.contains(key)
    }

    override fun scoreAllWords(): Int {
        return trie.scoreAllWords(letters)
    }

    override fun print() {
        trie.printAllWords()
    }

    override fun size(): Int {
        return trie.size()
    }
}

class ApiDictionaryRepository : DictionaryRepository {
    override fun isValid(word: String): Boolean {
        return try {
            val response = WordApi.retrofitService.checkWord(WordRequest(word))
            response.valid
        } catch (e: Exception) {
            false
        }
    }

    override fun scoreAllWords(): Int {
        return 0
    }

    override fun print() {
    }

    override fun size(): Int {
        return 0
    }
}