package com.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.timpushkin.tapok.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddRuleFab(vm: RulesViewModel = hiltViewModel()) {
    MediumFloatingActionButton(onClick = { vm.startRuleEditing(null) }) {
        Icon(
            painterResource(R.drawable.add),
            contentDescription = stringResource(R.string.create_rule)
        )
    }
}
