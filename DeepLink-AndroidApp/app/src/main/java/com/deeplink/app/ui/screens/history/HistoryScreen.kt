package com.deeplink.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deeplink.app.data.model.VideoHistoryItem
import com.deeplink.app.ui.components.AppCard
import com.deeplink.app.ui.components.EmptyStateIllustration
import com.deeplink.app.ui.components.ScreenEnterAnimation
import com.deeplink.app.ui.components.SkeletonBlock
import com.deeplink.app.ui.components.StatusChip
import com.deeplink.app.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    refreshOnOpen: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var videoToDelete by remember { mutableStateOf<VideoHistoryItem?>(null) }
    var initialLoadDone by remember { mutableStateOf(false) }

    LaunchedEffect(refreshOnOpen) {
        if (refreshOnOpen) {
            viewModel.refreshHistory()
            onRefreshConsumed()
            initialLoadDone = true
        } else if (!initialLoadDone) {
            viewModel.loadHistory()
            initialLoadDone = true
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete video?") },
            text = {
                Text(
                    "Remove \"${video.title ?: "Video #${video.id}"}\" from your history? " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(video.id) { videoToDelete = null }
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    repeat(5) {
                        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(90.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
            uiState.error != null && uiState.videos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshHistory() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (uiState.videos.isEmpty()) {
                        EmptyStateIllustration(
                            icon = Icons.Default.HourglassEmpty,
                            title = "No history yet",
                            subtitle = "Processed videos will appear here.",
                            modifier = Modifier.fillMaxSize().padding(24.dp)
                        )
                    } else {
                        ScreenEnterAnimation {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.videos, key = { it.id }) { video ->
                                    SwipeableHistoryItem(
                                        video = video,
                                        onClick = { onNavigateToDetail(video.id) },
                                        onDelete = { videoToDelete = video }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHistoryItem(
    video: VideoHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            VideoHistoryCard(
                video = video,
                onClick = onClick,
                onLongClick = onDelete
            )
        }
    )
}

@Composable
private fun VideoHistoryCard(
    video: VideoHistoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            Text(
                text = video.title ?: "Video #${video.id}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            video.url?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatusChip(video.status ?: "unknown")
            video.createdAt?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Swipe left or long press to delete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
