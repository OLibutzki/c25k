package com.example.c25k.data

import androidx.room.migration.Migration

object DatabaseMigrations {
    /**
     * Append new Room migrations here when increasing [AppDatabase] version.
     *
     * Keeping this explicit prevents silent data loss on production upgrades.
     */
    val ALL: Array<Migration> = emptyArray()
}
