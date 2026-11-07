package com.example.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.clipboard.ClipboardEntry
import com.example.clipboard.ClipboardPanel
import com.example.emoji.EmojiPanel
import com.example.language.KeyboardLanguage
import com.example.language.LanguageInputMethodKind
import com.example.language.selector.LanguageSelectorPanel
import com.example.layout.EnterActionType
import com.example.layout.KeyAction
import com.example.layout.KeyRole
import com.example.layout.KeyboardHeightBounds
import com.example.layout.KeyboardMode
import com.example.layout.LayoutManager
import com.example.layout.ShiftState
import com.example.theme.KeyboardPalette
import com.example.toolbar.KeyboardToolbar
import com.example.translate.TranslateLanguageItem
import com.example.translate.TranslatorPanel
import com.example.translate.TranslatorUiState
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceUiState

internal var debugRootRecompositionCount: Int = 0

@Composable
fun KeyboardSurface(
  mode: KeyboardMode,
  shiftState: ShiftState,
  enterActionType: EnterActionType,
  portraitHeightPercent: Int,
  landscapeHeightPercent: Int,
  showNumberRow: Boolean,
  showToolbar: Boolean,
  cutCopyPasteEnabled: Boolean,
  activeLanguageLabel: String,
  activeLanguageBadge: String,
  activeLanguageKind: LanguageInputMethodKind,
  palette: KeyboardPalette,
  clipboardEntries: List<ClipboardEntry>,
  onKeyAction: (KeyAction) -> Unit,
  onCommitDirectText: (String) -> Unit,
  onToggleNumberRow: () -> Unit,
  onSwitchLanguage: () -> Unit,
  onMoveCursorLeft: () -> Unit,
  onMoveCursorRight: () -> Unit,
  onCycleTheme: () -> Unit,
  onTogglePinClip: (Long) -> Unit,
  onDeleteClip: (Long) -> Unit,
  onClearUnpinnedClips: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
  isEmojiSearchFocused: Boolean = false,
  onEmojiSearchFocusChange: ((Boolean) -> Unit)? = null,
  emojiSearchQuery: String? = null,
  emojiSearchRawQuery: String = "",
  onEmojiSearchQueryChange: ((String) -> Unit)? = null,
  emojiSearchCursor: Int? = null,
  onEmojiSearchCursorChange: ((Int) -> Unit)? = null,
  activeLanguageId: String = "",
  availableLanguages: List<KeyboardLanguage> = emptyList(),
  onSelectLanguage: (String) -> Unit = {},
  isLanguageSelectorOpen: Boolean = false,
  onToggleLanguageSelector: () -> Unit = {},
  isVoiceTypingOpen: Boolean = false,
  voiceListeningState: State<Boolean>? = null,
  voiceRmsScaleState: State<Float>? = null,
  onToggleVoiceTyping: () -> Unit = {},
  voiceUiState: VoiceUiState = VoiceUiState(),
  onSelectVoiceLanguage: (VoiceLanguage) -> Unit = {},
  onStartVoiceListening: () -> Unit = {},
  onStopVoiceListening: () -> Unit = {},
  onCancelVoice: () -> Unit = {},
  onInsertVoice: () -> Unit = {},
  isTranslatorOpen: Boolean = false,
  onToggleTranslator: () -> Unit = {},
  translatorUiState: TranslatorUiState = TranslatorUiState(),
  onTranslatorInputTextChange: (String) -> Unit = {},
  onTranslatorClearInput: () -> Unit = {},
  onTranslatorPasteFromClipboard: () -> Unit = {},
  onTranslatorTranslate: () -> Unit = {},
  onTranslatorSwapLanguages: () -> Unit = {},
  onTranslatorOpenSourceSelector: () -> Unit = {},
  onTranslatorOpenTargetSelector: () -> Unit = {},
  onTranslatorSelectLanguage: (TranslateLanguageItem) -> Unit = {},
  onTranslatorCloseSelector: () -> Unit = {},
  onTranslatorSearchQueryChange: (String) -> Unit = {},
  onTranslatorCopyTranslation: (String) -> Unit = {},
  onTranslatorInsertTranslation: (String) -> Unit = {},
) {
  SideEffect {
    debugRootRecompositionCount++
  }

  var isClipboardOpen by remember { mutableStateOf(false) }
  var internalEmojiSearchFocused by remember { mutableStateOf(false) }
  var internalEmojiSearchQuery by remember { mutableStateOf("") }
  var internalEmojiSearchCursor by remember { mutableIntStateOf(0) }

  val currentOnEmojiSearchFocusChange by rememberUpdatedState(onEmojiSearchFocusChange)
  val currentOnEmojiSearchQueryChange by rememberUpdatedState(onEmojiSearchQueryChange)
  val currentOnEmojiSearchCursorChange by rememberUpdatedState(onEmojiSearchCursorChange)

  val activeEmojiSearchFocused =
    if (onEmojiSearchFocusChange != null) isEmojiSearchFocused else internalEmojiSearchFocused
  val activeEmojiSearchQuery =
    emojiSearchQuery ?: internalEmojiSearchQuery
  val activeEmojiSearchCursor =
    emojiSearchCursor ?: internalEmojiSearchCursor
  val currentActiveEmojiSearchQuery by rememberUpdatedState(activeEmojiSearchQuery)

  val handleEmojiSearchFocusChange: (Boolean) -> Unit = remember {
    { focused ->
      val external = currentOnEmojiSearchFocusChange
      if (external != null) {
        external(focused)
      } else {
        internalEmojiSearchFocused = focused
      }
    }
  }

  val handleEmojiSearchQueryChange: (String) -> Unit = remember {
    { query ->
      val external = currentOnEmojiSearchQueryChange
      if (external != null) {
        external(query)
      } else {
        internalEmojiSearchQuery = query
        internalEmojiSearchCursor = query.length
      }
    }
  }

  val handleEmojiSearchCursorChange: (Int) -> Unit = remember {
    { cursor ->
      val external = currentOnEmojiSearchCursorChange
      if (external != null) {
        external(cursor)
      } else {
        internalEmojiSearchCursor = cursor.coerceIn(0, currentActiveEmojiSearchQuery.length)
      }
    }
  }

  val currentOnKeyAction by rememberUpdatedState(onKeyAction)
  val currentOnCommitDirectText by rememberUpdatedState(onCommitDirectText)
  val currentIsClipboardOpen by rememberUpdatedState(isClipboardOpen)
  val currentIsLanguageSelectorOpen by rememberUpdatedState(isLanguageSelectorOpen)
  val currentIsTranslatorOpen by rememberUpdatedState(isTranslatorOpen)
  val currentIsVoiceTypingOpen by rememberUpdatedState(isVoiceTypingOpen)
  val currentVoiceListeningState by rememberUpdatedState(voiceListeningState)
  val currentOnToggleVoiceTyping by rememberUpdatedState(onToggleVoiceTyping)
  val currentOnToggleLanguageSelector by rememberUpdatedState(onToggleLanguageSelector)
  val currentOnToggleTranslator by rememberUpdatedState(onToggleTranslator)

  val handleToggleClipboard: () -> Unit = remember {
    {
      if (!currentIsClipboardOpen) {
        val voiceActive = currentVoiceListeningState?.value ?: currentIsVoiceTypingOpen
        if (voiceActive) currentOnToggleVoiceTyping()
        if (currentIsLanguageSelectorOpen) currentOnToggleLanguageSelector()
        if (currentIsTranslatorOpen) currentOnToggleTranslator()
      }
      isClipboardOpen = !currentIsClipboardOpen
    }
  }

  val handleOpenLanguageSelector: () -> Unit = remember {
    {
      if (!currentIsLanguageSelectorOpen) {
        isClipboardOpen = false
        val voiceActive = currentVoiceListeningState?.value ?: currentIsVoiceTypingOpen
        if (voiceActive) currentOnToggleVoiceTyping()
        if (currentIsTranslatorOpen) currentOnToggleTranslator()
      }
      currentOnToggleLanguageSelector()
    }
  }

  val handleToggleVoiceTyping: () -> Unit = remember {
    {
      val voiceActive = currentVoiceListeningState?.value ?: currentIsVoiceTypingOpen
      if (!voiceActive) {
        isClipboardOpen = false
      }
      currentOnToggleVoiceTyping()
    }
  }

  val handleToggleTranslator: () -> Unit = remember {
    {
      if (!currentIsTranslatorOpen) {
        isClipboardOpen = false
        if (currentIsLanguageSelectorOpen) currentOnToggleLanguageSelector()
        val voiceActive = currentVoiceListeningState?.value ?: currentIsVoiceTypingOpen
        if (voiceActive) currentOnToggleVoiceTyping()
      }
      currentOnToggleTranslator()
    }
  }

  val handlePasteClip: (String) -> Unit = remember {
    { text ->
      currentOnCommitDirectText(text)
      isClipboardOpen = false
    }
  }

  val handleCloseClipboard: () -> Unit = remember {
    { isClipboardOpen = false }
  }

  val handleEmojiBackspace: () -> Unit = remember {
    { currentOnKeyAction(KeyAction.Backspace) }
  }

  val handleEmojiSpace: () -> Unit = remember {
    { currentOnKeyAction(KeyAction.Space) }
  }

  val handleCloseEmoji: () -> Unit = remember {
    {
      handleEmojiSearchFocusChange(false)
      handleEmojiSearchQueryChange("")
      currentOnKeyAction(KeyAction.ModeAlphabet)
    }
  }

  val handleToggleEmoji: () -> Unit = remember {
    {
      if (mode == KeyboardMode.EMOJI) {
        handleEmojiSearchFocusChange(false)
        handleEmojiSearchQueryChange("")
        currentOnKeyAction(KeyAction.ModeAlphabet)
      } else {
        isClipboardOpen = false
        if (currentIsLanguageSelectorOpen) currentOnToggleLanguageSelector()
        if (currentIsTranslatorOpen) currentOnToggleTranslator()
        val voiceActive = currentVoiceListeningState?.value ?: currentIsVoiceTypingOpen
        if (voiceActive) currentOnToggleVoiceTyping()
        currentOnKeyAction(KeyAction.ModeEmoji)
      }
    }
  }

  LaunchedEffect(mode) {
    if (mode == KeyboardMode.EMOJI) {
      isClipboardOpen = false
    } else {
      internalEmojiSearchFocused = false
    }
  }

  val layoutSpec =
    remember(mode, showNumberRow, activeLanguageKind) {
      LayoutManager.getLayout(mode, showNumberRow, activeLanguageKind)
    }

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  val activePercent = if (isLandscape) landscapeHeightPercent else portraitHeightPercent
  val rowHeightDp = KeyboardHeightBounds.rowHeightDpForPercent(activePercent, isLandscape)
  val rowHeight = rowHeightDp.dp
  val toolbarHeight = KeyboardHeightBounds.toolbarHeightDpForPercent(activePercent, isLandscape).dp
  val heightScale = (rowHeightDp / (if (isLandscape) 38f else 50f)).coerceIn(0.60f, 1.80f)
  val totalRowsHeight = rowHeight * layoutSpec.rows.size + (8f * heightScale).dp

  val isEmojiSearchActive = mode == KeyboardMode.EMOJI && activeEmojiSearchFocused
  val isCompactKeyboardActive = isEmojiSearchActive

  val searchRowHeightDp =
    (rowHeightDp * 0.72f).coerceIn(
      if (isLandscape) 28f else 36f,
      if (isLandscape) 38f else 46f,
    )
  val activeRowHeightDp = if (isCompactKeyboardActive) searchRowHeightDp else rowHeightDp
  val activeRowHeight = activeRowHeightDp.dp
  val activeContainerVerticalPadding =
    if (isCompactKeyboardActive) 2.dp else (2.5f * heightScale).dp
  val searchKeyboardRowsHeight =
    activeRowHeight * layoutSpec.rows.size + (activeContainerVerticalPadding * 2) + 2.dp
  val compactEmojiSearchPanelHeight =
    (totalRowsHeight - searchKeyboardRowsHeight + 12.dp).coerceIn(
      if (isLandscape) 68.dp else 84.dp,
      if (isLandscape) 92.dp else 112.dp,
    )

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .focusProperties { canFocus = false }
        .background(palette.keyboardBackground)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .testTag("keyboard_surface"),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val maxKeyboardWidth =
      if (maxWidth > 600.dp) {
        580.dp.coerceAtMost(maxWidth * 0.85f)
      } else {
        maxWidth
      }

    val isNarrowScreen = maxKeyboardWidth < 360.dp
    val isVeryNarrowScreen = maxKeyboardWidth < 330.dp
    val keyHorizontalPaddingDp =
      if (isVeryNarrowScreen) 1.0f
      else if (isNarrowScreen) 1.4f
      else 1.8f

    Column(
      modifier =
        Modifier.widthIn(max = maxKeyboardWidth)
          .fillMaxWidth()
          .focusProperties { canFocus = false },
    ) {
      if (showToolbar) {
        KeyboardToolbar(
          palette = palette,
          isClipboardOpen = isClipboardOpen,
          clipboardCount = clipboardEntries.size,
          showNumberRow = showNumberRow,
          activeLanguageBadge = activeLanguageBadge,
          onToggleClipboard = handleToggleClipboard,
          onToggleNumberRow = onToggleNumberRow,
          onSwitchLanguage = onSwitchLanguage,
          onMoveCursorLeft = onMoveCursorLeft,
          onMoveCursorRight = onMoveCursorRight,
          onCycleTheme = onCycleTheme,
          onOpenSettings = onOpenSettings,
          height = toolbarHeight,
          isLanguageSelectorOpen = isLanguageSelectorOpen,
          onOpenLanguageSelector = handleOpenLanguageSelector,
          isVoiceTypingOpen =
            if (voiceListeningState != null) false
            else isVoiceTypingOpen && voiceUiState.state == com.example.voice.VoiceState.LISTENING,
          voiceListeningState = voiceListeningState,
          voiceRmsScaleState = voiceRmsScaleState,
          onToggleVoiceTyping = handleToggleVoiceTyping,
          isTranslatorOpen = isTranslatorOpen,
          onToggleTranslator = handleToggleTranslator,
          isEmojiOpen = (mode == KeyboardMode.EMOJI),
          onToggleEmoji = handleToggleEmoji,
        )
      }

      if (isLanguageSelectorOpen) {
        LanguageSelectorPanel(
          palette = palette,
          activeLanguageId = activeLanguageId,
          languages = availableLanguages,
          panelHeight = totalRowsHeight,
          onSelectLanguage = onSelectLanguage,
          onClose = onToggleLanguageSelector,
        )
      } else if (isTranslatorOpen) {
        TranslatorPanel(
          palette = palette,
          uiState = translatorUiState,
          panelHeight = totalRowsHeight,
          onInputTextChange = onTranslatorInputTextChange,
          onClearInput = onTranslatorClearInput,
          onPasteFromClipboard = onTranslatorPasteFromClipboard,
          onTranslate = onTranslatorTranslate,
          onSwapLanguages = onTranslatorSwapLanguages,
          onOpenSourceSelector = onTranslatorOpenSourceSelector,
          onOpenTargetSelector = onTranslatorOpenTargetSelector,
          onSelectLanguage = onTranslatorSelectLanguage,
          onCloseSelector = onTranslatorCloseSelector,
          onSearchQueryChange = onTranslatorSearchQueryChange,
          onCopyTranslation = onTranslatorCopyTranslation,
          onInsertTranslation = onTranslatorInsertTranslation,
          onCloseTranslator = onToggleTranslator,
        )
      } else if (isClipboardOpen) {
        ClipboardPanel(
          entries = clipboardEntries,
          palette = palette,
          panelHeight = totalRowsHeight,
          onPasteClip = handlePasteClip,
          onTogglePin = onTogglePinClip,
          onDeleteClip = onDeleteClip,
          onClearUnpinned = onClearUnpinnedClips,
          onClose = handleCloseClipboard,
        )
      } else {
        if (mode == KeyboardMode.EMOJI) {
          EmojiPanel(
            palette = palette,
            panelHeight =
              if (activeEmojiSearchFocused) compactEmojiSearchPanelHeight else totalRowsHeight,
            activeLanguageLabel = activeLanguageLabel,
            onCommitEmoji = onCommitDirectText,
            onBackspace = handleEmojiBackspace,
            onSpace = handleEmojiSpace,
            onCloseEmoji = handleCloseEmoji,
            externalSearchQuery = activeEmojiSearchQuery,
            externalRawSearchQuery = emojiSearchRawQuery,
            onExternalSearchQueryChange = handleEmojiSearchQueryChange,
            isSearchFocused = activeEmojiSearchFocused,
            onSearchFocusChange = handleEmojiSearchFocusChange,
            searchCursorIndex = activeEmojiSearchCursor,
            onSearchCursorChange = handleEmojiSearchCursorChange,
          )
        }

        val showKeyboardRows = mode != KeyboardMode.EMOJI || activeEmojiSearchFocused

        if (showKeyboardRows) {
          val activeEnterAction =
            if (isEmojiSearchActive) {
              EnterActionType.SEARCH
            } else {
              enterActionType
            }

          val currentIsEmojiSearchActive by rememberUpdatedState(isEmojiSearchActive)
          val handleLongPressSecondary: (String) -> Unit = remember {
            { hint ->
              if (currentIsEmojiSearchActive) {
                currentOnKeyAction(KeyAction.CommitChar(hint, hint))
              } else {
                currentOnCommitDirectText(hint)
              }
            }
          }

          val stableOnKeyAction: (KeyAction) -> Unit = remember {
            { action -> currentOnKeyAction(action) }
          }

          Column(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = activeContainerVerticalPadding)
                .focusProperties { canFocus = false }
                .testTag("keyboard_rows_container"),
          ) {
            layoutSpec.rows.forEach { rowKeys ->
              val isNineKeyRow =
                (mode == KeyboardMode.ALPHABET || mode == KeyboardMode.EMOJI) && rowKeys.size == 9
              val rowHorizontalPadding =
                if (isNineKeyRow) {
                  val factor =
                    if (activeLanguageKind == LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT) 0.042f
                    else 0.05f
                  (maxKeyboardWidth * factor).coerceIn(8.dp, 28.dp)
                } else {
                  0.dp
                }
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(activeRowHeight)
                    .focusProperties { canFocus = false }
                    .padding(horizontal = rowHorizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                rowKeys.forEach { keySpec ->
                  androidx.compose.runtime.key(keySpec.id) {
                    val isShiftDependent =
                      keySpec.action is KeyAction.Shift ||
                        keySpec.primaryLabel != keySpec.shiftedLabel ||
                        keySpec.secondaryHint != null ||
                        keySpec.longPressEditAction != null
                    KeyButton(
                      spec = keySpec,
                      shiftState = if (isShiftDependent) shiftState else ShiftState.LOWERCASE,
                      enterActionType =
                        if (keySpec.action is KeyAction.Enter) activeEnterAction
                        else EnterActionType.ENTER,
                      spacebarLabel =
                        if (keySpec.role == KeyRole.SPACEBAR) activeLanguageLabel else "",
                      cutCopyPasteEnabled =
                        cutCopyPasteEnabled && keySpec.longPressEditAction != null,
                      palette = palette,
                      onKeyAction = stableOnKeyAction,
                      onLongPressSecondary =
                        if (keySpec.secondaryHint != null) handleLongPressSecondary else null,
                      rowHeightDp = activeRowHeightDp,
                      horizontalPaddingDp = keyHorizontalPaddingDp,
                      modifier = Modifier.weight(keySpec.weight).focusProperties { canFocus = false },
                    )
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
          }
        }
      }
    }
  }
}

