package com.iykeafrica.budgettracker;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The Room database for this app.
 *
 * @Database lists every Entity (table) and the current schema version.
 * If you ever add a new column or table, increment the version number
 * and provide a Migration — or use fallbackToDestructiveMigration()
 * during development (which wipes and recreates the DB on version change).
 */
@Database(entities = {Expense.class}, version = 2, exportSchema = false)
public abstract class BudgetDatabase extends RoomDatabase {

    // ── Singleton instance ─────────────────────────────────────────────────

    private static volatile BudgetDatabase INSTANCE;

    /**
     * Returns the single shared instance of the database.
     * Creates it on the first call; returns the cached one after that.
     * "synchronized" prevents two threads creating two instances at once.
     */
    public static BudgetDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BudgetDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BudgetDatabase.class,
                                    "budget_tracker.db"        // name of the .db file on disk
                            )
                            .fallbackToDestructiveMigration() // OK for development
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ── DAO access ─────────────────────────────────────────────────────────

    /**
     * Room implements this method automatically.
     * Call BudgetDatabase.getInstance(context).expenseDao() to get the DAO.
     */
    public abstract ExpenseDao expenseDao();
}
