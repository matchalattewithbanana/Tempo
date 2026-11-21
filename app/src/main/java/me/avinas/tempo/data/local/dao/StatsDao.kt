package me.avinas.tempo.data.local.dao

import androidx.room.Dao

/**
 * Listening statistics DAO.
 *
 * Query declarations live in focused inherited interfaces so Room still exposes one generated
 * [StatsDao] through [me.avinas.tempo.data.local.AppDatabase.statsDao] while each query family can
 * be reviewed independently.
 */
@Dao
interface StatsDao :
    StatsOverviewQueries,
    StatsRankingQueries,
    StatsTemporalQueries,
    StatsHistoryQueries,
    StatsEntityQueries,
    StatsSearchQueries
