package org.pixelbays.rpg.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.Realmweavers;
import org.pixelbays.rpg.classes.component.ClassComponent;
import org.pixelbays.rpg.classes.config.ClassDefinition;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.PartyMember;
import org.pixelbays.rpg.party.PartyMemberType;
import org.pixelbays.rpg.party.PartyRole;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class PartyMembersHudServiceModule implements PlayerHudServiceModule {

    @Override
    public void prime(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        hud.getPartyMembersModule().prime(resolvePartyMembers(context.getPlayerRef()));
    }

    @Override
    public void update(@Nonnull PlayerHud hud, @Nonnull PlayerHudContext context) {
        hud.getPartyMembersModule().update(resolvePartyMembers(context.getPlayerRef()));
    }

    @Nonnull
    private static List<PartyMembersHudModule.PartyMemberHudData> resolvePartyMembers(@Nonnull PlayerRef viewerRef) {
        PartyManager partyManager = Realmweavers.get().getPartyManager();
        Party party = partyManager.getPartyForMember(viewerRef.getUuid());
        if (party == null) {
            return List.of();
        }

        List<PartyMember> sortedMembers = new ArrayList<>(party.getMemberList());
        UUID viewerWorldId = viewerRef.getWorldUuid();
        sortedMembers.sort(Comparator
                .comparingInt((PartyMember member) -> roleSortOrder(member.getRole()))
                .thenComparingLong(PartyMember::getJoinedAtMillis)
                .thenComparing(member -> member.getEntityId().toString()));

        List<PartyMembersHudModule.PartyMemberHudData> result = new ArrayList<>(sortedMembers.size());
        for (PartyMember member : sortedMembers) {
            if (member == null || member.getMemberType() != PartyMemberType.PLAYER) {
                continue;
            }

            PlayerRef memberRef = Universe.get().getPlayer(member.getEntityId());
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }

            UUID memberWorldId = memberRef.getWorldUuid();
            if (viewerWorldId != null && memberWorldId != null && !viewerWorldId.equals(memberWorldId)) {
                continue;
            }

            Ref<EntityStore> memberEntity = memberRef.getReference();
            if (memberEntity == null || !memberEntity.isValid()) {
                continue;
            }

            Store<EntityStore> memberStore = memberEntity.getStore();
            EntityStatMap statMap = memberStore.getComponent(memberEntity, EntityStatMap.getComponentType());
            if (statMap == null) {
                continue;
            }

            ClassComponent classComponent = memberStore.getComponent(memberEntity, ClassComponent.getComponentType());
            String activeClassId = PlayerHudServiceSupport.getClassManagementSystem().getPrimaryKnownClassId(classComponent);
            ClassDefinition classDefinition = activeClassId == null || activeClassId.isBlank()
                    ? null
                    : PlayerHudServiceSupport.getClassDefinition(activeClassId);

            String classLabel = "";
            if (classDefinition != null && classDefinition.getDisplayName() != null && !classDefinition.getDisplayName().isBlank()) {
                classLabel = classDefinition.getDisplayName();
            } else if (activeClassId != null && !activeClassId.isBlank()) {
                classLabel = PlayerHudServiceSupport.humanizeIdentifier(activeClassId);
            }

            List<PartyMembersHudModule.PartyStatBarData> bars = new ArrayList<>(3);
            appendPartyStatBar(bars, statMap, "Health", "HP", "#4FD36F");

            if (classDefinition != null && classDefinition.getResourceStats() != null) {
                for (String statId : classDefinition.getResourceStats()) {
                    if (statId == null || statId.isBlank() || "Health".equalsIgnoreCase(statId)) {
                        continue;
                    }
                    appendPartyStatBar(
                            bars,
                            statMap,
                            statId,
                            PlayerHudServiceSupport.shortStatLabel(statId),
                            PlayerHudServiceSupport.resolveStatColor(statId));
                }
            }

            if (bars.isEmpty()) {
                continue;
            }

            result.add(new PartyMembersHudModule.PartyMemberHudData(
                    member.getEntityId().toString(),
                    memberRef.getUsername(),
                    classLabel,
                    resolveRoleAccent(member.getRole()),
                    bars));
        }

        return result;
    }

    private static void appendPartyStatBar(
            @Nonnull List<PartyMembersHudModule.PartyStatBarData> bars,
            @Nonnull EntityStatMap statMap,
            @Nonnull String statId,
            @Nonnull String label,
            @Nonnull String fillColor) {
        int statIndex = EntityStatType.getAssetMap().getIndex(statId);
        EntityStatValue value = statMap.get(statIndex);
        if (value == null) {
            return;
        }

        String valueText = PlayerHudServiceSupport.formatCompactValue(value.get()) + " / "
                + PlayerHudServiceSupport.formatCompactValue(value.getMax());
        bars.add(new PartyMembersHudModule.PartyStatBarData(statId, label, valueText, fillColor, value.asPercentage()));
    }

    private static int roleSortOrder(@Nullable PartyRole role) {
        if (role == null) {
            return 2;
        }

        return switch (role) {
            case LEADER -> 0;
            case ASSISTANT -> 1;
            default -> 2;
        };
    }

    @Nonnull
    private static String resolveRoleAccent(@Nullable PartyRole role) {
        if (role == null) {
            return "#8E98A6";
        }

        return switch (role) {
            case LEADER -> "#D8B15B";
            case ASSISTANT -> "#6FA6FF";
            default -> "#8E98A6";
        };
    }
}