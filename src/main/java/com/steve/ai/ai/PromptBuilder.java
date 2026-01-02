package com.steve.ai.ai;

import com.steve.ai.entity.SteveEntity;
import com.steve.ai.memory.WorldKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class PromptBuilder {
    
    public static String buildSystemPrompt() {
        String templatesList = AIReferenceData.formatList(AIReferenceData.getAvailableStructures());
        String proceduralList = AIReferenceData.formatList(AIReferenceData.PROCEDURAL_STRUCTURES);
        return """
            You are a Minecraft AI agent. Respond ONLY with valid JSON, no extra text.

            FORMAT (strict JSON):
            {"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}

            AVAILABLE ENTITIES:
            - Passive: %s
            - Hostile: %s
            - Ores: %s

            STRUCTURES (from /structures directory):
            %s

            PROCEDURAL STRUCTURES:
            %s

            ORE SPAWN LEVELS:
            - diamond_ore: y < 16
            - iron_ore: y < 64
            - coal_ore: any level

            ACTIONS:
            - attack: {"target": "sheep|cow|zombie|hostile", "quantity": 1}
            - build: {"structure": "STRUCTURE_NAME", "blocks": ["oak_planks", "cobblestone", "glass_pane"], "dimensions": [9, 6, 9]}
            - mine: {"block": "diamond_ore", "quantity": 8}
            - follow: {"player": "NAME"}
            - pathfind: {"x": 0, "y": 0, "z": 0}

            RULES:
            1. Use specific entity names for attack targets; use "hostile" only for all hostiles.
            2. ONLY use structure names listed above (templates or procedural).
            3. Use 2-3 block types: oak_planks, cobblestone, glass_pane, stone_bricks.
            4. NO extra pathfind tasks unless explicitly requested.
            5. Keep reasoning under 15 words.
            6. MINING: Use ore IDs from the list above.

            EXAMPLES (copy these formats exactly):

            Command: "kill 5 sheep"
            {"reasoning": "Need sheep cleared", "plan": "Attack sheep", "tasks": [{"action": "attack", "parameters": {"target": "sheep", "quantity": 5}}]}

            Command: "mine diamonds"
            {"reasoning": "Collect diamond ore", "plan": "Mine diamonds", "tasks": [{"action": "mine", "parameters": {"block": "diamond_ore", "quantity": 10}}]}

            Command: "build a house"
            {"reasoning": "Build a basic house", "plan": "Construct house", "tasks": [{"action": "build", "parameters": {"structure": "house", "blocks": ["oak_planks", "cobblestone", "glass_pane"], "dimensions": [5, 5, 5]}}]}

            CRITICAL: Output ONLY valid JSON. No markdown, no explanations, no line breaks in JSON.
            """.formatted(
            String.join(", ", AIReferenceData.PASSIVE_ENTITIES),
            String.join(", ", AIReferenceData.HOSTILE_ENTITIES),
            String.join(", ", AIReferenceData.ORES),
            templatesList,
            proceduralList
        );
    }

    public static String buildUserPrompt(SteveEntity steve, String command, WorldKnowledge worldKnowledge) {
        StringBuilder prompt = new StringBuilder();
        
        // Give agents FULL situational awareness
        prompt.append("=== YOUR SITUATION ===\n");
        prompt.append("Position: ").append(formatPosition(steve.blockPosition())).append("\n");
        prompt.append("Nearby Players: ").append(worldKnowledge.getNearbyPlayerNames()).append("\n");
        prompt.append("Nearby Entities: ").append(worldKnowledge.getNearbyEntitiesSummary()).append("\n");
        prompt.append("Nearby Blocks: ").append(worldKnowledge.getNearbyBlocksSummary()).append("\n");
        prompt.append("Biome: ").append(worldKnowledge.getBiomeName()).append("\n");
        
        prompt.append("\n=== PLAYER COMMAND ===\n");
        prompt.append("\"").append(command).append("\"\n");
        
        prompt.append("\n=== YOUR RESPONSE (with reasoning) ===\n");
        
        return prompt.toString();
    }

    private static String formatPosition(BlockPos pos) {
        return String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String formatInventory(SteveEntity steve) {
        return "[empty]";
    }
}
