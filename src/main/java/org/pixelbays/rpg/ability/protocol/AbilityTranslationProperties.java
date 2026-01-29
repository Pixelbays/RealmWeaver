package org.pixelbays.rpg.ability.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbilityTranslationProperties {
   @Nullable
   public String name;
   @Nullable
   public String description;

   public AbilityTranslationProperties() {
   }

   public AbilityTranslationProperties(@Nullable String name, @Nullable String description) {
      this.name = name;
      this.description = description;
   }

   public AbilityTranslationProperties(@Nonnull AbilityTranslationProperties other) {
      this.name = other.name;
      this.description = other.description;
   }

   @Nonnull
   @Override
   public String toString() {
      return "AbilityTranslationProperties{name='" + this.name + "', description='" + this.description + "'}";
   }
}
