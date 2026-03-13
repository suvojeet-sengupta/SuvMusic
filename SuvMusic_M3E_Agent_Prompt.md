# SuvMusic — Material 3 Expressive (M3E) Full Migration Agent Prompt

> **Codebase:** `com.suvojeet.suvmusic`  
> **Stack:** Jetpack Compose · Kotlin · Hilt DI · Media3 · Navigation Compose  
> **Current M3 version:** `material3 = "1.5.0-alpha11"` · `composeBom = "2026.02.00"`  
> **Status:** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` already applied in `Theme.kt` ✅  
> **Goal:** Migrate all target screens to full M3E — spring motion, new components, expressive typography, shape morphing — while keeping the app fully functional.

---

## ⚠️ Agent Ground Rules (Read Before Every Phase)

1. **Never break existing logic.** ViewModel bindings, navigation routes, state collection, player control callbacks — all must remain intact.
2. **Always annotate with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`** on every composable that uses any M3E API.
3. **Spring motion over tween** — use `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)` for all interactive element animations. Reserve tween only for color transitions.
4. **Never hardcode colors.** Always pull from `MaterialTheme.colorScheme.*`.
5. **All imports must be explicit.** No wildcard `import androidx.compose.material3.*` — use specific named imports so it's clear which M3E APIs are being used.
6. **Compile-check mentally before writing.** If an M3E API might not exist in alpha11, check — then use an equivalent or custom implementation.
7. **One screen/component per task** — don't batch-edit multiple unrelated files in one shot.
8. **Preserve all `@Composable` function signatures** — changing parameter lists breaks callers in `NavGraph.kt`.

---

## Phase 0 — Dependency & Theme Foundation Audit

**Target files:**
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Shapes.kt`

### Task 0.1 — Confirm M3E Dependency Config

Verify the following are present in `libs.versions.toml`. If any is missing, add it:

```toml
[versions]
composeBom = "2026.02.00"
material3 = "1.5.0-alpha11"

[libraries]
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
```

In `app/build.gradle.kts`, confirm:
```kotlin
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
```

> ℹ️ **Do NOT upgrade the BOM or material3 version** — the current versions are intentional. Just confirm they are correct.

---

### Task 0.2 — Upgrade `Theme.kt` with M3E MotionScheme

Modify `SuvMusicTheme` to add `motionScheme` from M3E. The `MaterialTheme` composable in M3E accepts a `motionScheme` parameter:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuvMusicTheme(/* existing params */) {
    // ... existing colorScheme logic unchanged ...

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        motionScheme = MotionScheme.expressive(), // ← ADD THIS
        content = content
    )
}
```

> `MotionScheme.expressive()` wires up spring-based physics for all M3E component animations automatically.

---

### Task 0.3 — Upgrade `Type.kt` to M3E Expressive Typography

M3E introduces **30 type styles** — the standard 15 plus 15 "Emphasized" variants. Replace the existing `Typography` object:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Typography = Typography(
    // ── Standard styles (keep existing values) ──
    displayLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold,  fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,       fontSize = 45.sp, lineHeight = 52.sp,  letterSpacing = 0.sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,       fontSize = 36.sp, lineHeight = 44.sp,  letterSpacing = 0.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,  fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,  fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,  fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,     fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,       fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,       fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,       fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 11.sp, lineHeight = 16.sp,  letterSpacing = 0.5.sp),

    // ── M3E Emphasized variants (heavier weight, slightly larger) ──
    displayLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,    fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp),
    headlineMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 24.sp, lineHeight = 32.sp),
    titleLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,        fontSize = 22.sp, lineHeight = 28.sp),
    titleMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLargeEmphasized   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,      fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMediumEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,      fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmallEmphasized   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,      fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
