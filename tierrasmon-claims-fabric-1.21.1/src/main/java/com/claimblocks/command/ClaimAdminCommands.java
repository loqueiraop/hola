package com.claimblocks.command;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.gui.AdminPanelHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClaimAdminCommands {
   private static final SuggestionProvider<ServerCommandSource> GLOBAL_FLAG_NAMES = (ctx, builder) -> {
      String s = builder.getRemaining().toLowerCase();

      for (String n : new String[]{"globalPVP", "globalMobGriefing", "globalFireSpread", "globalNoMobSpawn"}) {
         if (n.toLowerCase().startsWith(s)) {
            builder.suggest(n);
         }
      }

      return builder.buildFuture();
   };
   private static final SuggestionProvider<ServerCommandSource> ON_OFF = (ctx, builder) -> {
      String s = builder.getRemaining().toLowerCase();

      for (String n : new String[]{"on", "off"}) {
         if (n.startsWith(s)) {
            builder.suggest(n);
         }
      }

      return builder.buildFuture();
   };

   public static void register() {
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
         LiteralCommandNode<ServerCommandSource> root = dispatcher.register(build("tmclaimsadmin"));
         dispatcher.register(CommandManager.literal("claimadmin").requires(s -> s.hasPermissionLevel(2)).redirect(root));
         dispatcher.register(CommandManager.literal("fsclaimadmin").requires(s -> s.hasPermissionLevel(2)).redirect(root));
      });
   }

   private static LiteralArgumentBuilder<ServerCommandSource> build(String name) {
      return CommandManager.literal(name)
         .requires(s -> s.hasPermissionLevel(2))
         .executes(ClaimAdminCommands::openPanel)
         .then(CommandManager.literal("list").executes(ClaimAdminCommands::list))
         .then(CommandManager.literal("bypass").executes(ClaimAdminCommands::toggleBypass))
         .then(CommandManager.literal("stats").executes(ClaimAdminCommands::stats))
         .then(CommandManager.literal("reload").executes(ClaimAdminCommands::reload))
         .then(
            CommandManager.literal("globalflag")
               .then(
                  CommandManager.argument("flag", StringArgumentType.word())
                     .suggests(GLOBAL_FLAG_NAMES)
                     .then(CommandManager.argument("value", StringArgumentType.word()).suggests(ON_OFF).executes(ClaimAdminCommands::globalFlag))
               )
         );
   }

   private static int reload(CommandContext<ServerCommandSource> ctx) {
      boolean ok = ClaimConfig.get().reload();
      if (ok) {
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("✔ Configuración recargada desde claimblocks_config.json").formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD}),
               true
            );
         return 1;
      } else {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Todavía no hay mundo cargado; no se pudo recargar.").formatted(Formatting.RED));
         return 0;
      }
   }

   private static int openPanel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      AdminPanelHandler.open(p, 0);
      return 1;
   }

   private static int list(CommandContext<ServerCommandSource> ctx) {
      List<Claim> all = ClaimManager.getInstance().getAllClaims();
      if (all.isEmpty()) {
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("[i] No hay zonas activas en el servidor.").formatted(Formatting.AQUA), false);
         return 0;
      } else {
         for (Claim c : all) {
            MutableText line = Text.literal("✔ ")
               .formatted(new Formatting[]{Formatting.AQUA, Formatting.BOLD})
               .append(Text.literal(c.getOwnerName()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
               .append(Text.literal(" | ").formatted(Formatting.GRAY))
               .append(Text.literal(c.sizeLabel()).formatted(Formatting.YELLOW))
               .append(Text.literal(" | ").formatted(Formatting.GRAY))
               .append(Text.literal("X:" + c.getX() + " Z:" + c.getZ()).formatted(Formatting.WHITE))
               .append(Text.literal(" | ").formatted(Formatting.GRAY))
               .append(Text.literal("Dim:" + c.getWorld()).formatted(Formatting.DARK_AQUA));
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> lambda$list$5((Text)line), false);
         }

         return all.size();
      }
   }

   private static int toggleBypass(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      boolean now = ClaimManager.getInstance().toggleBypass(p.getUuid());
      if (now) {
         p.sendMessage(Text.literal("✔ Modo bypass activado. Las zonas no te afectan.").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}), false);
      } else {
         p.sendMessage(Text.literal("✔ Modo bypass desactivado.").formatted(Formatting.GREEN), false);
      }

      return 1;
   }

   private static int stats(CommandContext<ServerCommandSource> ctx) {
      List<Claim> all = ClaimManager.getInstance().getAllClaims();
      HashSet<UUID> uniqueOwners = new HashSet<>();
      Claim biggest = null;
      Claim oldest = null;
      int paid = 0;

      for (Claim c : all) {
         uniqueOwners.add(c.getOwnerUUID());
         if (biggest == null || c.getRadius() > biggest.getRadius()) {
            biggest = c;
         }

         if (oldest == null || c.getCreatedAt() < oldest.getCreatedAt()) {
            oldest = c;
         }

         ClaimTier t;
         if ((t = c.getTier()) != null && t.isPaid()) {
            paid++;
         }
      }

      ServerCommandSource src = (ServerCommandSource)ctx.getSource();
      src.sendFeedback(() -> Text.literal("--- Estadísticas de Tierrasmon Claims ---").formatted(Formatting.GOLD), false);
      src.sendFeedback(() -> infoLine("Total de zonas activas: " + all.size()), false);
      src.sendFeedback(() -> infoLine("Jugadores con zona: " + uniqueOwners.size()), false);
      if (biggest != null) {
         Claim big = biggest;
         src.sendFeedback(() -> infoLine("Zona más grande: " + big.sizeLabel() + " de " + big.getOwnerName()), false);
      }

      if (oldest != null) {
         Claim o = oldest;
         String when = o.getCreatedAt() == 0L ? "(legacy)" : new Date(o.getCreatedAt()).toString();
         src.sendFeedback(
            () -> Text.literal("✔ Zona más antigua: ")
                  .formatted(new Formatting[]{Formatting.AQUA, Formatting.BOLD})
                  .append(Text.literal(o.sizeLabel() + " de " + o.getOwnerName()).formatted(Formatting.WHITE))
                  .append(Text.literal(" (" + when + ")").formatted(Formatting.DARK_GRAY)),
            false
         );
      }

      int paidCount = paid;
      src.sendFeedback(() -> Text.literal("✔ Zonas de pago activas: " + paidCount).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}), false);
      src.sendFeedback(() -> Text.literal("-----------------------------------").formatted(Formatting.GOLD), false);
      return 1;
   }

   private static Text infoLine(String text) {
      return Text.literal("✔ ").formatted(new Formatting[]{Formatting.AQUA, Formatting.BOLD}).append(Text.literal(text).formatted(Formatting.AQUA));
   }

   private static int globalFlag(CommandContext<ServerCommandSource> ctx) {
      String flag = StringArgumentType.getString(ctx, "flag");
      String value = StringArgumentType.getString(ctx, "value").toLowerCase();
      if (!flag.equals("globalPVP") && !flag.equals("globalMobGriefing") && !flag.equals("globalFireSpread") && !flag.equals("globalNoMobSpawn")) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Flag global desconocida: " + flag).formatted(Formatting.RED));
         return 0;
      } else {
         boolean on = value.equals("on") || value.equals("true");
         GlobalFlags.getInstance().set(flag, on, ((ServerCommandSource)ctx.getSource()).getServer());
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("✔ Flag global ")
                     .formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
                     .append(Text.literal(flag).formatted(Formatting.YELLOW))
                     .append(Text.literal(" establecida a ").formatted(Formatting.GOLD))
                     .append(Text.literal(on ? "[ON]" : "[OFF]").formatted(new Formatting[]{on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD}))
                     .append(Text.literal(".").formatted(Formatting.GOLD)),
               true
            );
         MutableText bcast = Text.literal("[!] Un administrador cambió una configuración global del servidor.").formatted(Formatting.YELLOW);
         ((ServerCommandSource)ctx.getSource()).getServer().getPlayerManager().getPlayerList().forEach(arg_0 -> lambda$globalFlag$14((Text)bcast, arg_0));
         return 1;
      }
   }

   private static void lambda$globalFlag$14(Text bcast, ServerPlayerEntity p) {
      p.sendMessage(bcast, false);
   }

   private static Text lambda$list$5(Text line) {
      return line;
   }
}
