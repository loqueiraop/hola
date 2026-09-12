package com.claimblocks.command;

import com.claimblocks.gui.ClaimMenuHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/** /claimmerge accept|reject <codigo> y /claimmerge leave. Alias: /fsclaimmerge. */
public final class ClaimMergeCommands {
   private ClaimMergeCommands() {
   }

   public static void register() {
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
         LiteralCommandNode<ServerCommandSource> root = dispatcher.register(build("claimmerge"));
         dispatcher.register(CommandManager.literal("fsclaimmerge").redirect(root));
      });
   }

   private static LiteralArgumentBuilder<ServerCommandSource> build(String name) {
      return CommandManager.literal(name)
         .then(
            CommandManager.literal("accept")
               .then(
                  CommandManager.argument("code", StringArgumentType.word()).executes(ctx -> {
                     ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
                     ClaimMenuHandler.acceptMerge(p, StringArgumentType.getString(ctx, "code"));
                     return 1;
                  })
               )
         )
         .then(
            CommandManager.literal("reject")
               .then(
                  CommandManager.argument("code", StringArgumentType.word()).executes(ctx -> {
                     ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
                     ClaimMenuHandler.rejectMerge(p, StringArgumentType.getString(ctx, "code"));
                     return 1;
                  })
               )
         )
         .then(CommandManager.literal("leave").executes(ctx -> {
            ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
            ClaimMenuHandler.leaveMerge(p);
            return 1;
         }));
   }
}
