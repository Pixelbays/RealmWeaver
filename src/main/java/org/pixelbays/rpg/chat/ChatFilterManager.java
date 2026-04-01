package org.pixelbays.rpg.chat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.pixelbays.rpg.chat.config.ChatFilterData;
import org.pixelbays.rpg.global.util.RpgLogging;

import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;

public final class ChatFilterManager {

    private static final String FILTER_WORDS_ASSET = "chat/filtered-words.json";
    private static final String DEFAULT_FILTER_ID = "Default";

    private final ChatFilterPersistence persistence;
    private volatile List<CompiledFilterPattern> defaultPatterns;
    private volatile List<CompiledFilterPattern> customPatterns;
    private volatile boolean defaultFilterEnabled;
    private volatile boolean customFilterEnabled;
    private volatile ChatFilterData currentData;

    public ChatFilterManager() {
        this(new ChatFilterPersistence(), loadDefaultWordsFromCommonAssetStore());
    }

    ChatFilterManager(@Nonnull ChatFilterPersistence persistence, @Nonnull List<String> defaultWords) {
        this.persistence = persistence;
        this.defaultPatterns = compilePatterns(defaultWords);
        this.customPatterns = List.of();
        this.currentData = createEmptyData();
    }

    public void configure(boolean defaultFilterEnabled, boolean customFilterEnabled) {
        this.defaultFilterEnabled = defaultFilterEnabled;
        this.customFilterEnabled = customFilterEnabled;
    }

    public void loadFromAssets() {
        refreshDefaultPatterns();

        ChatFilterData loaded = persistence.load();
        if (loaded == null) {
            currentData = createEmptyData();
            customPatterns = List.of();
            return;
        }

        currentData = loaded;
        customPatterns = compilePatterns(loaded.getCustomWords());
    }

    public void clear() {
        currentData = createEmptyData();
        customPatterns = List.of();
        defaultFilterEnabled = false;
        customFilterEnabled = false;
    }

    @Nonnull
    public FilterResult filterMessage(@Nullable String content) {
        String resolvedContent = content == null ? "" : content;
        if (resolvedContent.isEmpty()) {
            return new FilterResult(resolvedContent, false);
        }

        if (defaultFilterEnabled && defaultPatterns.isEmpty()) {
            refreshDefaultPatterns();
        }

        List<CompiledFilterPattern> activePatterns = new ArrayList<>();
        if (defaultFilterEnabled) {
            activePatterns.addAll(defaultPatterns);
        }
        if (customFilterEnabled) {
            activePatterns.addAll(customPatterns);
        }
        if (activePatterns.isEmpty()) {
            return new FilterResult(resolvedContent, false);
        }

        List<String> matchedWords = findMatchedWords(resolvedContent, activePatterns);
        if (matchedWords.isEmpty()) {
            return new FilterResult(resolvedContent, false);
        }

        String filtered = resolvedContent;
        for (CompiledFilterPattern compiledPattern : activePatterns) {
            Matcher matcher = compiledPattern.pattern().matcher(filtered);
            if (!matcher.find()) {
                continue;
            }

            filtered = matcher.replaceAll(matchResult -> maskMatch(matchResult.group()));
        }

        return new FilterResult(filtered, true, matchedWords);
    }

    @Nonnull
    public List<String> getCustomWords() {
        return currentData.getCustomWords();
    }

    @Nonnull
    public WordChangeResult addCustomWord(@Nullable String rawWord) {
        String normalized = normalizeWord(rawWord);
        if (normalized.isEmpty()) {
            return WordChangeResult.Invalid;
        }

        List<String> words = new ArrayList<>(currentData.getCustomWords());
        for (String existing : words) {
            if (existing.equalsIgnoreCase(normalized)) {
                return WordChangeResult.AlreadyPresent;
            }
        }

        words.add(normalized);
        persistCustomWords(words);
        return WordChangeResult.Added;
    }

    @Nonnull
    public WordChangeResult removeCustomWord(@Nullable String rawWord) {
        String normalized = normalizeWord(rawWord);
        if (normalized.isEmpty()) {
            return WordChangeResult.Invalid;
        }

        List<String> words = new ArrayList<>(currentData.getCustomWords());
        boolean removed = words.removeIf(existing -> existing.equalsIgnoreCase(normalized));
        if (!removed) {
            return WordChangeResult.NotPresent;
        }

        persistCustomWords(words);
        return WordChangeResult.Removed;
    }

    @Nonnull
    public String normalizeForDisplay(@Nullable String rawWord) {
        return normalizeWord(rawWord);
    }

    private void refreshDefaultPatterns() {
        defaultPatterns = compilePatterns(loadDefaultWordsFromCommonAssetStore());
    }

    private void persistCustomWords(@Nonnull List<String> words) {
        ChatFilterData next = createEmptyData();
        next.setCustomWords(sortAndUnique(words));
        currentData = next;
        customPatterns = compilePatterns(next.getCustomWords());
        persistence.save(next);
    }

    @Nonnull
    private static ChatFilterData createEmptyData() {
        ChatFilterData data = new ChatFilterData();
        data.setCustomWords(List.of());
        return data;
    }

