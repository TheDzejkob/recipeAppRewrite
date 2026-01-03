package com.example.recipeapprewrite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipeapprewrite.ui.theme.RecipeAppRewriteTheme
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// API Models
@Serializable
data class FoodSearchResponse(
    val products: List<Product> = emptyList()
)

@Serializable
data class Product(
    val product_name: String? = null,
    val brands: String? = null
) {
    val displayName: String
        get() = listOfNotNull(product_name, brands).joinToString(" - ")
}

// Data class for ingredients
data class IngredientItem(
    val name: String,
    val quantity: Int = 1
)

// Singleton Ktor Client
object ApiClient {
    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeAppRewriteTheme {
                var selectedItem by remember { mutableIntStateOf(0) }
                var showAddOverlay by remember { mutableStateOf(false) }

                val items = listOf("Home", "Add", "Profile")
                val icons = listOf(Icons.Filled.Home, Icons.Filled.Add, Icons.Filled.Person)

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar {
                                items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        icon = { Icon(icons[index], contentDescription = item) },
                                        label = { Text(item) },
                                        selected = selectedItem == index,
                                        onClick = {
                                            if (item == "Add") {
                                                showAddOverlay = true
                                            } else {
                                                selectedItem = index
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Greeting(
                                name = items[selectedItem],
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showAddOverlay,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        AddOverlay(onDismiss = { showAddOverlay = false })
                    }
                }
            }
        }
    }
}

@Composable
fun AddOverlay(onDismiss: () -> Unit) {
    val addedIngredients = remember { mutableStateListOf<IngredientItem>() }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Product>()) }

    LaunchedEffect(searchText) {
        if (searchText.length > 2) {
            try {
                val response: FoodSearchResponse = ApiClient.client.get("https://world.openfoodfacts.org/cgi/search.pl") {
                    parameter("search_terms", searchText)
                    parameter("search_simple", 1)
                    parameter("action", "process")
                    parameter("json", 1)
                    parameter("page_size", 5)
                }.body()
                searchResults = response.products
            } catch (e: Exception) {
                e.printStackTrace()
                searchResults = emptyList()
            }
        } else {
            searchResults = emptyList()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Row {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Camera")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search ingredients...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )

                if (searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            searchResults.forEach { product ->
                                val name = product.displayName
                                if (name.isNotBlank()) {
                                    Text(
                                        text = name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                addedIngredients.add(IngredientItem(name))
                                                searchText = ""
                                                searchResults = emptyList()
                                            }
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(addedIngredients) { ingredient ->
                        IngredientRow(
                            ingredient = ingredient,
                            onIncrement = {
                                val index = addedIngredients.indexOf(ingredient)
                                if (index != -1) {
                                    addedIngredients[index] = ingredient.copy(quantity = ingredient.quantity + 1)
                                }
                            },
                            onDecrement = {
                                val index = addedIngredients.indexOf(ingredient)
                                if (index != -1) {
                                    if (ingredient.quantity > 1) {
                                        addedIngredients[index] = ingredient.copy(quantity = ingredient.quantity - 1)
                                    } else {
                                        addedIngredients.removeAt(index)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientRow(
    ingredient: IngredientItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ingredient.name,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = ingredient.quantity.toString(),
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecipeAppRewriteTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Greeting(
                name = "Android",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
