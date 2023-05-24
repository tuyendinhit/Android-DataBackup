package com.xayah.databackup.ui.activity.main.page.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.component.EnvCard
import com.xayah.databackup.ui.activity.main.router.ScaffoldRoutes
import com.xayah.databackup.ui.activity.main.router.navigateAndPopBackStack
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.State
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.databackup.util.saveAppVersionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalMaterial3Api
@Composable
fun PageEnv(viewModel: GuideViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contents = listOf(
        stringResource(id = R.string.grant_root_access),
        stringResource(id = R.string.release_prebuilt_binaries),
    )
    val states = remember { mutableStateOf(listOf<State>(State.Loading, State.Loading)) }
    val runAndValidate: suspend (run: suspend () -> Unit) -> Unit = { run ->
        run()
        var allValidated = true
        states.value.forEach {
            if (it != State.Succeed) allValidated = false
        }
        if (allValidated) {
            context.saveAppVersionName()
            viewModel.toUiState(
                GuideUiState.Env(
                    title = context.getString(R.string.environment_detection),
                    fabIcon = Icons.Rounded.Check,
                    onFabClick = { scaffoldNavController, _ ->
                        scaffoldNavController.navigateAndPopBackStack(ScaffoldRoutes.Main.route)
                    }
                )
            )
        }
    }

    val onClicks = listOf<suspend () -> Unit>(
        {
            if (states.value[0] != State.Succeed)
                runAndValidate {
                    withContext(Dispatchers.IO) {
                        val statesList = states.value.toMutableList()
                        statesList[0] = if (Shell.getShell().isRoot) State.Succeed else State.Failed
                        states.value = statesList.toList()
                    }
                }
        },
        {
            if (states.value[1] != State.Succeed)
                runAndValidate {
                    val statesList = states.value.toMutableList()
                    statesList[1] = if (EnvUtil.releaseBin(context)) State.Succeed else State.Failed
                    states.value = statesList.toList()
                }
        }
    )

    LaunchedEffect(null) {
        viewModel.toUiState(
            GuideUiState.Env(
                title = context.getString(R.string.environment_detection),
                fabIcon = Icons.Rounded.ArrowForward,
                onFabClick = { _, _ ->
                    scope.launch {
                        for (i in onClicks) {
                            i()
                        }
                    }
                }
            )
        )
    }

    LazyColumn(
        modifier = Modifier.paddingTop(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
    ) {
        items(count = contents.size, key = { it }) {
            EnvCard(
                content = contents[it],
                state = states.value[it],
                onClick = {
                    scope.launch {
                        onClicks[it]()
                    }
                })
        }
        item {
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingSmall))
        }
    }
}
