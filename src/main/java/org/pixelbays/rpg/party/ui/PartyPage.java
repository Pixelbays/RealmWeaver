package org.pixelbays.rpg.party.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.party.Party;
import org.pixelbays.rpg.party.PartyActionResult;
import org.pixelbays.rpg.party.PartyManager;
import org.pixelbays.rpg.party.command.PartyCommandUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BoolParamValue;
import com.hypixel.hytale.protocol.DoubleParamValue;
import com.hypixel.hytale.protocol.IntParamValue;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.ParamValue;
import com.hypixel.hytale.protocol.StringParamValue;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@SuppressWarnings("null")
public class PartyPage extends CustomUIPage {

    private static final String STATUS_LABEL = "#StatusLabel";
    private static final String ROSTER_LABEL = "#RosterLabel";

    private static final String INVITE_FIELD = "#InviteeField";
    private static final String KICK_FIELD = "#KickField";
    private static final String PROMOTE_FIELD = "#PromoteField";

    private static final String INVITE_BUTTON = "#InviteButton";
    private static final String KICK_BUTTON = "#KickButton";
    private static final String PROMOTE_BUTTON = "#PromoteButton";
    private static final String LEAVE_BUTTON = "#LeaveButton";

    private final PartyManager partyManager;

    public PartyPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.partyManager = ExamplePlugin.get().getPartyManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PartyPage.ui");
        bindEvents(eventBuilder);
        appendView(commandBuilder, null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        String action = extractString(rawData, "Action");
        if (action == null) {
            return;
        }

        String invitee = extractString(rawData, "@Invitee");
        String kickTarget = extractString(rawData, "@KickTarget");
        String promoteTarget = extractString(rawData, "@PromoteTarget");

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> handleAction(ref, store, action, invitee, kickTarget, promoteTarget));
    }

    private void handleAction(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String action,
            @Nullable String invitee,
            @Nullable String kickTarget,
            @Nullable String promoteTarget) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Message statusMessage = null;

        if ("Invite".equals(action)) {
            statusMessage = handleInvite(invitee);
        } else if ("Kick".equals(action)) {
            statusMessage = handleKick(kickTarget);
        } else if ("Promote".equals(action)) {
            statusMessage = handlePromote(promoteTarget);
        } else if ("Leave".equals(action)) {
            PartyActionResult result = partyManager.leaveParty(playerRef.getUuid());
            statusMessage = PartyCommandUtil.managerResultMessage(result.getMessage());
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        appendView(commandBuilder, statusMessage);
        sendUpdate(commandBuilder);
    }

    private Message handleInvite(@Nullable String invitee) {
        if (invitee == null || invitee.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.party.usage.invite");
        }

        PlayerRef targetRef = PartyCommandUtil.findPlayerByName(invitee.trim());
        if (targetRef == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        PartyActionResult result = partyManager.invitePlayer(playerRef.getUuid(), targetRef.getUuid());
        Message message = PartyCommandUtil.managerResultMessage(result.getMessage());
        if (result.isSuccess()) {
            targetRef.sendMessage(Message.translation("pixelbays.rpg.party.notify.invitedBy").param("player", playerRef.getUsername()));
        }

        return message;
    }

    private Message handleKick(@Nullable String targetName) {
        if (targetName == null || targetName.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.party.usage.kick");
        }

        PlayerRef targetRef = PartyCommandUtil.findPlayerByName(targetName.trim());
        if (targetRef == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        PartyActionResult result = partyManager.kickMember(playerRef.getUuid(), targetRef.getUuid());
        return PartyCommandUtil.managerResultMessage(result.getMessage());
    }

    private Message handlePromote(@Nullable String targetName) {
        if (targetName == null || targetName.trim().isEmpty()) {
            return Message.translation("pixelbays.rpg.party.usage.promote");
        }

        PlayerRef targetRef = PartyCommandUtil.findPlayerByName(targetName.trim());
        if (targetRef == null) {
            return Message.translation("pixelbays.rpg.common.playerNotFound");
        }

        PartyActionResult result = partyManager.promoteToAssistant(playerRef.getUuid(), targetRef.getUuid());
        return PartyCommandUtil.managerResultMessage(result.getMessage());
    }

    private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                INVITE_BUTTON,
                new EventData()
                        .append("Action", "Invite")
                        .append("@Invitee", INVITE_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                KICK_BUTTON,
                new EventData()
                        .append("Action", "Kick")
                        .append("@KickTarget", KICK_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                PROMOTE_BUTTON,
                new EventData()
                        .append("Action", "Promote")
                        .append("@PromoteTarget", PROMOTE_FIELD + ".Value"));

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                LEAVE_BUTTON,
                new EventData().append("Action", "Leave"));
    }

    private void appendView(@Nonnull UICommandBuilder commandBuilder, @Nullable Message statusMessage) {
        if (statusMessage != null) {
            commandBuilder.setObject(STATUS_LABEL + ".Text", toLocalizableString(statusMessage));
        } else {
            commandBuilder.set(STATUS_LABEL + ".Text", "");
        }

        Party party = partyManager.getPartyForMember(playerRef.getUuid());
        if (party == null) {
            commandBuilder.set(ROSTER_LABEL + ".Text", "");
            return;
        }

        String leaderName = PartyCommandUtil.resolveDisplayName(party.getLeaderId());

        List<String> assistantNames = new ArrayList<>();
        for (UUID assistantId : party.getAssistants()) {
            assistantNames.add(PartyCommandUtil.resolveDisplayName(assistantId));
        }

        List<String> memberNames = new ArrayList<>();
        for (UUID memberId : party.getMembers().keySet()) {
            if (party.getLeaderId() != null && party.getLeaderId().equals(memberId)) {
                continue;
            }
            if (party.getAssistants().contains(memberId)) {
                continue;
            }
            memberNames.add(PartyCommandUtil.resolveDisplayName(memberId));
        }

        Map<String, String> params = new HashMap<>();
        params.put("leader", leaderName);
        params.put("assistants", String.join(", ", assistantNames));
        params.put("members", String.join(", ", memberNames));

        commandBuilder.setObject(ROSTER_LABEL + ".Text",
                LocalizableString.fromMessageId("pixelbays.rpg.party.ui.rosterBody", params));
    }

    @Nonnull
    private static LocalizableString toLocalizableString(@Nonnull Message message) {
        var formatted = message.getFormattedMessage();

        if (formatted.messageId != null) {
            Map<String, String> params = null;
            if (formatted.params != null && !formatted.params.isEmpty()) {
                params = new HashMap<>();
                for (var entry : formatted.params.entrySet()) {
                    String key = entry.getKey();
                    ParamValue value = entry.getValue();
                    String resolved = paramToString(value);
                    if (resolved != null) {
                        params.put(key, resolved);
                    }
                }
            }
            return LocalizableString.fromMessageId(formatted.messageId, params);
        }

        if (formatted.rawText != null) {
            return LocalizableString.fromString(formatted.rawText);
        }

        return LocalizableString.fromString("");
    }

    @Nullable
    private static String paramToString(@Nullable ParamValue value) {
        if (value == null) {
            return null;
        }
        if (value instanceof StringParamValue sp) {
            return sp.value;
        }
        if (value instanceof IntParamValue ip) {
            return String.valueOf(ip.value);
        }
        if (value instanceof LongParamValue lp) {
            return String.valueOf(lp.value);
        }
        if (value instanceof DoubleParamValue dp) {
            return String.valueOf(dp.value);
        }
        if (value instanceof BoolParamValue bp) {
            return String.valueOf(bp.value);
        }
        return String.valueOf(value);
    }

    @Nullable
    private static String extractString(@Nonnull String rawData, @Nonnull String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = rawData.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = rawData.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = rawData.indexOf('"', colonIndex + 1);
        if (firstQuote == -1) {
            return null;
        }

        int secondQuote = rawData.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return rawData.substring(firstQuote + 1, secondQuote);
    }
}