```

> **Usage:** Use `MaterialTheme.typography.titleLargeEmphasized` for song titles in the player, `headlineMediumEmphasized` for screen headers that deserve extra punch.

---

### Task 0.4 — Upgrade `Shapes.kt` with M3E ShapeKeyTokens

M3E introduces **ShapeDefaults** with semantic tokens. Extend the existing file:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ShapeDefaults

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Shapes = Shapes(
    extraSmall = ShapeDefaults.ExtraSmall,   // 4.dp rounded
    small      = ShapeDefaults.Small,        // 8.dp rounded  
    medium     = ShapeDefaults.Medium,       // 12.dp rounded
    large      = ShapeDefaults.Large,        // 16.dp rounded
    extraLarge = ShapeDefaults.ExtraLarge,   // 28.dp rounded
)

// Keep all custom shapes — they are used directly by name in components
val MusicCardShape   = RoundedCornerShape(20.dp)
val PlayerCardShape  = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val AlbumArtShape    = RoundedCornerShape(16.dp)
val PillShape        = RoundedCornerShape(50)
val SquircleShape    = RoundedCornerShape(28.dp)
```

---

## Phase 1 — Shared M3E Component Library

**Create:** `ui/components/M3EComponents.kt`  
This is a shared composable library used by all screens. Build it before editing any screens.

### Components to implement in `M3EComponents.kt`:

---

#### 1.1 — `M3ESettingsGroupHeader` (Section labels in settings)

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ESettingsGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp, end = 16.dp)
    )
}
```

---

#### 1.2 — `M3ESettingsItem` (Clickable row for all settings)

Replace every raw `ListItem` with `clickable` in settings with this. It uses M3's `ListItem` correctly and adds spring-based press feedback:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ESettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "settings_item_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (onClick != null) Modifier.clickable(interactionSource, indication = null) { onClick() } else Modifier),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        ListItem(
            headlineContent = {
                Text(text = title, style = MaterialTheme.typography.bodyLargeEmphasized)
            },
            supportingContent = subtitle?.let {
                { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            },
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
```

---

#### 1.3 — `M3ESwitchItem` (Settings toggle row)

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ESwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit,
) {
    M3ESettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        iconTint = iconTint,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        }
    )
}
```

---

#### 1.4 — `M3ENavigationItem` (Settings row with arrow — navigates to sub-screen)

```kotlin
@Composable
fun M3ENavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    badge: String? = null,
    onClick: () -> Unit,
) {
    M3ESettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        iconTint = iconTint,
        onClick = onClick,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (badge != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
```

---

#### 1.5 — `M3ELoadingIndicator` (Replaces all CircularProgressIndicator usages)

M3E introduces `LoadingIndicator` with a new multi-dot containment style:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ELoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    // Use M3E LoadingIndicator if available in alpha11, else fallback:
    // LoadingIndicator(modifier = modifier, color = color)
    // If not available, use a contained CircularProgressIndicator:
    CircularProgressIndicator(
        modifier = modifier.size(48.dp),
        color = color,
        strokeWidth = 3.dp,
        trackColor = color.copy(alpha = 0.12f)
    )
}
```

> **Note for agent:** Check if `androidx.compose.material3.LoadingIndicator` is exported in alpha11. If it is, use it directly. If not, use the fallback above. Never crash on missing APIs.

---

#### 1.6 — `M3EButtonGroup` (Segmented control for selecting options)

Used in PlaybackSettings for audio quality picker, seekbar style picker, etc.:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> M3EButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    // M3E ButtonGroup — horizontally arranged toggle buttons
    // If ButtonGroup is available in alpha11:
    // ButtonGroup(modifier = modifier) { ... }
    // Otherwise use a custom implementation:
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "btn_group_color"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "btn_group_text_color"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgColor)
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = if (isSelected) MaterialTheme.typography.labelMediumEmphasized
                            else MaterialTheme.typography.labelMedium,
                    color = textColor
                )
            }
        }
    }
}
```

---

#### 1.7 — `M3ESplitButton` (Primary action + overflow for secondary actions)

Used on PlayerScreen for the main play/pause area overflow:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ESplitButton(
    leadingIcon: ImageVector,
    leadingLabel: String,
    onLeadingClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Default.MoreVert,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Leading segment — primary action
        Button(
            onClick = onLeadingClick,
            enabled = enabled,
            shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(leadingLabel, style = MaterialTheme.typography.labelLargeEmphasized)
        }
        // Trailing segment — overflow
        FilledIconButton(
            onClick = onTrailingClick,
            enabled = enabled,
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 50.dp, bottomEnd = 50.dp),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(trailingIcon, contentDescription = "More options", modifier = Modifier.size(20.dp))
        }
    }
}
```

