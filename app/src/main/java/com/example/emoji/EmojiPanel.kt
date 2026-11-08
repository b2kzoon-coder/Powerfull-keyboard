package com.example.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.ime.ImeDiagnosticsLogger
import com.example.keyboard.ResponsiveLayoutEngine
import com.example.theme.KeyboardPalette
import com.example.ui.theme.BengaliFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class RecentTabMode {
  RECENT,
  FREQUENT,
}

private val SearchInputTextStyle =
  TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
  )

/**
 * Ridmik Keyboard-style Emoji Panel.
 *
 * Self-contained in-IME architecture:
 * Because PersonalKeyboardService IS the active Android InputMethodService, the Emoji Search Bar
 * renders directly inside the IME surface without creating a competing platform TextInputSession
 * or calling InputMethodManager.showSoftInput / hideSoftInputFromWindow on the IME's own window.
 * On-screen key presses are routed directly into the Emoji Search query by PersonalKeyboardService
 * while search mode is active.
 */
@Composable
fun EmojiPanel(
  palette: KeyboardPalette,
  panelHeight: Dp,
  activeLanguageLabel: String,
  onCommitEmoji: (String) -> Unit,
  onBackspace: () -> Unit,
  onSpace: () -> Unit,
  onCloseEmoji: () -> Unit,
  externalSearchQuery: String? = null,
  externalRawSearchQuery: String = "",
  onExternalSearchQueryChange: ((String) -> Unit)? = null,
  isSearchFocused: Boolean = false,
  onSearchFocusChange: ((Boolean) -> Unit)? = null,
  searchCursorIndex: Int? = null,
  onSearchCursorChange: ((Int) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  val repo = remember(context) { EmojiPreferencesRepository.getInstance(context) }
  val recentEmojisState = repo.recentEmojis.collectAsState()
  val frequentEmojisState = repo.frequentEmojis.collectAsState()
  val selectedSkinToneState = repo.selectedSkinTone.collectAsState()

  var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
  var internalSearchQuery by remember { mutableStateOf("") }
  var internalSearchFocused by remember { mutableStateOf(false) }
  var recentTabMode by remember { mutableStateOf(RecentTabMode.RECENT) }

  val searchQuery = externalSearchQuery ?: internalSearchQuery
  val activeSearchFocused =
    if (onSearchFocusChange != null) isSearchFocused else internalSearchFocused
  val effectiveCursorIndex =
    (searchCursorIndex ?: searchQuery.length).coerceIn(0, searchQuery.length)

  val currentOnCommitEmoji by rememberUpdatedState(onCommitEmoji)
  val currentOnExternalSearchQueryChange by rememberUpdatedState(onExternalSearchQueryChange)
  val currentOnSearchFocusChange by rememberUpdatedState(onSearchFocusChange)
  val currentOnSearchCursorChange by rememberUpdatedState(onSearchCursorChange)
  val currentSearchQuery by rememberUpdatedState(searchQuery)
  val currentActiveSearchFocused by rememberUpdatedState(activeSearchFocused)

  DisposableEffect(Unit) {
    ImeDiagnosticsLogger.log(
      category = "EmojiPanel",
      event = "onEnterComposition",
      details = "isSearchFocused=$currentActiveSearchFocused query='$currentSearchQuery'",
    )
    onDispose {
      ImeDiagnosticsLogger.log(
        category = "EmojiPanel",
        event = "onDisposeComposition",
        details = "isSearchFocused=$currentActiveSearchFocused query='$currentSearchQuery'",
      )
    }
  }

  val updateSearchQuery: (String) -> Unit = remember {
    { newQuery ->
      internalSearchQuery = newQuery
      currentOnExternalSearchQueryChange?.invoke(newQuery)
    }
  }

  val updateSearchFocus: (Boolean) -> Unit = remember {
    { focused ->
      internalSearchFocused = focused
      currentOnSearchFocusChange?.invoke(focused)
    }
  }

  val activateSearchKeyboard: () -> Unit = remember {
    {
      ImeDiagnosticsLogger.log(
        category = "EmojiPanel",
        event = "activateSearchKeyboard",
        details = "wasFocused=$currentActiveSearchFocused query='$currentSearchQuery'",
      )
      updateSearchFocus(true)
      currentOnSearchCursorChange?.invoke(currentSearchQuery.length)
    }
  }

  val dismissSearchKeyboard: () -> Unit = remember {
    {
      ImeDiagnosticsLogger.log(
        category = "EmojiPanel",
        event = "dismissSearchKeyboard",
        details = "wasFocused=$currentActiveSearchFocused query='$currentSearchQuery'",
      )
      updateSearchFocus(false)
    }
  }

  val clearSearchCallback: () -> Unit = remember {
    {
      ImeDiagnosticsLogger.log(
        category = "EmojiPanel",
        event = "clearSearchQuery",
        details = "previousQuery='$currentSearchQuery'",
      )
      updateSearchQuery("")
    }
  }

  // Skin tone popup state: baseEmoji
  var skinTonePopupAnchor by remember { mutableStateOf<String?>(null) }

  val handleSelectEmoji: (String) -> Unit = remember(repo) {
    { emoji ->
      currentOnCommitEmoji(emoji)
      repo.recordEmojiUsage(emoji)
    }
  }

  val handleLongPressSkinTone: (String) -> Unit = remember {
    { emoji ->
      skinTonePopupAnchor = emoji
    }
  }

  val scope = rememberCoroutineScope()
  val isSearching = searchQuery.isNotBlank() || externalRawSearchQuery.isNotBlank()
  val gridState = rememberLazyGridState()

  // Filter emojis based on search or active category (guaranteed distinct).
  // Uses derivedStateOf so recentEmojisState / frequentEmojisState / selectedSkinToneState
  // are only observed when their respective category is active.
  val displayedEmojis: List<String> by remember(
    selectedCategory,
    searchQuery,
    externalRawSearchQuery,
    recentTabMode,
  ) {
    derivedStateOf {
      if (searchQuery.isNotBlank() || externalRawSearchQuery.isNotBlank()) {
        EmojiData.searchUnicode(searchQuery, externalRawSearchQuery)
      } else {
        when (selectedCategory) {
          EmojiCategory.RECENT -> {
            if (recentTabMode == RecentTabMode.RECENT) {
              recentEmojisState.value
            } else {
              frequentEmojisState.value
            }
          }
          EmojiCategory.PEOPLE -> {
            EmojiData.getDisplayEmojisForCategory(EmojiCategory.PEOPLE, selectedSkinToneState.value)
          }
          else -> {
            EmojiData.getDisplayEmojisForCategory(selectedCategory, EmojiSkinTone.DEFAULT)
          }
        }
      }
    }
  }

  LaunchedEffect(searchQuery, externalRawSearchQuery, selectedCategory, activeSearchFocused) {
    if (activeSearchFocused || searchQuery.isNotEmpty()) {
      ImeDiagnosticsLogger.log(
        category = "EmojiPanel",
        event = "searchStateUpdated",
        details = "focused=$activeSearchFocused query='$searchQuery' raw='$externalRawSearchQuery' results=${displayedEmojis.size}",
      )
    }
    if (
      displayedEmojis.isNotEmpty() &&
        (gridState.firstVisibleItemIndex != 0 || gridState.firstVisibleItemScrollOffset != 0)
    ) {
      try {
        gridState.scrollToItem(0)
      } catch (_: Throwable) {}
    }
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .height(panelHeight)
        .focusProperties { canFocus = false }
        .background(palette.keyboardBackground)
        .testTag("emoji_panel"),
  ) {
    val density = LocalDensity.current
    val availableWidthDp =
      ResponsiveLayoutEngine.resolveAvailableWidthDp(
        constraints = constraints,
        density = density,
        fallbackWidthDp = maxWidth.value,
      )
    val gridColumns = ResponsiveLayoutEngine.computeEmojiGridColumns(availableWidthDp)
    val isCompactHeight = panelHeight < 185.dp || activeSearchFocused
    val returnButtonWidth = if (availableWidthDp < 330f) 44.dp else 52.dp
    val backspaceButtonWidth = if (availableWidthDp < 330f) 42.dp else 48.dp

    Column(
      modifier =
        Modifier.fillMaxSize()
          .focusProperties { canFocus = false },
    ) {
      // 1. TOP HEADER: Dedicated Search Bar + Quick Filter Sub-Row
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .focusProperties { canFocus = false }
            .background(palette.toolbarBackground)
            .padding(
              horizontal = if (availableWidthDp < 330f) 6.dp else 8.dp,
              vertical = if (isCompactHeight) 2.5.dp else 4.dp,
            ),
      ) {
        // Search Bar Row
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .height(if (isCompactHeight) 30.dp else 34.dp)
              .focusProperties { canFocus = false }
              .clip(RoundedCornerShape(8.dp))
              .background(palette.keyBackground)
              .then(
                if (activeSearchFocused) {
                  Modifier.border(
                    width = 1.2.dp,
                    color = palette.accentKeyBackground,
                    shape = RoundedCornerShape(8.dp),
                  )
                } else {
                  Modifier
                }
              )
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = activateSearchKeyboard,
              )
              .padding(horizontal = 8.dp)
              .testTag("emoji_search_bar_row"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search emojis",
            tint =
              if (activeSearchFocused) palette.accentKeyBackground else palette.keyTextSecondary,
            modifier = Modifier.size(16.dp),
          )

          // Self-contained In-IME Search Input Box (no competing BasicTextField / InputMethodManager)
          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .focusProperties { canFocus = false }
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = activateSearchKeyboard,
                )
                .testTag("emoji_search_input"),
            contentAlignment = Alignment.CenterStart,
          ) {
            val beforeCursor =
              if (searchQuery.isEmpty()) "" else searchQuery.substring(0, effectiveCursorIndex)
            val afterCursor =
              if (searchQuery.isEmpty()) "" else searchQuery.substring(effectiveCursorIndex)

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.fillMaxWidth()
                  .focusProperties { canFocus = false },
            ) {
              if (beforeCursor.isNotEmpty()) {
                Text(
                  text = beforeCursor,
                  color = palette.keyTextPrimary,
                  fontSize = 13.sp,
                  fontFamily = BengaliFontFamily,
                  maxLines = 1,
                  style = SearchInputTextStyle,
                )
              }
              if (activeSearchFocused) {
                Box(
                  modifier =
                    Modifier.width(1.8.dp)
                      .height(15.dp)
                      .background(palette.accentKeyBackground, RoundedCornerShape(1.dp))
                      .testTag("emoji_search_cursor"),
                )
                if (searchQuery.isEmpty()) {
                  Spacer(modifier = Modifier.width(2.dp))
                }
              }
              if (searchQuery.isEmpty()) {
                Text(
                  text = "Search emojis (e.g. smile, heart, flag, fire)...",
                  color = palette.keyTextSecondary.copy(alpha = 0.6f),
                  fontSize = 12.5.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              } else if (afterCursor.isNotEmpty()) {
                Text(
                  text = afterCursor,
                  color = palette.keyTextPrimary,
                  fontSize = 13.sp,
                  fontFamily = BengaliFontFamily,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  style = SearchInputTextStyle,
                )
              }
            }
          }

          if (searchQuery.isNotEmpty()) {
            Box(
              modifier =
                Modifier.size(24.dp)
                  .focusProperties { canFocus = false }
                  .clip(CircleShape)
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = clearSearchCallback,
                  )
                  .testTag("emoji_search_clear"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear search",
                tint = palette.keyTextSecondary,
                modifier = Modifier.size(14.dp),
              )
            }
          }

          if (activeSearchFocused) {
            Box(
              modifier =
                Modifier.size(24.dp)
                  .focusProperties { canFocus = false }
                  .clip(CircleShape)
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissSearchKeyboard,
                  )
                  .testTag("emoji_search_dismiss_keyboard"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Dismiss search keyboard",
                tint = palette.accentKeyBackground,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        // Context Sub-Row: Recent/Frequent Toggle OR Skin Tone Bar (hidden when keyboard is open to maximize emoji results area)
        if (!isSearching && !activeSearchFocused) {
          if (selectedCategory == EmojiCategory.RECENT) {
            val recentEmojis = recentEmojisState.value
            val onRecentClick = remember { { recentTabMode = RecentTabMode.RECENT } }
            val onFrequentClick = remember { { recentTabMode = RecentTabMode.FREQUENT } }
            val onClearRecent = remember(repo) { { repo.clearRecent() } }
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .focusProperties { canFocus = false }
                  .padding(top = 4.dp, bottom = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilterTabChip(
                text = "Recently Used",
                isSelected = recentTabMode == RecentTabMode.RECENT,
                palette = palette,
                onClick = onRecentClick,
              )
              FilterTabChip(
                text = "Frequently Used",
                isSelected = recentTabMode == RecentTabMode.FREQUENT,
                palette = palette,
                onClick = onFrequentClick,
              )
              Spacer(modifier = Modifier.weight(1f))
              if (recentEmojis.isNotEmpty()) {
                Text(
                  text = "Clear",
                  color = palette.accentKeyBackground,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Medium,
                  modifier =
                    Modifier.focusProperties { canFocus = false }
                      .clickable(onClick = onClearRecent)
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                      .testTag("emoji_recent_clear"),
                )
              }
            }
          } else if (selectedCategory == EmojiCategory.PEOPLE) {
            val selectedSkinTone = selectedSkinToneState.value
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .focusProperties { canFocus = false }
                  .padding(top = 4.dp, bottom = 2.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Skin tone:",
                color = palette.keyTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
              )
              EmojiSkinTone.entries.forEach { tone ->
                val isSelected = tone == selectedSkinTone
                val onToneClick = remember(repo, tone) { { repo.setSelectedSkinTone(tone) } }
                Box(
                  modifier =
                    Modifier.size(22.dp)
                      .focusProperties { canFocus = false }
                      .clip(CircleShape)
                      .background(
                        if (isSelected) palette.accentKeyBackground.copy(alpha = 0.35f)
                        else Color.Transparent
                      )
                      .clickable(onClick = onToneClick),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(text = tone.samplePreview, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }

      // 2. CENTER: Clean, Scrollable Emoji Grid
      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .padding(horizontal = if (availableWidthDp < 330f) 2.dp else 4.dp),
      ) {
        if (displayedEmojis.isEmpty()) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .focusProperties { canFocus = false },
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text =
                if (isSearching) "No emojis matching \"$searchQuery\""
                else "No recent emojis yet.\nTap any emoji below to use it!",
              color = palette.keyTextSecondary,
              fontSize = 12.5.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
          }
        } else {
          LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding =
              PaddingValues(horizontal = 2.dp, vertical = if (isCompactHeight) 2.dp else 4.dp),
            modifier =
              Modifier.fillMaxSize()
                .focusProperties { canFocus = false }
                .testTag("emoji_grid"),
          ) {
            items(
              items = displayedEmojis,
              key = { emoji -> emoji },
              contentType = { "emoji_cell" },
            ) { emoji ->
              EmojiGridCell(
                emoji = emoji,
                isCompact = isCompactHeight,
                onSelectEmoji = handleSelectEmoji,
                onLongPressSkinTone = handleLongPressSkinTone,
              )
            }
          }
        }

        // Skin Tone Popup Selector when an emoji is long-pressed
        skinTonePopupAnchor?.let { baseEmoji ->
          val dismissSkinTonePopup = remember { { skinTonePopupAnchor = null } }
          Popup(
            alignment = Alignment.Center,
            onDismissRequest = dismissSkinTonePopup,
            properties = PopupProperties(focusable = false),
          ) {
            val variants = remember(baseEmoji) { EmojiTextUtil.getSkinToneVariants(baseEmoji) }
            Row(
              modifier =
                Modifier.focusProperties { canFocus = false }
                  .clip(RoundedCornerShape(24.dp))
                  .background(palette.keyBackground)
                  .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              variants.forEach { variant ->
                Box(
                  modifier =
                    Modifier.size(36.dp)
                      .focusProperties { canFocus = false }
                      .clip(CircleShape)
                      .clickable {
                        handleSelectEmoji(variant)
                        skinTonePopupAnchor = null
                      },
                  contentAlignment = Alignment.Center,
                ) {
                  Text(text = variant, fontSize = 21.sp)
                }
              }
            }
          }
        }
      }

      // 3. BOTTOM: Ridmik Keyboard Bottom Category Navigation Bar (restored when search keyboard is dismissed)
      if (!activeSearchFocused) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .height(if (panelHeight < 185.dp) 38.dp else 44.dp)
              .focusProperties { canFocus = false }
              .background(palette.toolbarBackground)
              .padding(horizontal = if (availableWidthDp < 330f) 2.dp else 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Return to keyboard button ("ABC" or "বাং")
          Box(
            modifier =
              Modifier.width(returnButtonWidth)
                .height(if (panelHeight < 185.dp) 32.dp else 36.dp)
                .focusProperties { canFocus = false }
                .clip(RoundedCornerShape(6.dp))
                .background(palette.functionalKeyBackground)
                .clickable(onClick = onCloseEmoji)
                .testTag("emoji_return_keyboard"),
            contentAlignment = Alignment.Center,
          ) {
            val returnLabel =
              if (
                activeLanguageLabel.contains("বাংলা") ||
                  activeLanguageLabel.contains("প্রভাত")
              ) {
                "বাং"
              } else {
                "ABC"
              }
            Text(
              text = returnLabel,
              color = palette.keyTextPrimary,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }

          // Category Navigation Icons (Horizontal Scrollable Middle Bar)
          Row(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .focusProperties { canFocus = false }
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            EmojiCategory.entries.forEach { category ->
              val isSelected = !isSearching && category == selectedCategory
              Box(
                modifier =
                  Modifier.size(if (availableWidthDp < 330f) 32.dp else 36.dp)
                    .focusProperties { canFocus = false }
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      if (isSelected) palette.accentKeyBackground.copy(alpha = 0.22f)
                      else Color.Transparent
                    )
                    .clickable {
                      updateSearchQuery("")
                      selectedCategory = category
                    }
                    .testTag("emoji_cat_${category.name.lowercase()}"),
                contentAlignment = Alignment.Center,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                ) {
                  Text(
                    text = category.iconSymbol,
                    fontSize = if (isSelected) 17.sp else 15.sp,
                  )
                  if (isSelected) {
                    Box(
                      modifier =
                        Modifier.width(16.dp)
                          .height(2.dp)
                          .background(palette.accentKeyBackground, RoundedCornerShape(1.dp)),
                    )
                  }
                }
              }
            }
          }

          // Backspace button with repeat support
          Box(
            modifier =
              Modifier.width(backspaceButtonWidth)
                .height(if (panelHeight < 185.dp) 32.dp else 36.dp)
                .focusProperties { canFocus = false }
                .clip(RoundedCornerShape(6.dp))
                .background(palette.functionalKeyBackground)
                .pointerInput(Unit) {
                  awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onBackspace()

                    val repeatJob =
                      scope.launch {
                        delay(380L)
                        while (true) {
                          onBackspace()
                          delay(55L)
                        }
                      }

                    waitForUpOrCancellation()
                    repeatJob.cancel()
                  }
                }
                .testTag("emoji_backspace_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "⌫",
              color = palette.keyTextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FilterTabChip(
  text: String,
  isSelected: Boolean,
  palette: KeyboardPalette,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.focusProperties { canFocus = false }
        .clip(RoundedCornerShape(10.dp))
        .background(
          if (isSelected) palette.accentKeyBackground else palette.functionalKeyBackground
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = if (isSelected) palette.accentKeyText else palette.keyTextSecondary,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
    )
  }
}
