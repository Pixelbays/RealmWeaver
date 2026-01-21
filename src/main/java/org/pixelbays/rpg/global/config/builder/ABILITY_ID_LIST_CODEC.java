package org.pixelbays.rpg.global.config.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

@SuppressWarnings("deprecation")
public final class ABILITY_ID_LIST_CODEC {
    private ABILITY_ID_LIST_CODEC() {
    }

    public static final FunctionCodec<String[], List<String>> CODEC = new FunctionCodec<>(
            new ArrayCodec<>(new AbilityRefCodec(), String[]::new),
            arr -> arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr)),
            list -> list == null ? new String[0] : list.toArray(String[]::new));
}

