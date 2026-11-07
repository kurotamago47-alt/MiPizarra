package com.mipizarra.app

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val details: String,
    val category: String,
    val color: Long,
    val priority: String,
    val dueDate: Long?,
    val completedAt: Long? = null
)

private val noteColors = listOf(0xFFFFE6A7, 0xFFFFC9C9, 0xFFCDECCF, 0xFFCDE7FF, 0xFFE7D5FF, 0xFFFFDCC2)
private val categories = listOf("Personal", "Trabajo", "Compras", "Hogar", "Estudio", "Otros")
private val priorities = listOf("Baja", "Media", "Alta")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiPizarraApp() }
    }
}

@Composable
fun MiPizarraApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mi_pizarra", 0) }
    var tasks by remember { mutableStateOf(loadTasks(prefs)) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var tab by remember { mutableStateOf(0) }
    fun save(updated: List<Task>) { tasks = updated; prefs.edit().putString("tasks", encodeTasks(updated)).apply() }
    val pending = tasks.filter { it.completedAt == null }.sortedWith(compareByDescending<Task> { it.priority == "Alta" }.thenBy { it.dueDate ?: Long.MAX_VALUE })
    val done = tasks.filter { it.completedAt != null }.sortedByDescending { it.completedAt }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF795548), secondary = Color(0xFF9C765D), background = Color(0xFFF4E8D7))) {
        Scaffold(containerColor = Color(0xFFF4E8D7), floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = { editing = null; showEditor = true }, containerColor = Color(0xFF795548), contentColor = Color.White) { Icon(Icons.Default.Add, contentDescription = "Agregar tarea") }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxWidth().background(Color(0xFF795548)).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text("MI PIZARRA", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Tus ideas y pendientes, siempre a la vista", color = Color(0xFFFFE8D2), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip("Pendientes", pending.size.toString())
                        StatChip("Terminadas", done.size.toString())
                    }
                }
                TabRow(selectedTabIndex = tab, containerColor = Color(0xFFEAD8C2), contentColor = Color(0xFF5D4037)) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pizarra") }, icon = { Icon(Icons.Default.Edit, null) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historial") }, icon = { Icon(Icons.Default.Archive, null) })
                }
                if (tab == 0) {
                    if (pending.isEmpty()) EmptyState("¡Pizarra despejada!", "Toca + para agregar tu primer pendiente.")
                    else LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(pending, key = { it.id }) { task -> NoteCard(task, onDone = { save(tasks.map { if (it.id == task.id) it.copy(completedAt = System.currentTimeMillis()) else it }) }, onEdit = { editing = task; showEditor = true }, onDelete = { save(tasks.filterNot { it.id == task.id }) }) }
                    }
                } else {
                    if (done.isEmpty()) EmptyState("Aún no hay tareas terminadas", "Cuando completes un pendiente, aparecerá aquí.")
                    else androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(done, key = { it.id }) { task -> HistoryRow(task, onRestore = { save(tasks.map { if (it.id == task.id) it.copy(completedAt = null) else it }) }, onDelete = { save(tasks.filterNot { it.id == task.id }) }) }
                    }
                }
            }
        }
    }
    if (showEditor) TaskEditor(initial = editing, onDismiss = { showEditor = false }, onSave = { task ->
        save(if (editing == null) tasks + task else tasks.map { if (it.id == task.id) task else it }); showEditor = false
    })
}

@Composable private fun StatChip(label: String, value: String) {
    Surface(color = Color(0xFF8D6E63), shape = RoundedCornerShape(50)) { Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(value, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Text(label, color = Color.White) } }
}

@Composable private fun NoteCard(task: Task, onDone: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val bg = Color(task.color)
    Surface(color = bg, shape = RoundedCornerShape(5.dp), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("✦", color = Color(0xFF8D6E63))
            }
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF3E3028))
            if (task.details.isNotBlank()) Text(task.details, style = MaterialTheme.typography.bodySmall, color = Color(0xFF59483D))
            Spacer(Modifier.weight(1f, fill = false))
            Text("Prioridad: ${task.priority}", style = MaterialTheme.typography.labelSmall, color = priorityColor(task.priority))
            task.dueDate?.let { Text("Vence: ${formatDate(it)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037)) }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) { Icon(Icons.Default.CheckCircle, "Marcar terminada", tint = Color(0xFF52734D)) }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = Color(0xFF6D5142)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFF8A4942)) }
            }
        }
    }
}

