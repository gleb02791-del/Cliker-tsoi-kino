package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameDefinitions
import com.example.data.GameRepository
import com.example.data.GameStateEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

// Временный индикатор клика для анимации всплывающих цифр
data class ClickIndicator(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float,
    val y: Float
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private var gameLoopJob: Job? = null
    private var saveJob: Job? = null

    // Состояние игры во вьюмодели
    private val _points = MutableStateFlow(0.0)
    val points: StateFlow<Double> = _points.asStateFlow()

    private val _totalClicks = MutableStateFlow(0)
    val totalClicks: StateFlow<Int> = _totalClicks.asStateFlow()

    private val _totalPointsEarned = MutableStateFlow(0.0)
    val totalPointsEarned: StateFlow<Double> = _totalPointsEarned.asStateFlow()

    private val _guitarLevel = MutableStateFlow(0)
    val guitarLevel: StateFlow<Int> = _guitarLevel.asStateFlow()

    private val _venueLevels = MutableStateFlow(listOf(0, 0, 0, 0))
    val venueLevels: StateFlow<List<Int>> = _venueLevels.asStateFlow()

    private val _songLevels = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val songLevels: StateFlow<List<Int>> = _songLevels.asStateFlow()

    private val _peopleLevels = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val peopleLevels: StateFlow<List<Int>> = _peopleLevels.asStateFlow()

    private val _unlockedAchievements = MutableStateFlow(setOf<String>())
    val unlockedAchievements: StateFlow<Set<String>> = _unlockedAchievements.asStateFlow()

    // Всплывающие надписи при кликах
    private val _clickIndicators = MutableStateFlow(listOf<ClickIndicator>())
    val clickIndicators: StateFlow<List<ClickIndicator>> = _clickIndicators.asStateFlow()

    // Оффлайн доход
    private val _offlineEarned = MutableStateFlow<Double?>(null)
    val offlineEarned: StateFlow<Double?> = _offlineEarned.asStateFlow()
    
    private val _offlineSeconds = MutableStateFlow(0L)
    val offlineSeconds: StateFlow<Long> = _offlineSeconds.asStateFlow()

    // Загружено ли состояние из БД
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameStateDao())
        
        loadGameState()
    }

    private fun loadGameState() {
        viewModelScope.launch {
            val savedState = repository.getGameState()
            
            _points.value = savedState.points
            _totalClicks.value = savedState.totalClicks
            _totalPointsEarned.value = savedState.totalPointsEarned
            _guitarLevel.value = savedState.guitarLevel
            _venueLevels.value = parseCsv(savedState.venueLevels, 4)
            _songLevels.value = parseCsv(savedState.songLevels, 5)
            _peopleLevels.value = parseCsv(savedState.peopleLevels, 5)
            _unlockedAchievements.value = if (savedState.unlockedAchievements.isEmpty()) {
                emptySet()
            } else {
                savedState.unlockedAchievements.split(",").toSet()
            }

            _isLoaded.value = true

            // Расчет оффлайн дохода
            val timePassedMs = System.currentTimeMillis() - savedState.lastSavedTime
            val secondsPassed = timePassedMs / 1000
            
            if (secondsPassed >= 10) {
                val pps = calculatePassiveIncome()
                if (pps > 0) {
                    val earned = secondsPassed * pps
                    _points.value += earned
                    _totalPointsEarned.value += earned
                    _offlineSeconds.value = secondsPassed
                    _offlineEarned.value = earned
                    
                    // Сохраним сразу, чтобы обновить время
                    saveToDb()
                }
            }

            // Запускаем игровые таймеры
            startGameLoop()
            startPeriodicSave()
        }
    }

    private fun parseCsv(csv: String, expectedSize: Int): List<Int> {
        if (csv.isEmpty()) return List(expectedSize) { 0 }
        val parsed = csv.split(",").map { it.toIntOrNull() ?: 0 }
        return if (parsed.size < expectedSize) {
            parsed + List(expectedSize - parsed.size) { 0 }
        } else {
            parsed.take(expectedSize)
        }
    }

    private fun listToCsv(list: List<Int>): String {
        return list.joinToString(",")
    }

    // Рассчитать пассивный доход в секунду
    fun calculatePassiveIncome(): Double {
        if (!_isLoaded.value) return 0.0

        val hasMarianna = _peopleLevels.value.getOrElse(3) { 0 } >= 1
        val hasJoanna = _peopleLevels.value.getOrElse(4) { 0 } >= 1

        val songsIncome = _songLevels.value.indices.sumOf { index ->
            if (_songLevels.value[index] >= 1) {
                val base = GameDefinitions.songs[index].passiveIncome
                // Джоанна удваивает/увеличивает доход песен на 50%
                if (hasJoanna) base * 1.5 else base
            } else 0.0
        }

        val venuesIncome = _venueLevels.value.indices.sumOf { index ->
            val count = _venueLevels.value[index]
            count * GameDefinitions.venues[index].baseIncome
        }

        val basePps = songsIncome + venuesIncome
        
        // Марьяна дает +30% ко всему пассивному доходу
        var finalPps = if (hasMarianna) basePps * 1.30 else basePps

        // Георгий Гурьянов дает автоматические клики (половина силы клика в секунду)
        val hasGuryanov = _peopleLevels.value.getOrElse(1) { 0 } >= 1
        if (hasGuryanov) {
            finalPps += calculateClickPower() * 0.5
        }

        return finalPps
    }

    // Рассчитать силу одного клика
    fun calculateClickPower(): Double {
        val basePower = GameDefinitions.guitars.getOrNull(_guitarLevel.value)?.clickPower ?: 1
        val hasKasparyan = _peopleLevels.value.getOrElse(0) { 0 } >= 1
        val hasGuryanov = _peopleLevels.value.getOrElse(1) { 0 } >= 1

        // Георгий Гурьянов дает +10 к клику
        val rawPower = basePower.toDouble() + if (hasGuryanov) 10.0 else 0.0

        // Юрий Каспарян увеличивает силу клика на 25%
        return if (hasKasparyan) rawPower * 1.25 else rawPower
    }

    // Скидка от Тихомирова (12%)
    fun hasCostReduction(): Boolean {
        return _peopleLevels.value.getOrElse(2) { 0 } >= 1
    }

    fun getGuitarCost(level: Int): Double {
        val guitar = GameDefinitions.guitars.getOrNull(level) ?: return Double.MAX_VALUE
        return if (hasCostReduction()) guitar.cost * 0.88 else guitar.cost
    }

    fun getVenueCost(index: Int): Double {
        val count = _venueLevels.value.getOrElse(index) { 0 }
        val venue = GameDefinitions.venues.getOrNull(index) ?: return Double.MAX_VALUE
        val baseCost = venue.getCost(count)
        return if (hasCostReduction()) baseCost * 0.88 else baseCost
    }

    fun getSongCost(index: Int): Double {
        val song = GameDefinitions.songs.getOrNull(index) ?: return Double.MAX_VALUE
        return song.cost // Песни покупаются по фиксированной цене
    }

    fun getPersonCost(index: Int): Double {
        val person = GameDefinitions.people.getOrNull(index) ?: return Double.MAX_VALUE
        return person.cost // Люди нанимаются по фиксированной цене
    }

    // КЛИК!
    fun onTsoiClicked(x: Float, y: Float) {
        val power = calculateClickPower()
        _points.value += power
        _totalPointsEarned.value += power
        _totalClicks.value += 1

        // Добавляем красивую всплывающую циферку
        val text = "+${power.toInt()}"
        val indicator = ClickIndicator(text = text, x = x, y = y)
        _clickIndicators.update { it + indicator }

        // Удалим индикатор через 600мс
        viewModelScope.launch {
            delay(600)
            _clickIndicators.update { current -> current.filter { it.id != indicator.id } }
        }

        // Проверить достижения сразу после клика
        checkAchievements()
    }

    // Покупка/улучшение гитары
    fun buyGuitarUpgrade(): Boolean {
        val nextLevel = _guitarLevel.value + 1
        if (nextLevel >= GameDefinitions.guitars.size) return false

        val cost = getGuitarCost(nextLevel)
        if (_points.value >= cost) {
            _points.value -= cost
            _guitarLevel.value = nextLevel
            saveToDbAsync()
            checkAchievements()
            return true
        }
        return false
    }

    // Покупка площадки
    fun buyVenue(index: Int): Boolean {
        val venue = GameDefinitions.venues.getOrNull(index) ?: return false
        val cost = getVenueCost(index)
        if (_points.value >= cost) {
            _points.value -= cost
            val currentList = _venueLevels.value.toMutableList()
            currentList[index] = currentList.getOrElse(index) { 0 } + 1
            _venueLevels.value = currentList
            saveToDbAsync()
            checkAchievements()
            return true
        }
        return false
    }

    // Запись легендарной песни
    fun recordSong(index: Int): Boolean {
        val song = GameDefinitions.songs.getOrNull(index) ?: return false
        if (_songLevels.value.getOrElse(index) { 0 } >= 1) return false // Уже записана

        val cost = getSongCost(index)
        if (_points.value >= cost) {
            _points.value -= cost
            val currentList = _songLevels.value.toMutableList()
            currentList[index] = 1
            _songLevels.value = currentList
            saveToDbAsync()
            checkAchievements()
            return true
        }
        return false
    }

    // Нанять человека
    fun hirePerson(index: Int): Boolean {
        val person = GameDefinitions.people.getOrNull(index) ?: return false
        if (_peopleLevels.value.getOrElse(index) { 0 } >= 1) return false // Уже нанят

        val cost = getPersonCost(index)
        if (_points.value >= cost) {
            _points.value -= cost
            val currentList = _peopleLevels.value.toMutableList()
            currentList[index] = 1
            _peopleLevels.value = currentList
            saveToDbAsync()
            checkAchievements()
            return true
        }
        return false
    }

    // Закрыть диалог оффлайн дохода
    fun dismissOfflineDialog() {
        _offlineEarned.value = null
    }

    // Проверка достижений
    fun checkAchievements() {
        if (!_isLoaded.value) return

        val unlocked = _unlockedAchievements.value.toMutableSet()
        var changed = false

        for (ach in GameDefinitions.achievements) {
            if (ach.id !in unlocked) {
                // Особый чек для Rock Emperor (1,000,000 суммарных очков)
                val isUnlocked = if (ach.id == "rock_emperor") {
                    _totalPointsEarned.value >= 1000000.0
                } else {
                    ach.checkCondition(
                        _points.value,
                        _totalClicks.value,
                        _guitarLevel.value,
                        _venueLevels.value,
                        _songLevels.value,
                        _peopleLevels.value
                    )
                }

                if (isUnlocked) {
                    unlocked.add(ach.id)
                    changed = true
                }
            }
        }

        if (changed) {
            _unlockedAchievements.value = unlocked
            saveToDbAsync()
        }
    }

    // Игровой цикл для пассивного дохода
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            val tickIntervalMs = 200L
            while (isActive) {
                delay(tickIntervalMs)
                val pps = calculatePassiveIncome()
                if (pps > 0) {
                    val pointsPerTick = pps * (tickIntervalMs / 1000.0)
                    _points.value += pointsPerTick
                    _totalPointsEarned.value += pointsPerTick
                    
                    // Каждые 50 тиков (10 сек) проверяем ачивки пассивно
                    if (System.currentTimeMillis() % 10000 < tickIntervalMs) {
                        checkAchievements()
                    }
                }
            }
        }
    }

    // Периодическое сохранение
    private fun startPeriodicSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                saveToDb()
            }
        }
    }

    fun saveToDbAsync() {
        viewModelScope.launch {
            saveToDb()
        }
    }

    suspend fun saveToDb() {
        if (!_isLoaded.value) return
        val state = GameStateEntity(
            id = 1,
            points = _points.value,
            totalClicks = _totalClicks.value,
            totalPointsEarned = _totalPointsEarned.value,
            guitarLevel = _guitarLevel.value,
            venueLevels = listToCsv(_venueLevels.value),
            songLevels = listToCsv(_songLevels.value),
            peopleLevels = listToCsv(_peopleLevels.value),
            unlockedAchievements = _unlockedAchievements.value.joinToString(","),
            lastSavedTime = System.currentTimeMillis()
        )
        repository.saveGameState(state)
    }

    override fun onCleared() {
        super.onCleared()
        // Пытаемся сохранить при уничтожении вьюмодели
        gameLoopJob?.cancel()
        saveJob?.cancel()
    }
}
