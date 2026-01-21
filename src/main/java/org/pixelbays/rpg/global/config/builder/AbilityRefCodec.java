package org.pixelbays.rpg.global.config.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.rpg.ability.config.ClassAbilityDefinition;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.codec.AssetCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.codec.validation.ValidatableCodec;
import com.hypixel.hytale.codec.validation.ValidationResults;

@SuppressWarnings("deprecation")
public final class AbilityRefCodec implements ValidatableCodec<String> {
    public AbilityRefCodec() {
    }

    private static final AbilityRefStringCodec DELEGATE = new AbilityRefStringCodec();

    public static final FunctionCodec<String[], List<String>> CODEC = new FunctionCodec<>(
            new ArrayCodec<>(DELEGATE, String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));

    @Override
    public String decode(org.bson.BsonValue bsonValue, ExtraInfo extraInfo) {
        return DELEGATE.decode(bsonValue, extraInfo);
    }

    @Override
    public org.bson.BsonValue encode(String value, ExtraInfo extraInfo) {
        return DELEGATE.encode(value, extraInfo);
    }

    @Override
    @Nullable
    public String decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
        return DELEGATE.decodeJson(reader, extraInfo);
    }

    @Override
    @Nonnull
    public Schema toSchema(@Nonnull SchemaContext context) {
        return DELEGATE.toSchema(context);
    }

    @Override
    public void validate(String value, ExtraInfo extraInfo) {
        DELEGATE.validate(value, extraInfo);
    }

    @Override
    public void validateDefaults(ExtraInfo extraInfo, java.util.Set<Codec<?>> tested) {
        DELEGATE.validateDefaults(extraInfo, tested);
    }

    private static final class AbilityRefStringCodec implements ValidatableCodec<String> {
        @Nonnull
        @SuppressWarnings("null")
        private AssetCodec<String, ClassAbilityDefinition> getAssetCodec() {
            return AssetRegistry.getAssetStore(ClassAbilityDefinition.class).getCodec();
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
            schema.setTitle("Ability");
            schema.setHytaleAssetRef(ClassAbilityDefinition.class.getSimpleName());
            return schema;
        }

        @Override
        public void validate(String value, ExtraInfo extraInfo) {
            ValidationResults results = extraInfo.getValidationResults();
            if (results != null) {
                ClassAbilityDefinition.VALIDATOR_CACHE.getValidator().accept(value, results);
            }
        }

        @Override
        public void validateDefaults(ExtraInfo extraInfo, java.util.Set<Codec<?>> tested) {
            if (tested.add(this)) {
                // No default validation needed
            }
        }
    }
}

