package org.pixelbays.rpg.character.appearance;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.pixelbays.rpg.character.appearance.CharacterAppearanceCatalog.Category;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;
import com.hypixel.hytale.server.core.universe.Universe;

public final class RuntimeCosmeticPreviewGenerator {

    private static final String TARGET_PACK_NAME = "RealmweaverCore";
    private static final String GENERATED_COSMETIC_PREVIEW_ROOT = "Icons/ItemsGenerated/CosmeticsGenerated/";
    private static final int ICON_SIZE = 96;
    private static final int MODEL_INSET = 11;
    private static final int FALLBACK_INSET = 12;
    private static final double VIEW_YAW_RADIANS = Math.toRadians(-32d);
    private static final double VIEW_PITCH_RADIANS = Math.toRadians(18d);
    private static final Vector3 LIGHT_DIRECTION = normalizeVector3(new Vector3(0.45d, 0.8d, 0.55d));
    private static final Color TRANSLUCENT_MODEL_BASE_COLOR = new Color(224, 220, 214, 176);

    private final Map<String, BufferedImage> sourceImageCache = new HashMap<>();
    private final Map<String, BufferedImage> preparedTextureCache = new HashMap<>();
    private final Map<String, List<FaceSpec>> parsedModelCache = new HashMap<>();

    public void generateMissingPreviews() {
        CommonAssetModule commonAssetModule = CommonAssetModule.get();
        if (commonAssetModule == null) {
            RpgLogging.warn("[CosmeticPreviewRuntime] CommonAssetModule is unavailable; skipping runtime preview generation.");
            return;
        }

        AssetPack targetPack = resolveTargetPack();
        if (targetPack == null) {
            RpgLogging.warn("[CosmeticPreviewRuntime] Unable to find target asset pack %s; skipping runtime preview generation.", TARGET_PACK_NAME);
            return;
        }

        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        if (cosmeticsModule == null || cosmeticsModule.getRegistry() == null) {
            RpgLogging.warn("[CosmeticPreviewRuntime] CosmeticsModule registry is unavailable; skipping runtime preview generation.");
            return;
        }

        if (targetPack.isImmutable()) {
            RpgLogging.debugDeveloper(
                    "[CosmeticPreviewRuntime] Target pack %s is flagged immutable; attempting best-effort write to %s.",
                    targetPack.getName(),
                    targetPack.getRoot());
        }

        Path commonRoot = targetPack.getRoot().resolve("Common");
        List<PreviewSelectionSpec> selectionSpecs = collectSelectionSpecs(cosmeticsModule.getRegistry());
        int generatedCount = 0;
        int existingCount = 0;
        int missingCount = 0;
        int registeredCount = 0;

        for (PreviewSelectionSpec spec : selectionSpecs) {
            Path outputFile = commonRoot.resolve(spec.outputAssetPath());
            if (CommonAssetRegistry.hasCommonAsset(targetPack, spec.outputAssetPath())) {
                existingCount++;
                continue;
            }

            if (Files.isRegularFile(outputFile)) {
                try {
                    byte[] existingBytes = Files.readAllBytes(outputFile);
                    commonAssetModule.addCommonAsset(
                            targetPack.getName(),
                            new FileCommonAsset(outputFile, spec.outputAssetPath(), existingBytes),
                            false);
                    existingCount++;
                    registeredCount++;
                    continue;
                } catch (IOException ex) {
                    RpgLogging.error(ex,
                            "[CosmeticPreviewRuntime] Failed to register existing preview %s from %s",
                            spec.outputAssetPath(),
                            outputFile);
                }
            }

            CommonAsset sourceTextureAsset = CommonAssetRegistry.getByName(spec.sourceTexturePath());
            if (sourceTextureAsset == null) {
                missingCount++;
                continue;
            }

            BufferedImage preparedTexture = getPreparedTexture(sourceTextureAsset, spec.tintColor());
            if (preparedTexture == null) {
                missingCount++;
                continue;
            }

            BufferedImage previewSubject = null;
            if (!spec.modelPath().isBlank()) {
                CommonAsset modelAsset = CommonAssetRegistry.getByName(spec.modelPath());
                if (modelAsset != null) {
                    List<FaceSpec> modelFaces = parseBlockyModel(modelAsset);
                    if (!modelFaces.isEmpty()) {
                        previewSubject = renderModelPreviewIcon(modelFaces, preparedTexture);
                    }
                }
            }
            if (previewSubject == null) {
                previewSubject = renderPreviewIcon(preparedTexture);
            }

            try {
                BufferedImage preview = composeSubjectIcon(previewSubject);
                byte[] bytes = encodePng(preview);
                Files.createDirectories(outputFile.getParent());
                Files.write(outputFile, bytes);
                commonAssetModule.addCommonAsset(
                        targetPack.getName(),
                        new FileCommonAsset(outputFile, spec.outputAssetPath(), bytes),
                        false);
                generatedCount++;
                registeredCount++;
            } catch (IOException ex) {
                missingCount++;
                RpgLogging.error(ex,
                        "[CosmeticPreviewRuntime] Failed to write preview %s to %s",
                        spec.outputAssetPath(),
                        outputFile);
            }
        }

        if (registeredCount > 0) {
            Universe.get().broadcastPacketNoCache(new RequestCommonAssetsRebuild());
        }

        RpgLogging.info(
                "[CosmeticPreviewRuntime] pack=%s specs=%s generated=%s existing=%s missing=%s",
                targetPack.getName(),
                selectionSpecs.size(),
                generatedCount,
                existingCount,
                missingCount);
    }

