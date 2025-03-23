package me.pseudoapp.views.scrollbar

/*
    Copyright 2025 Jaehwa Noh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


/**
 * Scrollbar with [LazyListState].
 * @param lazyListState Pass the lazy composable state which will control by this scrollbar.
 * @param coroutineScope The coroutine scope to run scrollBy suspend function.
 * @param modifier Modify this screen.
 */
@Composable
internal fun LazyListScrollbarScreen(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    var thumbOffsetState by remember { mutableFloatStateOf(0f) }
    var totalItemNumberState by remember { mutableIntStateOf(0) }
    var totalSizeState by remember { mutableIntStateOf(0) }
    var viewportSizeState by remember { mutableIntStateOf(0) }
    var thumbSizeState by remember { mutableFloatStateOf(0f) }
    var isNeedAnUpdateState by remember { mutableStateOf(false) }
    var totalItemInfoState by remember { mutableStateOf(intArrayOf()) }
    var orientationState by remember { mutableStateOf(Orientation.Vertical) }
    var visibilityState by remember { mutableStateOf(true) }
    val thumbSizePx = with(LocalDensity.current) { thumbSizeState.dp.toPx() }

    LaunchedEffect(lazyListState) {
        combine(
            snapshotFlow {
                lazyListState.layoutInfo.orientation
            }.distinctUntilChanged(),
            snapshotFlow {
                lazyListState.layoutInfo.viewportSize
            }.distinctUntilChanged(),
        ) { orientation, viewportSize ->
            orientationState = orientation
            viewportSizeState =
                if (orientation == Orientation.Vertical) {
                    viewportSize.height
                } else {
                    viewportSize.width
                }
        }.collect()
    }

    LaunchedEffect(lazyListState, totalSizeState, viewportSizeState) {
        launch {
            combine(
                snapshotFlow {
                    lazyListState.layoutInfo.totalItemsCount
                }.distinctUntilChanged(),
                snapshotFlow {
                    lazyListState.layoutInfo.visibleItemsInfo
                },
            ) { totalItemCount, visibleItemInfo ->

                if (totalItemNumberState != totalItemCount) {
                    isNeedAnUpdateState = true
                    totalItemNumberState = totalItemCount
                }

                if (totalItemNumberState == 0) return@combine
                if (isNeedAnUpdateState) {
                    totalItemInfoState =
                        IntArray(totalItemNumberState) {
                            visibleItemInfo.getOrNull(0)?.size ?: 0
                        }
                    isNeedAnUpdateState = false
                }

                visibleItemInfo
                    .filter {
                        totalItemInfoState[it.index] != it.size
                    }.forEach {
                        totalItemInfoState[it.index] = it.size
                    }
                totalSizeState = totalItemInfoState.sum()
                thumbSizeState =
                    if (totalSizeState <= viewportSizeState) {
                        viewportSizeState.toFloat()
                    } else {
                        (viewportSizeState.toFloat() / totalItemNumberState.toFloat()).coerceAtLeast(
                            45f,
                        )
                    }
            }.collect()
        }
    }

    LaunchedEffect(lazyListState, totalSizeState, viewportSizeState) {
        launch {
            combine(
                snapshotFlow {
                    lazyListState.layoutInfo.visibleItemsInfo.lastIndex
                }.distinctUntilChanged(),
                snapshotFlow {
                    lazyListState.layoutInfo.visibleItemsInfo
                        .getOrNull(lazyListState.layoutInfo.visibleItemsInfo.lastIndex)
                        ?.offset
                }.filterNotNull(),
            ) { visibleItemsLastIndex, offset ->
                if (visibleItemsLastIndex == -1) return@combine
                val lastIndex =
                    lazyListState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0

                val sumRange = (lastIndex - 1).coerceAtLeast(0)
                val lastItemSize = viewportSizeState - offset

                val currentPositionByWhole =
                    totalItemInfoState.sumRange(until = sumRange) + lastItemSize
                val scrollEndRatio = 1f - (thumbSizePx / viewportSizeState)

                thumbOffsetState =
                    (currentPositionByWhole.toFloat() - viewportSizeState.toFloat()) /
                            (totalSizeState.toFloat() - viewportSizeState.toFloat()) *
                            scrollEndRatio *
                            viewportSizeState
            }.collect()
        }
    }

//    LaunchedEffect(thumbOffsetState) {
//        visibilityState = true
//        delay(1000)
//        visibilityState = false
//    }

    AnimatedVisibility(
        visibilityState,
        enter =
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = 1000,
                    ),
            ),
        exit =
            fadeOut(
                animationSpec =
                    tween(
                        durationMillis = 1000,
                    ),
            ),
    ) {
        if (orientationState == Orientation.Vertical) {
            Row(modifier = modifier) {
                Spacer(modifier = Modifier.weight(0.96f))
                Box(modifier = Modifier.weight(0.04f)) {
                    ScrollbarTrack()
                    ScrollbarThumb(
                        offset = thumbOffsetState,
                        size = thumbSizeState,
                        viewportSize = viewportSizeState,
                        orientation = orientationState,
                        onDrag = { delta ->
                            val movePercent = delta / (viewportSizeState - thumbSizePx)
                            val totalSize = (totalSizeState.toFloat() - viewportSizeState)
                            val scrollTo = totalSize * movePercent

                            coroutineScope.launch {
                                lazyListState.scrollBy(scrollTo)
                            }
                        },
                    )
                }
            }
        } else {
            Column(modifier = modifier) {
                Spacer(modifier = Modifier.weight(0.96f))
                Box(modifier = Modifier.weight(0.04f)) {
                    ScrollbarTrack()
                    ScrollbarThumb(
                        offset = thumbOffsetState,
                        size = thumbSizeState,
                        viewportSize = viewportSizeState,
                        orientation = orientationState,
                        onDrag = { delta ->
                            val movePercent = delta / (viewportSizeState - thumbSizePx)
                            val totalSize = (totalSizeState.toFloat() - viewportSizeState)
                            val scrollTo = totalSize * movePercent

                            coroutineScope.launch {
                                lazyListState.scrollBy(scrollTo)
                            }
                        },
                    )
                }
            }
        }
    }
}

internal fun IntArray.sumRange(
    start: Int = 0,
    until: Int,
): Int {
    if (this.isEmpty()) {
        return 0
    }
    var sum = 0
    for (i in start..until) {
        sum += this[i]
    }
    return sum
}
