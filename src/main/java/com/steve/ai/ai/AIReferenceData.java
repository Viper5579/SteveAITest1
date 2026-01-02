package com.steve.ai.ai;

import com.steve.ai.structure.StructureTemplateLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AIReferenceData {
    public static final List<String> PASSIVE_ENTITIES = List.of(
        "sheep",
        "cow",
        "pig",
        "chicken"
    );

    public static final List<String> HOSTILE_ENTITIES = List.of(
        "zombie",
        "skeleton",
        "spider",
        "creeper"
    );

    public static final List<String> ORES = List.of(
        "iron_ore",
        "diamond_ore",
        "coal_ore",
        "gold_ore",
        "copper_ore",
        "redstone_ore",
        "emerald_ore"
    );
    public static final List<String> PROCEDURAL_STRUCTURES = List.of(
        "castle",
        "tower",
        "barn",
        "modern"
    );

    private AIReferenceData() {
    }

    public static Set<String> getValidAttackTargets() {
        Set<String> targets = new HashSet<>();
        targets.add("hostile");
        targets.addAll(PASSIVE_ENTITIES);
        targets.addAll(HOSTILE_ENTITIES);
        return targets;
    }

    public static boolean isValidAttackTarget(String target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        String normalized = normalize(target);
        return getValidAttackTargets().contains(normalized);
    }

    public static List<String> getAvailableStructures() {
        return StructureTemplateLoader.getAvailableStructures();
    }

    public static List<String> getAllStructureOptions() {
        Set<String> options = new HashSet<>(getAvailableStructures());
        options.addAll(PROCEDURAL_STRUCTURES);
        return options.stream()
            .sorted()
            .collect(Collectors.toList());
    }

    public static boolean isValidStructureName(String structureName) {
        if (structureName == null || structureName.isBlank()) {
            return false;
        }
        List<String> available = getAvailableStructures();
        String normalized = normalize(structureName);
        boolean matchesTemplate = available.stream()
            .map(AIReferenceData::normalize)
            .anyMatch(normalized::equals);
        boolean matchesProcedural = PROCEDURAL_STRUCTURES.stream()
            .map(AIReferenceData::normalize)
            .anyMatch(normalized::equals);
        return matchesTemplate || matchesProcedural;
    }

    public static String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- none found";
        }
        return items.stream()
            .map(item -> "- " + item)
            .collect(Collectors.joining("\n"));
    }

    private static String normalize(String value) {
        return value.trim()
            .toLowerCase()
            .replace("minecraft:", "")
            .replace(" ", "_");
    }
}
