package org.pixelbays.rpg.global.config.validator;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.codec.validation.ValidatableCodec;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;

public final class InteractionChainRefCodec implements ValidatableCodec<String> {
    @Nonnull
    @SuppressWarnings("null")
    private AssetCodec<String, RootInteraction> getAssetCodec() {
        return AssetRegistry.getAssetStore(RootInteraction.class).getCodec();
    }

    @Override
    public String decode(org.bson.BsonValue bsonValue, ExtraInfo extraInfo) {
        return getAssetCodec().getKeyCodec().getChildCodec().decode(bsonValue, extraInfo);
    }

    @Override
    public org.bson.BsonValue encode(String value, ExtraInfo extraInfo) {
        return getAssetCodec().getKeyCodec().getChildCodec().encode(value, extraInfo);
    }

    @Override
    @Nullable
    public String decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
        return getAssetCodec().getKeyCodec().getChildCodec().decodeJson(reader, extraInfo);
    }

    @Override
    @Nonnull
    public Schema toSchema(@Nonnull SchemaContext context) {
        StringSchema schema = new StringSchema();
        schema.setTitle("Interaction Chain");
        schema.setHytaleAssetRef(RootInteraction.class.getSimpleName());
        return schema;
    }

    @Override
    public void validate(String value, ExtraInfo extraInfo) {
        ValidationResults results = extraInfo.getValidationResults();
        if (results != null) {
            RootInteraction def = AssetRegistry.getAssetStore(RootInteraction.class).getAssetMap().getAsset(value);
            if (def == null) {
                results.fail("Unknown interaction chain id: " + value);
            }
        }
    }

    @Override
    public void validateDefaults(ExtraInfo extraInfo, java.util.Set<Codec<?>> tested) {
        if (tested.add(this)) {
            // No default validation needed
        }
    }
}