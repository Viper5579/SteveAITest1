package com.steve.ai.ai;

import com.steve.ai.SteveMod;
import com.steve.ai.action.Task;
import com.steve.ai.config.SteveConfig;
import com.steve.ai.entity.SteveEntity;
import com.steve.ai.memory.WorldKnowledge;

import java.util.List;

public class TaskPlanner {
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;
    private final GroqClient groqClient;
    private final AnthropicClient anthropicClient;

    public TaskPlanner() {
        this.openAIClient = new OpenAIClient();
        this.geminiClient = new GeminiClient();
        this.groqClient = new GroqClient();
        this.anthropicClient = new AnthropicClient();
    }

    public ResponseParser.ParsedResponse planTasks(SteveEntity steve, String command) {
        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(steve);
            String userPrompt = PromptBuilder.buildUserPrompt(steve, command, worldKnowledge);
            
            String provider = SteveConfig.AI_PROVIDER.get().toLowerCase();
            SteveMod.LOGGER.info("Requesting AI plan for Steve '{}' using {}: {}", steve.getSteveName(), provider, command);
            
            String response = getAIResponse(provider, systemPrompt, userPrompt);
            
            if (response == null) {
                SteveMod.LOGGER.error("Failed to get AI response for command: {}", command);
                return null;
            }

            ResponseParser.ParsedResponse parsedResponse = ResponseParser.parseAIResponse(response);
            
            if (parsedResponse == null) {
                SteveMod.LOGGER.error("Failed to parse AI response");
                return null;
            }
            
            List<Task> validatedTasks = validateAndFilterTasks(parsedResponse.getTasks());
            if (validatedTasks.size() != parsedResponse.getTasks().size()) {
                SteveMod.LOGGER.warn("Filtered invalid tasks ({} -> {})",
                    parsedResponse.getTasks().size(), validatedTasks.size());
            }

            SteveMod.LOGGER.info("Plan: {} ({} tasks)", parsedResponse.getPlan(), validatedTasks.size());

            return new ResponseParser.ParsedResponse(
                parsedResponse.getReasoning(),
                parsedResponse.getPlan(),
                validatedTasks
            );
            
        } catch (Exception e) {
            SteveMod.LOGGER.error("Error planning tasks", e);
            return null;
        }
    }

    private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
        String response = switch (provider) {
            case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
            case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
            case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
            case "anthropic" -> anthropicClient.sendRequest(systemPrompt, userPrompt);
            default -> {
                SteveMod.LOGGER.warn("Unknown AI provider '{}', using Groq", provider);
                yield groqClient.sendRequest(systemPrompt, userPrompt);
            }
        };
        
        if (response == null && !provider.equals("groq")) {
            SteveMod.LOGGER.warn("{} failed, trying Groq as fallback", provider);
            response = groqClient.sendRequest(systemPrompt, userPrompt);
        }
        
        return response;
    }

    public boolean validateTask(Task task) {
        String action = task.getAction();
        
        return switch (action) {
            case "pathfind" -> task.hasParameters("x", "y", "z");
            case "mine" -> task.hasParameters("block", "quantity");
            case "place" -> task.hasParameters("block", "x", "y", "z");
            case "craft" -> task.hasParameters("item", "quantity");
            case "attack" -> isValidAttackTask(task);
            case "follow" -> task.hasParameters("player");
            case "gather" -> task.hasParameters("resource", "quantity");
            case "build" -> isValidBuildTask(task);
            default -> {
                SteveMod.LOGGER.warn("Unknown action type: {}", action);
                yield false;
            }
        };
    }

    public List<Task> validateAndFilterTasks(List<Task> tasks) {
        return tasks.stream()
            .filter(this::validateTask)
            .toList();
    }

    private boolean isValidBuildTask(Task task) {
        if (!task.hasParameters("structure", "blocks", "dimensions")) {
            return false;
        }

        Object structure = task.getParameters().get("structure");
        if (structure instanceof String structureName) {
            if (AIReferenceData.isValidStructureName(structureName)) {
                return true;
            }
            SteveMod.LOGGER.warn("Unknown structure '{}' (available: {})",
                structureName, AIReferenceData.getAllStructureOptions());
            return false;
        }

        return false;
    }

    private boolean isValidAttackTask(Task task) {
        if (!task.hasParameters("target")) {
            return false;
        }

        Object target = task.getParameters().get("target");
        if (target instanceof String targetName) {
            if (AIReferenceData.isValidAttackTarget(targetName)) {
                return true;
            }
            SteveMod.LOGGER.warn("Unknown attack target '{}' (allowed: {})",
                targetName, AIReferenceData.getValidAttackTargets());
            return false;
        }

        return false;
    }
}
