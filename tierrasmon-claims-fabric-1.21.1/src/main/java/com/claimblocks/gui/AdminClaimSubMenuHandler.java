package com.claimblocks.gui;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap.Type;

public class AdminClaimSubMenuHandler extends ScreenHandler {
   public static final int SIZE = 54;
   private static final int SLOT_TELEPORT = 11;
   private static final int SLOT_FLAGS = 12;
   private static final int SLOT_DELETE = 13;
   private static final int SLOT_TRANSFER = 15;
   private static final int SLOT_BACK = 22;
   private static final Map<UUID, UUID> pendingTransfers = new HashMap<>();
   private final SimpleInventory inv = new SimpleInventory(54) {
      public boolean canPlayerUse(PlayerEntity player) {
         return true;
      }
   };
   private final ServerPlayerEntity viewer;
   private final UUID claimId;
   private boolean awaitingDeleteConfirm = false;

   public AdminClaimSubMenuHandler(int syncId, PlayerInventory pInv, UUID claimId) {
      super(ScreenHandlerType.GENERIC_9X6, syncId);
      this.viewer = (ServerPlayerEntity)pInv.player;
      this.claimId = claimId;

      for (int row = 0; row < 6; row++) {
         for (int col = 0; col < 9; col++) {
            int idx = col + row * 9;
            this.addSlot(new Slot(this.inv, idx, 8 + col * 18, 18 + row * 18) {
               public boolean canTakeItems(PlayerEntity playerEntity) {
                  return false;
               }

               public boolean canInsert(ItemStack stack) {
                  return false;
               }
            });
         }
      }

      for (int var8 = 0; var8 < 3; var8++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(pInv, col + var8 * 9 + 9, 8 + col * 18, 140 + var8 * 18));
         }
      }

      for (int col2 = 0; col2 < 9; col2++) {
         this.addSlot(new Slot(pInv, col2, 8 + col2 * 18, 198));
      }

      this.rebuild();
   }

   private Claim claim() {
      return AdminPanelHandler.findClaim(this.claimId);
   }

   private void rebuild() {
      this.inv.clear();
      ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));

      for (int i = 0; i < 54; i++) {
         this.inv.setStack(i, bg.copy());
      }

      Claim c = this.claim();
      if (c != null) {
         String owner = c.getOwnerName();
         this.inv
            .setStack(
               11,
               withLore(
                  withName(new ItemStack(Items.COMPASS), Text.literal("Teleportar al claim").formatted(new Formatting[]{Formatting.AQUA, Formatting.BOLD})),
                  List.of(Text.literal("Te lleva al centro del claim de " + owner).formatted(Formatting.GRAY))
               )
            );
         this.inv
            .setStack(
               12,
               withLore(
                  withName(new ItemStack(Items.LEVER), Text.literal("Ver y editar flags").formatted(new Formatting[]{Formatting.YELLOW, Formatting.BOLD})),
                  List.of(Text.literal("Abre el menú de flags de este claim").formatted(Formatting.GRAY))
               )
            );
         if (this.awaitingDeleteConfirm) {
            this.inv
               .setStack(
                  13,
                  withLore(
                     withName(new ItemStack(Items.TNT), Text.literal("¿Confirmar eliminación?").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})),
                     List.of(
                        Text.literal("Esto eliminará la zona de " + owner).formatted(Formatting.YELLOW),
                        Text.literal("El bloque NO se devuelve al dueño").formatted(Formatting.RED),
                        Text.literal("Clic de nuevo para confirmar").formatted(Formatting.GRAY)
                     )
                  )
               );
         } else {
            this.inv
               .setStack(
                  13,
                  withLore(
                     withName(new ItemStack(Items.BARRIER), Text.literal("Eliminar este claim").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})),
                     List.of(
                        Text.literal("Elimina la zona de " + owner).formatted(Formatting.YELLOW),
                        Text.literal("Clic para pedir confirmación").formatted(Formatting.GRAY)
                     )
                  )
               );
         }

         this.inv
            .setStack(
               15,
               withLore(
                  withName(new ItemStack(Items.PAPER), Text.literal("Transferir claim").formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})),
                  List.of(Text.literal("Cambia el dueño de esta zona").formatted(Formatting.GRAY))
               )
            );
         this.inv.setStack(22, withName(new ItemStack(Items.ARROW), Text.literal("Volver al panel").formatted(Formatting.AQUA)));
         this.sendContentUpdates();
      }
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      if (slotIndex >= 0 && slotIndex < 54) {
         Claim c = this.claim();
         if (c == null) {
            this.viewer.closeHandledScreen();
         } else {
            if (slotIndex != 13 && this.awaitingDeleteConfirm) {
               this.awaitingDeleteConfirm = false;
            }

            if (slotIndex == 22) {
               AdminPanelHandler.open(this.viewer, 0);
            } else if (slotIndex == 11) {
               this.teleportToClaim(c);
            } else if (slotIndex == 12) {
               String title = "[Admin] Flags de " + c.getOwnerName() + " - " + c.sizeLabel();
               ClaimMenuHandler.open(this.viewer, c, 0, title);
            } else if (slotIndex == 13) {
               if (!this.awaitingDeleteConfirm) {
                  this.awaitingDeleteConfirm = true;
                  this.rebuild();
               } else {
                  this.adminDelete(c);
               }
            } else if (slotIndex == 15) {
               this.startTransfer(c);
            } else {
               this.rebuild();
            }
         }
      } else if (actionType != SlotActionType.QUICK_MOVE) {
         super.onSlotClick(slotIndex, button, actionType, player);
      }
   }

   private void teleportToClaim(Claim c) {
      ServerWorld world = null;

      for (ServerWorld w : this.viewer.getServer().getWorlds()) {
         if (w.getRegistryKey().getValue().toString().equals(c.getWorld())) {
            world = w;
            break;
         }
      }

      if (world == null) {
         this.viewer.sendMessage(Text.literal("[x] No se pudo encontrar la dimensión.").formatted(Formatting.RED), false);
         this.viewer.closeHandledScreen();
      } else {
         int topY = world.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
         BlockPos target = new BlockPos(c.getX(), topY, c.getZ());
         this.viewer
            .teleport(
               world,
               (double)target.getX() + 0.5,
               (double)target.getY(),
               (double)target.getZ() + 0.5,
               EnumSet.noneOf(PositionFlag.class),
               this.viewer.getYaw(),
               this.viewer.getPitch()
            );
         this.viewer.sendMessage(Text.literal("✔ Teletransportado a la zona de " + c.getOwnerName() + ".").formatted(Formatting.GREEN), false);
         this.viewer.closeHandledScreen();
      }
   }

   private void adminDelete(Claim c) {
      String ownerName = c.getOwnerName();
      UUID ownerId = c.getOwnerUUID();
      ServerWorld world = null;

      for (ServerWorld w : this.viewer.getServer().getWorlds()) {
         if (w.getRegistryKey().getValue().toString().equals(c.getWorld())) {
            world = w;
            break;
         }
      }

      BlockPos pos;
      if (world != null && ClaimBlocks.isClaimConcreteForTier(world.getBlockState(pos = c.getCenter()).getBlock(), c.getTier())) {
         world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
      }

      ClaimManager.getInstance().removeClaim(world, c.getCenter());
      this.viewer
         .sendMessage(Text.literal("✔ Zona de " + ownerName + " eliminada por admin.").formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD}), false);
      ServerPlayerEntity owner = this.viewer.getServer().getPlayerManager().getPlayer(ownerId);
      MutableText msg = Text.literal("[!] Un administrador eliminó tu zona ")
         .formatted(Formatting.YELLOW)
         .append(Text.literal(c.sizeLabel()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
         .append(Text.literal(" en X:" + c.getX() + " Z:" + c.getZ()).formatted(Formatting.YELLOW));
      if (owner != null) {
         owner.sendMessage(msg, false);
      } else {
         ClaimManager.getInstance().queueMessage(ownerId, msg);
      }

      this.viewer.closeHandledScreen();
   }

   private void startTransfer(Claim c) {
      pendingTransfers.put(this.viewer.getUuid(), c.getClaimId());
      this.viewer.sendMessage(Text.literal("[i] Escribe el nombre del nuevo dueño en el chat.").formatted(Formatting.AQUA), false);
      this.viewer.sendMessage(Text.literal("    Escribe 'cancelar' para abortar.").formatted(Formatting.GRAY), false);
      this.viewer.closeHandledScreen();
   }

   public static UUID popPendingTransfer(UUID opId) {
      return pendingTransfers.remove(opId);
   }

   public static void clearPendingTransfer(UUID opId) {
      if (opId != null) {
         pendingTransfers.remove(opId);
      }
   }

   public static boolean hasPendingTransfer(UUID opId) {
      return pendingTransfers.containsKey(opId);
   }

   public ItemStack quickMove(PlayerEntity player, int slot) {
      return ItemStack.EMPTY;
   }

   public boolean canUse(PlayerEntity player) {
      return true;
   }

   private static ItemStack withName(ItemStack s, Text t) {
      s.set(DataComponentTypes.CUSTOM_NAME, t);
      return s;
   }

   private static ItemStack withLore(ItemStack s, List<Text> lore) {
      s.set(DataComponentTypes.LORE, new LoreComponent(lore));
      return s;
   }

   public static void open(ServerPlayerEntity player, UUID claimId) {
      Claim c = AdminPanelHandler.findClaim(claimId);
      if (c == null) {
         player.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
      } else {
         String title = "Admin - " + c.getOwnerName() + " " + c.sizeLabel();
         if (title.length() > 40) {
            title = title.substring(0, 37) + "...";
         }

         player.openHandledScreen(
            new SimpleNamedScreenHandlerFactory(
               (syncId, pInv, plr) -> new AdminClaimSubMenuHandler(syncId, pInv, claimId),
               Text.literal(title).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
            )
         );
      }
   }
}
