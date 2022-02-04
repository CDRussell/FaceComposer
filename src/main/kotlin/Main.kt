// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("FunctionName")

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.opencv.core.CvException
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgcodecs.Imgcodecs.imread
import origami.Filter
import origami.Filters.NoOP
import origami.Origami
import origami.filters.*
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import kotlin.math.roundToInt

@Composable
@Preview
fun App(
    imageName: MutableState<String>,
    filePickerState: MutableState<FilePickerDialogState>,
    showMenu: MutableState<Boolean>,
    filter: MutableState<Filter>,
    filterModifiers: MutableState<ImageFilters>,
    showImageFilterAdjusters: MutableState<Boolean>,
) {

    MaterialTheme {

        Row() {
            AnimatedVisibility(visible = showMenu.value) {
                MenuPanel(filePickerState, filter, showImageFilterAdjusters)
            }
            ContentPanel(imageName, filePickerState, filter, filterModifiers, showImageFilterAdjusters)
        }

    }
}

@Composable
private fun ContentPanel(
    name: MutableState<String>,
    filePickerState: MutableState<FilePickerDialogState>,
    filter: MutableState<Filter>,
    filterModifiers: MutableState<ImageFilters>,
    showImageFilterAdjusters: MutableState<Boolean>,
) {
    Box(
        modifier = Modifier.fillMaxHeight()
            .fillMaxWidth()
            .background(Color(0, 255, 0, 100)),
        contentAlignment = Alignment.Center
    ) {

        if (filePickerState.value.showLoadDialog) {
            showLoadImageDialog(name, filePickerState)
        }

        if (name.value.isBlank()) {
            // hack to save loading an image manually every time
            name.value = "/Users/craig/dev/source/untitled/src/main/resources/sample-face.jpg"
        }

        if (name.value.isBlank()) {
            DropImageOrButton(filePickerState)
        } else {

            Column {
                FilterControls(filter.value, filterModifiers, showImageFilterAdjusters)
                applyFilter(filter.value, filterModifiers)

                Image(
                    bitmap = asImageAsset(filter.value.apply(imread(name.value))),
                    contentDescription = "Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

fun applyFilter(filter: Filter, filterModifiers: MutableState<ImageFilters>) {
    when (filter) {
        is Canny -> filter.threshold1 = filterModifiers.value.cannyFilter.threshold.toInt()
        is Contours -> filter.thickness = (filterModifiers.value.contourFilter.thickness * 100).roundToInt()
        is Fisheye -> filter.fishVal = filterModifiers.value.cannyFilter.threshold
    }
}

@Composable
private fun FilterControls(
    filter: Filter,
    filterModifiers: MutableState<ImageFilters>,
    showImageFilterAdjusters: MutableState<Boolean>
) {

    AnimatedVisibility(showImageFilterAdjusters.value) {
        when (filter) {
            is Canny -> CannyFilters(filterModifiers)
            is Contours -> ContoursFilters(filterModifiers)
            is Fisheye -> FisheyeFilters(filterModifiers)
        }
    }
}

@Composable
fun CannyFilters(filterModifiers: MutableState<ImageFilters>) {
    Column {
        Text(filterModifiers.value.cannyFilter.threshold.toString())
        Slider(steps = 0, valueRange = 1f..300f, value = filterModifiers.value.cannyFilter.threshold, onValueChange = { value->
            filterModifiers.update { it.cannyFilter.threshold = value }
        })
    }
}

@Composable
fun ContoursFilters(filterModifiers: MutableState<ImageFilters>) {
    Column {
        Text((filterModifiers.value.contourFilter.thickness * 100).roundToInt().toString())
        Slider(
            valueRange = 0f..0.15f,
            value = filterModifiers.value.contourFilter.thickness,
            onValueChange = { value -> filterModifiers.update { it.contourFilter.thickness = value } }
        )
    }
}


@Composable
fun FisheyeFilters(filterModifiers: MutableState<ImageFilters>) {
    Column {
        Text(filterModifiers.value.cannyFilter.threshold.toString())
        Slider(steps = 0, valueRange = 1f..300f, value = filterModifiers.value.cannyFilter.threshold, onValueChange = { value ->
            filterModifiers.update { it.cannyFilter.threshold = value }
        })
    }
}

@Composable
private fun DropImageOrButton(filePickerState: MutableState<FilePickerDialogState>) {
    Column {
        Button(onClick = {
            filePickerState.update() { it.showLoadDialog = true }
        }) {
            Text("Open File Picker")
        }
        Text("Or drop a file . . .")
    }
}

private fun <T> MutableState<T>.update(function: (T) -> Unit) {
    function(this.value)
    this.value = this.value
}

@Composable
private fun showLoadImageDialog(
    name: MutableState<String>,
    filePickerState: MutableState<FilePickerDialogState>
) {
    FileDialog(
        onCloseRequest = { selectedFiles ->
            filePickerState.update() { it.showLoadDialog = false }
            name.value = selectedFiles?.firstOrNull()?.absolutePath ?: ""
            println("Result: ${name.value}")
        }
    )
}

@Composable
private fun MenuPanel(filePickerState: MutableState<FilePickerDialogState>, filter: MutableState<Filter>, showImageFilterAdjusters: MutableState<Boolean>) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.3f)
            .background(Color(50, 200, 50, 255))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.3f)
                    .fillMaxWidth()
                    .background(Color(255, 0, 0, 100)), contentAlignment = Alignment.Center
            ) {
                MainMenu(filePickerState)
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(Color(22, 45, 200, 100)), contentAlignment = Alignment.Center
            ) {
                FilterMenu(filter, showImageFilterAdjusters)
            }
        }
    }
}

