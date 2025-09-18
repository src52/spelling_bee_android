package dev.swrc.bee.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.swrc.bee.repo.DictionaryRepository
import dev.swrc.bee.R
import dev.swrc.bee.constants.Ranking
import dev.swrc.bee.constants.ScoreState
import dev.swrc.bee.constants.SubmissionResponse
import dev.swrc.bee.repo.TrieDictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor

@Suppress("UNCHECKED_CAST")
class GameViewModelFactory(
    private val app: Application,
    private val letters: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            val repo = TrieDictionaryRepository(app, R.raw.main_words, letters)
            return GameViewModel(letters, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GameViewModel(
    private val letters: String,
    private val dictionary: DictionaryRepository
) : ViewModel() {

    private var _percentToNextRank = mutableFloatStateOf(0.0f)
    var percentToNextRank = _percentToNextRank

    private val rankScores: Map<Ranking, Int>
    init {
        val ranks = mutableMapOf<Ranking, Int>()
        val totalScore = dictionary.scoreAllWords()
        println("Total score is: $totalScore")
        Ranking.entries.forEach { ranking: Ranking ->
            ranks[ranking] = floor(totalScore * ranking.percentage).toInt()
        }
        rankScores = ranks
        _percentToNextRank.floatValue = 0.0f
        println("Ranks: $rankScores")
        println("Size: " + dictionary.size())
    }

    fun getScoreFromPoints(points: Int): ScoreState {
        var currentRank: Ranking = Ranking.BEGINNER
        var nextRank: Ranking = Ranking.GOOD_START
        var pointsNeeded = 0
        for (rank in rankScores) {
            if(rank.value > points) {
                pointsNeeded = rank.value - points
                nextRank = rank.key
                break
            } else {
                currentRank = rank.key
            }
        }
        val percentFloat: Float = (points.toFloat() / rankScores[nextRank]!!)
        _percentToNextRank.floatValue = percentFloat
        println("Percent: $_percentToNextRank")
        return ScoreState(points, currentRank, pointsNeeded)
    }

    fun print() {
        println("Printing")
        dictionary.print()
    }

    private val _foundWords = MutableStateFlow<List<String>>(emptyList())
    val foundWords: StateFlow<List<String>> = _foundWords.asStateFlow()

    private val _scoreState = mutableStateOf(ScoreState(0, Ranking.BEGINNER, 3))
    val scoreState: State<ScoreState> = _scoreState

    fun submitWord(word: String, letters: String, centerLetter: String): SubmissionResponse {
        if(word.length <= 3) return SubmissionResponse.TOO_SHORT
        if(!word.contains(centerLetter)) return SubmissionResponse.MISSING_CENTER_LETTER
        if(_foundWords.value.contains(word)) return SubmissionResponse.ALREADY_FOUND

        if (dictionary.isValid(word)) {
            val points = computePoints(word, letters)
            val newScoreState = getScoreFromPoints(scoreState.value.score + points)
            _scoreState.value = newScoreState
            _foundWords.value += word
            println("Word is valid!")
            return SubmissionResponse.VALID_WORD
        }
        println("Word is not valid!")
        return SubmissionResponse.INVALID_WORD
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
}