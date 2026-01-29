package org.pixelbays.rpg.ability.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.server.core.io.NetworkSerializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AbilityTranslationProperties implements NetworkSerializable<org.pixelbays.rpg.ability.protocol.AbilityTranslationProperties> {
   public static final BuilderCodec<AbilityTranslationProperties> CODEC = BuilderCodec.builder(AbilityTranslationProperties.class, AbilityTranslationProperties::new)
      .appendInherited(new KeyedCodec<>("Name", Codec.STRING), (data, s) -> data.name = s, data -> data.name, (o, p) -> o.name = p.name)
      .documentation("The translation key for the name of this ability.")
      .metadata(new UIEditor(new UIEditor.LocalizationKeyField("server.abilities.{assetId}.name", true)))
      .add()
      .<String>appendInherited(
         new KeyedCodec<>("Description", Codec.STRING), (data, s) -> data.description = s, data -> data.description, (o, p) -> o.description = p.description
      )
      .documentation("The translation key for the description of this ability.")
      .metadata(new UIEditor(new UIEditor.LocalizationKeyField("server.abilities.{assetId}.description")))
      .add()
      .build();
   @Nullable
   private String name;
   @Nullable
   private String description;

   AbilityTranslationProperties() {
   }

   public AbilityTranslationProperties(@Nonnull String name, @Nonnull String description) {
      this.name = name;
      this.description = description;
   }

   @Nullable
   public String getName() {
      return this.name;
   }

   @Nullable
   public String getDescription() {
      return this.description;
   }

   @Nonnull
   public org.pixelbays.rpg.ability.protocol.AbilityTranslationProperties toPacket() {
      org.pixelbays.rpg.ability.protocol.AbilityTranslationProperties packet = new org.pixelbays.rpg.ability.protocol.AbilityTranslationProperties();
      packet.name = this.name;
      packet.description = this.description;
      return packet;
   }

   @Nonnull
   @Override
   public String toString() {
      return "AbilityTranslationProperties{name=" + this.name + ", description=" + this.description + "}";
   }
}
