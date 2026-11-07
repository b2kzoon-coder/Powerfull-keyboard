package com.example.language.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.language.KeyboardLanguage
import com.example.language.LanguageManager
import com.example.theme.KeyboardPalette

@Composable
fun LanguageSelectorPanel(
  palette: KeyboardPalette,
  activeLanguageId: String,
  languages: List<KeyboardLanguage>,
  panelHeight: Dp,
  onSelectLanguage: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .height(panelHeight)
        .focusProperties { canFocus = false }
        .background(palette.keyboardBackground)
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .testTag("language_selector_panel"),
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.focusProperties { canFocus = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Language,
          contentDescription = null,
          tint = palette.accentKeyBackground,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = stringResource(R.string.lang_selector_title),
          color = palette.keyTextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      Box(
        modifier =
          Modifier.size(32.dp)
            .focusProperties { canFocus = false }
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClose)
            .testTag("lang_selector_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(R.string.cd_close_panel),
          tint = palette.keyTextSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // List of 4 typing modes: English, বাংলা (Probhat), Banglish, हिन्दी (Hinglish)
    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f).focusProperties { canFocus = false },
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      items(languages, key = { it.id }) { lang ->
        val isSelected = lang.id == activeLanguageId
        val description =
          when (lang.id) {
            LanguageManager.ID_ENGLISH -> stringResource(R.string.lang_english_desc)
            LanguageManager.ID_BANGLA_PROBHAT -> stringResource(R.string.lang_probhat_desc)
            LanguageManager.ID_BANGLISH -> stringResource(R.string.lang_banglish_desc)
            LanguageManager.ID_HINDI_HINGLISH -> stringResource(R.string.lang_hinglish_desc)
            else -> lang.spacebarLabel
          }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .focusProperties { canFocus = false }
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isSelected) palette.accentKeyBackground.copy(alpha = 0.15f)
                else palette.keyBackground
              )
              .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) palette.accentKeyBackground else palette.dividerColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
              )
              .clickable {
                onSelectLanguage(lang.id)
                onClose()
              }
              .padding(horizontal = 12.dp, vertical = 7.dp)
              .testTag("lang_option_${lang.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f).focusProperties { canFocus = false },
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Badge pill
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                  .background(
                    if (isSelected) palette.accentKeyBackground
                    else palette.keyboardBackground
                  )
                  .padding(horizontal = 7.dp, vertical = 3.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = lang.badgeLabel,
                color = if (isSelected) palette.accentKeyText else palette.keyTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.focusProperties { canFocus = false }) {
              Text(
                text = lang.displayName,
                color = palette.keyTextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              )
              Text(
                text = description,
                color = palette.keyTextSecondary,
                fontSize = 10.5.sp,
              )
            }
          }

          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected",
              tint = palette.accentKeyBackground,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}
