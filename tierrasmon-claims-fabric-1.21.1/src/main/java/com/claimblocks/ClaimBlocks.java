package com.claimblocks;

import com.claimblocks.data.ClaimTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClaimBlocks {
   public static final String NBT_KEY = "claimblocks";
   public static final String NBT_TIER_FIELD = "tier";

   private ClaimBlocks() {
   }

   public static Block blockForTier(ClaimTier tier) {
      if (tier == null) {
         return Blocks.WHITE_CONCRETE;
      } else {
         String var1 = tier.id;

         return switch (var1) {
            case "claimstone_10x10" -> Blocks.WHITE_CONCRETE;
            case "claimstone_25x25" -> Blocks.LIGHT_GRAY_CONCRETE;
            case "claimstone_40x40" -> Blocks.CYAN_CONCRETE;
            case "claimstone_64x64" -> Blocks.LIGHT_BLUE_CONCRETE;
            case "claimstone_80x80" -> Blocks.LIME_CONCRETE;
            case "claimstone_100x100" -> Blocks.YELLOW_CONCRETE;
            case "claimstone_150x150" -> Blocks.ORANGE_CONCRETE;
            case "claimstone_250x250" -> Blocks.PINK_CONCRETE;
            case "claimstone_300x300" -> Blocks.MAGENTA_CONCRETE;
            case "claimstone_500x500" -> Blocks.PURPLE_CONCRETE;
            default -> Blocks.WHITE_CONCRETE;
         };
      }
   }

   public static Item itemForTier(ClaimTier tier) {
      return blockForTier(tier).asItem();
   }

   public static boolean isClaimConcreteForTier(Block block, ClaimTier tier) {
      return block == blockForTier(tier);
   }

   public static boolean isAnyClaimConcrete(Block block) {
      for (ClaimTier t : ClaimTier.VALUES) {
         if (block == blockForTier(t)) {
            return true;
         }
      }

      return false;
   }

   public static Formatting colorForTier(ClaimTier tier) {
      if (tier == null) {
         return Formatting.WHITE;
      } else {
         String var1 = tier.id;

         return switch (var1) {
            case "claimstone_10x10" -> Formatting.WHITE;
            case "claimstone_25x25" -> Formatting.GRAY;
            case "claimstone_40x40" -> Formatting.AQUA;
            case "claimstone_64x64" -> Formatting.BLUE;
            case "claimstone_80x80" -> Formatting.GREEN;
            case "claimstone_100x100" -> Formatting.YELLOW;
            case "claimstone_150x150" -> Formatting.GOLD;
            case "claimstone_250x250" -> Formatting.LIGHT_PURPLE;
            case "claimstone_300x300" -> Formatting.LIGHT_PURPLE;
            case "claimstone_500x500" -> Formatting.DARK_PURPLE;
            default -> Formatting.WHITE;
         };
      }
   }

   public static ItemStack createTierItem(ClaimTier tier, int amount) {
      ItemStack stack = new ItemStack(itemForTier(tier), amount);
      NbtCompound nbt = new NbtCompound();
      NbtCompound root = new NbtCompound();
      root.putString("tier", tier.id);
      nbt.put("claimblocks", root);
      stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
      stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
      Formatting color = colorForTier(tier);
      stack.set(
         DataComponentTypes.CUSTOM_NAME,
         Text.literal("Piedra de Claim " + tier.label()).formatted(new Formatting[]{color, Formatting.BOLD}).styled(s -> s.withItalic(false))
      );
      List<Text> lore = new ArrayList<>();
      lore.add(Text.literal("Tier: " + tier.id).formatted(Formatting.GRAY));
      lore.add(Text.literal("Radio: " + tier.radius + " | Altura: +/-" + tier.height).formatted(Formatting.DARK_GRAY));
      lore.add(Text.literal("Coloca para crear una zona").formatted(color));
      stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
      return stack;
   }

   public static String readTierId(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         NbtComponent comp = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
         if (comp == null) {
            return null;
         } else {
            NbtCompound nbt = comp.copyNbt();
            if (nbt != null && nbt.contains("claimblocks", 10)) {
               NbtCompound root = nbt.getCompound("claimblocks");
               return !root.contains("tier", 8) ? null : root.getString("tier");
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static ClaimTier readTier(ItemStack stack) {
      String id = readTierId(stack);
      return id == null ? null : ClaimTier.byId(id);
   }
}
