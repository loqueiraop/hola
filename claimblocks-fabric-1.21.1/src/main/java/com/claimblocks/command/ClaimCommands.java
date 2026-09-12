package com.claimblocks.command;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class ClaimCommands {
   private static final SuggestionProvider<ServerCommandSource> CLAIMSTONE_IDS = (context, builder) -> {
      String start = builder.getRemaining().toLowerCase();

      for (ClaimTier t : ClaimTier.VALUES) {
         if (t.id.startsWith(start)) {
            builder.suggest(t.id);
         }
      }

      return builder.buildFuture();
   };

   private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS = (ctx, builder) -> CommandSource.suggestMatching(
      ((ServerCommandSource)ctx.getSource()).getPlayerNames(), builder
   );
   private static final SuggestionProvider<ServerCommandSource> MEMBER_NAMES = (ctx, builder) -> {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayer();
      if (p == null) {
         return builder.buildFuture();
      } else {
         Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
         return c == null ? builder.buildFuture() : CommandSource.suggestMatching(c.getMemberNames(), builder);
      }
   };

   public static void register() {
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
         LiteralCommandNode<ServerCommandSource> root = dispatcher.register(build("claim"));
         dispatcher.register(CommandManager.literal("fsclaim").redirect(root));
      });
   }

   private static LiteralArgumentBuilder<ServerCommandSource> build(String name) {
      return CommandManager.literal(name)
         .executes(ClaimCommands::help)
         .then(CommandManager.literal("help").executes(ClaimCommands::help))
         .then(CommandManager.literal("menu").executes(ClaimCommands::menu))
         .then(CommandManager.literal("info").executes(ClaimCommands::info))
         .then(CommandManager.literal("list").executes(ClaimCommands::list))
         .then(CommandManager.literal("remove").executes(ClaimCommands::remove))
         .then(CommandManager.literal("members").executes(ClaimCommands::members))
         .then(
            CommandManager.literal("addmember")
               .then(CommandManager.argument("jugador", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ClaimCommands::addMember))
         )
         .then(
            CommandManager.literal("delmember")
               .then(CommandManager.argument("jugador", StringArgumentType.word()).suggests(MEMBER_NAMES).executes(ClaimCommands::delMember))
         )
         .then(
            CommandManager.literal("ban")
               .requires(s -> s.hasPermissionLevel(2))
               .then(CommandManager.argument("jugador", EntityArgumentType.player()).executes(ClaimCommands::ban))
         )
         .then(
            CommandManager.literal("unban")
               .requires(s -> s.hasPermissionLevel(2))
               .then(CommandManager.argument("jugador", EntityArgumentType.player()).executes(ClaimCommands::unban))
         )
         .then(
            CommandManager.literal("transfer")
               .requires(s -> s.hasPermissionLevel(2))
               .then(CommandManager.argument("jugador", EntityArgumentType.player()).executes(ClaimCommands::transfer))
         )
         .then(
            CommandManager.literal("removemember")
               .requires(s -> s.hasPermissionLevel(2))
               .then(CommandManager.argument("jugador", EntityArgumentType.player()).executes(ClaimCommands::removeMember))
         )
         .then(
            CommandManager.literal("give")
               .requires(s -> s.hasPermissionLevel(2))
               .then(
                  CommandManager.argument("jugador", EntityArgumentType.players())
                     .then(CommandManager.argument("id", StringArgumentType.word()).suggests(CLAIMSTONE_IDS).executes(ClaimCommands::give))
               )
         )
         .then(
            CommandManager.literal("clear")
               .requires(s -> s.hasPermissionLevel(2))
               .then(CommandManager.argument("jugador", EntityArgumentType.player()).executes(ClaimCommands::clear))
         );
   }

   private static Claim ownedClaimAt(ServerCommandSource src, ServerPlayerEntity player) {
      Claim c = ClaimManager.getInstance().getClaimAt(player.getWorld(), player.getBlockPos());
      if (c == null) {
         src.sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return null;
      } else if (!c.isOwner(player) && !player.hasPermissionLevel(2)) {
         src.sendError(Text.literal("[x] Solo el dueño puede gestionar los miembros de esta zona.").formatted(Formatting.RED));
         return null;
      } else {
         return c;
      }
   }

   private static int addMember(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      Claim c = ownedClaimAt((ServerCommandSource)ctx.getSource(), p);
      if (c == null) {
         return 0;
      } else {
         String name = StringArgumentType.getString(ctx, "jugador");
         return ClaimMenuHandler.addMemberByName(p, c, name, 0, false) ? 1 : 0;
      }
   }

   private static int delMember(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      Claim c = ownedClaimAt((ServerCommandSource)ctx.getSource(), p);
      if (c == null) {
         return 0;
      } else {
         String name = StringArgumentType.getString(ctx, "jugador");
         return ClaimMenuHandler.removeMemberByName(p, c, name, 0, false) ? 1 : 0;
      }
   }

   private static int members(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      Claim c = ownedClaimAt((ServerCommandSource)ctx.getSource(), p);
      if (c == null) {
         return 0;
      } else {
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> {
                  MutableText t = Text.literal("=== Miembros de la zona de " + c.getOwnerName() + " (" + c.getMembers().size() + ") ===\n")
                     .formatted(Formatting.YELLOW);
                  if (c.getMembers().isEmpty()) {
                     t.append(Text.literal("(sin miembros)").formatted(Formatting.DARK_GRAY));
                  } else {
                     for (int i = 0; i < c.getMembers().size(); i++) {
                        String n = i < c.getMemberNames().size() ? c.getMemberNames().get(i) : c.getMembers().get(i).toString();
                        t.append(Text.literal("- " + n + "\n").formatted(Formatting.WHITE));
                     }
                  }

                  return t;
               },
               false
            );
         return c.getMembers().size();
      }
   }

   private static int help(CommandContext<ServerCommandSource> ctx) {
      boolean isOp = ((ServerCommandSource)ctx.getSource()).hasPermissionLevel(2);
      ((ServerCommandSource)ctx.getSource())
         .sendFeedback(
            () -> {
               MutableText t = Text.literal("=== ClaimBlocks Comandos ===\n")
                  .formatted(new Formatting[]{Formatting.YELLOW, Formatting.BOLD})
                  .append(Text.literal("/claim menu  ").formatted(Formatting.AQUA))
                  .append(Text.literal("- abre el menu de la zona donde estas\n").formatted(Formatting.GRAY))
                  .append(Text.literal("/claim info  ").formatted(Formatting.AQUA))
                  .append(Text.literal("- info de la zona donde estas\n").formatted(Formatting.GRAY))
                  .append(Text.literal("/claim list  ").formatted(Formatting.AQUA))
                  .append(Text.literal("- lista tus zonas\n").formatted(Formatting.GRAY))
                  .append(Text.literal("/claim remove  ").formatted(Formatting.AQUA))
                  .append(Text.literal("- borra tu zona actual\n").formatted(Formatting.GRAY));
               if (isOp) {
                  t.append(Text.literal("\n--- Solo Operadores ---\n").formatted(Formatting.RED))
                     .append(Text.literal("/claim give <jugador> <tier>\n").formatted(Formatting.YELLOW))
                     .append(Text.literal("/claim clear <jugador>\n").formatted(Formatting.YELLOW))
                     .append(Text.literal("/claim ban|unban <jugador>\n").formatted(Formatting.YELLOW))
                     .append(Text.literal("/claim transfer <jugador>\n").formatted(Formatting.YELLOW))
                     .append(Text.literal("/claim removemember <jugador>\n").formatted(Formatting.YELLOW))
                     .append(Text.literal("/claimadmin").formatted(Formatting.YELLOW));
               }

               return t;
            },
            false
         );
      return 1;
   }

   private static int give(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "jugador");
      String id = StringArgumentType.getString(ctx, "id");
      ClaimTier tier = ClaimTier.byId(id);
      if (tier == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] ID no válido: " + id).formatted(Formatting.RED));
         return 0;
      } else {
         Block block = ClaimBlocks.blockForTier(tier);

         for (ServerPlayerEntity p : targets) {
            ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
            if (!p.getInventory().insertStack(stack)) {
               p.dropItem(stack, false);
            }

            p.sendMessage(
               Text.literal("[+] ")
                  .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                  .append(Text.literal("Recibiste Piedra de Claim ").formatted(Formatting.GREEN))
                  .append(Text.literal(tier.label()).formatted(new Formatting[]{Formatting.YELLOW, Formatting.BOLD})),
               false
            );
            ((ServerCommandSource)ctx.getSource())
               .sendFeedback(
                  () -> Text.literal("✔ ")
                        .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                        .append(Text.literal("Le diste Piedra ").formatted(Formatting.GREEN))
                        .append(Text.literal(tier.label()).formatted(Formatting.YELLOW))
                        .append(Text.literal(" a ").formatted(Formatting.GREEN))
                        .append(Text.literal(p.getName().getString()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD})),
                  true
               );
         }

         return targets.size();
      }
   }

   private static int clear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
      int n = ClaimManager.getInstance().clearClaimsOf(target.getUuid());
      ((ServerCommandSource)ctx.getSource())
         .sendFeedback(
            () -> Text.literal("✔ ")
                  .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                  .append(Text.literal("Eliminadas " + n + " zona(s) de ").formatted(Formatting.GREEN))
                  .append(Text.literal(target.getName().getString()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD})),
            true
         );
      return n;
   }

   private static int remove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Solo el dueño puede eliminar esta zona.").formatted(Formatting.RED));
         return 0;
      } else {
         BlockPos centre = c.getCenter();
         ClaimTier tier = c.getTier();
         if (tier != null && ClaimBlocks.isClaimConcreteForTier(p.getWorld().getBlockState(centre).getBlock(), tier)) {
            p.getWorld().breakBlock(centre, false, p);
         }

         ClaimManager.getInstance().removeClaim(p.getWorld(), centre);
         p.getWorld().playSound(null, centre, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0F, 1.0F);
         if (tier != null) {
            ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
            if (!p.getInventory().insertStack(stack)) {
               p.dropItem(stack, false);
            }
         }

         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(() -> Text.literal("✔ Zona eliminada. Piedra devuelta a tu inventario.").formatted(Formatting.GREEN), false);
         return 1;
      }
   }

   private static int menu(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Solo el dueño puede administrar esta zona.").formatted(Formatting.RED));
         return 0;
      } else {
         ClaimMenuHandler.open(p, c, 0);
         return 1;
      }
   }

   private static int list(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      List<Claim> claims = ClaimManager.getInstance().getClaimsOf(p.getUuid());
      ((ServerCommandSource)ctx.getSource())
         .sendFeedback(
            () -> Text.literal("[Claim] ").formatted(Formatting.GRAY).append(Text.literal("Tus zonas (" + claims.size() + "):").formatted(Formatting.AQUA)),
            false
         );

      for (Claim c : claims) {
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("  >> ")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal(c.sizeLabel()).formatted(Formatting.YELLOW))
                     .append(Text.literal(" en X=" + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ()).formatted(Formatting.WHITE))
                     .append(Text.literal(" - " + c.getWorld()).formatted(Formatting.DARK_GRAY)),
               false
            );
      }

      if (claims.isEmpty()) {
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("  (no tienes ninguna)").formatted(Formatting.DARK_GRAY), false);
      }

      return claims.size();
   }

   private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else {
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("[Claim] ")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal("Información de la zona:").formatted(new Formatting[]{Formatting.AQUA, Formatting.BOLD})),
               false
            );
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> labelLine("Dueño", c.getOwnerName(), Formatting.WHITE), false);
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(() -> labelLine("Zona", c.sizeLabel() + " bloques | Altura: +/-" + c.getHeight(), Formatting.YELLOW), false);
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(() -> labelLine("Coords", "X=" + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ() + " - " + c.getWorld(), Formatting.WHITE), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> labelLine("Miembros", String.valueOf(c.getMembers().size()), Formatting.WHITE), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> labelLine("Baneados", String.valueOf(c.getBannedPlayers().size()), Formatting.WHITE), false);
         ClaimFlags f = c.getFlags();
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("  Flags activas:").formatted(Formatting.GRAY), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Construir", f.blockBuilding), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Romper", f.blockBreaking), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Explosiones", f.blockExplosions), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Fuego", f.blockFire), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Mobs hostiles", f.blockMobSpawn), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("PVP", f.blockPVP), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Daño de mobs", f.blockMobDamage), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Alertas", f.trespasserAlerts), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Usar items", f.blockItemUse), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Entidades", f.blockEntityInteract), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Cultivos", f.blockTrampling), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Fluidos", f.blockFluids), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("PVP libre", f.pvpAll), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Árboles", f.blockTreeChopping), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Modo visita", f.publicMode), false);
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Bienvenida", f.showWelcome), false);
         ClaimTier tier = c.getTier();
         if (tier != null && tier.isPaid()) {
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Regeneración", f.effectRegeneration), false);
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Resistencia", f.effectResistance), false);
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> formatFlag("Velocidad", f.effectSpeed), false);
         }

         return 1;
      }
   }

   private static Text labelLine(String key, String value, Formatting valueColor) {
      return Text.literal("  " + key + ": ").formatted(Formatting.GRAY).append(Text.literal(value).formatted(valueColor));
   }

   private static Text formatFlag(String name, boolean on) {
      return Text.literal("    " + name + ": ")
         .formatted(Formatting.GRAY)
         .append(Text.literal(on ? "[ON]" : "[OFF]").formatted(new Formatting[]{on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD}));
   }

   private static int ban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity exec = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
      Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Solo el dueño puede banear de esta zona.").formatted(Formatting.RED));
         return 0;
      } else if (target.hasPermissionLevel(2) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No puedes banear a un operador.").formatted(Formatting.RED));
         return 0;
      } else {
         c.banPlayer(target.getUuid());
         ClaimManager.getInstance().save();
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("✔ ")
                     .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                     .append(Text.literal(target.getName().getString()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
                     .append(Text.literal(" baneado.").formatted(Formatting.GREEN)),
               true
            );
         return 1;
      }
   }

   private static int unban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity exec = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
      Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Solo el dueño puede desbanear de esta zona.").formatted(Formatting.RED));
         return 0;
      } else {
         c.unbanPlayer(target.getUuid());
         ClaimManager.getInstance().save();
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("✔ ")
                     .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                     .append(Text.literal(target.getName().getString()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
                     .append(Text.literal(" desbaneado.").formatted(Formatting.GREEN)),
               true
            );
         return 1;
      }
   }

   private static int transfer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity exec = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
      Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Solo el dueño puede transferir esta zona.").formatted(Formatting.RED));
         return 0;
      } else if (target.hasPermissionLevel(2) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No puedes transferir tu zona a un operador.").formatted(Formatting.RED));
         return 0;
      } else if (c.isOwner(target.getUuid())) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Ya es el dueño actual.").formatted(Formatting.RED));
         return 0;
      } else {
         ClaimManager.getInstance().transferOwnership(c, target.getUuid(), target.getName().getString());
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("✔ Zona transferida a ")
                     .formatted(Formatting.GREEN)
                     .append(Text.literal(target.getName().getString()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD})),
               true
            );
         target.sendMessage(
            Text.literal("[Claim] ")
               .formatted(Formatting.GRAY)
               .append(Text.literal("Has recibido la propiedad de una zona en X=" + c.getX() + " Z=" + c.getZ()).formatted(Formatting.GREEN)),
            false
         );
         return 1;
      }
   }

   private static int removeMember(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
      ServerPlayerEntity exec = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
      ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
      Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
      if (c == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No estás en ninguna zona protegida.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] Solo el dueño puede gestionar miembros.").formatted(Formatting.RED));
         return 0;
      } else if (target.hasPermissionLevel(2) && !exec.hasPermissionLevel(2)) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] No puedes gestionar a un operador.").formatted(Formatting.RED));
         return 0;
      } else if (!c.isMember(target.getUuid())) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("[x] " + target.getName().getString() + " no es miembro.").formatted(Formatting.RED));
         return 0;
      } else {
         c.removeMember(target.getUuid());
         ClaimManager.getInstance().save();
         ((ServerCommandSource)ctx.getSource())
            .sendFeedback(
               () -> Text.literal("✔ ")
                     .formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
                     .append(Text.literal(target.getName().getString()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
                     .append(Text.literal(" eliminado de la zona.").formatted(Formatting.GREEN)),
               true
            );
         return 1;
      }
   }
}
