package pl.zamowieniaapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: Long,

    val data: String,
    val firma: String,
    val platnosc: String,
    val produkt: String,
    val opis: String,
    val isRozliczone: Boolean = false,
    val dataRozliczenia: String = ""
)