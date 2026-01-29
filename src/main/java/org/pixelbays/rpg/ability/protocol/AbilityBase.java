package org.pixelbays.rpg.ability.protocol;

import com.hypixel.hytale.protocol.AssetIconProperties;
import com.hypixel.hytale.protocol.InteractionConfiguration;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ItemPullbackConfiguration;
import com.hypixel.hytale.protocol.ModelParticle;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbilityBase {
   @Nullable
   public String id;
   @Nullable
   public String icon;
   @Nullable
   public AssetIconProperties iconProperties;
   @Nullable
   public AbilityTranslationProperties translationProperties;
   public int abilityLevel;
   public int qualityIndex;
   @Nullable
   public String playerAnimationsId;
   public boolean usePlayerAnimations;
   @Nullable
   public String animation;
   @Nullable
   public String[] categories;
   public int soundEventIndex;
   public int abilitySoundSetIndex;
   @Nullable
   public ModelParticle[] particles;
   @Nullable
   public ModelParticle[] firstPersonParticles;
   @Nullable
   public Object2IntOpenHashMap<InteractionType> interactions;
   @Nullable
   public Object2IntOpenHashMap<String> interactionVars;
   @Nullable
   public InteractionConfiguration interactionConfig;
   @Nullable
   public int[] tagIndexes;
   @Nullable
   public ItemPullbackConfiguration pullbackConfig;

   public AbilityBase() {
   }

   public AbilityBase(
      @Nullable String id,
      @Nullable String icon,
      @Nullable AssetIconProperties iconProperties,
      @Nullable AbilityTranslationProperties translationProperties,
      int abilityLevel,
      int qualityIndex,
      @Nullable String playerAnimationsId,
      boolean usePlayerAnimations,
      @Nullable String animation,
      @Nullable String[] categories,
      int soundEventIndex,
      int abilitySoundSetIndex,
      @Nullable ModelParticle[] particles,
      @Nullable ModelParticle[] firstPersonParticles,
      @Nullable Object2IntOpenHashMap<InteractionType> interactions,
      @Nullable Object2IntOpenHashMap<String> interactionVars,
      @Nullable InteractionConfiguration interactionConfig,
      @Nullable int[] tagIndexes,
      @Nullable ItemPullbackConfiguration pullbackConfig
   ) {
      this.id = id;
      this.icon = icon;
      this.iconProperties = iconProperties;
      this.translationProperties = translationProperties;
      this.abilityLevel = abilityLevel;
      this.qualityIndex = qualityIndex;
      this.playerAnimationsId = playerAnimationsId;
      this.usePlayerAnimations = usePlayerAnimations;
      this.animation = animation;
      this.categories = categories;
      this.soundEventIndex = soundEventIndex;
      this.abilitySoundSetIndex = abilitySoundSetIndex;
      this.particles = particles;
      this.firstPersonParticles = firstPersonParticles;
      this.interactions = interactions;
      this.interactionVars = interactionVars;
      this.interactionConfig = interactionConfig;
      this.tagIndexes = tagIndexes;
      this.pullbackConfig = pullbackConfig;
   }

   public AbilityBase(@Nonnull AbilityBase other) {
      this.id = other.id;
      this.icon = other.icon;
      this.iconProperties = other.iconProperties;
      this.translationProperties = other.translationProperties;
      this.abilityLevel = other.abilityLevel;
      this.qualityIndex = other.qualityIndex;
      this.playerAnimationsId = other.playerAnimationsId;
      this.usePlayerAnimations = other.usePlayerAnimations;
      this.animation = other.animation;
      this.categories = other.categories;
      this.soundEventIndex = other.soundEventIndex;
      this.abilitySoundSetIndex = other.abilitySoundSetIndex;
      this.particles = other.particles;
      this.firstPersonParticles = other.firstPersonParticles;
      this.interactions = other.interactions;
      this.interactionVars = other.interactionVars;
      this.interactionConfig = other.interactionConfig;
      this.tagIndexes = other.tagIndexes;
      this.pullbackConfig = other.pullbackConfig;
   }
}
