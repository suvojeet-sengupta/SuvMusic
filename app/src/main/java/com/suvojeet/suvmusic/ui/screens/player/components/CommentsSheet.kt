package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.data.model.Comment
import com.suvojeet.suvmusic.ui.components.PulseLoadingIndicator

import com.suvojeet.suvmusic.ui.components.DominantColors

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
    onLoadMore: () -> Unit = {},
    dominantColors: DominantColors? = null
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        // Use provided dominant colors or fall back to MaterialTheme
        val backgroundColor = dominantColors?.secondary ?: MaterialTheme.colorScheme.surfaceContainerHigh
        val contentColor = dominantColors?.onBackground ?: MaterialTheme.colorScheme.onSurface
        val finalAccentColor = dominantColors?.accent ?: accentColor
        
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = onDismiss,
            containerColor = backgroundColor,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Header - Material 3 Expressive Style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = contentColor
                        )
                        
                        if (comments != null) {
                            Text(
                                text = "${comments.size} responses",
                                style = MaterialTheme.typography.labelLarge,
                                color = finalAccentColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Surface(
                        color = finalAccentColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                            tint = finalAccentColor
                        )
                    }
                }
                
                // Comments list
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith 
                            fadeOut(animationSpec = tween(400))
                        },
                        label = "loadingState"
                    ) { loading ->
                        if (loading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    PulseLoadingIndicator(modifier = Modifier.size(56.dp), color = finalAccentColor)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Fetching conversation...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else if (comments.isNullOrEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = contentColor.copy(alpha = 0.15f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No comments yet. Start the conversation!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = contentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val listState = rememberLazyListState()
                            
                            // Infinite scroll logic
                            val scrollInfo by remember {
                                derivedStateOf {
                                    val layoutInfo = listState.layoutInfo
                                    val visibleItemsInfo = layoutInfo.visibleItemsInfo
                                    val totalItems = layoutInfo.totalItemsCount
                                    val isAtBottom = if (totalItems == 0 || visibleItemsInfo.isEmpty()) {
                                        false
                                    } else {
                                        val lastVisibleItem = visibleItemsInfo.last()
                                        lastVisibleItem.index + 1 == totalItems
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
                            ) {
                                itemsIndexed(
                                    items = comments,
                                    key = { index, comment -> "${comment.id}_$index" }
                                ) { index, comment ->
                                    val isHighlighted = comment.id.startsWith("temp_")
                                    CommentItem(
                                        comment = comment, 
                                        accentColor = finalAccentColor,
                                        contentColor = contentColor,
                                        isHighlighted = isHighlighted,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                                
                                if (isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp), 
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp), 
                                                color = finalAccentColor,
                                                strokeWidth = 3.dp,
                                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        }
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
                    accentColor = finalAccentColor,
                    backgroundColor = backgroundColor,
                    contentColor = contentColor
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
    accentColor: Color,
    backgroundColor: Color,
    contentColor: Color
) {
    var commentText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Focus state for animation
    var isFocused by remember { mutableStateOf(false) }
    
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) contentColor.copy(alpha = 0.15f)
                      else contentColor.copy(alpha = 0.08f),
        label = "inputContainerColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .imePadding(),
        color = containerColor,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = if (isFocused) 4.dp else 0.dp
    ) {
        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign in to join the discussion",
                    color = accentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            "Add a comment...", 
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accentColor,
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
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
                    singleLine = false,
                    maxLines = 4,
                    enabled = !isPosting
                )
                
                // Post Button with M3E animation
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = commentText.isNotBlank() || isPosting,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (commentText.isNotBlank() && !isPosting) {
                                    onPostComment(commentText)
                                    commentText = ""
                                    keyboardController?.hide()
                                }
                            },
                            enabled = !isPosting,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = accentColor,
                                contentColor = backgroundColor
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = backgroundColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Post",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isHighlighted) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Avatar with expressive border
            Box(contentAlignment = Alignment.Center) {
                if (isHighlighted) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.2f), accentColor)
                                )
                            )
                    )
                }
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(comment.authorThumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Author and Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Text(
                        text = comment.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Text with improved readability
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        letterSpacing = 0.1.sp
                    ),
                    color = contentColor
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Actions (Likes & Replies)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Like Count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = "Likes",
                            tint = if (comment.likeCount.toIntOrNull() ?: 0 > 0) accentColor else contentColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        if (comment.likeCount != "0" && comment.likeCount.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = comment.likeCount,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Reply Count (if available)
                    if (comment.replyCount > 0) {
                        Text(
                            text = "${comment.replyCount} replies",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}
