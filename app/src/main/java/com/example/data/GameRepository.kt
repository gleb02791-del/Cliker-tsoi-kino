package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameStateDao: GameStateDao) {

    val gameStateFlow: Flow<GameStateEntity> = gameStateDao.getGameStateFlow()
        .map { entity ->
            entity ?: GameStateEntity() // Возвращаем дефолтный стейт, если база пуста
        }

    suspend fun getGameState(): GameStateEntity {
        return gameStateDao.getGameState() ?: GameStateEntity()
    }

    suspend fun saveGameState(state: GameStateEntity) {
        gameStateDao.insertOrUpdate(state)
    }
}
