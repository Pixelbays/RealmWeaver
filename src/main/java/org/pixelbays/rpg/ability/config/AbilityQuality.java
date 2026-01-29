package org.pixelbays.rpg.ability.config;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.io.NetworkSerializable;
import java.lang.ref.SoftReference;
import javax.annotation.Nonnull;

public class AbilityQuality
   implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, AbilityQuality>>,
   NetworkSerializable<org.pixelbays.rpg.ability.protocol.AbilityQuality> {
   @Nonnull
   public static final AssetBuilderCodec<String, AbilityQuality> CODEC = AssetBuilderCodec.builder(
         AbilityQuality.class,
         AbilityQuality::new,
         Codec.STRING,
         (abilityQuality, s) -> abilityQuality.id = s,
         AbilityQuality::getId,
         (abilityQuality, data) -> abilityQuality.data = data,
         abilityQuality -> abilityQuality.data
      )
      .append(
         new KeyedCodec<>("QualityValue", Codec.INTEGER), (abilityQuality, integer) -> abilityQuality.qualityValue = integer, abilityQuality -> abilityQuality.qualityValue
      )
      .documentation("Define the value of the quality to order them, 0 being the lowest quality.")
      .add()
      .<String>append(
         new KeyedCodec<>("AbilityTooltipTexture", Codec.STRING),
         (abilityQuality, s) -> abilityQuality.abilityTooltipTexture = s,
         abilityQuality -> abilityQuality.abilityTooltipTexture
      )
      .documentation("The path to the texture of the ability tooltip. It has to be located in Common/Items/Qualities.")
      .addValidator(Validators.nonNull())
      .addValidator(Validators.nonEmptyString())
      .addValidator(CommonAssetValidator.TEXTURE_ITEM_QUALITY)
      .add()
      .<String>append(
         new KeyedCodec<>("AbilityTooltipArrowTexture", Codec.STRING),
         (abilityQuality, s) -> abilityQuality.abilityTooltipArrowTexture = s,
         abilityQuality -> abilityQuality.abilityTooltipArrowTexture
      )
      .documentation("The path to the texture of the ability tooltip arrow. It has to be located in Common/Items/Qualities.")
      .addValidator(Validators.nonNull())
      .addValidator(Validators.nonEmptyString())
      .addValidator(CommonAssetValidator.TEXTURE_ITEM_QUALITY)
      .add()
      .<String>append(new KeyedCodec<>("SlotTexture", Codec.STRING), (abilityQuality, s) -> abilityQuality.slotTexture = s, abilityQuality -> abilityQuality.slotTexture)
      .documentation("The path to the texture of the ability slot. It has to be located in Common/Items/Qualities.")
      .addValidator(Validators.nonNull())
      .addValidator(Validators.nonEmptyString())
      .addValidator(CommonAssetValidator.TEXTURE_ITEM_QUALITY)
      .add()
      .<Color>append(new KeyedCodec<>("TextColor", com.hypixel.hytale.server.core.codec.ProtocolCodecs.COLOR), (abilityQuality, s) -> abilityQuality.textColor = s, abilityQuality -> abilityQuality.textColor)
      .documentation("The color that'll be used to display the text of the ability in the UI.")
      .addValidator(Validators.nonNull())
      .add()
      .<String>append(
         new KeyedCodec<>("LocalizationKey", Codec.STRING), (abilityQuality, s) -> abilityQuality.localizationKey = s, abilityQuality -> abilityQuality.localizationKey
      )
      .documentation("The localization key for the ability quality name.")
      .addValidator(Validators.nonNull())
      .addValidator(Validators.nonEmptyString())
      .metadata(new UIEditor(new UIEditor.LocalizationKeyField("qualities.{assetId}", true)))
      .add()
      .<Boolean>append(
         new KeyedCodec<>("VisibleQualityLabel", Codec.BOOLEAN),
         (abilityQuality, aBoolean) -> abilityQuality.visibleQualityLabel = aBoolean,
         abilityQuality -> abilityQuality.visibleQualityLabel
      )
      .documentation("To specify the quality label should be displayed in the tooltip.")
      .add()
      .<Boolean>append(
         new KeyedCodec<>("HideFromSearch", Codec.BOOLEAN),
         (abilityQuality, aBoolean) -> abilityQuality.hideFromSearch = aBoolean,
         abilityQuality -> abilityQuality.hideFromSearch
      )
      .documentation("Whether this ability is hidden from typical public search, like the creative library")
      .add()
      .build();
   public static final int DEFAULT_INDEX = 0;
   public static final String DEFAULT_ID = "Default";
   @Nonnull
   public static final AbilityQuality DEFAULT_ABILITY_QUALITY = new AbilityQuality("Default") {
      {
         this.qualityValue = -1;
         this.abilityTooltipTexture = "UI/ItemQualities/Tooltips/ItemTooltipDefault.png";
         this.abilityTooltipArrowTexture = "UI/ItemQualities/Tooltips/ItemTooltipDefaultArrow.png";
         this.slotTexture = "UI/ItemQualities/Slots/SlotDefault.png";
         this.textColor = ColorParseUtil.hexStringToColor("#c9d2dd");
         this.localizationKey = "server.general.qualities.Default";
         this.hideFromSearch = false;
      }
   };
   @Nonnull
   public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(AbilityQuality::getAssetStore));
   private static AssetStore<String, AbilityQuality, IndexedLookupTableAssetMap<String, AbilityQuality>> ASSET_STORE;
   protected AssetExtraInfo.Data data;
   protected String id;
   protected int qualityValue;
   protected String abilityTooltipTexture;
   protected String abilityTooltipArrowTexture;
   protected String slotTexture;
   protected Color textColor;
   protected String localizationKey;
   protected boolean visibleQualityLabel;
   protected boolean hideFromSearch = false;
   private transient SoftReference<org.pixelbays.rpg.ability.protocol.AbilityQuality> cachedPacket;

   @Nonnull
   public static AssetStore<String, AbilityQuality, IndexedLookupTableAssetMap<String, AbilityQuality>> getAssetStore() {
      if (ASSET_STORE == null) {
         ASSET_STORE = AssetRegistry.getAssetStore(AbilityQuality.class);
      }

      return ASSET_STORE;
   }

   @Nonnull
   public static IndexedLookupTableAssetMap<String, AbilityQuality> getAssetMap() {
      return (IndexedLookupTableAssetMap<String, AbilityQuality>)getAssetStore().getAssetMap();
   }

   public AbilityQuality(
      String id,
      int qualityValue,
      String abilityTooltipTexture,
      String abilityTooltipArrowTexture,
      String slotTexture,
      Color textColor,
      String localizationKey,
      boolean visibleQualityLabel,
      boolean hideFromSearch
   ) {
      this.id = id;
      this.qualityValue = qualityValue;
      this.abilityTooltipTexture = abilityTooltipTexture;
      this.abilityTooltipArrowTexture = abilityTooltipArrowTexture;
      this.slotTexture = slotTexture;
      this.textColor = textColor;
      this.localizationKey = localizationKey;
      this.visibleQualityLabel = visibleQualityLabel;
      this.hideFromSearch = hideFromSearch;
   }

   public AbilityQuality(@Nonnull String id) {
      this.id = id;
   }

   protected AbilityQuality() {
   }

   public String getId() {
      return this.id;
   }

   public int getQualityValue() {
      return this.qualityValue;
   }

   public String getAbilityTooltipTexture() {
      return this.abilityTooltipTexture;
   }

   public String getAbilityTooltipArrowTexture() {
      return this.abilityTooltipArrowTexture;
   }

   public String getSlotTexture() {
      return this.slotTexture;
   }

   public Color getTextColor() {
      return this.textColor;
   }

   public String getLocalizationKey() {
      return this.localizationKey;
   }

   public boolean isVisibleQualityLabel() {
      return this.visibleQualityLabel;
   }

   public boolean isHiddenFromSearch() {
      return this.hideFromSearch;
   }

   @Nonnull
   public org.pixelbays.rpg.ability.protocol.AbilityQuality toPacket() {
      org.pixelbays.rpg.ability.protocol.AbilityQuality cached = this.cachedPacket == null ? null : this.cachedPacket.get();
      if (cached != null) {
         return cached;
      } else {
         org.pixelbays.rpg.ability.protocol.AbilityQuality packet = new org.pixelbays.rpg.ability.protocol.AbilityQuality();
         packet.id = this.id;
         packet.abilityTooltipTexture = this.abilityTooltipTexture;
         packet.abilityTooltipArrowTexture = this.abilityTooltipArrowTexture;
         packet.slotTexture = this.slotTexture;
         packet.textColor = this.textColor;
         packet.localizationKey = this.localizationKey;
         packet.visibleQualityLabel = this.visibleQualityLabel;
         packet.hideFromSearch = this.hideFromSearch;
         this.cachedPacket = new SoftReference<>(packet);
         return packet;
      }
   }

   @Nonnull
   @Override
   public String toString() {
      return "AbilityQuality{id='"
         + this.id
         + "', qualityValue="
         + this.qualityValue
         + ", abilityTooltipTexture='"
         + this.abilityTooltipTexture
         + "', abilityTooltipArrowTexture='"
         + this.abilityTooltipArrowTexture
         + "', slotTexture='"
         + this.slotTexture
         + "', textColor='"
         + this.textColor
         + "', localizationKey='"
         + this.localizationKey
         + "', visibleQualityLabel="
         + this.visibleQualityLabel
         + ", hideFromSearch="
         + this.hideFromSearch
         + "}";
   }

   public AssetExtraInfo.Data getData() {
      return this.data;
   }
}
