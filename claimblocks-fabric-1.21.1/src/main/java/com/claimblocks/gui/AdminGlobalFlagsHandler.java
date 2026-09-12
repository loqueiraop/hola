package com.claimblocks.gui;

import com.claimblocks.data.GlobalFlags;
import java.util.List;
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

public class AdminGlobalFlagsHandler extends ScreenHandler {
   public static final int SIZE = 54;
   private static final int SLOT_PVP = 11;
   private static final int SLOT_GRIEF = 13;
   private static final int SLOT_FIRE = 15;
   private static final int SLOT_BACK = 22;
   private final SimpleInventory inv = new SimpleInventory(54) {
      public boolean canPlayerUse(PlayerEntity player) {
         return true;
      }
   };
   private final ServerPlayerEntity viewer;

   public AdminGlobalFlagsHandler(int syncId, PlayerInventory pInv) {
      super(ScreenHandlerType.GENERIC_9X6, syncId);
      this.viewer = (ServerPlayerEntity)pInv.player;

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

      for (int var7 = 0; var7 < 3; var7++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(pInv, col + var7 * 9 + 9, 8 + col * 18, 140 + var7 * 18));
         }
      }

      for (int col2 = 0; col2 < 9; col2++) {
         this.addSlot(new Slot(pInv, col2, 8 + col2 * 18, 198));
      }

      this.rebuild();
   }

   private void rebuild() {
      this.inv.clear();
      ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));

      for (int i = 0; i < 54; i++) {
         this.inv.setStack(i, bg.copy());
      }

      GlobalFlags g = GlobalFlags.getInstance();
      this.inv.setStack(11, flagButton("PVP global", g.globalPVP, "Permite PVP fuera de claims"));
      this.inv.setStack(13, flagButton("Mob griefing global", g.globalMobGriefing, "Mobs destruyen bloques fuera de claims"));
      this.inv.setStack(15, flagButton("Propagación de fuego", g.globalFireSpread, "Fire spread global gamerule"));
      this.inv.setStack(17, flagButton("Sin spawn de mobs (global)", g.globalNoMobSpawn, "Ningún mob spawnea en TODO el servidor"));
      this.inv.setStack(22, withName(new ItemStack(Items.ARROW), Text.literal("Volver al panel").formatted(Formatting.AQUA)));
      this.sendContentUpdates();
   }

   private static ItemStack flagButton(String name, boolean on, String desc) {
      ItemStack stack = new ItemStack(on ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE);
      MutableText title = Text.literal(name + " " + (on ? "[ON]" : "[OFF]"))
         .formatted(new Formatting[]{on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD});
      return withLore(
         withName(stack, title),
         List.of(
            Text.literal(desc).formatted(Formatting.GRAY),
            Text.literal("Estado: " + (on ? "ACTIVO" : "INACTIVO") + " - Clic para cambiar").formatted(Formatting.GRAY)
         )
      );
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      if (slotIndex >= 0 && slotIndex < 54) {
         if (slotIndex == 22) {
            AdminPanelHandler.open(this.viewer, 0);
         } else {
            GlobalFlags g = GlobalFlags.getInstance();
            String name = null;
            boolean newVal = false;
            if (slotIndex == 11) {
               name = "globalPVP";
               newVal = !g.globalPVP;
            }

            if (slotIndex == 13) {
               name = "globalMobGriefing";
               newVal = !g.globalMobGriefing;
            }

            if (slotIndex == 15) {
               name = "globalFireSpread";
               newVal = !g.globalFireSpread;
            }

            if (slotIndex == 17) {
               name = "globalNoMobSpawn";
               newVal = !g.globalNoMobSpawn;
            }

            if (name != null) {
               g.set(name, newVal, this.viewer.getServer());
               MutableText bcast = Text.literal("[!] Un administrador cambió una configuración global del servidor.").formatted(Formatting.YELLOW);
               this.viewer.getServer().getPlayerManager().getPlayerList().forEach(arg_0 -> lambda$onSlotClick$0(bcast, arg_0));
               this.rebuild();
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

   private static ItemStack withName(ItemStack s, Text t) {
      s.set(DataComponentTypes.CUSTOM_NAME, t);
      return s;
   }

   private static ItemStack withLore(ItemStack s, List<Text> lore) {
      s.set(DataComponentTypes.LORE, new LoreComponent(lore));
      return s;
   }

   public static void open(ServerPlayerEntity player) {
      player.openHandledScreen(
         new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new AdminGlobalFlagsHandler(syncId, pInv),
            Text.literal("Flags Globales").formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
         )
      );
   }

   private static void lambda$onSlotClick$0(Text bcast, ServerPlayerEntity p) {
      p.sendMessage(bcast, false);
   }
}
