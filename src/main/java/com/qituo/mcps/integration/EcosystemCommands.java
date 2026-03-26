package com.qituo.mcps.integration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.qituo.mcps.api.APIDocumentation;

public class EcosystemCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("ecosystem")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("api")
                    .then(CommandManager.literal("generate")
                        .executes(EcosystemCommands::generateAPIDocumentation)
                    )
                    .then(CommandManager.literal("list")
                        .executes(EcosystemCommands::listAPIClasses)
                    )
                )
                .then(CommandManager.literal("compatibility")
                    .then(CommandManager.literal("test")
                        .executes(EcosystemCommands::runCompatibilityTests)
                    )
                    .then(CommandManager.literal("report")
                        .executes(EcosystemCommands::generateCompatibilityReport)
                    )
                )
                .then(CommandManager.literal("mods")
                    .then(CommandManager.literal("list")
                        .executes(EcosystemCommands::listMods)
                    )
                    .then(CommandManager.literal("info")
                        .then(CommandManager.argument("modId", StringArgumentType.string())
                            .executes(EcosystemCommands::getModInfo)
                        )
                    )
                )
        );
    }
    
    private static int generateAPIDocumentation(CommandContext<ServerCommandSource> context) {
        APIDocumentation.getInstance().generateDocumentation();
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.api.generate.success"));
        return 1;
    }
    
    private static int listAPIClasses(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.api.list.header"));
        APIDocumentation.getInstance().getAPIClasses().forEach(apiClass -> {
            context.getSource().sendMessage(Text.literal("- " + apiClass.getClassName()));
        });
        return 1;
    }
    
    private static int runCompatibilityTests(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.compatibility.test.start"));
        CompatibilityTestTool.getInstance().runCompatibilityTests();
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.compatibility.test.complete"));
        return 1;
    }
    
    private static int generateCompatibilityReport(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.compatibility.report.start"));
        CompatibilityTestTool.CompatibilityReport report = CompatibilityTestTool.getInstance().runCompatibilityTests();
        report.generateReport();
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.compatibility.report.complete"));
        return 1;
    }
    
    private static int listMods(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.mods.list.header"));
        ModInteractionFramework.getInstance().getAllMods().forEach(modInfo -> {
            context.getSource().sendMessage(Text.literal("- " + modInfo.getModId() + " v" + modInfo.getVersion()));
        });
        return 1;
    }
    
    private static int getModInfo(CommandContext<ServerCommandSource> context) {
        String modId = StringArgumentType.getString(context, "modId");
        ModInteractionFramework.ModInfo modInfo = ModInteractionFramework.getInstance().getModInfo(modId);
        if (modInfo != null) {
            context.getSource().sendMessage(Text.literal("Mod: " + modInfo.getModId()));
            context.getSource().sendMessage(Text.literal("Version: " + modInfo.getVersion()));
            context.getSource().sendMessage(Text.literal("Description: " + modInfo.getDescription()));
        } else {
            context.getSource().sendMessage(Text.translatable("mcps.command.ecosystem.mods.info.not_found"));
        }
        return 1;
    }
}