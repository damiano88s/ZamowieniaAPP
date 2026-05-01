package pl.zamowieniaapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [OrderEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
}