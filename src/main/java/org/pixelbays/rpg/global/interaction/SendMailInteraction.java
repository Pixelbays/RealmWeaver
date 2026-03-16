package org.pixelbays.rpg.global.interaction;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.pixelbays.plugin.ExamplePlugin;
import org.pixelbays.rpg.mail.MailActionResult;
import org.pixelbays.rpg.mail.MailManager;
import org.pixelbays.rpg.mail.MailMessage;
import org.pixelbays.rpg.mail.config.MailData;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;

@SuppressWarnings({ "FieldHidesSuperclassField", "null" })
public class SendMailInteraction extends SimpleInstantInteraction {

	@Nonnull
	public static final BuilderCodec<SendMailInteraction> CODEC = BuilderCodec.builder(
			SendMailInteraction.class,
			SendMailInteraction::new,
			SimpleInstantInteraction.CODEC)
			.documentation("Queues a system mail for the interacting player. A MailId template can be used, or inline sender, subject, and body text can be supplied.")
			.append(new KeyedCodec<>("MailId", Codec.STRING, false),
					(i, v) -> i.mailId = v,
					i -> i.mailId)
			.add()
			.append(new KeyedCodec<>("SenderName", Codec.STRING, false),
					(i, v) -> i.senderName = v,
					i -> i.senderName)
			.add()
			.append(new KeyedCodec<>("Subject", Codec.STRING, false),
					(i, v) -> i.subject = v,
					i -> i.subject)
			.add()
			.append(new KeyedCodec<>("Body", Codec.STRING, false),
					(i, v) -> i.body = v,
					i -> i.body)
			.add()
			.build();

	private String mailId = "";
	private String senderName = "";
	private String subject = "";
	private String body = "";

	@Override
	protected void firstRun(@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nonnull CooldownHandler cooldownHandler) {
		PlayerRef playerRef = InteractionPlayerUtil.resolvePlayerRef(context);
		if (playerRef == null) {
			context.getState().state = InteractionState.Failed;
			return;
		}

		MailManager mailManager = ExamplePlugin.get().getMailManager();
		MailActionResult result;
		MailMessage template = resolveTemplate();
		if (template != null) {
			result = mailManager.sendSystemMail(
					playerRef.getUuid(),
					template.getSenderName(),
					template.getSubject(),
					template.getBody(),
					template.getAttachedItemStacks(),
					template.getAttachedCurrency(),
					template.getCashOnDelivery());
		} else {
			result = mailManager.sendSystemMail(playerRef.getUuid(), senderName, subject, body, List.of(), null, null);
		}

		context.getState().state = result.isSuccess() ? InteractionState.Finished : InteractionState.Failed;
	}

	@Nullable
	private MailMessage resolveTemplate() {
		if (mailId == null || mailId.isBlank()) {
			return null;
		}

		DefaultAssetMap<String, MailData> assetMap = MailData.getAssetMap();
		if (assetMap == null) {
			return null;
		}

		MailData mailData = assetMap.getAsset(mailId);
		return mailData == null ? null : mailData.toMailMessage();
	}
}