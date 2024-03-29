package com.virogu.pager.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.virogu.bean.LogEntity

/**
 * @author Virogu
 * @since 2022-08-31 15:47
 **/

@Composable
fun LogView(
    modifier: Modifier = Modifier.fillMaxSize(),
    logList: SnapshotStateList<LogEntity>,
    state: LazyListState,
    adapter: ScrollbarAdapter,
    reverseLayout: Boolean = true,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    //val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(logList.size) {
        state.scrollListToEnd()
    }
    Box(modifier = modifier) {
        SelectionContainer {
            LazyColumn(
                Modifier.fillMaxSize(),
                state,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
            ) {
                items(logList) { log ->
                    Text(text = log.msg, modifier = Modifier.fillMaxWidth(), color = log.color)
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = adapter,
            reverseLayout = reverseLayout,
        )
    }
}

private suspend fun LazyListState.scrollListToEnd(
    //当前列表停留位置距离底部不超过此数量时就自动滑动到底部
    offset: Int = 10
) {
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val total = layoutInfo.totalItemsCount
    visibleItemsInfo.getOrNull(visibleItemsInfo.lastIndex)?.also { layout ->
        if (layout.index in (total - offset)..total) {
            //println("${layout.index} scrollToItem: $total layout.offset: ${layout.offset}")
            try {
                scrollToItem(total, 0)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            //println("${layout.index} not scrollToItem: $total")
        }
    }
}