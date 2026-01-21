package org.pixelbays.rpg.global.config.validator;

import org.pixelbays.rpg.leveling.config.ExpCurveDefinition;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

public final class ExpCurveRefValidator implements Validator<String> {
    @Override
    public void accept(String key, ValidationResults results) {
        if (key == null || key.isEmpty()) {
            return;
        }

        ExpCurveDefinition def = AssetRegistry.getAssetStore(ExpCurveDefinition.class)
                .getAssetMap().getAsset(key);
        if (def == null) {
            results.fail("Asset '" + key + "' of type " + ExpCurveDefinition.class.getName()
                    + " doesn't exist!");
        }
    }

    @Override
    public void updateSchema(SchemaContext context, Schema target) {
        if (target instanceof StringSchema stringSchema) {
            stringSchema.setTitle("Exp Curve");
            stringSchema.setHytaleAssetRef(ExpCurveDefinition.class.getSimpleName());
        }
    }
}
