package com.supermemo.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supermemo.app.ui.backup.BackupRestoreScreen
import com.supermemo.app.ui.backup.BackupRestoreViewModel
import com.supermemo.app.ui.editor.EditNoteScreen
import com.supermemo.app.ui.editor.EditNoteViewModel
import com.supermemo.app.ui.home.HomeScreen
import com.supermemo.app.ui.home.HomeViewModel
import com.supermemo.app.ui.search.SearchScreen
import com.supermemo.app.ui.search.SearchViewModel
import com.supermemo.app.ui.settings.SettingsScreen
import com.supermemo.app.ui.theme.SuperMemoTheme

sealed class AppScreen {
    object Home : AppScreen()
    data class Editor(val noteId: Long) : AppScreen()
    object Search : AppScreen()
    object BackupRestore : AppScreen()
    object Settings : AppScreen()
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 适配 Android 16 Edge-to-Edge 沉浸全屏
        enableEdgeToEdge()

        val repository = (application as SuperMemoApp).repository

        // 检测是否有外部打开文件的 Intent
        val initialScreen = if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            AppScreen.BackupRestore
        } else {
            AppScreen.Home
        }

        setContent {
            var isAmoledMode by remember { mutableStateOf(false) }

            SuperMemoTheme(isAmoledBlack = isAmoledMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(initialScreen) }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val animDuration = 320
                            val exitDuration = 260
                            val enterEasing = FastOutSlowInEasing
                            val exitEasing = FastOutSlowInEasing

                            when {
                                // 进入编辑器：轻微从底部升起 + 柔和缩放淡入
                                targetState is AppScreen.Editor -> {
                                    (slideInVertically(
                                        initialOffsetY = { it / 6 },
                                        animationSpec = tween(animDuration, easing = enterEasing)
                                    ) + scaleIn(
                                        initialScale = 0.95f,
                                        animationSpec = tween(animDuration, easing = enterEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(animDuration)
                                    )) togetherWith (slideOutVertically(
                                        targetOffsetY = { -it / 16 },
                                        animationSpec = tween(exitDuration, easing = exitEasing)
                                    ) + scaleOut(
                                        targetScale = 0.98f,
                                        animationSpec = tween(exitDuration, easing = exitEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(exitDuration)
                                    ))
                                }
                                // 从编辑器退回主页：轻微向下滑出 + 缩放淡出
                                initialState is AppScreen.Editor && targetState is AppScreen.Home -> {
                                    (slideInVertically(
                                        initialOffsetY = { -it / 16 },
                                        animationSpec = tween(animDuration, easing = enterEasing)
                                    ) + scaleIn(
                                        initialScale = 0.98f,
                                        animationSpec = tween(animDuration, easing = enterEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(animDuration)
                                    )) togetherWith (slideOutVertically(
                                        targetOffsetY = { it / 6 },
                                        animationSpec = tween(exitDuration, easing = exitEasing)
                                    ) + scaleOut(
                                        targetScale = 0.95f,
                                        animationSpec = tween(exitDuration, easing = exitEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(exitDuration)
                                    ))
                                }
                                // 进入搜索、设置、备份恢复页面：现代右侧平滑推入
                                targetState is AppScreen.Search || targetState is AppScreen.Settings || targetState is AppScreen.BackupRestore -> {
                                    (slideInHorizontally(
                                        initialOffsetX = { it / 3 },
                                        animationSpec = tween(animDuration, easing = enterEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(animDuration)
                                    )) togetherWith (slideOutHorizontally(
                                        targetOffsetX = { -it / 6 },
                                        animationSpec = tween(exitDuration, easing = exitEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(exitDuration)
                                    ))
                                }
                                // 从二级页面返回：右侧滑出
                                else -> {
                                    (slideInHorizontally(
                                        initialOffsetX = { -it / 6 },
                                        animationSpec = tween(animDuration, easing = enterEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(animDuration)
                                    )) togetherWith (slideOutHorizontally(
                                        targetOffsetX = { it / 3 },
                                        animationSpec = tween(exitDuration, easing = exitEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(exitDuration)
                                    ))
                                }
                            }
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Home -> {
                                val homeViewModel = remember { HomeViewModel(repository) }
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToEditor = { noteId ->
                                        currentScreen = AppScreen.Editor(noteId)
                                    },
                                    onNavigateToSearch = {
                                        currentScreen = AppScreen.Search
                                    },
                                    onNavigateToBackupRestore = {
                                        currentScreen = AppScreen.BackupRestore
                                    },
                                    onNavigateToSettings = {
                                        currentScreen = AppScreen.Settings
                                    }
                                )
                            }
                            is AppScreen.Editor -> {
                                val editViewModel = remember(screen.noteId) {
                                    EditNoteViewModel(repository, screen.noteId)
                                }
                                EditNoteScreen(
                                    viewModel = editViewModel,
                                    onNavigateBack = {
                                        currentScreen = AppScreen.Home
                                    }
                                )
                            }
                            is AppScreen.Search -> {
                                val searchViewModel = remember { SearchViewModel(repository) }
                                SearchScreen(
                                    viewModel = searchViewModel,
                                    onNavigateBack = {
                                        currentScreen = AppScreen.Home
                                    },
                                    onNavigateToEditor = { noteId ->
                                        currentScreen = AppScreen.Editor(noteId)
                                    }
                                )
                            }
                            is AppScreen.BackupRestore -> {
                                val backupViewModel = remember { BackupRestoreViewModel(repository) }
                                BackupRestoreScreen(
                                    viewModel = backupViewModel,
                                    onNavigateBack = {
                                        currentScreen = AppScreen.Home
                                    }
                                )
                            }
                            is AppScreen.Settings -> {
                                SettingsScreen(
                                    onNavigateBack = {
                                        currentScreen = AppScreen.Home
                                    },
                                    isAmoledMode = isAmoledMode,
                                    onToggleAmoledMode = { isAmoledMode = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
