package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for spacing, radii and control sizing.
 * Every screen should use these tokens instead of ad-hoc dp values so
 * density stays consistent and the layout can be tuned in one place.
 */
object Dimens {
    // Spacing scale — 4dp grid, deliberately short.
    val space0: Dp = 0.dp
    val space2: Dp = 2.dp
    val space4: Dp = 4.dp
    val space6: Dp = 6.dp
    val space8: Dp = 8.dp
    val space10: Dp = 10.dp
    val space12: Dp = 12.dp
    val space16: Dp = 16.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp

    /** Horizontal gutter used by every scrolling screen. */
    val screenGutter: Dp = 14.dp

    /** Vertical gap between top-level sections of a screen. */
    val sectionGap: Dp = 12.dp

    /** Vertical gap between blocks inside one card. */
    val blockGap: Dp = 10.dp

    /** Inner padding of cards, chips and tiles. */
    val cardPadding: Dp = 12.dp
    val cardPaddingTight: Dp = 10.dp
    val cardPaddingLoose: Dp = 14.dp

    // Radii
    val radiusXs: Dp = 6.dp
    val radiusSm: Dp = 8.dp
    val radiusMd: Dp = 12.dp
    val radiusLg: Dp = 16.dp
    val radiusXl: Dp = 20.dp
    val radiusPill: Dp = 999.dp

    // Control sizing
    val minTapTarget: Dp = 44.dp
    val rowHeight: Dp = 44.dp
    val iconTileSm: Dp = 30.dp
    val iconTileMd: Dp = 36.dp
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 20.dp
    val iconLg: Dp = 24.dp
    val audioButton: Dp = 40.dp
    val progressHeight: Dp = 5.dp
    val hairline: Dp = 1.dp
}

/** Convenience aliases so call sites read cleanly. */
val space2: Dp get() = Dimens.space2
val space4: Dp get() = Dimens.space4
val space6: Dp get() = Dimens.space6
val space8: Dp get() = Dimens.space8
val space12: Dp get() = Dimens.space12
val space16: Dp get() = Dimens.space16
val space20: Dp get() = Dimens.space20
val radiusXs: Dp get() = Dimens.radiusXs
val radiusSm: Dp get() = Dimens.radiusSm
val radiusMd: Dp get() = Dimens.radiusMd
val radiusLg: Dp get() = Dimens.radiusLg
val radiusXl: Dp get() = Dimens.radiusXl
