package com.example.a2dand3dapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.a2dand3dapp.ui.theme._2DAnd3DAppTheme
import io.github.sceneview.Scene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.node.ModelNode
import androidx.compose.runtime.LaunchedEffect
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.node.CameraNode
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

enum class ModelType { MODEL_2D, MODEL_3D }

data class ModelItem(
    val name: String,
    val type: ModelType,
    val assetPath: String,
    val format: String,
    val createdAt: String
) {
    val url: String get() = "file:///android_asset/" + assetPath.trimStart('/')
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _2DAnd3DAppTheme {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App() {
    val context = LocalContext.current
    // Wczytanie listy modeli z assets/models.json (offline)
    val items = remember {
        loadModelsFromAssets(context) ?: defaultModelsFallback()
    }

    var selected by remember { mutableStateOf<ModelItem?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selected == null) "Modele 2D i 3D" else selected!!.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (selected != null) {
                        IconButton(onClick = { selected = null }) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_media_previous),
                                contentDescription = "Wstecz"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (selected == null) {
            ModelList(
                items = items,
                onItemClick = { selected = it },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            ModelViewer(
                item = selected!!,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun ModelList(
    items: List<ModelItem>,
    onItemClick: (ModelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Miniatura (dla 2D pokaż obraz, dla 3D ikonka)
                if (item.type == ModelType.MODEL_2D) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.height(48.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_rotate),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Format: ${item.format}  •  Data: ${item.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelViewer(item: ModelItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Obszar podglądu
        if (item.type == ModelType.MODEL_3D) {
            ThreeDViewerNative(url = item.url, modifier = Modifier.weight(1f))
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.url)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Pasek z metadanymi
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Nazwa pliku: ${item.name}")
            Text(text = "Format: ${item.format}")
            Text(text = "Data utworzenia: ${item.createdAt}")
            Text(text = "Źródło: ${item.url}")
        }
    }
}

@Composable
private fun ThreeDViewerNative(url: String, modifier: Modifier = Modifier) {
    val assetPath = url.removePrefix("file:///android_asset/")
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val coroutineScope = rememberCoroutineScope()
    val cameraNode = rememberCameraNode(engine).apply {
        position = dev.romainguy.kotlin.math.Float3(0f, 1f, 5f)
    }

    var modelNode by remember { mutableStateOf<ModelNode?>(null) }

    LaunchedEffect(assetPath) {
        coroutineScope.launch {
            try {
                val modelInstance = modelLoader.createModelInstance(assetPath)
                modelNode = ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = 2.0f
                ).apply {
                    centerOrigin()
                }
            } catch (e: Exception) {
                android.util.Log.e("ThreeDViewer", "Błąd ładowania modelu: $e")
            }
        }
    }

    Scene(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        childNodes = listOfNotNull(modelNode),
        onFrame = {
            // Wycentruj kamerę na modelu przy pierwszej klatce
            modelNode?.let { node ->
                cameraNode.lookAt(node)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    _2DAnd3DAppTheme { App() }
}

// --- Offline: loader models.json z assets ---
private fun loadModelsFromAssets(context: android.content.Context): List<ModelItem>? = try {
    val json = context.assets.open("models.json").bufferedReader().use { it.readText() }
    parseModelsJson(json)
} catch (t: Throwable) {
    null
}

private fun parseModelsJson(json: String): List<ModelItem> {
    val arr = JSONArray(json)
    val out = ArrayList<ModelItem>(arr.length())
    for (i in 0 until arr.length()) {
        val o: JSONObject = arr.getJSONObject(i)
        val type = when (o.optString("type").lowercase()) {
            "3d" -> ModelType.MODEL_3D
            else -> ModelType.MODEL_2D
        }
        out += ModelItem(
            name = o.optString("name"),
            type = type,
            assetPath = o.optString("path"),
            format = o.optString("format"),
            createdAt = o.optString("createdAt")
        )
    }
    return out
}

// Fallback, gdy brak models.json – wskazuje na przykładowe ścieżki w assets
private fun defaultModelsFallback(): List<ModelItem> = listOf(
    ModelItem(
        name = "Logo (SVG)",
        type = ModelType.MODEL_2D,
        assetPath = "images/2d/logo.svg",
        format = "svg",
        createdAt = "2025-01-01"
    ),
    ModelItem(
        name = "Przykładowy obraz (SVG)",
        type = ModelType.MODEL_2D,
        assetPath = "images/2d/sample.svg",
        format = "svg",
        createdAt = "2025-01-01"
    ),
    ModelItem(
        name = "Model 3D (GLB)",
        type = ModelType.MODEL_3D,
        assetPath = "models/3d/helmet.glb",
        format = "glb",
        createdAt = "2025-01-01"
    )
)