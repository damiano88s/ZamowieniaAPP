package pl.zamowieniaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pl.zamowieniaapp.ui.theme.ZamowieniaAPPTheme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FabPosition
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.room.Room
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeParseException
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext


data class Order(
    val data: String,
    val firma: String,
    val platnosc: String,
    val produkt: String,
    val opis: String,
    val isRozliczone: Boolean = false,
    val dataRozliczenia: String = "",
    val id: Long = System.currentTimeMillis()
)

fun Order.toEntity(): OrderEntity {
    return OrderEntity(
        id = id,
        data = data,
        firma = firma,
        platnosc = platnosc,
        produkt = produkt,
        opis = opis,
        isRozliczone = isRozliczone,
        dataRozliczenia = dataRozliczenia
    )
}

fun OrderEntity.toOrder(): Order {
    return Order(
        id = id,
        data = data,
        firma = firma,
        platnosc = platnosc,
        produkt = produkt,
        opis = opis,
        isRozliczone = isRozliczone,
        dataRozliczenia = dataRozliczenia
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "zamowienia_database"
        ).build()

        val orderDao = db.orderDao()
        enableEdgeToEdge()
        setContent {
            ZamowieniaAPPTheme {
                MainScreen(orderDao)
            }
        }
    }
}

@Composable
fun MainScreen(orderDao: OrderDao) {

    var showAddScreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val orders by orderDao.getAllOrders()
        .collectAsState(initial = emptyList())

    val ordersUi = orders.map { it.toOrder() }
    var showHistory by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var selectedOrderIds by remember { mutableStateOf(setOf<Long>()) }

    if (selectedOrder != null) {
        OrderDetailsScreen(
            order = selectedOrder!!,
            onBack = { selectedOrder = null },
            onRozlicz = {
                val updated = selectedOrder!!.copy(
                    isRozliczone = true,
                    dataRozliczenia = java.text.SimpleDateFormat(
                        "dd.MM.yyyy",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                )

                scope.launch {
                    orderDao.updateOrder(updated.toEntity())
                }
                selectedOrder = null
                showHistory = true
            },
            onCofnij = {
                val updated = selectedOrder!!.copy(
                    isRozliczone = false,
                    dataRozliczenia = ""
                )

                scope.launch {
                    orderDao.updateOrder(updated.toEntity())
                }
                selectedOrder = null
                showHistory = false
            }
        )
    } else if (showAddScreen) {
        AddOrderScreen(
            onBack = { showAddScreen = false },
            onSave = { newOrder ->

                scope.launch {
                    orderDao.insertOrder(newOrder.toEntity())
                }


                showAddScreen = false
                showHistory = false
            }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (selectedOrderIds.isNotEmpty()) {

                            val idsToDelete = selectedOrderIds.toList()

                            scope.launch {
                                orderDao.deleteOrders(idsToDelete)
                            }

                            selectedOrderIds = emptySet()

                        } else {
                            showAddScreen = true
                        }
                    },
                    containerColor = Color(0xFF00C853),
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                ) {
                    if (selectedOrderIds.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń",
                            tint = Color.White,
                            modifier = Modifier.size(45.dp)
                        )
                    } else {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontSize = 55.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "ZAMÓWIENIA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NIEROZLICZONE",
                        fontFamily = FontFamily.Monospace,
                        color = if (!showHistory) Color(0xFF00C853) else Color.White,
                        modifier = Modifier.clickable {
                            showHistory = false
                            selectedOrderIds = emptySet()
                        }
                    )

                    Text(
                        text = "ROZLICZONE",
                        fontFamily = FontFamily.Monospace,
                        color = if (showHistory) Color(0xFF00C853) else Color.White,
                        modifier = Modifier.clickable {
                            showHistory = true
                            selectedOrderIds = emptySet()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredOrders = if (showHistory) {
                    ordersUi.filter { it.isRozliczone }
                } else {
                    ordersUi.filter { !it.isRozliczone }
                }

// 👉 sortowanie tylko dla NIEROZLICZONYCH (czyli showHistory == false)
                val ordersToShow = if (!showHistory) {
                    filteredOrders.sortedBy { order ->
                        parseOrderDate(order.data)
                    }
                } else {
                    filteredOrders
                }

                Text(
                    text = "${ordersToShow.size} ${if (showHistory) "rozliczone" else "nierozliczone"}",
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (ordersToShow.isEmpty()) {
                        item {
                            Text(
                                text = if (showHistory) {
                                    "Brak rozliczonych zamówień"
                                } else {
                                    "Brak nierozliczonych zamówień"
                                },
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    items(
                        items = ordersToShow,
                        key = { order -> order.id }
                    ) { order ->
                        OrderCard(
                            order = order,
                            isSelected = selectedOrderIds.contains(order.id),
                            onClick = {
                                if (selectedOrderIds.isEmpty()) {
                                    selectedOrder = order
                                } else {
                                    selectedOrderIds =
                                        if (selectedOrderIds.contains(order.id)) {
                                            selectedOrderIds - order.id
                                        } else {
                                            selectedOrderIds + order.id
                                        }
                                }
                            },
                            onLongClick = {
                                selectedOrderIds =
                                    if (selectedOrderIds.contains(order.id)) {
                                        selectedOrderIds - order.id
                                    } else {
                                        selectedOrderIds + order.id
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrderScreen(
    onBack: () -> Unit,
    onSave: (Order) -> Unit
) {

    val dzisiejszaData = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())

    var data by remember { mutableStateOf(dzisiejszaData) }
    var firma by remember { mutableStateOf("") }
    var platnosc by remember { mutableStateOf("") }
    var produkt by remember { mutableStateOf("") }
    var opis by remember { mutableStateOf("") }
    var czyEdytowanoDate by remember { mutableStateOf(false) }
    var czyWyczyszczonoDate by remember { mutableStateOf(false) }
    var produktFocused by remember { mutableStateOf(false) }
    var opisFocused by remember { mutableStateOf(false) }
    var dataFocused by remember { mutableStateOf(false) }
    val dataDoZapisu = if (data.isNotBlank()) data else dzisiejszaData
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color.Black
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
        )  {

            Text(
                text = "Dodaj zamówienie",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))



            OutlinedTextField(
                value = data,
                onValueChange = { data = it },
                label = { Text("Data") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,

                    focusedBorderColor = Color(0xFF00C853), // zielony przy edycji

                    unfocusedBorderColor = when {
                        data.isNotEmpty() -> Color.White
                        else -> Color.Gray
                    },

                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        dataFocused = it.isFocused

                        if (it.isFocused && !czyWyczyszczonoDate) {
                            data = ""
                            czyWyczyszczonoDate = true
                        }
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Firma", color = Color.White)

            Spacer(modifier = Modifier.height(6.dp))

            Row {

                OutlinedButton(
                    onClick = { firma = "ZNT" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (firma == "ZNT") Color(0xFF00C853) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (firma == "ZNT") Color(0xFF00C853) else Color.White
                    )
                ) {
                    Text("ZNT")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { firma = "MTBS" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (firma == "MTBS") Color(0xFF00C853) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (firma == "MTBS") Color(0xFF00C853) else Color.White
                    )
                ) {
                    Text("MTBS")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Płatność",  color = Color.White)

            Spacer(modifier = Modifier.height(6.dp))

            Row {

                OutlinedButton(
                    onClick = { platnosc = "Karta" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (platnosc == "Karta") Color(0xFF00C853) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (platnosc == "Karta") Color(0xFF00C853) else Color.White
                    )
                ) {
                    Text("Karta")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { platnosc = "Środki własne" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (platnosc == "Środki własne") Color(0xFF00C853) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (platnosc == "Środki własne") Color(0xFF00C853) else Color.White
                    )
                ) {
                    Text("Środki własne")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = produkt,
                onValueChange = { produkt = it },
                label = { Text("Produkt") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,

                    focusedBorderColor = Color(0xFF00C853),

                    unfocusedBorderColor = when {
                        produkt.isNotEmpty() -> Color.White
                        else -> Color.Gray
                    },

                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        produktFocused = it.isFocused
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = opis,
                onValueChange = { opis = it },
                label = { Text("Opis") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,

                    focusedBorderColor = Color(0xFF00C853),

                    unfocusedBorderColor = when {
                        opis.isNotEmpty() -> Color.White
                        else -> Color.Gray
                    },

                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        opisFocused = it.isFocused
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val canSave = produkt.isNotBlank() && firma.isNotBlank() && platnosc.isNotBlank()

                OutlinedButton(
                    enabled = canSave,
                    onClick = {
                        if (!czyDataPoprawna(dataDoZapisu)) {
                            Toast.makeText(context, "Niepoprawna data!", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }

                        val newOrder = Order(
                            data = dataDoZapisu,
                            firma = firma,
                            platnosc = platnosc,
                            produkt = produkt.trim(),
                            opis = opis.trim()
                        )
                        onSave(newOrder)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (canSave) Color(0xFF00C853) else Color.Transparent,
                        contentColor = if (canSave) Color.Black else Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Gray
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (canSave) Color(0xFF00C853) else Color.Gray
                    )
                ) {
                    Text("ZAPISZ")
                }

                Spacer(modifier = Modifier.width(30.dp))
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Text("WRÓĆ")
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
)
{



    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFF00C853) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = order.produkt,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            Text(
                text = "${order.firma} • ${order.platnosc}",
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            Text(
                text = "Data: ${order.data}",
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            if (!order.isRozliczone) {

                val dni = policzDni(order.data)

                val kolorDni = when {
                    dni < 5 -> Color.LightGray
                    dni < 7 -> Color(0xFFFFC107)
                    else -> Color.Red
                }

                Text(
                    text = "Minęło: $dni dni",
                    fontFamily = FontFamily.Monospace,
                    color = kolorDni,
                    fontSize = 12.sp
                )
            }



            Text(
                text = if (order.isRozliczone) "ROZLICZONE" else "NIEROZLICZONE",
                fontFamily = FontFamily.Monospace,
                color = if (order.isRozliczone) Color.White else Color(0xFF00C853)
            )
        }
    }
}

@Composable
fun OrderDetailsScreen(
    order: Order,
    onBack: () -> Unit,
    onRozlicz: () -> Unit,
    onCofnij: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp)) // 👈 to dodaje odstęp od góry

        Text(
            text = "SZCZEGÓŁY",
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Produkt: ${order.produkt}", color = Color.White, fontFamily = FontFamily.Monospace)
        Text("Firma: ${order.firma}", color = Color.White, fontFamily = FontFamily.Monospace)
        Text("Płatność: ${order.platnosc}", color = Color.White, fontFamily = FontFamily.Monospace)

        if (order.opis.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Opis:", color = Color.LightGray, fontFamily = FontFamily.Monospace)
            Text(order.opis, color = Color.White, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Data: ${order.data}",
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        if (!order.isRozliczone) {
            val dni = policzDni(order.data)

            Text(
                text = "Minęło: $dni dni",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }

        if (order.isRozliczone) {
            Text(
                text = "Zakończono: ${order.dataRozliczenia}",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!order.isRozliczone) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                OutlinedButton(
                    onClick = onRozlicz,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Text("ROZLICZ")
                }

                Spacer(modifier = Modifier.width(30.dp)) // odstęp

                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Text("WRÓĆ")
                }
            }

        } else {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                OutlinedButton(
                    onClick = onCofnij,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Text("COFNIJ")
                }

                Spacer(modifier = Modifier.width(30.dp))

                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Text("WRÓĆ")
                }
            }
        }
    }
}

fun policzDni(dataString: String): Long {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    return try {
        val dataZamowienia = LocalDate.parse(dataString, formatter)
        val dzis = LocalDate.now()

        if (dataZamowienia.isAfter(dzis)) return 0

        ChronoUnit.DAYS.between(dataZamowienia, dzis)
    } catch (e: Exception) {
        0
    }
}

fun parseOrderDate(dataString: String): LocalDate {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    return try {
        LocalDate.parse(dataString, formatter)
    } catch (e: DateTimeParseException) {
        LocalDate.MAX
    }
}

fun czyDataPoprawna(data: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    return try {
        val wpisanaData = LocalDate.parse(data, formatter)
        val dzisiaj = LocalDate.now()

        !wpisanaData.isAfter(dzisiaj)
    } catch (e: DateTimeParseException) {
        false
    }
}
