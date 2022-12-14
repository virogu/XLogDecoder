import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.virogu.pager.MainView
import theme.MainTheme

/**
 * @author Virogu
 * @since 2022-09-01 10:50
 **/

@Composable
@Preview
fun WindowScope.App(
    window: ComposeWindow,
    applicationScope: ApplicationScope,
    state: WindowState
) {
    MainTheme {
        Surface {
            Column {
                //AppTitleView(applicationScope, state)
                MainView(window)
            }
        }
    }
}

@Composable
private fun WindowScope.AppTitleView(
    applicationScope: ApplicationScope, state: WindowState
) {
    WindowDraggableArea {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            Row(Modifier.align(Alignment.CenterEnd)) {
                Button(
                    modifier = Modifier.size(50.dp, 40.dp), onClick = {
                        applicationScope.exitApplication()
                    }, colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = Color.White,
                    ), elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ), contentPadding = PaddingValues(0.dp)
                ) {
                    Image(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }
        }
    }
}