    @Nullable
    private AssetPack resolveTargetPack() {
        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            return null;
        }

        for (AssetPack pack : assetModule.getAssetPacks()) {
            String manifestName = pack.getManifest() == null ? "" : safeString(pack.getManifest().getName());
            String packName = safeString(pack.getName());
            if (TARGET_PACK_NAME.equalsIgnoreCase(manifestName)
                    || TARGET_PACK_NAME.equalsIgnoreCase(packName)
                    || packName.endsWith(":" + TARGET_PACK_NAME)) {
                return pack;
            }
        }
        return null;
    }

    @Nonnull
    private List<PreviewSelectionSpec> collectSelectionSpecs(@Nonnull CosmeticRegistry registry) {
        LinkedHashMap<String, PreviewSelectionSpec> specsByOutput = new LinkedHashMap<>();
        for (Category category : CharacterAppearanceCatalog.getOrderedCategories()) {
            Map<String, PlayerSkinPart> parts = category.getParts(registry);
            if (parts == null || parts.isEmpty()) {
                continue;
            }

            String categoryPathId = CharacterAppearanceCatalog.normalizeKey(category.fieldName());
            for (PlayerSkinPart part : parts.values()) {
                if (part == null) {
                    continue;
                }

                if (part.getVariants() != null && !part.getVariants().isEmpty()) {
                    for (Map.Entry<String, PlayerSkinPart.Variant> variantEntry : part.getVariants().entrySet()) {
                        PlayerSkinPart.Variant variant = variantEntry.getValue();
                        if (variant == null) {
                            continue;
                        }

                        String variantId = safeString(variantEntry.getKey()).trim();
                        String modelPath = firstNonBlank(variant.getModel(), part.getModel());
                        Map<String, PlayerSkinPartTexture> textures = variant.getTextures();
                        if (textures != null && !textures.isEmpty()) {
                            addTextureSelections(specsByOutput, categoryPathId, part.getId(), variantId, modelPath, textures);
                        }
                        addGradientSelections(
                                specsByOutput,
                                registry,
                                categoryPathId,
                                part.getId(),
                                variantId,
                                modelPath,
                                variant.getGreyscaleTexture(),
                                part.getGradientSet());
                    }
                    continue;
                }

                Map<String, PlayerSkinPartTexture> textures = part.getTextures();
                if (textures != null && !textures.isEmpty()) {
                    addTextureSelections(specsByOutput, categoryPathId, part.getId(), "", part.getModel(), textures);
                }
                addGradientSelections(
                        specsByOutput,
                        registry,
                        categoryPathId,
                        part.getId(),
                        "",
                        part.getModel(),
                        part.getGreyscaleTexture(),
                        part.getGradientSet());
            }
        }
        return List.copyOf(specsByOutput.values());
    }

    private void addTextureSelections(
            @Nonnull Map<String, PreviewSelectionSpec> specsByOutput,
            @Nonnull String categoryPathId,
            @Nonnull String assetId,
            @Nonnull String variantId,
            @Nullable String modelPath,
            @Nonnull Map<String, PlayerSkinPartTexture> textures) {
        for (Map.Entry<String, PlayerSkinPartTexture> textureEntry : textures.entrySet()) {
            String textureId = safeString(textureEntry.getKey()).trim();
            PlayerSkinPartTexture texture = textureEntry.getValue();
            if (textureId.isBlank() || texture == null) {
                continue;
            }

            addSelection(
                    specsByOutput,
                    categoryPathId,
                    assetId,
                    textureId,
                    variantId,
                    texture.getTexture(),
                    modelPath,
                    null);
        }
    }

    private void addGradientSelections(
            @Nonnull Map<String, PreviewSelectionSpec> specsByOutput,
            @Nonnull CosmeticRegistry registry,
            @Nonnull String categoryPathId,
            @Nonnull String assetId,
            @Nonnull String variantId,
            @Nullable String modelPath,
            @Nullable String greyscaleTexturePath,
            @Nullable String gradientSetId) {
        String normalizedGreyscalePath = normalizeCommonAssetPath(greyscaleTexturePath);
        if (normalizedGreyscalePath.isBlank()) {
            return;
        }

        PlayerSkinGradientSet gradientSet = registry.getGradientSets().get(safeString(gradientSetId));
        if (gradientSet == null || gradientSet.getGradients() == null || gradientSet.getGradients().isEmpty()) {
            return;
        }

        for (Map.Entry<String, PlayerSkinPartTexture> gradientEntry : gradientSet.getGradients().entrySet()) {
            String textureId = safeString(gradientEntry.getKey()).trim();
            PlayerSkinPartTexture gradientTexture = gradientEntry.getValue();
            if (textureId.isBlank()) {
                continue;
            }

            addSelection(
                    specsByOutput,
                    categoryPathId,
                    assetId,
                    textureId,
                    variantId,
                    normalizedGreyscalePath,
                    modelPath,
                    parseBaseColor(gradientTexture == null ? null : gradientTexture.getBaseColor()));
        }
    }

    private void addSelection(
            @Nonnull Map<String, PreviewSelectionSpec> specsByOutput,
            @Nonnull String categoryPathId,
            @Nonnull String assetId,
            @Nonnull String textureId,
            @Nonnull String variantId,
            @Nullable String texturePath,
            @Nullable String modelPath,
            @Nullable Color tintColor) {
        String normalizedTexturePath = normalizeCommonAssetPath(texturePath);
        if (normalizedTexturePath.isBlank()) {
            return;
        }

        String optionId = CharacterAppearanceCatalog.buildOptionId(assetId, textureId, variantId);
        String outputAssetPath = GENERATED_COSMETIC_PREVIEW_ROOT
                + categoryPathId
                + "/"
                + sanitizeOptionToken(optionId)
                + ".png";
        specsByOutput.put(outputAssetPath, new PreviewSelectionSpec(
                outputAssetPath,
                normalizedTexturePath,
                normalizeCommonAssetPath(modelPath),
                tintColor));
    }

    @Nullable
    private BufferedImage getPreparedTexture(@Nonnull CommonAsset sourceTextureAsset, @Nullable Color tintColor) {
        String preparedKey = sourceTextureAsset.getHash() + "|" + (tintColor == null ? "none" : Integer.toHexString(tintColor.getRGB()));
        BufferedImage cachedPrepared = preparedTextureCache.get(preparedKey);
        if (cachedPrepared != null) {
            return cachedPrepared;
        }

        BufferedImage sourceImage = getSourceImage(sourceTextureAsset);
        if (sourceImage == null) {
            return null;
        }

        BufferedImage prepared = tintColor == null ? sourceImage : tintGreyscaleImage(sourceImage, tintColor);
        preparedTextureCache.put(preparedKey, prepared);
        return prepared;
    }

    @Nullable
    private BufferedImage getSourceImage(@Nonnull CommonAsset asset) {
        BufferedImage cachedImage = sourceImageCache.get(asset.getHash());
        if (cachedImage != null) {
            return cachedImage;
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(asset.getBlob().join())) {
            BufferedImage image = ImageIO.read(input);
            if (image != null) {
                sourceImageCache.put(asset.getHash(), image);
            }
            return image;
        } catch (RuntimeException | IOException ex) {
            RpgLogging.error(ex, "[CosmeticPreviewRuntime] Failed to read common asset image %s", asset.getName());
            return null;
        }
    }

    @Nonnull
    private List<FaceSpec> parseBlockyModel(@Nonnull CommonAsset modelAsset) {
        List<FaceSpec> cachedFaces = parsedModelCache.get(modelAsset.getHash());
        if (cachedFaces != null) {
            return cachedFaces;
        }

        List<FaceSpec> faces = new ArrayList<>();
        try {
            String json = new String(modelAsset.getBlob().join(), StandardCharsets.UTF_8);
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            JsonArray rootNodes = getArray(parsed, "nodes");
            double[][] identity = identityMatrix3();
            for (JsonElement rawNode : rootNodes) {
                if (rawNode != null && rawNode.isJsonObject()) {
                    visitNode(rawNode.getAsJsonObject(), identity, new Vector3(0d, 0d, 0d), faces);
                }
            }
        } catch (RuntimeException ex) {
            RpgLogging.error(ex, "[CosmeticPreviewRuntime] Failed to parse blockymodel %s", modelAsset.getName());
        }

        List<FaceSpec> parsedFaces = List.copyOf(faces);
        parsedModelCache.put(modelAsset.getHash(), parsedFaces);
        return parsedFaces;
    }

    private void visitNode(
            @Nonnull JsonObject node,
            @Nonnull double[][] parentBasis,
            @Nonnull Vector3 parentOrigin,
            @Nonnull List<FaceSpec> faces) {
        JsonObject shape = getObject(node, "shape");
        Vector3 position = readVector3(node.get("position"), 0d);
        Quaternion quaternion = readQuaternion(node.get("orientation"));
        Vector3 stretch = readVector3(shape.get("stretch"), 1d);
        double[][] rotationMatrix = quaternionToMatrix3(quaternion);
        double[][] localBasis = multiplyMatrix3(rotationMatrix, scaleMatrix3(stretch));
        double[][] worldBasis = multiplyMatrix3(parentBasis, localBasis);
        Vector3 worldOrigin = addVector3(parentOrigin, transformVector3(parentBasis, position));

        String shapeType = getString(shape.get("type"));
        boolean visible = !shape.has("visible") || getBoolean(shape.get("visible"));
        if (visible && "box".equalsIgnoreCase(shapeType)) {
            Vector3 offset = readVector3(shape.get("offset"), 0d);
            JsonObject settings = getObject(shape, "settings");
            Vector3 size = readVector3(settings.get("size"), 0d);
            JsonObject textureLayout = getObject(shape, "textureLayout");

            double minX = offset.x();
            double maxX = offset.x() + size.x();
            double minY = offset.y();
            double maxY = offset.y() + size.y();
            double minZ = offset.z();
            double maxZ = offset.z() + size.z();

            int widthX = Math.max(1, (int) Math.round(Math.abs(size.x())));
            int heightY = Math.max(1, (int) Math.round(Math.abs(size.y())));
            int depthZ = Math.max(1, (int) Math.round(Math.abs(size.z())));

                addFace(faces, buildFaceSpec("front", getOptionalObject(textureLayout, "front"), widthX, heightY, List.of(
                    toWorld(worldOrigin, worldBasis, minX, maxY, maxZ),
                    toWorld(worldOrigin, worldBasis, maxX, maxY, maxZ),
                    toWorld(worldOrigin, worldBasis, maxX, minY, maxZ),
                    toWorld(worldOrigin, worldBasis, minX, minY, maxZ))));
                addFace(faces, buildFaceSpec("back", getOptionalObject(textureLayout, "back"), widthX, heightY, List.of(
                    toWorld(worldOrigin, worldBasis, maxX, maxY, minZ),
                    toWorld(worldOrigin, worldBasis, minX, maxY, minZ),
                    toWorld(worldOrigin, worldBasis, minX, minY, minZ),
                    toWorld(worldOrigin, worldBasis, maxX, minY, minZ))));
                addFace(faces, buildFaceSpec("left", getOptionalObject(textureLayout, "left"), depthZ, heightY, List.of(
                    toWorld(worldOrigin, worldBasis, minX, maxY, minZ),
                    toWorld(worldOrigin, worldBasis, minX, maxY, maxZ),
                    toWorld(worldOrigin, worldBasis, minX, minY, maxZ),
                    toWorld(worldOrigin, worldBasis, minX, minY, minZ))));
                addFace(faces, buildFaceSpec("right", getOptionalObject(textureLayout, "right"), depthZ, heightY, List.of(
                    toWorld(worldOrigin, worldBasis, maxX, maxY, maxZ),
                    toWorld(worldOrigin, worldBasis, maxX, maxY, minZ),
                    toWorld(worldOrigin, worldBasis, maxX, minY, minZ),
                    toWorld(worldOrigin, worldBasis, maxX, minY, maxZ))));
                addFace(faces, buildFaceSpec("top", getOptionalObject(textureLayout, "top"), widthX, depthZ, List.of(
                    toWorld(worldOrigin, worldBasis, minX, maxY, minZ),
                    toWorld(worldOrigin, worldBasis, maxX, maxY, minZ),
                    toWorld(worldOrigin, worldBasis, maxX, maxY, maxZ),
                    toWorld(worldOrigin, worldBasis, minX, maxY, maxZ))));
                addFace(faces, buildFaceSpec("bottom", getOptionalObject(textureLayout, "bottom"), widthX, depthZ, List.of(
                    toWorld(worldOrigin, worldBasis, minX, minY, maxZ),
                    toWorld(worldOrigin, worldBasis, maxX, minY, maxZ),
                    toWorld(worldOrigin, worldBasis, maxX, minY, minZ),
                    toWorld(worldOrigin, worldBasis, minX, minY, minZ))));
        }

        JsonArray children = getArray(node, "children");
        for (JsonElement rawChild : children) {
            if (rawChild != null && rawChild.isJsonObject()) {
                visitNode(rawChild.getAsJsonObject(), worldBasis, worldOrigin, faces);
            }
        }
    }

    @Nullable
    private FaceSpec buildFaceSpec(
            @Nonnull String faceName,
            @Nullable JsonObject faceLayout,
            int faceWidth,
            int faceHeight,
            @Nonnull List<Vector3> vertices) {
        if (faceLayout == null || faceWidth <= 0 || faceHeight <= 0 || vertices.size() != 4) {
            return null;
        }
        return new FaceSpec(faceName, faceLayout, faceWidth, faceHeight, vertices);
    }

    private void addFace(@Nonnull List<FaceSpec> faces, @Nullable FaceSpec faceSpec) {
        if (faceSpec != null) {
            faces.add(faceSpec);
        }
    }

    @Nullable
    private BufferedImage renderModelPreviewIcon(
            @Nonnull List<FaceSpec> modelFaces,
            @Nonnull BufferedImage preparedTexture) {
        List<VisibleFace> visibleFaces = new ArrayList<>();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (FaceSpec faceSpec : modelFaces) {
            BufferedImage faceTexture = extractFaceTexture(
                    preparedTexture,
                    faceSpec.faceLayout(),
                    faceSpec.faceWidth(),
                    faceSpec.faceHeight());
            if (faceTexture == null) {
                continue;
            }

            List<Vector3> cameraVertices = new ArrayList<>(faceSpec.vertices().size());
            for (Vector3 vertex : faceSpec.vertices()) {
                Vector3 cameraVertex = applyViewTransform(vertex);
                cameraVertices.add(cameraVertex);
                minX = Math.min(minX, cameraVertex.x());
                minY = Math.min(minY, cameraVertex.y());
                maxX = Math.max(maxX, cameraVertex.x());
                maxY = Math.max(maxY, cameraVertex.y());
            }

            Vector3 topLeft = cameraVertices.get(0);
            Vector3 topRight = cameraVertices.get(1);
            Vector3 bottomLeft = cameraVertices.get(3);
            Vector3 normal = crossVector3(subtractVector3(bottomLeft, topLeft), subtractVector3(topRight, topLeft));
            if (Math.abs(normal.z()) <= 0.0001d) {
                continue;
            }

            double depth = cameraVertices.stream().mapToDouble(Vector3::z).average().orElse(0d);
            visibleFaces.add(new VisibleFace(faceTexture, cameraVertices, normal, depth));
        }

        if (visibleFaces.isEmpty()
                || !Double.isFinite(minX)
                || !Double.isFinite(minY)
                || !Double.isFinite(maxX)
                || !Double.isFinite(maxY)) {
            return null;
        }

        double width = Math.max(0.0001d, maxX - minX);
        double height = Math.max(0.0001d, maxY - minY);
        double scale = Math.min((ICON_SIZE - (MODEL_INSET * 2d)) / width, (ICON_SIZE - (MODEL_INSET * 2d)) / height);
        scale = Math.max(scale, 0.01d);
        double offsetX = ((ICON_SIZE - (width * scale)) / 2d) - (minX * scale);
        double offsetY = ((ICON_SIZE - (height * scale)) / 2d) + (maxY * scale) + 2d;

        BufferedImage subject = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = subject.createGraphics();
        configureGraphics(graphics);

        visibleFaces.sort(Comparator.comparingDouble(VisibleFace::depth));
        for (VisibleFace face : visibleFaces) {
            List<Vector2> screenVertices = new ArrayList<>(face.cameraVertices().size());
            for (Vector3 point : face.cameraVertices()) {
                screenVertices.add(projectToScreen(point, scale, offsetX, offsetY));
            }
            Vector2 topLeft = screenVertices.get(0);
            Vector2 topRight = screenVertices.get(1);
            Vector2 bottomRight = screenVertices.get(2);
            Vector2 bottomLeft = screenVertices.get(3);
            BufferedImage faceTexture = face.faceTexture();

            Path2D path = new Path2D.Double();
            path.moveTo(topLeft.x(), topLeft.y());
            path.lineTo(topRight.x(), topRight.y());
            path.lineTo(bottomRight.x(), bottomRight.y());
            path.lineTo(bottomLeft.x(), bottomLeft.y());
            path.closePath();

            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(TRANSLUCENT_MODEL_BASE_COLOR);
            graphics.fill(path);

            AffineTransform transform = new AffineTransform(
                    (topRight.x() - topLeft.x()) / faceTexture.getWidth(),
                    (topRight.y() - topLeft.y()) / faceTexture.getWidth(),
                    (bottomLeft.x() - topLeft.x()) / faceTexture.getHeight(),
                    (bottomLeft.y() - topLeft.y()) / faceTexture.getHeight(),
                    topLeft.x(),
                    topLeft.y());
            graphics.drawImage(faceTexture, transform, null);

            Vector3 normalizedNormal = normalizeVector3(face.normal());
            double brightness = 0.62d + (0.48d * Math.max(0d, dotVector3(normalizedNormal, LIGHT_DIRECTION)));
            brightness = clamp(brightness, 0.38d, 1.18d);
            if (Math.abs(brightness - 1d) > 0.01d) {
                if (brightness < 1d) {
                    graphics.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(0.55d, 1d - brightness)));
                    graphics.setColor(Color.BLACK);
                } else {
                    graphics.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(0.22d, brightness - 1d)));
                    graphics.setColor(Color.WHITE);
                }
                graphics.fill(path);
                graphics.setComposite(AlphaComposite.SrcOver);
            }
        }

        graphics.dispose();
        return subject;
    }

    @Nonnull
    private BufferedImage renderPreviewIcon(@Nonnull BufferedImage preparedTexture) {
        int[] bounds = opaqueBounds(preparedTexture);
        BufferedImage subject = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        if (bounds == null) {
            return subject;
        }

        int cropX = bounds[0];
        int cropY = bounds[1];
        int cropWidth = bounds[2] - cropX + 1;
        int cropHeight = bounds[3] - cropY + 1;
        BufferedImage cropped = preparedTexture.getSubimage(cropX, cropY, cropWidth, cropHeight);

        Graphics2D graphics = subject.createGraphics();
        configureGraphics(graphics);

        double scale = Math.min(
                (ICON_SIZE - (FALLBACK_INSET * 2d)) / cropWidth,
                (ICON_SIZE - (FALLBACK_INSET * 2d)) / cropHeight);
        scale = Math.max(scale, 0.01d);
        int drawWidth = Math.max(1, (int) Math.round(cropWidth * scale));
        int drawHeight = Math.max(1, (int) Math.round(cropHeight * scale));
        int drawX = Math.max(0, (ICON_SIZE - drawWidth) / 2);
        int drawY = Math.max(0, (ICON_SIZE - drawHeight) / 2);

        graphics.drawImage(cropped, drawX, drawY, drawWidth, drawHeight, null);
        graphics.dispose();
        return subject;
    }

    @Nonnull
    private BufferedImage composeSubjectIcon(@Nullable BufferedImage subject) {
        BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        if (subject == null) {
            return icon;
        }

        Graphics2D graphics = icon.createGraphics();
        configureGraphics(graphics);

        BufferedImage shadow = createShadowMask(subject);
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.92f));
        graphics.drawImage(shadow, 3, 4, null);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(subject, 0, 0, null);
        graphics.dispose();
        return icon;
    }

    @Nonnull
    private BufferedImage createShadowMask(@Nonnull BufferedImage source) {
        BufferedImage shadow = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    shadow.setRGB(x, y, 0);
                    continue;
                }
                int shadowAlpha = clamp((int) Math.round(alpha * 0.36d), 0, 255);
                shadow.setRGB(x, y, shadowAlpha << 24);
            }
        }
        return shadow;
    }

    @Nullable
    private BufferedImage extractFaceTexture(
            @Nonnull BufferedImage sourceTexture,
            @Nonnull JsonObject faceLayout,
            int faceWidth,
            int faceHeight) {
        if (faceWidth <= 0 || faceHeight <= 0) {
            return null;
        }

        Vector2 offset = readVector2(faceLayout.get("offset"), 0d);
        JsonObject mirror = getObject(faceLayout, "mirror");
        boolean mirrorX = mirror.has("x") && getBoolean(mirror.get("x"));
        boolean mirrorY = mirror.has("y") && getBoolean(mirror.get("y"));
        int angle = normalizeAngle(faceLayout.get("angle"));

        int sampleWidth = angle == 90 || angle == 270 ? faceHeight : faceWidth;
        int sampleHeight = angle == 90 || angle == 270 ? faceWidth : faceHeight;
        BufferedImage sampled = copyTextureRegion(
                sourceTexture,
                (int) Math.round(offset.x()),
                (int) Math.round(offset.y()),
                sampleWidth,
                sampleHeight);
        if (sampled == null) {
            return null;
        }

        BufferedImage result = new BufferedImage(faceWidth, faceHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < faceHeight; y++) {
            double baseV = (y + 0.5d) / faceHeight;
            for (int x = 0; x < faceWidth; x++) {
                double baseU = (x + 0.5d) / faceWidth;
                double u = mirrorX ? 1d - baseU : baseU;
                double v = mirrorY ? 1d - baseV : baseV;

                double sampleU;
                double sampleV;
                switch (angle) {
                    case 90 -> {
                        sampleU = v;
                        sampleV = 1d - u;
                    }
                    case 180 -> {
                        sampleU = 1d - u;
                        sampleV = 1d - v;
                    }
                    case 270 -> {
                        sampleU = 1d - v;
                        sampleV = u;
                    }
                    default -> {
                        sampleU = u;
                        sampleV = v;
                    }
                }

                int sampleX = clamp((int) Math.floor(sampleU * sampled.getWidth()), 0, sampled.getWidth() - 1);
                int sampleY = clamp((int) Math.floor(sampleV * sampled.getHeight()), 0, sampled.getHeight() - 1);
                result.setRGB(x, y, sampled.getRGB(sampleX, sampleY));
            }
        }
        return result;
    }

    @Nullable
    private BufferedImage copyTextureRegion(
            @Nonnull BufferedImage source,
            int sourceX,
            int sourceY,
            int width,
            int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage region = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int readX = sourceX + x;
                int readY = sourceY + y;
                if (readX < 0 || readY < 0 || readX >= source.getWidth() || readY >= source.getHeight()) {
                    continue;
                }
                region.setRGB(x, y, source.getRGB(readX, readY));
            }
        }
        return region;
    }

    @Nullable
    private int[] opaqueBounds(@Nonnull BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha < 12) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX >= 0 ? new int[] { minX, minY, maxX, maxY } : null;
    }

    @Nonnull
    private BufferedImage tintGreyscaleImage(@Nonnull BufferedImage source, @Nonnull Color tintColor) {
        BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    tinted.setRGB(x, y, 0);
                    continue;
                }
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                int luminance = (int) Math.round((red * 0.2126d) + (green * 0.7152d) + (blue * 0.0722d));
                int tintedRed = clamp((int) Math.round((tintColor.getRed() * luminance) / 255d), 0, 255);
                int tintedGreen = clamp((int) Math.round((tintColor.getGreen() * luminance) / 255d), 0, 255);
                int tintedBlue = clamp((int) Math.round((tintColor.getBlue() * luminance) / 255d), 0, 255);
                tinted.setRGB(x, y, (alpha << 24) | (tintedRed << 16) | (tintedGreen << 8) | tintedBlue);
            }
        }
        return tinted;
    }

    @Nonnull
    private byte[] encodePng(@Nonnull BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static void configureGraphics(@Nonnull Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Nonnull
    private static Vector3 toWorld(
            @Nonnull Vector3 worldOrigin,
            @Nonnull double[][] worldBasis,
            double x,
            double y,
            double z) {
        return addVector3(worldOrigin, transformVector3(worldBasis, new Vector3(x, y, z)));
    }

    @Nonnull
    private static Vector3 applyViewTransform(@Nonnull Vector3 point) {
        double yawCos = Math.cos(VIEW_YAW_RADIANS);
        double yawSin = Math.sin(VIEW_YAW_RADIANS);
        double rotatedX = (point.x() * yawCos) - (point.z() * yawSin);
        double rotatedZ = (point.x() * yawSin) + (point.z() * yawCos);
        double rotatedY = point.y();

        double pitchCos = Math.cos(VIEW_PITCH_RADIANS);
        double pitchSin = Math.sin(VIEW_PITCH_RADIANS);
        double pitchedY = (rotatedY * pitchCos) - (rotatedZ * pitchSin);
        double pitchedZ = (rotatedY * pitchSin) + (rotatedZ * pitchCos);

        return new Vector3(rotatedX, pitchedY, pitchedZ);
    }

    @Nonnull
    private static Vector2 projectToScreen(@Nonnull Vector3 point, double scale, double offsetX, double offsetY) {
        return new Vector2((point.x() * scale) + offsetX, (-point.y() * scale) + offsetY);
    }

    @Nonnull
    private static Vector3 readVector3(@Nullable JsonElement rawValue, double defaultValue) {
        JsonObject rawMap = rawValue != null && rawValue.isJsonObject() ? rawValue.getAsJsonObject() : null;
        return new Vector3(
                readDoubleValue(rawMap == null ? null : rawMap.get("x"), defaultValue),
                readDoubleValue(rawMap == null ? null : rawMap.get("y"), defaultValue),
                readDoubleValue(rawMap == null ? null : rawMap.get("z"), defaultValue));
    }

    @Nonnull
    private static Vector2 readVector2(@Nullable JsonElement rawValue, double defaultValue) {
        JsonObject rawMap = rawValue != null && rawValue.isJsonObject() ? rawValue.getAsJsonObject() : null;
        return new Vector2(
                readDoubleValue(rawMap == null ? null : rawMap.get("x"), defaultValue),
                readDoubleValue(rawMap == null ? null : rawMap.get("y"), defaultValue));
    }

    @Nonnull
    private static Quaternion readQuaternion(@Nullable JsonElement rawValue) {
        JsonObject rawMap = rawValue != null && rawValue.isJsonObject() ? rawValue.getAsJsonObject() : null;
        return new Quaternion(
                readDoubleValue(rawMap == null ? null : rawMap.get("x"), 0d),
                readDoubleValue(rawMap == null ? null : rawMap.get("y"), 0d),
                readDoubleValue(rawMap == null ? null : rawMap.get("z"), 0d),
                readDoubleValue(rawMap == null ? null : rawMap.get("w"), 1d));
    }

    private static double readDoubleValue(@Nullable JsonElement rawValue, double defaultValue) {
        if (rawValue == null || rawValue.isJsonNull()) {
            return defaultValue;
        }
        try {
            return rawValue.getAsDouble();
        } catch (RuntimeException ignored) {
            return defaultValue;
        }
    }

    @Nonnull
    private static double[][] identityMatrix3() {
        return new double[][] {
                { 1d, 0d, 0d },
                { 0d, 1d, 0d },
                { 0d, 0d, 1d }
        };
    }

    @Nonnull
    private static double[][] multiplyMatrix3(@Nonnull double[][] left, @Nonnull double[][] right) {
        double[][] result = new double[3][3];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                result[row][column] = (left[row][0] * right[0][column])
                        + (left[row][1] * right[1][column])
                        + (left[row][2] * right[2][column]);
            }
        }
        return result;
    }

    @Nonnull
    private static Vector3 transformVector3(@Nonnull double[][] matrix, @Nonnull Vector3 vector) {
        return new Vector3(
                (matrix[0][0] * vector.x()) + (matrix[0][1] * vector.y()) + (matrix[0][2] * vector.z()),
                (matrix[1][0] * vector.x()) + (matrix[1][1] * vector.y()) + (matrix[1][2] * vector.z()),
                (matrix[2][0] * vector.x()) + (matrix[2][1] * vector.y()) + (matrix[2][2] * vector.z()));
    }

    @Nonnull
    private static double[][] scaleMatrix3(@Nonnull Vector3 scale) {
        return new double[][] {
                { scale.x(), 0d, 0d },
                { 0d, scale.y(), 0d },
                { 0d, 0d, scale.z() }
        };
    }

    @Nonnull
    private static double[][] quaternionToMatrix3(@Nonnull Quaternion quaternion) {
        double x = quaternion.x();
        double y = quaternion.y();
        double z = quaternion.z();
        double w = quaternion.w();

        double xx = x * x;
        double yy = y * y;
        double zz = z * z;
        double xy = x * y;
        double xz = x * z;
        double yz = y * z;
        double wx = w * x;
        double wy = w * y;
        double wz = w * z;

        return new double[][] {
                { 1d - (2d * (yy + zz)), 2d * (xy - wz), 2d * (xz + wy) },
                { 2d * (xy + wz), 1d - (2d * (xx + zz)), 2d * (yz - wx) },
                { 2d * (xz - wy), 2d * (yz + wx), 1d - (2d * (xx + yy)) }
        };
    }

    @Nonnull
    private static Vector3 addVector3(@Nonnull Vector3 left, @Nonnull Vector3 right) {
        return new Vector3(left.x() + right.x(), left.y() + right.y(), left.z() + right.z());
    }

    @Nonnull
    private static Vector3 subtractVector3(@Nonnull Vector3 left, @Nonnull Vector3 right) {
        return new Vector3(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }

    @Nonnull
    private static Vector3 crossVector3(@Nonnull Vector3 left, @Nonnull Vector3 right) {
        return new Vector3(
                (left.y() * right.z()) - (left.z() * right.y()),
                (left.z() * right.x()) - (left.x() * right.z()),
                (left.x() * right.y()) - (left.y() * right.x()));
    }

    private static double dotVector3(@Nonnull Vector3 left, @Nonnull Vector3 right) {
        return (left.x() * right.x()) + (left.y() * right.y()) + (left.z() * right.z());
    }

    @Nonnull
    private static Vector3 normalizeVector3(@Nonnull Vector3 vector) {
        double length = Math.sqrt(dotVector3(vector, vector));
        if (length < 0.000001d) {
            return new Vector3(0d, 0d, 0d);
        }
        return new Vector3(vector.x() / length, vector.y() / length, vector.z() / length);
    }

    @Nullable
    private static Color parseBaseColor(@Nullable String[] baseColors) {
        if (baseColors == null || baseColors.length == 0) {
            return null;
        }
        String value = safeString(baseColors[0]).trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return null;
        }
        try {
            return new Color(Integer.parseInt(value, 16), false);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nonnull
    private static String normalizeCommonAssetPath(@Nullable String rawPath) {
        String normalized = safeString(rawPath).replace('\\', '/').trim();
        if (normalized.startsWith("Common/")) {
            normalized = normalized.substring("Common/".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Nonnull
    private static String sanitizeOptionToken(@Nullable String rawValue) {
        String value = safeString(rawValue).trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @Nonnull
    private static String firstNonBlank(@Nullable String primary, @Nullable String fallback) {
        String primaryValue = safeString(primary).trim();
        if (!primaryValue.isBlank()) {
            return primaryValue;
        }
        return safeString(fallback).trim();
    }

    @Nonnull
    private static String safeString(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nonnull
    private static JsonObject getObject(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    @Nullable
    private static JsonObject getOptionalObject(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    @Nonnull
    private static JsonArray getArray(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    @Nonnull
    private static String getString(@Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }
        try {
            return value.getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean getBoolean(@Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return false;
        }
        try {
            return value.getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int normalizeAngle(@Nullable JsonElement rawAngle) {
        int angle = (int) Math.round(readDoubleValue(rawAngle, 0d));
        return ((angle % 360) + 360) % 360;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Vector2(double x, double y) {
    }

    private record Vector3(double x, double y, double z) {
    }

    private record Quaternion(double x, double y, double z, double w) {
    }

    private record FaceSpec(
            @Nonnull String faceName,
            @Nonnull JsonObject faceLayout,
            int faceWidth,
            int faceHeight,
            @Nonnull List<Vector3> vertices) {
    }

    private record VisibleFace(
            @Nonnull BufferedImage faceTexture,
            @Nonnull List<Vector3> cameraVertices,
            @Nonnull Vector3 normal,
            double depth) {
    }

    private record PreviewSelectionSpec(
            @Nonnull String outputAssetPath,
            @Nonnull String sourceTexturePath,
            @Nonnull String modelPath,
            @Nullable Color tintColor) {
    }
}