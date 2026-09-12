package com.claimblocks.gui;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.render.ParticleBorder;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Selector de particula y densidad para el borde de la zona. */
public class ClaimParticleMenuHandler extends ScreenHandler {
   private static final int[] PARTICLE_SLOTS = new int[]{
      9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35
   };
   private static final int SLOT_TOGGLE = 4;
   private static final int SLOT_BACK = 45;
   private static final int SLOT_DENSITY_DOWN = 48;
   private static final int SLOT_DENSITY_INFO = 49;
   private static final int SLOT_DENSITY_UP = 50;

   private final SimpleInventory chest = new SimpleInventory(54) {
      public boolean canPlayerUse(PlayerEntity player) {
         return true;
      }
   };
   private final Claim claim;
   private final ServerPlayerEntity viewer;
   private final int returnPage;

   public ClaimParticleMenuHandler(int syncId, PlayerInventory pInv, Claim claim, int returnPage) {
      super(ScreenHandlerType.GENERIC_9X6, syncId);
      this.claim = claim;
      this.viewer = (ServerPlayerEntity)pInv.player;
      this.returnPage = returnPage;

      for (int row = 0; row < 6; row++) {
         for (int col = 0; col < 9; col++) {
            int idx = col + row * 9;
            this.addSlot(new Slot(this.chest, idx, 8 + col * 18, 18 + row * 18) {
               public boolean canTakeItems(PlayerEntity playerEntity) {
                  return false;
               }

               public boolean canInsert(ItemStack stack) {
                  return false;
               }
            });
         }
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(pInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(pInv, col, 8 + col * 18, 198));
      }

      this.rebuild();
   }

   public boolean canUse(PlayerEntity player) {
      return true;
   }

   public ItemStack quickMove(PlayerEntity player, int slot) {
      return ItemStack.EMPTY;
   }

