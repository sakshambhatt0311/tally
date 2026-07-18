package com.tally.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tally.app.R

/**
 * Plus Jakarta Sans — bundled locally in res/font, so it's guaranteed offline with no runtime
 * fetch. Weights map straight onto the .ttf files placed at:
 *   res/font/plus_jakarta_sans_{regular,medium,semibold,bold,extrabold}.ttf
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val Family = PlusJakartaSans
private val Extra = FontWeight.ExtraBold
private val Bold = FontWeight.Bold
private val Semi = FontWeight.SemiBold
private val Medium = FontWeight.Medium
private val Normal = FontWeight.Normal

val TallyTypography = Typography(
    // "Tally" splash wordmark.
    displaySmall = TextStyle(fontFamily = Family, fontWeight = Extra, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),

    // Screen titles: "My Circles". Big + ExtraBold.
    headlineLarge = TextStyle(fontFamily = Family, fontWeight = Extra, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Family, fontWeight = Extra, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Family, fontWeight = Bold, fontSize = 22.sp, lineHeight = 28.sp),

    // Section / large card title.
    titleLarge = TextStyle(fontFamily = Family, fontWeight = Bold, fontSize = 22.sp, lineHeight = 28.sp),
    // Card titles ("Common Room Crew"), player/game names. Bold.
    titleMedium = TextStyle(fontFamily = Family, fontWeight = Bold, fontSize = 19.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Family, fontWeight = Semi, fontSize = 15.sp, lineHeight = 20.sp),

    // Button labels.
    labelLarge = TextStyle(fontFamily = Family, fontWeight = Semi, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    // Metadata: "5 members · Last night · Catan".
    labelMedium = TextStyle(fontFamily = Family, fontWeight = Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    // Avatar initials, tight chip text.
    labelSmall = TextStyle(fontFamily = Family, fontWeight = Semi, fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp),

    // Body copy.
    bodyLarge = TextStyle(fontFamily = Family, fontWeight = Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Family, fontWeight = Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Family, fontWeight = Normal, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.2.sp),
)
