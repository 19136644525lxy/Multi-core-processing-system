package com.qituo.mcps.diagnostic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.qituo.mcps.core.MCPSMod;

public class PerformanceCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("performance")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("analyze")
                    .executes(PerformanceCommands::analyzePerformance)
                )
                .then(CommandManager.literal("bottleneck")
                    .then(CommandManager.literal("detect")
                        .executes(PerformanceCommands::detectBottlenecks)
                    )
                    .then(CommandManager.literal("list")
                        .executes(PerformanceCommands::listBottlenecks)
                    )
                )
                .then(CommandManager.literal("suggestions")
                    .then(CommandManager.literal("generate")
                        .executes(PerformanceCommands::generateSuggestions)
                    )
                    .then(CommandManager.literal("list")
                        .executes(PerformanceCommands::listSuggestions)
                    )
                )
                .then(CommandManager.literal("metrics")
                    .then(CommandManager.literal("reset")
                        .executes(PerformanceCommands::resetMetrics)
                    )
                    .then(CommandManager.literal("list")
                        .executes(PerformanceCommands::listMetrics)
                    )
                )
        );
    }
    
    private static int analyzePerformance(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.analyze.start"));
        RealTimePerformanceAnalyzer.getInstance().analyze();
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.analyze.complete"));
        return 1;
    }
    
    private static int detectBottlenecks(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.bottleneck.detect.start"));
        BottleneckDetector.getInstance().detectBottlenecks();
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.bottleneck.detect.complete"));
        return 1;
    }
    
    private static int listBottlenecks(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.bottleneck.list.header"));
        BottleneckDetector.getInstance().getDetectedBottlenecks().forEach(bottleneck -> {
            context.getSource().sendMessage(Text.literal(bottleneck.toString()));
        });
        return 1;
    }
    
    private static int generateSuggestions(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.suggestions.generate.start"));
        OptimizationSuggestionSystem.getInstance().generateSuggestions();
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.suggestions.generate.complete"));
        return 1;
    }
    
    private static int listSuggestions(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.suggestions.list.header"));
        OptimizationSuggestionSystem.getInstance().getSuggestions().forEach(suggestion -> {
            context.getSource().sendMessage(Text.literal(suggestion.toString()));
        });
        return 1;
    }
    
    private static int resetMetrics(CommandContext<ServerCommandSource> context) {
        RealTimePerformanceAnalyzer.getInstance().resetMetrics();
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.metrics.reset.success"));
        return 1;
    }
    
    private static int listMetrics(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.performance.metrics.list.header"));
        RealTimePerformanceAnalyzer.getInstance().getMetrics().forEach((name, metric) -> {
            if (metric.hasData()) {
                context.getSource().sendMessage(Text.literal(name + ": " + metric.getAverage() + "ms"));
            }
        });
        return 1;
    }
}