---

#### 1.8 — `M3EPageHeader` (Consistent LargeTopAppBar for all settings sub-screens)

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3EPageHeader(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    LargeTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}
```

---

## Phase 2 — Settings Screen Complete Revamp

**Target:** `ui/screens/SettingsScreen.kt`

### Architecture Design

The settings screen must be rebuilt with:
- **Grouped sections** with `M3ESettingsGroupHeader` labels
- **`M3ENavigationItem`** for every sub-screen navigation row
- **`M3ESwitchItem`** for every toggle
- **Collapsible account card** at the top
- **Spring-animated transitions** on item press
- **`TopAppBar`** (medium size — not large — because Settings is a top-level destination)

### Full Screen Structure

```
SettingsScreen
├── TopAppBar ("Settings") — medium, pinned
├── LazyColumn
│   ├── [Account Card] — shows logged-in YouTube/LastFM account or login prompt
│   ├── [Section: AUDIO]
│   │   └── M3ENavigationItem(icon=GraphicEq, title="Playback", subtitle="Quality, gapless, speed", to=PlaybackSettings)
│   ├── [Section: APPEARANCE]
│   │   ├── M3ENavigationItem(icon=Palette, title="Appearance", subtitle="Themes, dark mode", to=AppearanceSettings)
│   │   └── M3ENavigationItem(icon=Tune, title="Customization", subtitle="Player layout, artwork", to=CustomizationSettings)
│   ├── [Section: SERVICES]
│   │   ├── M3ENavigationItem(icon=MusicNote, title="Lyrics Providers", to=LyricsProviders)
│   │   ├── M3ENavigationItem(icon=Block, title="SponsorBlock", to=SponsorBlockSettings)
│   │   ├── M3ENavigationItem(icon=Discord, title="Discord RPC", to=DiscordSettings)
│   │   └── M3ENavigationItem(icon=LastFm, title="Last.fm Scrobbling", to=LastFmLogin)
│   ├── [Section: STORAGE & CACHE]
│   │   ├── M3ENavigationItem(icon=Storage, title="Storage", subtitle="Downloads, cache", to=Storage)
│   │   └── M3ENavigationItem(icon=Memory, title="Player Cache", to=PlayerCache)
│   ├── [Section: MISC]
│   │   ├── M3ESwitchItem(icon=WifiOff, title="Offline Mode", checked=..., ...)
│   │   └── M3ENavigationItem(icon=Notes, title="Misc Settings", to=Misc)
│   ├── [Section: INFO]
│   │   ├── M3ENavigationItem(icon=SystemUpdate, title="Check for Updates", badge=currentVersion, to=Updater)
│   │   ├── M3ENavigationItem(icon=Info, title="About", to=About)
│   │   └── M3ENavigationItem(icon=Changelog, title="Changelog", to=Changelog)
│   └── [Bottom spacer for navigation bar]
```

### Account Card Component

Build `M3EAccountCard` inside SettingsScreen.kt (private composable):

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EAccountCard(
    accountName: String?,
    accountEmail: String?,
    accountPhotoUrl: String?,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    // ElevatedCard with:
    // - AsyncImage avatar (circular, 56dp) on left
    // - Name + email stacked text
    // - If logged in: outline button "Switch" + text button "Logout" as trailing
    // - If not logged in: filled button "Sign In with YouTube"
    // Shape: MaterialTheme.shapes.extraLarge
    // Spring animation on press (scale 0.98f)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            if (isLoggedIn && accountPhotoUrl != null) {
                AsyncImage(
                    model = accountPhotoUrl,
                    contentDescription = "Account avatar",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }
            }
            // Text
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) accountName ?: "YouTube Account" else "Not signed in",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLoggedIn && accountEmail != null) {
                    Text(
                        text = accountEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!isLoggedIn) {
                    Text(
                        text = "Sign in to sync your library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Actions
            if (isLoggedIn) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onSwitchAccount, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Switch", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onLogoutClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Sign out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                FilledTonalButton(onClick = onLoginClick) {
                    Text("Sign in", style = MaterialTheme.typography.labelMediumEmphasized)
                }
            }
        }
    }
}
```

---

## Phase 3 — About Screen Complete Revamp

**Target:** `ui/screens/AboutScreen.kt`

