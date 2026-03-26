package org.pixelbays.rpg.global.prereq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pixelbays.rpg.global.config.builder.ClassRefCodec;
import org.pixelbays.rpg.global.config.builder.LevelSystemRefCodec;
import org.pixelbays.rpg.global.config.builder.RaceRefCodec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.function.FunctionCodec;

@SuppressWarnings("deprecation")
public class PrerequisiteRequirements {

	public static final FunctionCodec<String[], List<String>> CLASS_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(new ClassRefCodec(), String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	public static final FunctionCodec<String[], List<String>> RACE_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(new RaceRefCodec(), String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	public static final FunctionCodec<String[], List<String>> STRING_LIST_CODEC = new FunctionCodec<>(
			new ArrayCodec<>(Codec.STRING, String[]::new),
			values -> values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values)),
			values -> values == null ? new String[0] : values.toArray(String[]::new));

	public static final BuilderCodec<PrerequisiteRequirements> CODEC = BuilderCodec.builder(
			PrerequisiteRequirements.class,
			PrerequisiteRequirements::new)
			.append(new KeyedCodec<>("RequiredClassIds", CLASS_LIST_CODEC, false),
					(i, v) -> i.requiredClassIds = v,
					i -> i.requiredClassIds)
			.add()
			.append(new KeyedCodec<>("RequiredClassLevel", Codec.INTEGER, false),
					(i, v) -> i.requiredClassLevel = v,
					i -> i.requiredClassLevel)
			.add()
			.append(new KeyedCodec<>("RequiredRaceIds", RACE_LIST_CODEC, false),
					(i, v) -> i.requiredRaceIds = v,
					i -> i.requiredRaceIds)
			.add()
			.append(new KeyedCodec<>("RequiredExpansionIds", STRING_LIST_CODEC, false),
					(i, v) -> i.requiredExpansionIds = v,
					i -> i.requiredExpansionIds)
			.add()
			.append(new KeyedCodec<>("RequiredLevel", Codec.INTEGER, false),
					(i, v) -> i.requiredLevel = v,
					i -> i.requiredLevel)
			.add()
			.append(new KeyedCodec<>("RequiredLevelSystemId", new LevelSystemRefCodec(), false),
					(i, v) -> i.requiredLevelSystemId = v,
					i -> i.requiredLevelSystemId)
			.add()
			.append(new KeyedCodec<>("RequiredAchievementIds", STRING_LIST_CODEC, false),
					(i, v) -> i.requiredAchievementIds = v,
					i -> i.requiredAchievementIds)
			.add()
			.append(new KeyedCodec<>("RequireParty", Codec.BOOLEAN, false),
					(i, v) -> i.requireParty = v,
					i -> i.requireParty)
			.add()
			.append(new KeyedCodec<>("RequiredPartyCount", Codec.INTEGER, false),
					(i, v) -> i.requiredPartyCount = v,
					i -> i.requiredPartyCount)
			.add()
			.append(new KeyedCodec<>("RequireRaid", Codec.BOOLEAN, false),
					(i, v) -> i.requireRaid = v,
					i -> i.requireRaid)
			.add()
			.append(new KeyedCodec<>("RequiredRaidCount", Codec.INTEGER, false),
					(i, v) -> i.requiredRaidCount = v,
					i -> i.requiredRaidCount)
			.add()
			.build();

	private List<String> requiredClassIds = new ArrayList<>();
	private int requiredClassLevel = 0;
	private List<String> requiredRaceIds = new ArrayList<>();
	private List<String> requiredExpansionIds = new ArrayList<>();
	private int requiredLevel = 0;
	private String requiredLevelSystemId = "";
	private List<String> requiredAchievementIds = new ArrayList<>();
	private boolean requireParty = false;
	private int requiredPartyCount = 0;
	private boolean requireRaid = false;
	private int requiredRaidCount = 0;

	public List<String> getRequiredClassIds() {
		return requiredClassIds == null ? List.of() : requiredClassIds;
	}

	public void setRequiredClassIds(List<String> requiredClassIds) {
		this.requiredClassIds = requiredClassIds == null ? new ArrayList<>() : new ArrayList<>(requiredClassIds);
	}

	public int getRequiredClassLevel() {
		return requiredClassLevel;
	}

	public void setRequiredClassLevel(int requiredClassLevel) {
		this.requiredClassLevel = requiredClassLevel;
	}

	public List<String> getRequiredRaceIds() {
		return requiredRaceIds == null ? List.of() : requiredRaceIds;
	}

	public void setRequiredRaceIds(List<String> requiredRaceIds) {
		this.requiredRaceIds = requiredRaceIds == null ? new ArrayList<>() : new ArrayList<>(requiredRaceIds);
	}

	public List<String> getRequiredExpansionIds() {
		return requiredExpansionIds == null ? List.of() : requiredExpansionIds;
	}

	public void setRequiredExpansionIds(List<String> requiredExpansionIds) {
		this.requiredExpansionIds = requiredExpansionIds == null ? new ArrayList<>() : new ArrayList<>(requiredExpansionIds);
	}

	public int getRequiredLevel() {
		return requiredLevel;
	}

	public void setRequiredLevel(int requiredLevel) {
		this.requiredLevel = requiredLevel;
	}

	public String getRequiredLevelSystemId() {
		return requiredLevelSystemId == null ? "" : requiredLevelSystemId;
	}

	public void setRequiredLevelSystemId(String requiredLevelSystemId) {
		this.requiredLevelSystemId = requiredLevelSystemId == null ? "" : requiredLevelSystemId;
	}

	public List<String> getRequiredAchievementIds() {
		return requiredAchievementIds == null ? List.of() : requiredAchievementIds;
	}

	public void setRequiredAchievementIds(List<String> requiredAchievementIds) {
		this.requiredAchievementIds = requiredAchievementIds == null ? new ArrayList<>() : new ArrayList<>(requiredAchievementIds);
	}

	public boolean isRequireParty() {
		return requireParty;
	}

	public void setRequireParty(boolean requireParty) {
		this.requireParty = requireParty;
	}

	public int getRequiredPartyCount() {
		return requiredPartyCount;
	}

	public void setRequiredPartyCount(int requiredPartyCount) {
		this.requiredPartyCount = requiredPartyCount;
	}

	public boolean isRequireRaid() {
		return requireRaid;
	}

	public void setRequireRaid(boolean requireRaid) {
		this.requireRaid = requireRaid;
	}

	public int getRequiredRaidCount() {
		return requiredRaidCount;
	}

	public void setRequiredRaidCount(int requiredRaidCount) {
		this.requiredRaidCount = requiredRaidCount;
	}

	public boolean hasRequirements() {
		return !getRequiredClassIds().isEmpty()
				|| getRequiredClassLevel() > 0
				|| !getRequiredRaceIds().isEmpty()
				|| !getRequiredExpansionIds().isEmpty()
				|| getRequiredLevel() > 0
				|| !getRequiredLevelSystemId().isBlank()
				|| !getRequiredAchievementIds().isEmpty()
				|| isRequireParty()
				|| getRequiredPartyCount() > 0
				|| isRequireRaid()
				|| getRequiredRaidCount() > 0;
	}
}