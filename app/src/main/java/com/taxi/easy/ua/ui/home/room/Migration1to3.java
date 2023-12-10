package com.taxi.easy.ua.ui.home.room;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration1to3 extends Migration {
    public Migration1to3() {
        super(1, 3); // От версии 1 к версии 3
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // В этом методе опишите изменения схемы базы данных от версии 1 к версии 3
        // Например, создайте новые таблицы или добавьте новые поля
        database.execSQL("ALTER TABLE RouteCost ADD COLUMN addCost TEXT");
        database.execSQL("ALTER TABLE RouteCost ADD COLUMN tarif TEXT");
        database.execSQL("ALTER TABLE RouteCost ADD COLUMN payment_type TEXT");
        database.execSQL("ALTER TABLE RouteCost ADD COLUMN servicesChecked TEXT");
    }
}