### Design Vision

The About screen should feel like a **premium product showcase**, not just an info dump. Use:
- **Large hero section** with animated app icon + gradient background
- **`M3EPageHeader`** (LargeTopAppBar)
- **Feature cards** using `ElevatedCard` with icon + description
- **Links section** using `M3ENavigationItem` 
- **Credits section** with `M3ESettingsGroupHeader` and developer cards
- **Version badge** as a `SuggestionChip`

### Screen Structure

```
AboutScreen
├── M3EPageHeader("About SuvMusic", onBack)
├── LazyColumn (with nestedScrollConnection for parallax)
│   ├── [Hero Block]
│   │   ├── Animated app logo (spring entrance animation, scale 0→1)
│   │   ├── App name: displaySmallEmphasized
│   │   ├── Tagline: bodyLarge, onSurfaceVariant
│   │   └── SuggestionChip(label="v{versionName}", icon=NewReleases)
│   ├── [What's Inside — ElevatedCard grid]
│   │   ├── M3EFeatureCard(icon=MusicNote,     title="YouTube Music", desc="Millions of songs")
│   │   ├── M3EFeatureCard(icon=OfflinePin,    title="Offline Play",  desc="Download & listen anywhere")
│   │   ├── M3EFeatureCard(icon=Lyrics,        title="Synced Lyrics", desc="Multiple providers")
│   │   ├── M3EFeatureCard(icon=GraphicEq,     title="Equalizer",     desc="10-band EQ")
│   │   ├── M3EFeatureCard(icon=Group,         title="Listen Together",desc="Real-time sync")
│   │   └── M3EFeatureCard(icon=AutoAwesome,   title="AI Queue",      desc="Smart recommendations")
│   ├── [Section: LINKS]
│   │   ├── M3ENavigationItem(icon=GitHub, title="Source Code", subtitle="github.com/suvojeet-sengupta/SuvMusic")
│   │   ├── M3ENavigationItem(icon=BugReport, title="Report a Bug")
│   │   ├── M3ENavigationItem(icon=Star, title="How It Works", to=HowItWorks)
│   │   └── M3ENavigationItem(icon=Favorite, title="Support the Project", to=Support)
│   ├── [Section: LEGAL]
│   │   ├── M3ENavigationItem(icon=Gavel, title="Open Source Licenses")
│   │   └── M3ENavigationItem(icon=Security, title="Privacy Policy")
│   ├── [Section: TEAM]
│   │   ├── M3EDeveloperCard(name="Suvojeet Sengupta", role="Developer", github="suvojeet-sengupta")
│   │   └── M3ENavigationItem(icon=People, title="All Contributors", to=Credits)
│   └── [Bottom: copyright text centered, labelSmall, onSurfaceVariant]
```

