package com.example.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.theme.KeyboardPalette

@Composable
fun TranslatorPanel(
  palette: KeyboardPalette,
  uiState: TranslatorUiState,
  panelHeight: Dp,
  onInputTextChange: (String) -> Unit,
  onClearInput: () -> Unit,
  onPasteFromClipboard: () -> Unit,
  onTranslate: () -> Unit,
  onSwapLanguages: () -> Unit,
  onOpenSourceSelector: () -> Unit,
  onOpenTargetSelector: () -> Unit,
  onSelectLanguage: (TranslateLanguageItem) -> Unit,
  onCloseSelector: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onCopyTranslation: (String) -> Unit,
  onInsertTranslation: (String) -> Unit,
  onCloseTranslator: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(panelHeight)
        .background(palette.keyboardBackground)
        .testTag("translator_panel"),
  ) {
    if (uiState.selectorMode != LanguageSelectorMode.NONE) {
      LanguageSelectorView(
        palette = palette,
        isSource = uiState.selectorMode == LanguageSelectorMode.SOURCE,
        currentLanguage =
          if (uiState.selectorMode == LanguageSelectorMode.SOURCE) uiState.sourceLanguage
          else uiState.targetLanguage,
        searchQuery = uiState.languageSearchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSelectLanguage = onSelectLanguage,
        onClose = onCloseSelector,
      )
    } else {
      TranslatorMainView(
        palette = palette,
        uiState = uiState,
        onInputTextChange = onInputTextChange,
        onClearInput = onClearInput,
        onPasteFromClipboard = onPasteFromClipboard,
        onTranslate = onTranslate,
        onSwapLanguages = onSwapLanguages,
        onOpenSourceSelector = onOpenSourceSelector,
        onOpenTargetSelector = onOpenTargetSelector,
        onCopyTranslation = onCopyTranslation,
        onInsertTranslation = onInsertTranslation,
        onCloseTranslator = onCloseTranslator,
      )
    }
  }
}

