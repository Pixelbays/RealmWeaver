package org.pixelbays.rpg.ability.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.protocol.Color;

public class AbilityQuality {
   @Nullable
   public String id;
   @Nullable
   public String abilityTooltipTexture;
   @Nullable
   public String abilityTooltipArrowTexture;
   @Nullable
   public String slotTexture;
   @Nullable
   public Color textColor;
   @Nullable
   public String localizationKey;
   public boolean visibleQualityLabel;
   public boolean hideFromSearch;

   public AbilityQuality() {
   }

   public AbilityQuality(
      @Nullable String id,
      @Nullable String abilityTooltipTexture,
      @Nullable String abilityTooltipArrowTexture,
      @Nullable String slotTexture,
      @Nullable Color textColor,
      @Nullable String localizationKey,
      boolean visibleQualityLabel,
      boolean hideFromSearch
   ) {
      this.id = id;
      this.abilityTooltipTexture = abilityTooltipTexture;
      this.abilityTooltipArrowTexture = abilityTooltipArrowTexture;
      this.slotTexture = slotTexture;
      this.textColor = textColor;
      this.localizationKey = localizationKey;
      this.visibleQualityLabel = visibleQualityLabel;
      this.hideFromSearch = hideFromSearch;
   }

   public AbilityQuality(@Nonnull AbilityQuality other) {
      this.id = other.id;
      this.abilityTooltipTexture = other.abilityTooltipTexture;
      this.abilityTooltipArrowTexture = other.abilityTooltipArrowTexture;
      this.slotTexture = other.slotTexture;
      this.textColor = other.textColor;
      this.localizationKey = other.localizationKey;
      this.visibleQualityLabel = other.visibleQualityLabel;
      this.hideFromSearch = other.hideFromSearch;
   }
}