### `M3EFeatureCard` (private to AboutScreen)

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EFeatureCard(
    icon: ImageVector,
    title: String,
    desc: String,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(48.dp).background(tint.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

Feature cards should be laid out in a `LazyVerticalGrid(columns = GridCells.Fixed(2))` inside a fixed-height `Box` (do NOT use `LazyVerticalGrid` inside a `LazyColumn` directly — measure the height manually or use `FlowRow` from Accompanist/Compose).

> **Use `FlowRow`** from `androidx.compose.foundation.layout.FlowRow` (stable in Compose 1.6+):
```kotlin
FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    maxItemsInEachRow = 2
) {
    featureItems.forEach { M3EFeatureCard(..., modifier = Modifier.weight(1f)) }
}
```

---

## Phase 4 — Playback Settings Screen Revamp

**Target:** `ui/screens/PlaybackSettingsScreen.kt`

### Design Vision

Use `M3EPageHeader` + `M3EButtonGroup` for picker-style choices + `M3ESwitchItem` for toggles. Organize into logical groups.

### Screen Structure

```
PlaybackSettingsScreen
├── M3EPageHeader("Playback", onBack, scrollBehavior)
├── LazyColumn(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection))
│   ├── M3ESettingsGroupHeader("AUDIO QUALITY")
│   │   ├── [AudioQuality picker — M3EButtonGroup with options: Auto, Low, Medium, High]
│   │   └── M3ENavigationItem(icon=Download, "Download Quality", subtitle=currentDownloadQuality, onClick=showDownloadQualitySheet)
│   ├── M3ESettingsGroupHeader("PLAYBACK BEHAVIOR")
│   │   ├── M3ESwitchItem(icon=SkipNext,     "Gapless Playback",      checked=..., ...)
│   │   ├── M3ESwitchItem(icon=RepeatOne,    "Normalize Volume",      checked=..., ...)
│   │   ├── M3ESwitchItem(icon=Vibration,    "Haptic Feedback",       checked=..., ...)
│   │   └── M3ENavigationItem("Haptic Intensity",  subtitle=currentHapticIntensity, onClick=showHapticSheet)
│   ├── M3ESettingsGroupHeader("SEEK & SKIP")
│   │   ├── [Seek increment — M3EButtonGroup: 5s, 10s, 15s, 30s]
│   │   └── M3ESwitchItem(icon=FastForward, "Skip Silence",           checked=..., ...)
│   ├── M3ESettingsGroupHeader("VIDEO")
│   │   ├── M3ESwitchItem(icon=Videocam,    "Video Playback",         checked=..., ...)
│   │   └── M3ENavigationItem("Video Quality", subtitle=currentVideoQuality, onClick=showVideoQualitySheet)
│   ├── M3ESettingsGroupHeader("EQUALIZER & EFFECTS")
│   │   ├── M3ENavigationItem(icon=GraphicEq, "Equalizer", onClick=showEqualizerSheet)
│   │   └── M3ENavigationItem(icon=SurroundSound, "Spatial Audio", subtitle="Experimental", onClick=...)
│   ├── M3ESettingsGroupHeader("HISTORY & DATA")
│   │   ├── M3ESwitchItem(icon=History, "Save Listening History", checked=..., ...)
│   │   └── M3ENavigationItem(icon=BarChart, "Listening Stats", to=ListeningStats)
│   └── [Bottom spacer]
```

### Quality Picker Bottom Sheet Pattern

All quality picker dialogs should be replaced with `ModalBottomSheet` using `M3EPickerSheet` pattern:

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> M3EPickerSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(bottom = 16.dp))
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { onSelect(option); onDismiss() },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = MaterialTheme.shapes.large,
                ) {
                    ListItem(
                        headlineContent = { Text(label(option), style = if (isSelected) MaterialTheme.typography.bodyLargeEmphasized else MaterialTheme.typography.bodyLarge) },
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
```

---

## Phase 5 — Appearance Settings Screen Revamp

**Target:** `ui/screens/AppearanceSettingsScreen.kt`

### Screen Structure

```
AppearanceSettingsScreen
├── M3EPageHeader("Appearance", onBack, scrollBehavior)
├── LazyColumn
│   ├── M3ESettingsGroupHeader("COLOR THEME")
│   │   └── [Theme palette picker — horizontal LazyRow of colored circles]
│   │       Each circle: 48dp, colored with primary of that theme, selected = ring border
│   ├── M3ESettingsGroupHeader("DISPLAY")
│   │   ├── [Dark mode — M3EButtonGroup: System, Light, Dark]
│   │   ├── M3ESwitchItem(icon=ColorLens, "Material You (Dynamic Color)", checked=...)
│   │   └── M3ESwitchItem(icon=DarkMode, "Pure Black AMOLED", checked=...)
│   ├── M3ESettingsGroupHeader("PLAYER")
│   │   ├── M3ENavigationItem("Player Background", subtitle="Animated mesh / static", to=...)
│   │   ├── M3ENavigationItem("Artwork Shape", to=ArtworkShapeSettings)
│   │   ├── M3ENavigationItem("Artwork Size", to=ArtworkSizeSettings)
│   │   └── M3ENavigationItem("Seekbar Style", to=SeekbarStyleSettings)
│   └── [Bottom spacer]
```

### Theme Palette Picker (inline)

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemePaletteRow(
    themes: List<AppTheme>,
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(themes) { theme ->
            val isSelected = theme == selected
            val themeColor = theme.primaryColor // resolve via a local mapping
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "theme_scale"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable { onSelect(theme) }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(themeColor, CircleShape)
                        .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(20.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(theme.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

---

## Phase 6 — All Remaining Settings Sub-Screens

Apply the same M3E pattern to these screens. For each screen, the pattern is identical:

**Pattern:** `M3EPageHeader` + `LazyColumn` + `M3ESettingsGroupHeader` + `M3ESettingsItem`/`M3ESwitchItem`/`M3ENavigationItem`

### 6.1 `MiscScreen.kt`
Groups: LYRICS, WIDGETS, BEHAVIOR, NOTIFICATIONS, DEVELOPER

### 6.2 `LyricsProvidersScreen.kt`
Groups: PROVIDERS (ranked drag list), PROVIDER SETTINGS (per-provider switches)

### 6.3 `SponsorBlockSettingsScreen.kt`
Groups: SEGMENTS (per-type switches with color chips), SKIP BEHAVIOR (M3EButtonGroup: Auto/Ask/Manual)

### 6.4 `StorageScreen.kt`
Groups: DOWNLOAD LOCATION, CACHE (progress bars using LinearProgressIndicator with M3E shapes), CLEANUP ACTIONS (destructive buttons in ErrorContainer color)

### 6.5 `PlayerCacheScreen.kt`
Groups: CACHE SIZE (Slider with M3E style), CACHE STATISTICS (read-only info rows)

### 6.6 `DiscordSettingsScreen.kt`
Groups: CONNECTION, DISPLAY OPTIONS

### 6.7 `ChangelogScreen.kt`
Display each version as an `ElevatedCard` with:
- Version number: `titleMediumEmphasized` + `SuggestionChip`
- Date: `labelSmall`, `onSurfaceVariant`
- Changes: bulleted `bodyMedium` text blocks

---

## Phase 7 — Home Screen M3E Enhancement

**Target:** `ui/screens/HomeScreen.kt`

> **Strategy:** Do NOT rebuild the HomeScreen from scratch — it has complex ViewModel bindings, state collection, and sections logic. Apply M3E **selectively and additively**.

### Tasks

#### 7.1 — Replace greeting section with M3E expressive header

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeGreetingHeader(
    greeting: String,      // "Good morning", "Good evening", etc.
    userName: String?,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName ?: "Listener",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Outlined.Notifications, "Notifications")
            }
            // Avatar button
            FilledIconButton(
                onClick = onAvatarClick,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.Person, "Account", modifier = Modifier.size(20.dp))
            }
        }
    }
}
```

#### 7.2 — Quick Actions Row (M3E ButtonGroup style)

Replace the current chip row with a horizontally scrolling `FilterChip` group:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeQuickActionsRow(
    onPlaylistsClick: () -> Unit,
    onRadioClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onListenTogetherClick: () -> Unit,
) {
    val actions = listOf(
        Triple(Icons.Filled.LibraryMusic, "Playlists",     onPlaylistsClick),
        Triple(Icons.Filled.Radio,        "Radio",         onRadioClick),
        Triple(Icons.Filled.History,      "Recents",       onRecentsClick),
        Triple(Icons.Filled.Group,        "Listen Together",onListenTogetherClick),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { (icon, label, onClick) ->
            InputChip(
                selected = false,
                onClick = onClick,
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(icon, null, modifier = Modifier.size(InputChipDefaults.IconSize)) },
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}
```

#### 7.3 — Section Header Row

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(0.dp)) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
```

#### 7.4 — Song Card Enhancement

Wrap existing `MusicCard` in spring-scale interaction. Add `ElevatedCard` shadow lift on press:

```kotlin
// In HomeScreen or as a wrapper — do NOT rewrite MusicCard internals
@Composable
fun SpringMusicCard(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "card_scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        MusicCard(
            song = song,
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interactionSource
        )
    }
}
```

> **Important:** Only add `interactionSource` parameter to `MusicCard` if it's not already there — check `ui/components/MusicCard.kt` before editing.

---

## Phase 8 — Player Screen M3E Enhancement

**Target:** `ui/screens/player/PlayerScreen.kt` and subcomponents in `ui/screens/player/components/`

> **Strategy:** The player is the most complex screen. Apply M3E **additively** — enhance the existing structure rather than rebuilding. The animated background, media3 integration, and lyric systems must stay untouched.

### 8.1 — PlayerTopBar (`PlayerTopBar.kt`)

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerTopBar(
    onDismiss: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreClick: () -> Unit,
    title: String = "Now Playing",
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            // Chevron down — dismiss player
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.KeyboardArrowDown, "Dismiss", modifier = Modifier.size(28.dp))
            }
        },
        actions = {
            IconButton(onClick = onQueueClick) {
                Icon(Icons.Filled.QueueMusic, "Queue")
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Filled.MoreVert, "More options")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}
```

### 8.2 — PlayerSongInfo (`PlayerSongInfo.kt`)

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerSongInfo(
    title: String,
    artist: String,
    onArtistClick: () -> Unit,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            // Song title — Emphasized + marquee scroll for long names
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, spacing = MarqueeSpacing(32.dp))
            )
            Spacer(Modifier.height(4.dp))
            // Artist — clickable, underlined on hover
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtistClick() }
            )
        }
        // Like button — spring-animated heart
        val likeScale by animateFloatAsState(
            targetValue = if (isLiked) 1.2f else 1f,
            animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow),
            label = "like_scale"
        )
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp).graphicsLayer { scaleX = likeScale; scaleY = likeScale }
            )
        }
    }
}
```

### 8.3 — PlayerControls (`PlayerControls.kt`)

Upgrade the play/pause and control buttons with M3E-style sizing and spring physics:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    isShuffled: Boolean,
    repeatMode: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Main controls row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Shuffle
            IconButton(onClick = onShuffle, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (isShuffled) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
                    "Shuffle",
                    tint = if (isShuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Skip Previous
            IconButton(onClick = onSkipPrev, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipPrevious, "Previous", modifier = Modifier.size(32.dp))
            }
            // Play/Pause — FilledIconButton, large, spring-animated
            val playScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "play_scale"
            )
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp).graphicsLayer { scaleX = playScale; scaleY = playScale },
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(spring(Spring.DampingRatioMediumBouncy))).togetherWith(scaleOut(tween(100)))
                    },
                    label = "play_pause_icon"
                ) { playing ->
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (playing) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            // Skip Next
            IconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(32.dp))
            }
            // Repeat
            IconButton(onClick = onRepeat, modifier = Modifier.size(48.dp)) {
                Icon(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                        Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
                        else -> Icons.Filled.Repeat
                    },
                    "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
```

### 8.4 — PlayerBottomActions (`PlayerBottomActions.kt`)

Replace the current bottom action row with an M3E-style horizontal row using `FilledTonalIconButton` for secondary actions:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerBottomActions(
    onLyricsClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onOutputDeviceClick: () -> Unit,
    onCommentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple(Icons.Filled.Lyrics,       "Lyrics",        onLyricsClick),
            Triple(Icons.Filled.Bedtime,      "Sleep Timer",   onSleepTimerClick),
            Triple(Icons.Filled.Speed,        "Speed",         onSpeedClick),
            Triple(Icons.Filled.Speaker,      "Output",        onOutputDeviceClick),
            Triple(Icons.Filled.Comment,      "Comments",      onCommentsClick),
        ).forEach { (icon, label, onClick) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onClick,
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(icon, label, modifier = Modifier.size(20.dp))
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

### 8.5 — MiniPlayer Enhancement (all three styles)

For `PillMiniPlayer.kt`, `StandardMiniPlayer.kt`, `YTMusicMiniPlayer.kt`:

- Add spring-scale press animation on the entire mini player container
- Replace `LinearProgressIndicator` with M3E style: `LinearProgressIndicator(progress = { progress }, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, strokeCap = StrokeCap.Round)`
- Apply `ElevatedCard` as the container for `StandardMiniPlayer` for proper M3E elevation

---

## Phase 9 — ExpressiveBottomNav Final Polish

**Target:** `ui/components/ExpressiveBottomNav.kt`

The existing ExpressiveBottomNav is already using a custom liquid glass style. Apply these M3E-aligned improvements:

1. **Selected indicator**: Instead of a custom drawn indicator, use `M3E NavigationBar` internally if alpha11 supports the new expressive variant — check for `NavigationBarItem` with the new M3E `indicatorShape` parameter.

2. **Icon animation**: Add `AnimatedContent` on the icon swap between filled/outlined variants with spring transition:

```kotlin
AnimatedContent(
    targetState = isSelected,
    transitionSpec = {
        scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            .togetherWith(scaleOut(tween(80)))
    },
    label = "nav_icon_${item.label}"
) { selected ->
    Icon(
        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
        contentDescription = item.label,
        modifier = Modifier.size(24.dp),
        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

3. **Label animation**: Spring-animate the width of the label text in selected items using `AnimatedVisibility(isSelected, enter = expandHorizontally(spring(...)), exit = shrinkHorizontally(...))`.

---

## Phase 10 — Loading Indicator & Empty State Standardization

**Target:** All screens that show loading/empty states

### 10.1 — Replace `CircularProgressIndicator` everywhere

Search for all usages of `CircularProgressIndicator` in the codebase and replace with `M3ELoadingIndicator` from Phase 1.

### 10.2 — Standardize empty states with M3E

Create `ui/components/M3EEmptyState.kt`:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3EEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon container
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
            label = "empty_icon_scale"
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
```

Apply `M3EEmptyState` to: `DownloadsScreen`, `LibraryScreen`, `SearchScreen` (no results state), `RecentsScreen`.

---

## Final Verification Checklist

After completing all phases, verify the following before declaring the migration done:

### Functionality
- [ ] App builds without compile errors
- [ ] All navigation routes in `NavGraph.kt` still resolve correctly
- [ ] ViewModel state flows still bind correctly in all screens
- [ ] Player controls (play/pause/skip) still function
- [ ] Settings values persist correctly through ViewModel
- [ ] Downloads, lyrics, and queue screens still work

### M3E Design Quality
- [ ] All `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` annotations present where M3E APIs are used
- [ ] No hardcoded colors anywhere — all from `MaterialTheme.colorScheme.*`
- [ ] All interactive elements have spring-based press animation
- [ ] Typography uses Emphasized variants for all headings and emphasized content
- [ ] Settings screens all use `M3ESettingsGroupHeader` for section labels
- [ ] All bottom sheets use `MaterialTheme.shapes.extraLarge` at the top
- [ ] `MotionScheme.expressive()` is wired in `SuvMusicTheme`

### Performance
- [ ] No nested `LazyColumn`/`LazyVerticalGrid` without explicit height constraints
- [ ] `remember {}` and `rememberCoroutineScope()` used correctly — no unnecessary recompositions
- [ ] `animationSpec` uses `spring()` (not `tween`) for all scale/position animations

---

## M3E API Quick Reference for This Codebase

| What you need | M3E API (alpha11) | Import |
|---|---|---|
| Expressive motion | `MotionScheme.expressive()` | `androidx.compose.material3.MotionScheme` |
| Expressive typography | `MaterialTheme.typography.titleLargeEmphasized` | auto via MaterialTheme |
| Shape tokens | `ShapeDefaults.ExtraLarge` | `androidx.compose.material3.ShapeDefaults` |
| Spring spec | `spring(DampingRatioMediumBouncy, StiffnessMedium)` | `androidx.compose.animation.core.spring` |
| Filled icon button | `FilledIconButton` | `androidx.compose.material3.FilledIconButton` |
| Filled tonal icon button | `FilledTonalIconButton` | `androidx.compose.material3.FilledTonalIconButton` |
| Elevated card | `ElevatedCard` | `androidx.compose.material3.ElevatedCard` |
| Large top bar | `LargeTopAppBar` | `androidx.compose.material3.LargeTopAppBar` |
| Bottom sheet | `ModalBottomSheet` + `SheetState` | `androidx.compose.material3.*` |
| Loading (if available) | `LoadingIndicator` | `androidx.compose.material3.LoadingIndicator` |
| Suggestion chip | `SuggestionChip` | `androidx.compose.material3.SuggestionChip` |
| Input chip | `InputChip` | `androidx.compose.material3.InputChip` |
| Flow row | `FlowRow` | `androidx.compose.foundation.layout.FlowRow` |
| Marquee scroll | `Modifier.basicMarquee()` | `androidx.compose.foundation.basicMarquee` |

---

*Generated for SuvMusic · com.suvojeet.suvmusic · M3E Migration v1.0*  
*Author: Suvojeet Sengupta | Agent Prompt by Claude Sonnet 4.6*
