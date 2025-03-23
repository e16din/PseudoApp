package me.pseudoapp.views.scrollbar

import androidx.compose.ui.graphics.Color

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


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag


/**
 * Track for scrollbar
 *
 * @param modifier modify this screen.
 */
@Composable
internal fun ScrollbarTrack(modifier: Modifier = Modifier) {
    val trackColor = Color.White

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    trackColor,
                    alpha = 0.6f,
                )
            },
    )
}