   private void rebuild() {
      this.chest.clear();
      ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));

      for (int i = 0; i < 54; i++) {
         this.chest.setStack(i, bg.copy());
      }

      boolean on = this.claim.getFlags().showParticles;
      this.chest
         .setStack(
            SLOT_TOGGLE,
            withLore(
               withName(
                  new ItemStack(on ? Items.LIME_DYE : Items.GRAY_DYE),
                  Text.literal(on ? "Partículas: ACTIVAS" : "Partículas: INACTIVAS")
                     .formatted(new Formatting[]{on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD})
               ),
               List.of(
                  Text.literal("Llena tu protección con partículas.").formatted(Formatting.GRAY),
                  Text.literal("Clic para " + (on ? "desactivar" : "activar")).formatted(Formatting.YELLOW)
               )
            )
         );

      String selected = this.claim.getFlags().borderParticle;
      String[] available = ParticleBorder.availableParticles();

      for (int i = 0; i < available.length && i < PARTICLE_SLOTS.length; i++) {
         String id = available[i];
         boolean isSelected = id.equals(selected);
         this.chest
            .setStack(
               PARTICLE_SLOTS[i],
               withLore(
                  withName(
                     new ItemStack(iconFor(id)),
                     Text.literal(ParticleBorder.particleLabel(id))
                        .formatted(new Formatting[]{isSelected ? Formatting.GREEN : Formatting.AQUA, Formatting.BOLD})
                  ),
                  List.of(
                     Text.literal(isSelected ? "✔ Partícula seleccionada" : "Clic para usar esta partícula")
                        .formatted(isSelected ? Formatting.GREEN : Formatting.GRAY),
                     Text.literal("Activa las partículas automáticamente").formatted(Formatting.DARK_GRAY)
                  )
               )
            );
      }

      int density = this.claim.getFlags().particleDensity;
      this.chest.setStack(SLOT_DENSITY_DOWN, withName(new ItemStack(Items.REDSTONE), Text.literal("- Menos partículas (-5)").formatted(Formatting.RED)));
      this.chest
         .setStack(
            SLOT_DENSITY_INFO,
            withLore(
               withName(
                  new ItemStack(Items.GLOWSTONE_DUST), Text.literal("Densidad: " + density).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
               ),
               List.of(
                  Text.literal("Cantidad de partículas por emisión.").formatted(Formatting.GRAY),
                  Text.literal("Rango 1 - 200. Recomendado 5 - 40.").formatted(Formatting.DARK_GRAY)
               )
            )
         );
      this.chest.setStack(SLOT_DENSITY_UP, withName(new ItemStack(Items.GLOWSTONE), Text.literal("+ Más partículas (+5)").formatted(Formatting.GREEN)));
      this.chest.setStack(SLOT_BACK, withName(new ItemStack(Items.ARROW), Text.literal("<< Volver").formatted(Formatting.AQUA)));
      this.sendContentUpdates();
   }

   private static Item iconFor(String id) {
      return switch (id) {
         case "minecraft:heart" -> Items.POPPY;
         case "minecraft:flame" -> Items.BLAZE_POWDER;
         case "minecraft:small_flame" -> Items.TORCH;
         case "minecraft:soul_fire_flame" -> Items.SOUL_TORCH;
         case "minecraft:soul" -> Items.SOUL_LANTERN;
         case "minecraft:end_rod" -> Items.END_ROD;
         case "minecraft:crit" -> Items.IRON_SWORD;
         case "minecraft:enchanted_hit" -> Items.DIAMOND_SWORD;
         case "minecraft:enchant" -> Items.ENCHANTED_BOOK;
         case "minecraft:dragon_breath" -> Items.DRAGON_BREATH;
         case "minecraft:portal" -> Items.ENDER_PEARL;
         case "minecraft:reverse_portal" -> Items.ENDER_EYE;
         case "minecraft:cloud" -> Items.WHITE_WOOL;
         case "minecraft:electric_spark" -> Items.AMETHYST_SHARD;
         case "minecraft:wax_on" -> Items.HONEYCOMB;
         case "minecraft:glow" -> Items.GLOW_INK_SAC;
         case "minecraft:totem_of_undying" -> Items.TOTEM_OF_UNDYING;
         case "minecraft:firework" -> Items.FIREWORK_ROCKET;
         case "minecraft:note" -> Items.NOTE_BLOCK;
         case "minecraft:snowflake" -> Items.SNOWBALL;
         case "minecraft:cherry_leaves" -> Items.CHERRY_LEAVES;
         case "minecraft:spore_blossom_air" -> Items.SPORE_BLOSSOM;
         case "minecraft:sculk_soul" -> Items.SCULK_CATALYST;
         case "minecraft:lava" -> Items.LAVA_BUCKET;
         case "minecraft:splash" -> Items.WATER_BUCKET;
         case "minecraft:witch" -> Items.FERMENTED_SPIDER_EYE;
         default -> Items.EMERALD;
      };
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      if (slotIndex >= 0 && slotIndex < 54) {
         if (slotIndex == SLOT_TOGGLE) {
            this.claim.getFlags().showParticles = !this.claim.getFlags().showParticles;
            ClaimManager.getInstance().save();
            this.rebuild();
         } else if (slotIndex == SLOT_BACK) {
            ClaimMenuHandler.open(this.viewer, this.claim, this.returnPage);
         } else if (slotIndex == SLOT_DENSITY_DOWN || slotIndex == SLOT_DENSITY_UP) {
            int next = this.claim.getFlags().particleDensity + (slotIndex == SLOT_DENSITY_UP ? 5 : -5);
            this.claim.getFlags().particleDensity = Math.max(1, Math.min(200, next));
            ClaimManager.getInstance().save();
            this.rebuild();
         } else {
            String[] available = ParticleBorder.availableParticles();

            for (int i = 0; i < PARTICLE_SLOTS.length && i < available.length; i++) {
               if (PARTICLE_SLOTS[i] == slotIndex) {
                  this.claim.getFlags().borderParticle = available[i];
                  this.claim.getFlags().showParticles = true;
                  ClaimManager.getInstance().save();
                  this.viewer.sendMessage(Text.literal("✔ Partícula: " + ParticleBorder.particleLabel(available[i])).formatted(Formatting.GREEN), true);
                  this.rebuild();
                  return;
               }
            }
         }
      } else if (actionType != SlotActionType.QUICK_MOVE) {
         super.onSlotClick(slotIndex, button, actionType, player);
      }
   }

   private static ItemStack withName(ItemStack stack, Text name) {
      stack.set(DataComponentTypes.CUSTOM_NAME, name);
      return stack;
   }

   private static ItemStack withLore(ItemStack stack, List<Text> lore) {
      stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
      return stack;
   }

   public static void open(ServerPlayerEntity player, Claim claim, int returnPage) {
      player.openHandledScreen(
         new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new ClaimParticleMenuHandler(syncId, pInv, claim, returnPage),
            Text.literal("Partículas de la protección").formatted(new Formatting[]{Formatting.LIGHT_PURPLE, Formatting.BOLD})
         )
      );
   }
}
