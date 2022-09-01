@file:Suppress("unused", "FunctionName")

package com.virogu.pager

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.virogu.bean.LogEntity
import com.virogu.bean.print
import com.virogu.bean.printE
import com.virogu.bean.printI
import com.virogu.decoder.XLogFileDecoder
import com.virogu.pager.view.FileSelectView
import com.virogu.pager.view.LogView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import views.Spacer
import java.io.File
import java.io.FileNotFoundException
import javax.swing.JFileChooser

/**
 * @author Virogu
 * @since 2022-08-31 15:40
 **/


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainView(window: ComposeWindow) {
    //val stateHorizontal = rememberScrollState(0)
    val coroutineScope = rememberCoroutineScope()

    val logList = remember {
        SnapshotStateList<LogEntity>()
    }
    val logState = rememberLazyListState()
    val logAdapter = rememberScrollbarAdapter(
        scrollState = logState // TextBox height + Spacer height
    )

    val inputPath = remember {
        mutableStateOf(listOf<File>())
    }
    val outputPath = remember {
        mutableStateOf("")
    }
    val autoDecoder = remember {
        mutableStateOf(true)
    }
    val showLogView = remember {
        mutableStateOf(false)
    }

    fun startDecoder() {
        coroutineScope.decoderFiles(
            inputPath.value,
            File(outputPath.value).absoluteFile,
            onError = {
                logList.printE(it.localizedMessage)
            },
            onProcess = {
                logList.print(it)
            },
            onFinish = {
                logList.printI("成功解密 $it 个文件")
            },
        )
    }
    LaunchedEffect(inputPath.value) {
        println("inputPath: ${inputPath.value}")
        if (inputPath.value.isEmpty()) {
            return@LaunchedEffect
        }
        val first = inputPath.value.first()
        if (first.exists()) {
            outputPath.value = if (first.isFile) {
                first.parentFile.absolutePath
            } else {
                first.absolutePath
            }
            println("auto fix output path: ${outputPath.value}")
            if (autoDecoder.value) {
                startDecoder()
            }
        }
    }
    LaunchedEffect(outputPath.value) {
        println("outputPath: ${outputPath.value}")
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(Modifier.weight(5f).fillMaxWidth()) {
            itemWithSpacer {
                FileSelectView(
                    window,
                    label = "待解密文件或目录",
                    text = inputPath.value.joinToString(";"),
                    fileChooserType = JFileChooser.FILES_AND_DIRECTORIES,
                    placeholder = "选择或拖动文件到此处",
                    filesFilter = arrayOf("xlog"),
                    defaultPath = inputPath.value.firstOrNull()?.absolutePath ?: "",
                    multiSelectionEnabled = true,
                ) {
                    inputPath.value = it.toList()
                }
            }
            itemWithSpacer {
                FileSelectView(
                    window,
                    label = "输出目录",
                    text = outputPath.value,
                    fileChooserType = JFileChooser.DIRECTORIES_ONLY,
                    placeholder = "选择或拖动目录到此处",
                    defaultPath = outputPath.value,
                    multiSelectionEnabled = false,
                ) {
                    outputPath.value = it.first().absolutePath
                }
            }
            itemWithSpacer {
                Row {
                    Checkbox(
                        autoDecoder.value, onCheckedChange = {
                            autoDecoder.value = it
                        }, colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.primary
                        )
                    )
                    Text("选择文件后自动解密", modifier = Modifier.align(Alignment.CenterVertically).clickable {
                        autoDecoder.value = !autoDecoder.value
                    })
                }
            }
        }
        Spacer(16.dp)
        Box(Modifier.fillMaxWidth()) {
            Button(
                onClick = ::startDecoder,
                modifier = Modifier.size(50.dp).align(Alignment.Center),
                shape = AbsoluteRoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(8.dp)
            ) {
                Image(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "开始解密",
                    colorFilter = ColorFilter.tint(contentColorFor(MaterialTheme.colors.primary))
                )
            }
        }
        Spacer(16.dp)
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.CenterStart)) {
                Image(
                    modifier = Modifier.align(Alignment.CenterVertically).clickable {
                        showLogView.value = !showLogView.value
                    },
                    imageVector = if (showLogView.value) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.Filled.KeyboardArrowUp
                    },
                    contentDescription = "展开/收起日志",
                    colorFilter = ColorFilter.tint(contentColorFor(MaterialTheme.colors.background)),
                )
                TextButton(
                    onClick = {
                        showLogView.value = !showLogView.value
                    }, modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = if (showLogView.value) {
                            "收起日志"
                        } else {
                            "展开日志"
                        }
                    )
                }
            }
            Column(Modifier.align(Alignment.CenterEnd)) {
                AnimatedVisibility(logList.isNotEmpty()) {
                    TextButton(onClick = {
                        logList.clear()
                    }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("清空日志")
                    }
                }
            }
        }
        AnimatedContent(showLogView.value, transitionSpec = {
            fadeIn() + expandVertically() with fadeOut() + shrinkVertically()
        }) {
            LogView(
                modifier = Modifier.height(if (showLogView.value) 160.dp else 60.dp),
                logList = logList,
                state = logState,
                adapter = logAdapter,
            )
        }
    }
}

