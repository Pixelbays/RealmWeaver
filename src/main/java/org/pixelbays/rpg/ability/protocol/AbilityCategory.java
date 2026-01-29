package org.pixelbays.rpg.ability.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.protocol.ItemGridInfoDisplayMode;

public class AbilityCategory {
   @Nullable
   public String id;
   @Nullable
   public String name;
   @Nullable
   public String icon;
   public int order;
   @Nonnull
   public ItemGridInfoDisplayMode infoDisplayMode = ItemGridInfoDisplayMode.Tooltip;
   @Nullable
   public AbilityCategory[] children;

   public AbilityCategory() {
   }

   public AbilityCategory(
      @Nullable String id,
      @Nullable String name,
      @Nullable String icon,
      int order,
      @Nonnull ItemGridInfoDisplayMode infoDisplayMode,
      @Nullable AbilityCategory[] children
   ) {
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.order = order;
      this.infoDisplayMode = infoDisplayMode;
      this.children = children;
   }

   public AbilityCategory(@Nonnull AbilityCategory other) {
      this.id = other.id;
      this.name = other.name;
      this.icon = other.icon;
      this.order = other.order;
      this.infoDisplayMode = other.infoDisplayMode;
      this.children = other.children;
   }
}
