package com.iykeafrica.budgettracker.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {Expense.class, MonthlyBudget.class, CategoryBudget.class},
    version  = 6,
    exportSchema = false
)
public abstract class BudgetDatabase extends RoomDatabase {

    private static volatile BudgetDatabase INSTANCE;

    // No-op: only updates Room's stored identity hash after entity annotation changes
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) { /* schema unchanged */ }
    };

    // Fixes dates stored in JS Date.toString() format back to yyyy-MM-dd.
    // Sheets was returning "Wed May 28 2026 00:00:00 GMT+..." instead of "2026-05-28".
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // SQLite date('...') understands ISO dates only.
            // Use a CASE to detect JS-format dates (they don't start with a digit)
            // and replace them using SQLite's strftime on a best-effort substr parse.
            // For rows we can't fix, leave the date as-is (they'll be corrected on next sync).
            db.execSQL(
                "UPDATE expenses SET date = " +
                "  CASE " +
                "    WHEN substr(date,1,1) BETWEEN '0' AND '9' THEN date " + // already ISO
                "    ELSE " +
                "      substr(date, instr(date,' ')+5, 4) || '-' || " +          // year
                "      CASE substr(date, instr(date,' ')+1, 3) " +               // month name
                "        WHEN 'Jan' THEN '01' WHEN 'Feb' THEN '02' WHEN 'Mar' THEN '03' " +
                "        WHEN 'Apr' THEN '04' WHEN 'May' THEN '05' WHEN 'Jun' THEN '06' " +
                "        WHEN 'Jul' THEN '07' WHEN 'Aug' THEN '08' WHEN 'Sep' THEN '09' " +
                "        WHEN 'Oct' THEN '10' WHEN 'Nov' THEN '11' WHEN 'Dec' THEN '12' " +
                "        ELSE '01' END || '-' || " +
                "      printf('%02d', CAST(trim(substr(date, instr(date,' ')+5, " +
                "             instr(substr(date, instr(date,' ')+5), ' '))) AS INTEGER)) " +  // day
                "  END"
            );
        }
    };

    // MIGRATION_4_5 had a bug: year extraction used fixed offset from first space, which
    // is wrong for 2-digit day numbers (e.g. "28" vs "8"). Corrupted dates look like
    // "28 2-05-28" (day + first-digit-of-year instead of "2026-05-28").
    // Fix: delete those rows — they are Sheets-sourced and will re-sync with the corrected
    // normalizeDate() in ExpenseRepository. Locally-added ISO dates are unaffected.
    // Detection: corrupted dates have a space at position 3 (e.g. "28 2-05-28"),
    // whereas valid ISO dates have a digit there (e.g. "2026-05-28").
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("DELETE FROM expenses WHERE substr(date, 3, 1) = ' '");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // notes is nullable in the entity (String without @NonNull)
            db.execSQL("ALTER TABLE expenses ADD COLUMN notes TEXT");

            // userId/monthYear/category are nullable Strings; totalBudget/budgetAmount are
            // primitive double (NOT NULL but no SQL DEFAULT so Room reports defaultValue=undefined)
            db.execSQL("CREATE TABLE IF NOT EXISTS monthly_budgets (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "monthYear TEXT, " +
                    "totalBudget REAL NOT NULL)");

            db.execSQL("CREATE TABLE IF NOT EXISTS category_budgets (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "monthYear TEXT, " +
                    "category TEXT, " +
                    "budgetAmount REAL NOT NULL)");
        }
    };

    public static BudgetDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BudgetDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BudgetDatabase.class,
                                    "budget_tracker.db"
                            )
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract ExpenseDao expenseDao();
    public abstract BudgetDao  budgetDao();
}