    @Nonnull
    private static List<CompiledFilterPattern> compilePatterns(@Nonnull Collection<String> words) {
        List<String> normalizedWords = sortAndUnique(words);
        List<CompiledFilterPattern> patterns = new ArrayList<>(normalizedWords.size());
        for (String word : normalizedWords) {
            if (word.isEmpty()) {
                continue;
            }
            patterns.add(new CompiledFilterPattern(word, compileWordPattern(word)));
        }
        return List.copyOf(patterns);
    }

    @Nonnull
    private static List<String> sortAndUnique(@Nonnull Collection<String> words) {
        Set<String> unique = new LinkedHashSet<>();
        for (String rawWord : words) {
            String normalized = normalizeWord(rawWord);
            if (!normalized.isEmpty()) {
                unique.add(normalized);
            }
        }

        List<String> ordered = new ArrayList<>(unique);
        ordered.sort(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));
        return ordered;
    }

    @Nonnull
    private static Pattern compileWordPattern(@Nonnull String word) {
        StringBuilder regex = new StringBuilder("(?iu)");
        int firstCodePoint = word.codePointAt(0);
        int lastCodePoint = word.codePointBefore(word.length());

        if (Character.isLetterOrDigit(firstCodePoint)) {
            regex.append("(?<![\\p{L}\\p{N}])");
        }

        appendEscapedWord(regex, word);

        if (Character.isLetterOrDigit(lastCodePoint)) {
            regex.append("(?![\\p{L}\\p{N}])");
        }

        return Pattern.compile(regex.toString());
    }

    private static void appendEscapedWord(@Nonnull StringBuilder regex, @Nonnull String word) {
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                regex.append("\\s+");
            } else {
                regex.append(Pattern.quote(new String(Character.toChars(codePoint))));
            }
        }
    }

    @Nonnull
    private static String maskMatch(@Nonnull String match) {
        StringBuilder builder = new StringBuilder(match.length());
        for (int offset = 0; offset < match.length();) {
            int codePoint = match.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                builder.appendCodePoint(codePoint);
            } else {
                builder.append('*');
            }
        }
        return builder.toString();
    }

    @Nonnull
    private static String normalizeWord(@Nullable String rawWord) {
        if (rawWord == null) {
            return "";
        }

        String normalized = rawWord.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return normalized;
    }

    @Nonnull
    private static List<String> findMatchedWords(
            @Nonnull String content,
            @Nonnull List<CompiledFilterPattern> activePatterns) {
        List<MatchedWordOccurrence> occurrences = new ArrayList<>();
        for (CompiledFilterPattern compiledPattern : activePatterns) {
            Matcher matcher = compiledPattern.pattern().matcher(content);
            while (matcher.find()) {
                String matchedText = matcher.group();
                String normalized = normalizeWord(matchedText);
                if (!normalized.isEmpty()) {
                    occurrences.add(new MatchedWordOccurrence(matcher.start(), normalized, matchedText));
                }
            }
        }

        if (occurrences.isEmpty()) {
            return List.of();
        }

        occurrences.sort(Comparator.comparingInt(MatchedWordOccurrence::start));

        LinkedHashMap<String, String> orderedMatches = new LinkedHashMap<>();
        for (MatchedWordOccurrence occurrence : occurrences) {
            orderedMatches.putIfAbsent(occurrence.normalized(), occurrence.displayText());
        }
        return List.copyOf(orderedMatches.values());
    }

    @Nonnull
    private static List<String> loadDefaultWordsFromCommonAssetStore() {
        CommonAsset asset = CommonAssetRegistry.getByName(FILTER_WORDS_ASSET);
        if (asset == null) {
            return List.of();
        }

        try {
            String json = new String(asset.getBlob().join(), StandardCharsets.UTF_8);
            return parseWordsFromJson(json);
        } catch (RuntimeException ex) {
            RpgLogging.error(ex, "Failed to load filtered words common asset %s", FILTER_WORDS_ASSET);
            return List.of();
        }
    }

    @Nonnull
    private static List<String> parseWordsFromJson(@Nonnull String json) {
        BsonDocument document = BsonDocument.parse(json);
        BsonValue wordsValue = document.get("Words");
        if (!(wordsValue instanceof BsonArray wordsArray)) {
            return List.of();
        }

        List<String> words = new ArrayList<>();
        for (BsonValue value : wordsArray) {
            if (value instanceof BsonString stringValue) {
                String normalized = normalizeWord(stringValue.getValue());
                if (!normalized.isEmpty()) {
                    words.add(normalized);
                }
            }
        }
        return sortAndUnique(words);
    }

    private record CompiledFilterPattern(@Nonnull String word, @Nonnull Pattern pattern) {
    }

    private record MatchedWordOccurrence(int start, @Nonnull String normalized, @Nonnull String displayText) {
    }

    public record FilterResult(@Nonnull String filteredContent, boolean changed, @Nonnull List<String> matchedWords) {

        public FilterResult {
            matchedWords = matchedWords == null ? List.of() : List.copyOf(matchedWords);
        }

        public FilterResult(@Nonnull String filteredContent, boolean changed) {
            this(filteredContent, changed, List.of());
        }
    }

    public enum WordChangeResult {
        Added,
        Removed,
        AlreadyPresent,
        NotPresent,
        Invalid
    }
}