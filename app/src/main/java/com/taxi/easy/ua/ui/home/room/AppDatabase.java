package com.taxi.easy.ua.ui.home.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;

@Database(entities = {RouteCost.class}, version = 3)
@TypeConverters(ListStringConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract RouteCostDao routeCostDao();
    public static final Migration MIGRATION_1_3 = new Migration1to3();
}

