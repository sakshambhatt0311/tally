package com.tally.app.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The master catalog of playable games — an injectable, fast, in-memory dependency wrapping the
 * static [MockGameData] library. This is the SSOT for game identities; a tracked game references a
 * template by [GameTemplate.id], so any of the ~25 games keeps its real name/icon/scoring type.
 */
@Singleton
class GameCatalog @Inject constructor() {

    val templates: List<GameTemplate> = MockGameData.getAllGamesFlatList()

    val categorized: Map<String, List<GameTemplate>> = MockGameData.allGamesCategorized

    fun byId(id: String): GameTemplate? = MockGameData.templateById(id)
}
