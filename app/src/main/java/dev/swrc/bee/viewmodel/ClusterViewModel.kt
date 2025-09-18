package dev.swrc.bee.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.swrc.bee.constants.Hex
import dev.swrc.bee.constants.Response
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClusterViewModel : ViewModel() {
    private val _hexagons = MutableStateFlow<List<Hex>>(emptyList())
    val hexagons: StateFlow<List<Hex>> = _hexagons
    private var nonCenterLetters = "ABEHLT"
    val centerLetter = "P"
    var currentInput by mutableStateOf("")
        private set

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage = _errorMessage

    private val _isErrorVisible = mutableStateOf(false)
    val isErrorVisible = _isErrorVisible

    private val _isPopupVisible = mutableStateOf(false)
    val isPopupVisible = _isPopupVisible

    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage = _successMessage


    init {
        _hexagons.value = generateHexList()
    }

    fun letters(): String {
        return nonCenterLetters.plus(centerLetter)
    }

    fun hidePopups() {
        _isErrorVisible.value = false
        _isPopupVisible.value = false
    }

    fun displayErrorMessage(response: Response, durationMillis: Long = 3_000L) {
        _isErrorVisible.value = true
        _errorMessage.value = response.message
        viewModelScope.launch {
            delay(durationMillis)
            _isErrorVisible.value = false
        }
    }

    fun displaySuccessMessage(achievement: String = "", points: Int, durationMillis: Long = 3_000L) {
        _isPopupVisible.value = true
        _successMessage.value = "+$points! Good job!"
        viewModelScope.launch {
            delay(durationMillis)
            _isPopupVisible.value = false
        }
    }

    fun addLetter(index: Int): Boolean {
        val letter = when (index) {
            0 -> centerLetter
            in 1..(nonCenterLetters.length) -> nonCenterLetters[index - 1].toString()
            else -> ""
        }
        currentInput += letter
        return (currentInput.length == 20)
    }

    fun clearWord() {
        currentInput = ""
    }

    fun removeLetter() {
        if(currentInput.isEmpty()) return
        currentInput = currentInput.substring(0, currentInput.lastIndex)
    }

    private fun generateHexList(): List<Hex> =
        (0 until nonCenterLetters.length + 1).map { i ->
            Hex(
                index = i,
                isCenter = (i == 0),
                label = if (i == 0) centerLetter else nonCenterLetters[i - 1].toString()
            )
        }

    fun shuffleLetters() {
        nonCenterLetters = nonCenterLetters
            .toList()
            .shuffled()
            .joinToString("")

        _hexagons.value = generateHexList()
    }
}