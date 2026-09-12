package com.claimblocks.gui;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.util.PlayerLookup;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Menu con cabezas de los jugadores conectados para anadirlos como miembros con un clic. */
public class MemberSelectMenu extends ScreenHandler {
   private static final int SIZE = 54;
   private static final int ENTRIES_PER_PAGE = 45;
   private static final int SLOT_PREV = 45;
   private static final int SLOT_BY_NAME = 48;
   private static final int SLOT_BACK = 49;
   private static final int SLOT_INFO = 50;
   private static final int SLOT_NEXT = 53;

   private final SimpleInventory chest = new SimpleInventory(54) {
      public boolean canPlayerUse(PlayerEntity player) {
         return true;
      }
   };
   private final Claim claim;
   private final ServerPlayerEntity viewer;
   private final int returnPage;
   private int page;
   private List<ServerPlayerEntity> candidates;

   public MemberSelectMenu(int syncId, PlayerInventory pInv, Claim claim, int returnPage, int page) {
      super(ScreenHandlerType.GENERIC_9X6, syncId);
      this.claim = claim;
      this.viewer = (ServerPlayerEntity)pInv.player;
      this.returnPage = returnPage;
      this.page = page;

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

   private List<ServerPlayerEntity> collectCandidates() {
      ArrayList<ServerPlayerEntity> out = new ArrayList<>();
      if (this.viewer.getServer() == null) {
         return out;
      } else {
         for (ServerPlayerEntity p : this.viewer.getServer().getPlayerManager().getPlayerList()) {
            if (!this.claim.isOwner(p.getUuid()) && !this.claim.isMember(p.getUuid())) {
               out.add(p);
            }
         }

         out.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase()));
         return out;
      }
   }

   private void rebuild() {
      this.candidates = this.collectCandidates();
      this.chest.clear();
      ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));

      for (int i = 0; i < 54; i++) {
         this.chest.setStack(i, bg.copy());
      }

      int start = this.page * ENTRIES_PER_PAGE;
      int end = Math.min(start + ENTRIES_PER_PAGE, this.candidates.size());
      if (this.candidates.isEmpty()) {
         this.chest
            .setStack(
               22,
               withLore(
                  withName(new ItemStack(Items.BARRIER), Text.literal("No hay jugadores disponibles").formatted(Formatting.RED)),
                  List.of(
                     Text.literal("Todos los conectados ya son miembros,").formatted(Formatting.GRAY),
                     Text.literal("o eres el único en el servidor.").formatted(Formatting.GRAY),
                     Text.literal("Usa \"Escribir nombre\" para alguien offline.").formatted(Formatting.YELLOW)
                  )
               )
            );
      } else {
         for (int i = start; i < end; i++) {
            this.chest.setStack(i - start, playerHead(this.candidates.get(i)));
         }
      }

      if (this.page > 0) {
         this.chest.setStack(SLOT_PREV, withName(new ItemStack(Items.ARROW), Text.literal("<< Página anterior").formatted(Formatting.AQUA)));
      }

      this.chest
         .setStack(
            SLOT_BY_NAME,
            withLore(
               withName(new ItemStack(Items.NAME_TAG), Text.literal("Escribir nombre").formatted(new Formatting[]{Formatting.YELLOW, Formatting.BOLD})),
               List.of(
                  Text.literal("Para añadir a alguien que NO está conectado.").formatted(Formatting.GRAY),
                  Text.literal("Se pide el nombre por chat.").formatted(Formatting.GRAY)
               )
            )
         );
      this.chest.setStack(SLOT_BACK, withName(new ItemStack(Items.ARROW), Text.literal("Volver al menú de la zona").formatted(Formatting.AQUA)));
      this.chest
         .setStack(
            SLOT_INFO,
            withLore(
               withName(
                  new ItemStack(Items.PAPER),
                  Text.literal("Miembros: " + this.claim.getMembers().size()).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
               ),
               List.of(Text.literal("Clic en una cabeza para añadir a ese jugador.").formatted(Formatting.GRAY))
            )
         );
      if (end < this.candidates.size()) {
         this.chest.setStack(SLOT_NEXT, withName(new ItemStack(Items.ARROW), Text.literal("Página siguiente >>").formatted(Formatting.AQUA)));
      }

      this.sendContentUpdates();
   }

   private static ItemStack playerHead(ServerPlayerEntity player) {
      String name = player.getName().getString();
      ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
      stack.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));
      return withLore(
         withName(stack, Text.literal(name).formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})),
         List.of(Text.literal("Clic para añadirlo como miembro").formatted(Formatting.GRAY))
      );
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      if (slotIndex >= 0 && slotIndex < 54) {
         if (slotIndex == SLOT_BACK) {
            ClaimMenuHandler.open(this.viewer, this.claim, this.returnPage);
         } else if (slotIndex == SLOT_PREV && this.page > 0) {
            this.page--;
            this.rebuild();
         } else if (slotIndex == SLOT_NEXT) {
            int maxPage = Math.max(0, (this.candidates.size() - 1) / ENTRIES_PER_PAGE);
            if (this.page < maxPage) {
               this.page++;
               this.rebuild();
            }
         } else if (slotIndex == SLOT_BY_NAME) {
            ClaimMenuHandler.requestAddMember(this.viewer, this.claim, this.returnPage);
            this.viewer.closeHandledScreen();
         } else if (slotIndex < ENTRIES_PER_PAGE) {
            int index = this.page * ENTRIES_PER_PAGE + slotIndex;
            if (index < this.candidates.size()) {
               if (ClaimManager.getInstance().findClaimById(this.claim.getClaimId()) == null) {
                  this.viewer.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
                  this.viewer.closeHandledScreen();
                  return;
               }

               ServerPlayerEntity target = this.candidates.get(index);
               PlayerLookup.Resolved resolved = PlayerLookup.resolve(this.viewer.getServer(), target.getName().getString());
               if (resolved == null) {
                  resolved = new PlayerLookup.Resolved(target.getUuid(), target.getName().getString(), target);
               }

               ClaimMenuHandler.addMemberResolved(this.viewer, this.claim, resolved);
               this.rebuild();
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

   public static void open(ServerPlayerEntity player, Claim claim, int returnPage, int page) {
      int p = Math.max(0, page);
      player.openHandledScreen(
         new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new MemberSelectMenu(syncId, pInv, claim, returnPage, p),
            Text.literal("Añadir miembro").formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})
         )
      );
   }
}
