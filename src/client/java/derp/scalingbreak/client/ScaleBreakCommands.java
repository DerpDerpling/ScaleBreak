package derp.scalingbreak.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ScaleBreakCommands {
    private ScaleBreakCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("scalebreak").executes(context -> show(context.getSource()))

                .then(ClientCommandManager.literal("toggle").executes(context -> {
                    boolean value = !ScaleBreakConfig.get().enabled;
                    ScaleBreakConfig.get().enabled = value;
                    ScaleBreakConfig.save();

                    if (!value) {
                        BlockBreakScaleController.clearAnimations();
                    }

                    sendToggleFeedback(context.getSource(), "ScaleBreak", value);

                    return 1;
                }))

                .then(ClientCommandManager.literal("shrinkAmount").then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.0F, 1F)).executes(context -> {
                    float value = FloatArgumentType.getFloat(context, "value");

                    ScaleBreakConfig.get().shrinkAmount = value;
                    ScaleBreakConfig.save();

                    sendValueFeedback(context.getSource(), "Shrink Amount", format(value));

                    return 1;
                })))

                .then(ClientCommandManager.literal("shrinkSpeed").then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1F, 50.0F)).executes(context -> {
                    float value = FloatArgumentType.getFloat(context, "value");

                    ScaleBreakConfig.get().shrinkSpeed = value;
                    ScaleBreakConfig.save();

                    sendValueFeedback(context.getSource(), "Shrink Speed", format(value));

                    return 1;
                })))

                .then(ClientCommandManager.literal("recoverySpeed").then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1F, 50.0F)).executes(context -> {
                    float value = FloatArgumentType.getFloat(context, "value");

                    ScaleBreakConfig.get().recoverySpeed = value;
                    ScaleBreakConfig.save();

                    sendValueFeedback(context.getSource(), "Recovery Speed", format(value));

                    return 1;
                })))

                .then(ClientCommandManager.literal("multiblocks").then(ClientCommandManager.argument("value", BoolArgumentType.bool()).executes(context -> {
                    boolean value = BoolArgumentType.getBool(context, "value");

                    ScaleBreakConfig.get().multiblocks = value;
                    ScaleBreakConfig.save();

                    BlockBreakScaleController.clearAnimations();

                    sendToggleFeedback(context.getSource(), "Multiblock Linking", value);

                    return 1;
                })))

                .then(ClientCommandManager.literal("reload").executes(context -> {
                    ScaleBreakConfig.load();
                    BlockBreakScaleController.clearAnimations();

                    sendSuccess(context.getSource(), "Config reloaded.");

                    return 1;
                }))

                .then(ClientCommandManager.literal("reset").executes(context -> {
                    ScaleBreakConfig.reset();
                    BlockBreakScaleController.clearAnimations();

                    sendSuccess(context.getSource(), "Config reset to defaults.");

                    return 1;
                }))));
    }

    private static int show(FabricClientCommandSource source) {
        ScaleBreakConfig config = ScaleBreakConfig.get();

        source.sendFeedback(prefix().append(Component.literal("Settings").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));

        sendToggleLine(source, "Enabled", config.enabled);
        sendValueLine(source, "Shrink Amount", format(config.shrinkAmount));
        sendValueLine(source, "Shrink Speed", format(config.shrinkSpeed));
        sendValueLine(source, "Recovery Speed", format(config.recoverySpeed));
        sendToggleLine(source, "Multiblock Linking", config.multiblocks);

        return 1;
    }

    private static void sendToggleFeedback(FabricClientCommandSource source, String setting, boolean value) {
        source.sendFeedback(prefix().append(Component.literal(setting + ": ").withStyle(ChatFormatting.GRAY)).append(toggleComponent(value)));
    }

    private static void sendValueFeedback(FabricClientCommandSource source, String setting, String value) {
        source.sendFeedback(prefix().append(Component.literal(setting + ": ").withStyle(ChatFormatting.GRAY)).append(Component.literal(value).withStyle(ChatFormatting.YELLOW)));
    }

    private static void sendSuccess(FabricClientCommandSource source, String message) {
        source.sendFeedback(prefix().append(Component.literal(message).withStyle(ChatFormatting.GREEN)));
    }

    private static void sendToggleLine(FabricClientCommandSource source, String name, boolean value) {
        source.sendFeedback(Component.literal("  " + name + ": ").withStyle(ChatFormatting.GRAY).append(toggleComponent(value)));
    }

    private static void sendValueLine(FabricClientCommandSource source, String name, String value) {
        source.sendFeedback(Component.literal("  " + name + ": ").withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.YELLOW)));
    }

    private static Component toggleComponent(boolean value) {
        return Component.literal(value ? "ON" : "OFF").withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static MutableComponent prefix() {
        return Component.literal("[ScaleBreak] ").withStyle(ChatFormatting.GOLD);
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