@Composable private fun HistoryRow(task: Task, onRestore: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Color(0xFFFFF9F0), shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(Color(task.color), RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.LineThrough, color = Color(0xFF55483F))
                Text("${task.category} · ${task.priority} · ${task.completedAt?.let { formatDateTime(it) } ?: "Terminada"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF806F62))
            }
            IconButton(onClick = onRestore) { Icon(Icons.Default.Restore, "Recuperar tarea", tint = Color(0xFF795548)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar del historial", tint = Color(0xFF8A4942)) }
        }
    }
}

@Composable private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("📌", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
        Text(subtitle, color = Color(0xFF806F62))
    }
}

@Composable private fun TaskEditor(initial: Task?, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var details by remember { mutableStateOf(initial?.details ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: categories.first()) }
    var priority by remember { mutableStateOf(initial?.priority ?: "Media") }
    var color by remember { mutableLongStateOf(initial?.color ?: noteColors.first()) }
    var dueDate by remember { mutableStateOf(initial?.dueDate) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Nueva nota" else "Editar nota") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Pendiente") }, singleLine = true)
            OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Detalles (opcional)") }, minLines = 2)
            Box {
                OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Categoría: $category") }
                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) { categories.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { category = it; categoryExpanded = false }) } }
            }
            Box {
                OutlinedButton(onClick = { priorityExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Prioridad: $priority") }
                DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) { priorities.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { priority = it; priorityExpanded = false }) } }
            }
            Text("Color de la nota", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { noteColors.forEach { c -> Surface(color = Color(c), shape = RoundedCornerShape(50), border = if (c == color) androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF5D4037)) else null, modifier = Modifier.size(30.dp).clickable { color = c }) {} } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance(); dueDate?.let { cal.timeInMillis = it }
                    DatePickerDialog(context, { _, y, m, d -> val chosen = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0); set(Calendar.MILLISECOND, 0) }; dueDate = chosen.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text(if (dueDate == null) "Agregar fecha límite" else "Vence: ${formatDate(dueDate!!)}") }
                if (dueDate != null) TextButton(onClick = { dueDate = null }) { Text("Quitar") }
            }
        }
    }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(Task(id = initial?.id ?: UUID.randomUUID().toString(), title = title.trim(), details = details.trim(), category = category, color = color, priority = priority, dueDate = dueDate, completedAt = initial?.completedAt)) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun priorityColor(p: String) = when (p) { "Alta" -> Color(0xFF9B3F35); "Media" -> Color(0xFF8A641F); else -> Color(0xFF52734D) }
private fun formatDate(time: Long) = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(Date(time))
private fun formatDateTime(time: Long) = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "MX")).format(Date(time))

private fun encodeTasks(tasks: List<Task>): String = JSONArray().apply { tasks.forEach { t -> put(JSONObject().apply {
    put("id", t.id); put("title", t.title); put("details", t.details); put("category", t.category); put("color", t.color); put("priority", t.priority)
    if (t.dueDate == null) put("dueDate", JSONObject.NULL) else put("dueDate", t.dueDate)
    if (t.completedAt == null) put("completedAt", JSONObject.NULL) else put("completedAt", t.completedAt)
}) } }.toString()
private fun loadTasks(prefs: android.content.SharedPreferences): List<Task> = runCatching {
    val arr = JSONArray(prefs.getString("tasks", "[]"))
    (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); Task(
        id = o.getString("id"), title = o.getString("title"), details = o.optString("details"), category = o.optString("category", "Otros"), color = o.optLong("color", noteColors.first()), priority = o.optString("priority", "Media"),
        dueDate = if (o.isNull("dueDate")) null else o.getLong("dueDate"), completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt")
    ) }
}.getOrDefault(emptyList())
