package com.claimblocks.gui;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AdminPanelHandler extends ScreenHandler {
   public static final int SIZE = 54;
   private static final int CLAIMS_PER_PAGE = 45;
   private static final int SLOT_PREV = 45;
   private static final int SLOT_STATS = 46;
   private static final int SLOT_GFLAG = 47;
   private static final int SLOT_BYPASS = 48;
   private static final int SLOT_CLOSE = 49;
   private static final int SLOT_NEXT = 53;
   private final SimpleInventory inv = new SimpleInventory(54) {
      public boolean canPlayerUse(PlayerEntity player) {
         return true;
      }
   };
   private final ServerPlayerEntity viewer;
   private final int page;
   private final List<Claim> claims;

   public AdminPanelHandler(int syncId, PlayerInventory pInv, int page) {
      super(ScreenHandlerType.GENERIC_9X6, syncId);
      this.viewer = (ServerPlayerEntity)pInv.player;
      this.page = page;
      this.claims = ClaimManager.getInstance().getAllClaims();

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

   private void rebuild() {
      this.inv.clear();
      ItemStack bg = withName(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), Text.literal(" "));

      for (int i = 0; i < 54; i++) {
         this.inv.setStack(i, bg.copy());
      }

      int start = this.page * 45;
      int end = Math.min(start + 45, this.claims.size());

      for (int i = start; i < end; i++) {
         Claim c = this.claims.get(i);
         int slot = i - start;
         this.inv.setStack(slot, claimItem(c));
      }

      if (this.page > 0) {
         this.inv.setStack(45, withName(new ItemStack(Items.ARROW), Text.literal("<< Página anterior").formatted(Formatting.AQUA)));
      }

      this.inv
         .setStack(
            46,
            withLore(
               withName(new ItemStack(Items.BOOK), Text.literal("Estadísticas").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})),
               List.of(Text.literal("Resumen del servidor").formatted(Formatting.GRAY))
            )
         );
      this.inv
         .setStack(
            47,
            withLore(
               withName(new ItemStack(Items.LEVER), Text.literal("Flags Globales").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})),
               List.of(Text.literal("PVP / Mob griefing / Fire").formatted(Formatting.GRAY))
            )
         );
      boolean bypassing = ClaimManager.getInstance().isBypassing(this.viewer.getUuid());
      this.inv
         .setStack(
            48,
            withLore(
               withName(
                  new ItemStack(Items.GOLDEN_SWORD),
                  Text.literal("Modo Bypass: " + (bypassing ? "ON" : "OFF"))
                     .formatted(new Formatting[]{bypassing ? Formatting.GREEN : Formatting.RED, Formatting.BOLD})
               ),
               List.of(Text.literal("Ignorar protecciones de zonas").formatted(Formatting.GRAY))
            )
         );
      this.inv.setStack(49, withName(new ItemStack(Items.BARRIER), Text.literal("Cerrar panel").formatted(Formatting.WHITE)));
      if (end < this.claims.size()) {
         this.inv.setStack(53, withName(new ItemStack(Items.ARROW), Text.literal("Página siguiente >>").formatted(Formatting.AQUA)));
      }

      this.sendContentUpdates();
   }

   private static ItemStack claimItem(Claim c) {
      ClaimTier tier = c.getTier();
      Block block = tier != null ? ClaimBlocks.blockForTier(tier) : null;
      ItemStack stack = block != null ? new ItemStack(block.asItem()) : new ItemStack(Items.PAPER);
      MutableText name = Text.literal(c.getOwnerName() + " - " + c.sizeLabel()).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD});
      return withLore(
         withName(stack, name),
         List.of(
            Text.literal("Posición: X:" + c.getX() + " Z:" + c.getZ()).formatted(Formatting.GRAY),
            Text.literal("Dimensión: " + c.getWorld()).formatted(Formatting.DARK_AQUA),
            Text.literal("Clic para gestionar este claim").formatted(Formatting.YELLOW)
         )
      );
   }

   private static ItemStack withName(ItemStack s, Text t) {
      s.set(DataComponentTypes.CUSTOM_NAME, t);
      return s;
   }

   private static ItemStack withLore(ItemStack s, List<Text> lore) {
      s.set(DataComponentTypes.LORE, new LoreComponent(lore));
      return s;
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      if (slotIndex >= 0 && slotIndex < 54) {
         if (slotIndex == 45 && this.page > 0) {
            open(this.viewer, this.page - 1);
         } else if (slotIndex == 53) {
            int max = (this.claims.size() - 1) / 45;
            if (this.page < max) {
               open(this.viewer, this.page + 1);
            }
         } else if (slotIndex == 49) {
            this.viewer.closeHandledScreen();
         } else if (slotIndex == 46) {
            this.viewer.closeHandledScreen();
            this.viewer.getServer().getCommandManager().executeWithPrefix(this.viewer.getCommandSource(), "claimadmin stats");
         } else if (slotIndex == 47) {
            AdminGlobalFlagsHandler.open(this.viewer);
         } else if (slotIndex == 48) {
            ClaimManager.getInstance().toggleBypass(this.viewer.getUuid());
            this.rebuild();
         } else {
            int idx = this.page * 45 + slotIndex;
            if (idx < this.claims.size()) {
               AdminClaimSubMenuHandler.open(this.viewer, this.claims.get(idx).getClaimId());
            }
         }
      } else if (actionType != SlotActionType.QUICK_MOVE) {
         super.onSlotClick(slotIndex, button, actionType, player);
      }
   }

   public ItemStack quickMove(PlayerEntity player, int slot) {
      return ItemStack.EMPTY;
   }

   public boolean canUse(PlayerEntity player) {
      return true;
   }

   public static void open(ServerPlayerEntity player, int page) {
      int p = Math.max(0, page);
      player.openHandledScreen(
         new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new AdminPanelHandler(syncId, pInv, p),
            Text.literal("Panel de Administración").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
         )
      );
   }

   public static Claim findClaim(UUID id) {
      for (Claim c : ClaimManager.getInstance().getAllClaims()) {
         if (c.getClaimId().equals(id)) {
            return c;
         }
      }

      return null;
   }
}
