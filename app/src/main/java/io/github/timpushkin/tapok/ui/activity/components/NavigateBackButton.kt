package io.github.timpushkin.tapok.ui.activity.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.timpushkin.tapok.R

@Composable
fun NavigateBackButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            painterResource(R.drawable.arrow_back),
            contentDescription = stringResource(R.string.navigate_back)
        )
    }
}
