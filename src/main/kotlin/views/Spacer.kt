package views

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * @author Virogu
 * @since 2022-08-31 18:22
 **/

@Composable
fun Spacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}