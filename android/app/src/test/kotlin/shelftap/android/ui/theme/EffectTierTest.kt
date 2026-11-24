package shelftap.android.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class EffectTierTest {
    private fun tier(sdk: Int, lowRam: Boolean = false, powerSave: Boolean = false, animationsOff: Boolean = false, blur: Boolean? = true) =
        decideTier(sdk, lowRam, powerSave, animationsOff, blur)

    @Test
    fun android17IsFull() = assertEquals(EffectTier.FULL, tier(37))

    @Test
    fun android12To16IsMedium() {
        assertEquals(EffectTier.MEDIUM, tier(31))
        assertEquals(EffectTier.MEDIUM, tier(36))
    }

    @Test
    fun android8To11IsLight() {
        assertEquals(EffectTier.LIGHT, tier(26, blur = null))
        assertEquals(EffectTier.LIGHT, tier(30, blur = null))
    }

    @Test
    fun lowRamBatterySaverAndNoAnimationsDropToLight() {
        assertEquals(EffectTier.LIGHT, tier(37, lowRam = true))
        assertEquals(EffectTier.LIGHT, tier(36, powerSave = true))
        assertEquals(EffectTier.LIGHT, tier(37, animationsOff = true))
    }

    @Test
    fun systemBlurOffDropsToLight() = assertEquals(EffectTier.LIGHT, tier(37, blur = false))

    @Test
    fun forcedTierNeverAsksForBlurBelowAndroid12() {
        assertEquals(EffectTier.LIGHT, clampForced(EffectTier.FULL, 30))
        assertEquals(EffectTier.MEDIUM, clampForced(EffectTier.MEDIUM, 31))
        assertEquals(EffectTier.LIGHT, clampForced(EffectTier.LIGHT, 36))
    }
}