@Composable
private fun TranslatorMainView(
  palette: KeyboardPalette,
  uiState: TranslatorUiState,
  onInputTextChange: (String) -> Unit,
  onClearInput: () -> Unit,
  onPasteFromClipboard: () -> Unit,
  onTranslate: () -> Unit,
  onSwapLanguages: () -> Unit,
  onOpenSourceSelector: () -> Unit,
  onOpenTargetSelector: () -> Unit,
  onCopyTranslation: (String) -> Unit,
  onInsertTranslation: (String) -> Unit,
  onCloseTranslator: () -> Unit,
) {
  val scrollState = rememberScrollState()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // 1. Language Control Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(palette.toolbarBackground)
          .padding(horizontal = 6.dp, vertical = 4.dp)
          .testTag("translator_header_bar"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Source Language Pill
      LanguagePill(
        language = uiState.sourceLanguage,
        palette = palette,
        onClick = onOpenSourceSelector,
        tag = "translator_source_lang_btn",
        modifier = Modifier.weight(1f),
      )

      // Swap button
      Box(
        modifier =
          Modifier.padding(horizontal = 4.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(if (uiState.canSwap) palette.keyBackground else palette.keyBackground.copy(alpha = 0.4f))
            .clickable(enabled = uiState.canSwap, onClick = onSwapLanguages)
            .testTag("translator_swap_languages_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.SwapHoriz,
          contentDescription = stringResource(R.string.translator_swap_languages),
          tint = if (uiState.canSwap) palette.keyTextPrimary else palette.keyTextSecondary.copy(alpha = 0.5f),
          modifier = Modifier.size(18.dp),
        )
      }

      // Target Language Pill
      LanguagePill(
        language = uiState.targetLanguage,
        palette = palette,
        onClick = onOpenTargetSelector,
        tag = "translator_target_lang_btn",
        modifier = Modifier.weight(1f),
      )

      Spacer(modifier = Modifier.width(6.dp))

      // Close Button
      Box(
        modifier =
          Modifier.size(28.dp)
            .clip(CircleShape)
            .background(palette.keyBackground)
            .clickable(onClick = onCloseTranslator)
            .testTag("translator_close_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(R.string.cd_close_panel),
          tint = palette.keyTextSecondary,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    // 2. Input Box Card
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(palette.keyBackground)
          .padding(8.dp)
          .testTag("translator_input_card"),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Detected language indicator (if auto detect is active and language detected)
      if (uiState.sourceLanguage.isAutoDetect && uiState.detectedLanguage != null) {
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(4.dp))
              .background(palette.accentKeyBackground.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
              .testTag("translator_detected_badge"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Detected: ${uiState.detectedLanguage.displayName} (${uiState.detectedLanguage.nativeName})",
            color = palette.accentKeyBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      // Multiline Input Field
      Box(
        modifier = Modifier.fillMaxWidth().height(52.dp),
      ) {
        BasicTextField(
          value = uiState.inputText,
          onValueChange = onInputTextChange,
          modifier = Modifier.fillMaxSize().testTag("translator_input_field"),
          textStyle = TextStyle(
            color = palette.keyTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
          ),
          cursorBrush = SolidColor(palette.accentKeyBackground),
          decorationBox = { innerTextField ->
            if (uiState.inputText.isEmpty()) {
              Text(
                text = stringResource(R.string.translator_input_placeholder),
                color = palette.keyTextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
              )
            }
            innerTextField()
          },
        )
      }

      // Input Bottom Bar: Paste, Clear, & Live Translate
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Paste Button
          SmallActionPill(
            icon = Icons.Outlined.ContentPaste,
            label = stringResource(R.string.translator_btn_paste),
            palette = palette,
            onClick = onPasteFromClipboard,
            tag = "translator_paste_btn",
          )

          // Clear Button
          if (uiState.inputText.isNotEmpty()) {
            SmallActionPill(
              icon = Icons.Default.Clear,
              label = stringResource(R.string.translator_btn_clear),
              palette = palette,
              onClick = onClearInput,
              tag = "translator_clear_btn",
            )
          }
        }

        // Translate Primary Button
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(16.dp))
              .background(
                if (uiState.inputText.isNotBlank() && !uiState.isLoading)
                  palette.accentKeyBackground
                else
                  palette.accentKeyBackground.copy(alpha = 0.45f)
              )
              .clickable(
                enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                onClick = onTranslate,
              )
              .padding(horizontal = 14.dp, vertical = 6.dp)
              .testTag("translator_execute_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (uiState.isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                color = palette.accentKeyText,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = palette.accentKeyText,
                modifier = Modifier.size(14.dp),
              )
            }
            Text(
              text = stringResource(R.string.translator_btn_translate),
              color = palette.accentKeyText,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Status or Download Model Notice
    if (uiState.statusMessage != null && uiState.isLoading) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.toolbarBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("translator_status_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(12.dp),
          color = palette.accentKeyBackground,
          strokeWidth = 1.5.dp,
        )
        Text(
          text = uiState.statusMessage,
          color = palette.keyTextSecondary,
          fontSize = 11.sp,
        )
      }
    }

    // Error Message
    if (uiState.errorMessage != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(androidx.compose.ui.graphics.Color(0xFFE53935).copy(alpha = 0.12f))
            .padding(8.dp)
            .testTag("translator_error_box"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.ErrorOutline,
          contentDescription = null,
          tint = androidx.compose.ui.graphics.Color(0xFFE53935),
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = uiState.errorMessage,
          color = androidx.compose.ui.graphics.Color(0xFFE53935),
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    // 3. Translation Result Card
    if (uiState.hasResult) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.toolbarBackground)
            .border(1.dp, palette.accentKeyBackground.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(8.dp)
            .testTag("translator_result_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Translated (${uiState.targetLanguage.displayName} - ${uiState.targetLanguage.nativeName}):",
            color = palette.accentKeyBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          )
        }

        Text(
          text = uiState.translatedText,
          color = palette.keyTextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 18.sp,
          modifier = Modifier.testTag("translator_result_text"),
        )

        // Actions: Copy & Insert directly into host app text field
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Copy button
          SmallActionPill(
            icon = Icons.Default.ContentCopy,
            label = stringResource(R.string.translator_btn_copy),
            palette = palette,
            onClick = { onCopyTranslation(uiState.translatedText) },
            tag = "translator_copy_result_btn",
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Insert into host editor button
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp))
                .background(palette.accentKeyBackground)
                .clickable { onInsertTranslation(uiState.translatedText) }
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("translator_insert_result_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = palette.accentKeyText,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = stringResource(R.string.translator_btn_insert),
                color = palette.accentKeyText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LanguagePill(
  language: TranslateLanguageItem,
  palette: KeyboardPalette,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(6.dp))
        .background(palette.keyBackground)
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = language.displayName,
        color = palette.keyTextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!language.isAutoDetect) {
        Text(
          text = language.nativeName,
          color = palette.keyTextSecondary,
          fontSize = 10.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SmallActionPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  palette: KeyboardPalette,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(12.dp))
        .background(palette.toolbarBackground)
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = palette.keyTextSecondary,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = label,
        color = palette.keyTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun LanguageSelectorView(
  palette: KeyboardPalette,
  isSource: Boolean,
  currentLanguage: TranslateLanguageItem,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onSelectLanguage: (TranslateLanguageItem) -> Unit,
  onClose: () -> Unit,
) {
  val allLanguages = remember(isSource) {
    if (isSource) TranslateLanguageItem.getSourceLanguages()
    else TranslateLanguageItem.getTargetLanguages()
  }

  val filteredLanguages = remember(searchQuery, allLanguages) {
    val query = searchQuery.trim().lowercase()
    if (query.isEmpty()) {
      allLanguages
    } else {
      allLanguages.filter { it.searchKeywords.contains(query) }
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("translator_language_selector_view"),
  ) {
    // Top Bar: Back button, Title & Search
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(32.dp)
            .clip(CircleShape)
            .background(palette.keyBackground)
            .clickable(onClick = onClose)
            .testTag("translator_selector_back_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_close_panel),
          tint = palette.keyTextPrimary,
          modifier = Modifier.size(18.dp),
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Search Box
      Row(
        modifier =
          Modifier.weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.keyBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = palette.keyTextSecondary,
          modifier = Modifier.size(15.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
          BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().testTag("translator_language_search_input"),
            textStyle = TextStyle(
              color = palette.keyTextPrimary,
              fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(palette.accentKeyBackground),
            decorationBox = { innerTextField ->
              if (searchQuery.isEmpty()) {
                Text(
                  text = if (isSource) "Search source language…" else "Search target language…",
                  color = palette.keyTextSecondary.copy(alpha = 0.7f),
                  fontSize = 12.sp,
                )
              }
              innerTextField()
            },
          )
        }

        if (searchQuery.isNotEmpty()) {
          Box(
            modifier =
              Modifier.size(18.dp)
                .clip(CircleShape)
                .clickable { onSearchQueryChange("") },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = null,
              tint = palette.keyTextSecondary,
              modifier = Modifier.size(13.dp),
            )
          }
        }
      }
    }

    // Language List
    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f).testTag("translator_language_list"),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      contentPadding = PaddingValues(vertical = 2.dp),
    ) {
      items(filteredLanguages, key = { it.code }) { item ->
        val isSelected = item.code == currentLanguage.code
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) palette.accentKeyBackground.copy(alpha = 0.2f) else palette.keyBackground)
              .clickable { onSelectLanguage(item) }
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .testTag("lang_item_${item.code}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // Badge
            Box(
              modifier =
                Modifier.size(24.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isSelected) palette.accentKeyBackground else palette.toolbarBackground),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = item.badge,
                color = if (isSelected) palette.accentKeyText else palette.keyTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              )
            }

            Column {
              Text(
                text = item.displayName,
                color = if (isSelected) palette.accentKeyBackground else palette.keyTextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              )
              if (!item.isAutoDetect) {
                Text(
                  text = item.nativeName,
                  color = palette.keyTextSecondary,
                  fontSize = 11.sp,
                )
              }
            }
          }

          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = palette.accentKeyBackground,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}
