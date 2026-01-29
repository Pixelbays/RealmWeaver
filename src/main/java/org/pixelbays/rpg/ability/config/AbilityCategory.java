package org.pixelbays.rpg.ability.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.protocol.ItemGridInfoDisplayMode;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.io.NetworkSerializable;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nonnull;

public class AbilityCategory
   implements JsonAssetWithMap<String, DefaultAssetMap<String, AbilityCategory>>,
   NetworkSerializable<org.pixelbays.rpg.ability.protocol.AbilityCategory> {
   private static final AssetBuilderCodec.Builder<String, AbilityCategory> CODEC_BUILDER = AssetBuilderCodec.builder(
         AbilityCategory.class,
         AbilityCategory::new,
         Codec.STRING,
         (abilityCategory, k) -> abilityCategory.id = k,
         abilityCategory -> abilityCategory.id,
         (asset, data) -> asset.data = data,
         asset -> asset.data
      )
      .addField(new KeyedCodec<>("Id", Codec.STRING), (abilityCategory, s) -> abilityCategory.id = s, abilityCategory -> abilityCategory.id)
      .addField(new KeyedCodec<>("Name", Codec.STRING), (abilityCategory, s) -> abilityCategory.name = s, abilityCategory -> abilityCategory.name)
      .<String>append(new KeyedCodec<>("Icon", Codec.STRING), (abilityCategory, s) -> abilityCategory.icon = s, abilityCategory -> abilityCategory.icon)
      .addValidator(CommonAssetValidator.ICON_ITEM_CATEGORIES)
      .add()
      .<ItemGridInfoDisplayMode>append(
         new KeyedCodec<>("InfoDisplayMode", new EnumCodec<>(ItemGridInfoDisplayMode.class), false, true),
         (abilityCategory, s) -> abilityCategory.infoDisplayMode = s,
         abilityCategory -> abilityCategory.infoDisplayMode
      )
      .addValidator(Validators.nonNull())
      .add()
      .addField(new KeyedCodec<>("Order", Codec.INTEGER), (abilityCategory, s) -> abilityCategory.order = s, abilityCategory -> abilityCategory.order)
      .afterDecode(abilityCategory -> {
         if (abilityCategory.children != null) {
            Arrays.sort(abilityCategory.children, Comparator.comparingInt(value -> value.order));
         }
      });
   public static final AssetBuilderCodec<String, AbilityCategory> CODEC = CODEC_BUILDER.build();
   private static AssetStore<String, AbilityCategory, DefaultAssetMap<String, AbilityCategory>> ASSET_STORE;
   public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(AbilityCategory::getAssetStore));
   protected AssetExtraInfo.Data data;
   protected String id;
   protected String name;
   protected String icon;
   protected int order;
   @Nonnull
   protected ItemGridInfoDisplayMode infoDisplayMode = ItemGridInfoDisplayMode.Tooltip;
   protected AbilityCategory[] children;
   private SoftReference<org.pixelbays.rpg.ability.protocol.AbilityCategory> cachedPacket;

   public static AssetStore<String, AbilityCategory, DefaultAssetMap<String, AbilityCategory>> getAssetStore() {
      if (ASSET_STORE == null) {
         ASSET_STORE = AssetRegistry.getAssetStore(AbilityCategory.class);
      }

      return ASSET_STORE;
   }

   public static DefaultAssetMap<String, AbilityCategory> getAssetMap() {
      return (DefaultAssetMap<String, AbilityCategory>)getAssetStore().getAssetMap();
   }

   public AbilityCategory(String id, String name, String icon, ItemGridInfoDisplayMode infoDisplayMode, AbilityCategory[] children) {
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.infoDisplayMode = infoDisplayMode;
      this.children = children;
   }

   protected AbilityCategory() {
   }

   @Nonnull
   public org.pixelbays.rpg.ability.protocol.AbilityCategory toPacket() {
      org.pixelbays.rpg.ability.protocol.AbilityCategory cached = this.cachedPacket == null ? null : this.cachedPacket.get();
      if (cached != null) {
         return cached;
      } else {
         org.pixelbays.rpg.ability.protocol.AbilityCategory packet = new org.pixelbays.rpg.ability.protocol.AbilityCategory();
         packet.id = this.id;
         packet.name = this.name;
         packet.icon = this.icon;
         packet.order = this.order;
         packet.infoDisplayMode = this.infoDisplayMode;
         if (this.children != null && this.children.length > 0) {
            packet.children = ArrayUtil.copyAndMutate(this.children, AbilityCategory::toPacket, org.pixelbays.rpg.ability.protocol.AbilityCategory[]::new);
         }

         this.cachedPacket = new SoftReference<>(packet);
         return packet;
      }
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getIcon() {
      return this.icon;
   }

   public int getOrder() {
      return this.order;
   }

   @Nonnull
   public ItemGridInfoDisplayMode getInfoDisplayMode() {
      return this.infoDisplayMode;
   }

   public AbilityCategory[] getChildren() {
      return this.children;
   }

   public AssetExtraInfo.Data getData() {
      return this.data;
   }
}
