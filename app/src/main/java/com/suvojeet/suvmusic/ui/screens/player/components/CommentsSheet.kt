package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.data.model.Comment
import com.suvojeet.suvmusic.ui.components.PulseLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    isVisible: Boolean,
    comments: List<Comment>?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    accentColor: Color,
    isLoggedIn: Boolean = false,
    isPostingComment: Boolean = false,
    onPostComment: (String) -> Unit = {},
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (comments != null) {
                        Text(
                            text = "${comments.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Comments list
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PulseLoadingIndicator(modifier = Modifier.size(48.dp), color = accentColor)
                        }
                    } else if (comments.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No comments available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                        
                        // Check for infinite scroll — track totalItemsCount so that
                        // after new comments load, if still at bottom, it re-triggers.
                        val scrollInfo by remember {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                                val totalItems = layoutInfo.totalItemsCount
                                val isAtBottom = if (totalItems == 0 || visibleItemsInfo.isEmpty()) {
                                    false
                                } else {
                                    val lastVisibleItem = visibleItemsInfo.last()
                                    val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
                                    (lastVisibleItem.index + 1 == totalItems) &&
                                    (lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight)
                                }
                                Pair(isAtBottom, totalItems)
                            }
                        }
                        
                        LaunchedEffect(scrollInfo) {
                            if (scrollInfo.first && !isLoadingMore && !isLoading) {
                                onLoadMore()
                            }
                        }
                        
                        // Scroll to top when a new comment is posted (optimistic)
                        LaunchedEffect(comments.size) {
                            if (comments.isNotEmpty() && comments.first().id.startsWith("temp_")) {
                                listState.animateScrollToItem(0)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            itemsIndexed(
                                items = comments,
                                key = { index, comment -> "${comment.id}_$index" }
                            ) { index, comment ->
                                val isHighlighted = comment.id.startsWith("temp_")
                                CommentItem(comment = comment, accentColor = accentColor, isHighlighted = isHighlighted)
                            }
                            
                            if (isLoadingMore) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                        LoadingIndicator(modifier = Modifier.size(24.dp), color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Comment input section
                CommentInputSection(
                    isLoggedIn = isLoggedIn,
                    isPosting = isPostingComment,
                    onPostComment = onPostComment,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun CommentInputSection(
    isLoggedIn: Boolean,
    isPosting: Boolean,
    onPostComment: (String) -> Unit,
    accentColor: Color
) {
    var commentText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp)
    ) {
        if (!isLoggedIn) {
            // Not logged in message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign in to add a comment",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Comment input field
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text("Add a comment...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = accentColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank() && !isPosting) {
                                onPostComment(commentText)
                                commentText = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                    singleLine = true,
                    enabled = !isPosting
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank() && !isPosting) {
                            onPostComment(commentText)
                            commentText = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = commentText.isNotBlank() && !isPosting
                ) {
                    if (isPosting) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Post comment",
                            tint = if (commentText.isNotBlank()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    accentColor: Color,
    isHighlighted: Boolean = false
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHighlighted) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        // Avatar
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(comment.authorThumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            // Author and Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Text
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actions (Likes)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = "Likes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = comment.likeCount,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

