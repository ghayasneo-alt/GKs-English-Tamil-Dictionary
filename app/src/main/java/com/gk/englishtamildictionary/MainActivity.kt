package com.gk.englishtamildictionary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Teal = Color(0xFF1B5E7B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Teal)) {
                DictionaryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(vm: DictionaryViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GK's English Tamil Dictionary", fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal, titleContentColor = Color.White
                )
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 12.dp).fillMaxSize()
        ) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = vm.query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Type an English word") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (vm.query.isNotEmpty()) IconButton(onClick = { vm.onQueryChange(""); }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() })
            )

            if (vm.modelState == ModelState.MISSING) {
                Card(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Tamil model not downloaded yet. Connect to the internet once; after that Tamil works offline.",
                            Modifier.weight(1f), fontSize = 13.sp
                        )
                        TextButton(onClick = vm::prepareModel) { Text("Retry") }
                    }
                }
            } else if (vm.modelState == ModelState.CHECKING) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("Preparing Tamil translation…", fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }

            if (vm.suggestions.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(vm.suggestions) { s ->
                        Text(
                            s,
                            Modifier.fillMaxWidth().clickableText { vm.search(s) }.padding(vertical = 12.dp, horizontal = 8.dp),
                            fontSize = 16.sp
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                ResultView(vm)
            }
        }
    }
}

@Composable
private fun ResultView(vm: DictionaryViewModel) {
    val e = vm.entry
    when {
        vm.notFound -> Text("No entry found for “${vm.query}”.", Modifier.padding(16.dp))
        e == null -> Text(
            "Start typing a word to see suggestions, then tap one or press search.",
            Modifier.padding(16.dp), color = Color.Gray
        )
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            item {
                Text(e.word, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Teal)
                vm.tamil[e.word]?.let { Text(it, fontSize = 20.sp, color = Color(0xFF8A3B12)) }
            }
            items(e.senses) { s ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F7F9))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(s.pos, fontSize = 12.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("English", fontSize = 11.sp, color = Color.Gray)
                        Text(s.definition, fontSize = 16.sp)
                        s.example?.let {
                            Text("“$it”", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Color.DarkGray)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("தமிழ் (Tamil)", fontSize = 11.sp, color = Color.Gray)
                        val ta = vm.tamil[s.definition]
                        Text(
                            ta ?: when (vm.modelState) {
                                ModelState.READY -> "…"
                                ModelState.CHECKING -> "…"
                                ModelState.MISSING -> "(download the Tamil model once to see translations)"
                            },
                            fontSize = 17.sp, color = Color(0xFF8A3B12)
                        )
                    }
                }
            }
            e.collocations?.let { c ->
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F3E8))) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Commonly used with", fontWeight = FontWeight.SemiBold, color = Teal)
                            Spacer(Modifier.height(4.dp))
                            Text(c, fontSize = 14.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