@Composable
private fun MainMenu(filePickerState: MutableState<FilePickerDialogState>) {
    LazyColumn {
        item {
            Button(onClick = {
                filePickerState.update() { it.showLoadDialog = true }
            }) {
                Text("Load new image")
            }
        }
        item {
            Button(onClick = {

            }) {
                Text("Save image")
            }
        }
    }
}

@Composable
private fun FilterMenu(filter: MutableState<Filter>, showImageFilterAdjusters: MutableState<Boolean>) {
    Column {
        Text("Apply filters")

        Button(onClick = { filter.value = Canny(); showImageFilterAdjusters.value = true }) { Text("Canny") }
        Button(onClick = { filter.value = Cartoon2(); showImageFilterAdjusters.value = false }) { Text("Cartoon") }
        Button(onClick = { filter.value = Contours(); showImageFilterAdjusters.value = true }) { Text("Contours") }
        Button(onClick = { filter.value = Fisheye(); showImageFilterAdjusters.value = true }) { Text("Fisheye") }
        Button(onClick = { filter.value = NoOP(); showImageFilterAdjusters.value = false }) { Text("Clear filter") }
    }
}

fun main() = application {
    val imagePath = remember { mutableStateOf("") }
    val filePickerState = remember { mutableStateOf(FilePickerDialogState(), policy = neverEqualPolicy()) }
    val showMenu = remember { mutableStateOf(false) }
    val filter: MutableState<Filter> = remember { mutableStateOf(NoOP(), policy = neverEqualPolicy()) }
    val filterModifiers = remember { mutableStateOf(ImageFilters(), policy = neverEqualPolicy()) }
    val showImageFilterAdjusters = remember { mutableStateOf(false) }

    showMenu.value = imagePath.value.isNotEmpty()

    Window(onCloseRequest = ::exitApplication) {

        Origami.init()

        LaunchedEffect(Unit) {
            window.dropTarget = DropTarget().apply {
                addDropTargetListener(object : DropTargetAdapter() {
                    override fun drop(event: DropTargetDropEvent) {
                        println("file dropped")
                        event.acceptDrop(DnDConstants.ACTION_COPY)
                        val droppedFiles = event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        println("coroutine started")
                        println(droppedFiles)
                        val file = droppedFiles.first() as File
                        imagePath.value = file.absolutePath
                    }
                })
            }
        }

        App(imagePath, filePickerState, showMenu, filter, filterModifiers, showImageFilterAdjusters)
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    onCloseRequest: (files: List<File>?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (visible) {
                    onCloseRequest(files.asList())
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

fun asImageAsset(image: Mat): ImageBitmap {
    return try {
        val bytes = MatOfByte()
        Imgcodecs.imencode(".jpg", image, bytes)
        val byteArray = ByteArray((image.total() * image.channels()).toInt())
        bytes.get(0, 0, byteArray)
        org.jetbrains.skia.Image.makeFromEncoded(byteArray).toComposeImageBitmap()
    } catch (e: CvException) {
        println(e.stackTraceToString())
        ImageBitmap(0, 0)
    }
}

data class FilePickerDialogState(
    var showLoadDialog: Boolean = false,
    var showSaveDialog: Boolean = false
)

data class ImageFilters(
    val cannyFilter: CannyFilter = CannyFilter(),
    val contourFilter: ContourFilter = ContourFilter()
)

data class CannyFilter(
    var threshold: Float = 200.0F
)

data class ContourFilter(
    var thickness: Float = 0.05F
)