private var decoderJob: Job? = null
private fun CoroutineScope.decoderFiles(
    inputFiles: List<File>,
    outputPath: File,
    onError: (Throwable) -> Unit = {},
    onProcess: (String) -> Unit = {},
    onFinish: (Int) -> Unit = {},
) {
    if (decoderJob?.isActive == true) {
        onError(IllegalStateException("已经有解密任务进行中"))
        return
    }
    if (inputFiles.isEmpty()) {
        onError(IllegalArgumentException("请选择待解密文件"))
        return
    }
    if (!outputPath.isDirectory || !outputPath.exists()) {
        onError(IllegalArgumentException("[${outputPath.absolutePath}] 输出目录无效"))
        return
    }
    decoderJob = launch(Dispatchers.IO) {
        inputFiles.forEach { inputFile ->
            var count = 0
            try {
                if (!inputFile.exists()) {
                    throw FileNotFoundException("[${inputFile.absolutePath}] 文件或目录不存在")
                }
                if (inputFile.isFile) {
                    val targetFile = File(outputPath, inputFile.name.plus(".log"))
                    val r = XLogFileDecoder.parseFile(inputFile, targetFile)
                    onProcess("解密${if (r) "成功" else "失败"}[$inputFile] => [${targetFile}]")
                    if (r) {
                        count++
                    }
                } else if (inputFile.isDirectory) {
                    val list = inputFile.listFiles { f ->
                        f.isFile && f.extension.equals("xlog", true)
                    }
                    if (list.isNullOrEmpty()) {
                        throw IllegalArgumentException("[${inputFile.absolutePath}] 目录下未发现任何XLog文件")
                    }
                    list.forEach {
                        val inPath = it
                        val targetFile = File(outputPath, it.name.plus(".log"))
                        try {
                            val r = XLogFileDecoder.parseFile(inPath, targetFile)
                            onProcess("解密${if (r) "成功" else "失败"} [$inPath] => [${targetFile}]")
                            if (r) {
                                count++
                            }
                        } catch (e: Throwable) {
                            onProcess("解密失败 [${inPath}] => [${targetFile}]")
                            onError(e)
                        }
                    }
                }
            } catch (e: Throwable) {
                onError(e)
            }
            onFinish(count)
        }
    }
}

private fun LazyListScope.itemWithSpacer(
    spacerHeight: Dp = 16.dp, key: Any? = null, content: @Composable LazyItemScope.() -> Unit
) {
    item(key) {
        content()
        Spacer(spacerHeight)
    }
}
