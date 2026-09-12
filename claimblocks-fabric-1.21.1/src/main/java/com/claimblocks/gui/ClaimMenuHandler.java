package com.claimblocks.gui;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.chat.ChatPromptRouter;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimConfig;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimGroup;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.util.PlayerLookup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents.AllowChatMessage;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClaimMenuHandler extends ScreenHandler {
   public static final int SIZE = 54;
   private static final int SLOT_TITLE = 4;
   private static final int SLOT_COORDS = 11;
   private static final int SLOT_OWNER = 13;
   private static final int SLOT_TIER = 15;
   private static final int SLOT_WORLD = 17;
   private static final int SLOT_VIEW_MEMBERS = 38;
   private static final int SLOT_BAN = 39;
   private static final int SLOT_REMOVE_MEMBER = 40;
   private static final int SLOT_UNBAN = 41;
   private static final int SLOT_ADD_MEMBER = 42;
   private static final int SLOT_MERGE = 43;
   private static final int SLOT_DISSOLVE = 44;
   private static final int SLOT_PREV = 45;
   private static final int SLOT_DELETE = 46;
   private static final int SLOT_CANCEL_DEL = 47;
   private static final int SLOT_CLOSE = 49;
   private static final int SLOT_LIST = 52;
   private static final int SLOT_NEXT = 53;

   private static final int[] FLAG_SLOTS_P0 = new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31};
   private static final int[] FLAG_SLOTS_P1 = new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 27};
   private static final int[] FLAG_SLOTS_P2 = new int[]{20, 22, 24};
   private static final ClaimFlags.FlagId[] PAGE_0 = new ClaimFlags.FlagId[]{
      ClaimFlags.FlagId.BUILDING,
      ClaimFlags.FlagId.BREAKING,
      ClaimFlags.FlagId.EXPLOSIONS,
      ClaimFlags.FlagId.FIRE,
      ClaimFlags.FlagId.MOB_SPAWN,
      ClaimFlags.FlagId.PVP,
      ClaimFlags.FlagId.MOB_DAMAGE,
      ClaimFlags.FlagId.ALERTS,
      ClaimFlags.FlagId.PUBLIC_MODE,
      ClaimFlags.FlagId.ANIMAL_KILLING,
      ClaimFlags.FlagId.CHEST_ACCESS,
      ClaimFlags.FlagId.CROP_HARVEST,
      ClaimFlags.FlagId.BURN_HOSTILES
   };
   private static final ClaimFlags.FlagId[] PAGE_1 = new ClaimFlags.FlagId[]{
      ClaimFlags.FlagId.ITEM_USE,
      ClaimFlags.FlagId.ENTITY_INTERACT,
      ClaimFlags.FlagId.TRAMPLING,
      ClaimFlags.FlagId.FLUIDS,
      ClaimFlags.FlagId.PVP_ALL,
      ClaimFlags.FlagId.TREE_CHOPPING,
      ClaimFlags.FlagId.SHOW_WELCOME,
      ClaimFlags.FlagId.ANVIL_USE,
      ClaimFlags.FlagId.ENDER_PEARL,
      ClaimFlags.FlagId.SIGN_EDITING,
      ClaimFlags.FlagId.DOORS_ACCESS,
      ClaimFlags.FlagId.EFFECT_REGEN,
      ClaimFlags.FlagId.EFFECT_RESIST,
      ClaimFlags.FlagId.EFFECT_SPEED,
      ClaimFlags.FlagId.ALLOW_FLIGHT,
      ClaimFlags.FlagId.SHOW_LEAVE,
      ClaimFlags.FlagId.SHOW_BORDER,
      ClaimFlags.FlagId.SHOW_PARTICLES
   };
   private static final ClaimFlags.FlagId[] PAGE_2 = new ClaimFlags.FlagId[]{
      ClaimFlags.FlagId.ALL_MOB_SPAWN, ClaimFlags.FlagId.PASSIVE_MOB_SPAWN, ClaimFlags.FlagId.BLOCK_ALL_INTERACT
   };
   private static final ClaimFlags.FlagId[][] PAGES = new ClaimFlags.FlagId[][]{PAGE_0, PAGE_1, PAGE_2};
   private static final int[][] PAGE_SLOTS = new int[][]{FLAG_SLOTS_P0, FLAG_SLOTS_P1, FLAG_SLOTS_P2};
   private static final int LAST_PAGE = PAGES.length - 1;

   private static final Map<UUID, ClaimMenuHandler.PendingChat> pending = new ConcurrentHashMap<>();
   private static final Map<UUID, String> pendingMergeName = new ConcurrentHashMap<>();
   private static final Map<String, ClaimMenuHandler.MergeInvite> invites = new ConcurrentHashMap<>();

   private final SimpleInventory chest = new SimpleInventory(54) {
      public boolean canPlayerUse(PlayerEntity player) {
         return true;
      }
   };
   private final Claim claim;
   private final ServerPlayerEntity viewer;
   private int page;
   private boolean awaitingDeleteConfirm = false;

   public ClaimMenuHandler(int syncId, PlayerInventory pInv, Claim claim, int page) {
      super(ScreenHandlerType.GENERIC_9X6, syncId);
      this.claim = claim;
      this.viewer = (ServerPlayerEntity)pInv.player;
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

   public Claim getClaim() {
      return this.claim;
   }

   public int getPage() {
      return this.page;
   }

   private void rebuild() {
      this.chest.clear();
      ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));

      for (int i = 0; i < 54; i++) {
         this.chest.setStack(i, bg.copy());
      }

      ClaimGroup group = ClaimManager.getInstance().getGroupOf(this.claim);
      String header = group != null ? "Grupo: " + group.getName() : "Zona " + this.claim.sizeLabel() + " - " + this.claim.getOwnerName();
      this.chest
         .setStack(
            SLOT_TITLE,
            withName(new ItemStack(Items.PAPER), Text.literal(truncate(header, 30)).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD}))
         );
      this.chest
         .setStack(
            SLOT_COORDS,
            withLore(
               withName(new ItemStack(Items.COMPASS), Text.literal("Coordenadas").formatted(Formatting.AQUA)),
               List.of(Text.literal("X=" + this.claim.getX() + " Y=" + this.claim.getY() + " Z=" + this.claim.getZ()).formatted(Formatting.WHITE))
            )
         );
      this.chest
         .setStack(
            SLOT_OWNER,
            withLore(
               withName(new ItemStack(Items.PLAYER_HEAD), Text.literal("Dueño").formatted(Formatting.AQUA)),
               List.of(Text.literal(truncate(this.claim.getOwnerName(), 35)).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
            )
         );
      this.chest
         .setStack(
            SLOT_TIER,
            withLore(
               withName(new ItemStack(Items.DIAMOND), Text.literal("Zona " + this.claim.sizeLabel()).formatted(Formatting.YELLOW)),
               List.of(
                  Text.literal(truncate("Zona " + this.claim.sizeLabel() + " bloques", 35)).formatted(Formatting.GRAY),
                  Text.literal(truncate("Altura: +/-" + this.claim.getHeight(), 35)).formatted(Formatting.GRAY)
               )
            )
         );
      this.chest
         .setStack(
            SLOT_WORLD,
            withLore(
               withName(new ItemStack(Items.MAP), Text.literal("Mundo").formatted(Formatting.AQUA)),
               List.of(Text.literal(truncate(this.claim.getWorld(), 35)).formatted(Formatting.GRAY))
            )
         );

      ClaimFlags f = this.claim.getFlags();
      ClaimFlags.FlagId[] ids = PAGES[this.pageIndex()];
      int[] slots = PAGE_SLOTS[this.pageIndex()];
      int tierLevel = paidLevelOf(this.claim.getTier());

      for (int i = 0; i < ids.length; i++) {
         ClaimFlags.FlagId id = ids[i];
         int reqLevel = requiredPaidLevel(id);
         if (reqLevel > 0 && tierLevel < reqLevel) {
            this.chest.setStack(slots[i], this.lockedEffectButton(id, reqLevel));
         } else {
            this.chest.setStack(slots[i], this.flagButton(id, f.get(id)));
         }
      }

      this.chest
         .setStack(
            SLOT_VIEW_MEMBERS,
            withLore(
               withName(new ItemStack(Items.WRITABLE_BOOK), Text.literal("Miembros (" + this.claim.getMembers().size() + ")").formatted(Formatting.YELLOW)),
               this.buildMemberLore()
            )
         );
      this.chest
         .setStack(
            SLOT_REMOVE_MEMBER,
            withLore(
               withName(new ItemStack(Items.NAME_TAG), Text.literal("Quitar miembro").formatted(Formatting.RED)),
               List.of(
                  Text.literal("Pide nombre por chat").formatted(Formatting.GRAY),
                  Text.literal("Clic para eliminar a un invitado").formatted(Formatting.GRAY)
               )
            )
         );
      this.chest
         .setStack(
            SLOT_ADD_MEMBER,
            withLore(
               withName(new ItemStack(Items.PLAYER_HEAD), Text.literal("Añadir miembro").formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})),
               List.of(
                  Text.literal("Clic izq: elegir de una lista").formatted(Formatting.GRAY),
                  Text.literal("Clic der: escribir el nombre por chat").formatted(Formatting.GRAY),
                  Text.literal("También sirve /claim addmember <jugador>").formatted(Formatting.DARK_GRAY)
               )
            )
         );
      this.chest
         .setStack(
            SLOT_BAN,
            withLore(
               withName(new ItemStack(Items.IRON_BARS), Text.literal("Banear jugador").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})),
               this.buildBanLore()
            )
         );
      this.chest
         .setStack(
            SLOT_UNBAN,
            withLore(
               withName(new ItemStack(Items.TRIPWIRE_HOOK), Text.literal("Desbanear jugador").formatted(Formatting.GREEN)),
               List.of(
                  Text.literal("Pide nombre por chat").formatted(Formatting.GRAY),
                  Text.literal("Clic para quitar del baneo").formatted(Formatting.GRAY)
               )
            )
         );

      if (this.page > 0) {
         this.chest.setStack(SLOT_PREV, withName(new ItemStack(Items.ARROW), Text.literal("<< Página anterior").formatted(Formatting.AQUA)));
      }

      if (this.awaitingDeleteConfirm) {
         this.chest
            .setStack(
               SLOT_DELETE,
               withLore(
                  withName(new ItemStack(Items.TNT), Text.literal("Confirmar eliminación").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})),
                  List.of(Text.literal("Haz clic de nuevo para confirmar").formatted(Formatting.YELLOW))
               )
            );
         this.chest
            .setStack(
               SLOT_CANCEL_DEL,
               withLore(
                  withName(new ItemStack(Items.LIME_DYE), Text.literal("Cancelar").formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD})),
                  List.of(Text.literal("Cancela la eliminación").formatted(Formatting.GRAY))
               )
            );
      } else {
         this.chest
            .setStack(
               SLOT_DELETE,
               withLore(
                  withName(new ItemStack(Items.BARRIER), Text.literal("Eliminar zona").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})),
                  List.of(
                     Text.literal("Clic para iniciar eliminación").formatted(Formatting.YELLOW),
                     Text.literal("Devuelve la protección al inv.").formatted(Formatting.GRAY)
                  )
               )
            );
      }

      this.chest.setStack(SLOT_CLOSE, withName(new ItemStack(Items.RED_DYE), Text.literal("Cerrar").formatted(Formatting.WHITE)));
      this.chest.setStack(SLOT_LIST, withName(new ItemStack(Items.BOOK), Text.literal("Ver lista de zonas").formatted(Formatting.AQUA)));
      if (this.page < LAST_PAGE) {
         this.chest
            .setStack(
               SLOT_NEXT,
               withLore(
                  withName(new ItemStack(Items.ARROW), Text.literal("Página siguiente >>").formatted(Formatting.AQUA)),
                  List.of(Text.literal("Página " + (this.page + 1) + " de " + (LAST_PAGE + 1)).formatted(Formatting.DARK_GRAY))
               )
            );
      }

      if (group == null) {
         this.chest
            .setStack(
               SLOT_MERGE,
               withLore(
                  withName(
                     new ItemStack(Items.SLIME_BALL), Text.literal("Unir protección").formatted(new Formatting[]{Formatting.LIGHT_PURPLE, Formatting.BOLD})
                  ),
                  List.of(
                     Text.literal("Crea un grupo y une zonas de tu equipo").formatted(Formatting.GRAY),
                     Text.literal("Clic: elegir nombre e invitar jugadores").formatted(Formatting.GRAY)
                  )
               )
            );
      } else {
         this.chest
            .setStack(
               SLOT_MERGE,
               withLore(
                  withName(
                     new ItemStack(Items.SLIME_BALL),
                     Text.literal(truncate("Grupo: " + group.getName(), 30)).formatted(new Formatting[]{Formatting.LIGHT_PURPLE, Formatting.BOLD})
                  ),
                  List.of(
                     Text.literal("Miembros registrados: " + group.getRegisteredPlayers().size()).formatted(Formatting.GRAY),
                     Text.literal("Clic: invitar mas jugadores").formatted(Formatting.GRAY)
                  )
               )
            );
         this.chest
            .setStack(
               SLOT_DISSOLVE,
               withLore(
                  withName(new ItemStack(Items.SHEARS), Text.literal("Disolver grupo").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD})),
                  List.of(
                     Text.literal("Separa todas las piedras del grupo").formatted(Formatting.GRAY),
                     Text.literal("Cada zona vuelve a ser independiente").formatted(Formatting.GRAY)
                  )
               )
            );
      }

      this.sendContentUpdates();
   }

   private List<Text> buildMemberLore() {
      ArrayList<Text> lore = new ArrayList<>();
      if (this.claim.getMembers().isEmpty()) {
         lore.add(Text.literal("(sin miembros)").formatted(Formatting.DARK_GRAY));
         return lore;
      } else {
         int max = Math.min(5, this.claim.getMembers().size());

         for (int i = 0; i < max; i++) {
            String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
            lore.add(Text.literal(truncate(" - " + n, 35)).formatted(Formatting.WHITE));
         }

         if (this.claim.getMembers().size() > max) {
            lore.add(Text.literal(" - ... y " + (this.claim.getMembers().size() - max) + " más").formatted(Formatting.GRAY));
         }

         return lore;
      }
   }

   private List<Text> buildBanLore() {
      ArrayList<Text> lore = new ArrayList<>();
      lore.add(Text.literal("Escribe el nombre por chat para banear.").formatted(Formatting.GRAY));
      lore.add(Text.literal("Si entran, la barrera los saca de la zona.").formatted(Formatting.DARK_GRAY));
      Set<UUID> banned = this.claim.getBannedPlayers();
      lore.add(Text.literal("Baneados: " + banned.size()).formatted(new Formatting[]{Formatting.RED, Formatting.BOLD}));
      int i = 0;

      for (UUID id : banned) {
         if (i++ >= 8) {
            lore.add(Text.literal(" - ...").formatted(Formatting.GRAY));
            break;
         }

         lore.add(Text.literal(truncate(" - " + PlayerLookup.nameOf(this.viewer.getServer(), id), 35)).formatted(Formatting.WHITE));
      }

      return lore;
   }

   private static int paidLevelOf(ClaimTier t) {
      if (t == null) {
         return 0;
      } else {
         return switch (t.id) {
            case "claimstone_250x250" -> 1;
            case "claimstone_300x300" -> 2;
            case "claimstone_500x500" -> 3;
            default -> 0;
         };
      }
   }

   private static int requiredPaidLevel(ClaimFlags.FlagId id) {
      return switch (id) {
         case EFFECT_REGEN -> 1;
         case EFFECT_RESIST -> 2;
         case EFFECT_SPEED -> 2;
         case ALLOW_FLIGHT -> 3;
         default -> 0;
      };
   }

   private static String requiredTierLabel(int reqLevel) {
      return switch (reqLevel) {
         case 1 -> "250x250";
         case 2 -> "300x300";
         case 3 -> "500x500";
         default -> "?";
      };
   }

   private ItemStack lockedEffectButton(ClaimFlags.FlagId id, int reqLevel) {
      ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
      return withLore(
         withName(stack, Text.literal(effectName(id) + " [LOCKED]").formatted(Formatting.DARK_GRAY)),
         List.of(
            Text.literal("Requiere zona " + requiredTierLabel(reqLevel) + " o superior").formatted(Formatting.GRAY),
            Text.literal(effectShortDesc(id)).formatted(Formatting.DARK_GRAY)
         )
      );
   }

   private static String effectShortDesc(ClaimFlags.FlagId id) {
      return switch (id) {
         case EFFECT_REGEN -> "Regenera vida a duenio y miembros";
         case EFFECT_RESIST -> "Reduce dano a duenio y miembros";
         case EFFECT_SPEED -> "Da velocidad a duenio y miembros";
         case ALLOW_FLIGHT -> "El duenio y los miembros pueden volar en la zona";
         default -> "Perk pasivo";
      };
   }

   private static String effectName(ClaimFlags.FlagId id) {
      return switch (id) {
         case EFFECT_REGEN -> "Regeneración pasiva";
         case EFFECT_RESIST -> "Resistencia pasiva";
         case EFFECT_SPEED -> "Velocidad pasiva";
         case ALLOW_FLIGHT -> "Vuelo en zona";
         default -> "Perk pasivo";
      };
   }

   private ItemStack flagButton(ClaimFlags.FlagId id, boolean enabled) {
      ItemStack stack = new ItemStack(enabled ? Items.LIME_DYE : Items.GRAY_DYE);
      MutableText name = Text.literal(flagDisplayName(id, enabled)).formatted(new Formatting[]{enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD});
      String[] lore = flagLore(id);
      return withLore(
         withName(stack, name),
         List.of(
            Text.literal(lore[0]).formatted(Formatting.GRAY),
            Text.literal("Estado: " + (enabled ? "ACTIVO" : "INACTIVO") + " - " + lore[1]).formatted(Formatting.GRAY)
         )
      );
   }

   private static String flagDisplayName(ClaimFlags.FlagId id, boolean on) {
      return switch (id) {
         case EFFECT_REGEN -> on ? "Regeneración pasiva [ON]" : "Regeneración pasiva [OFF]";
         case EFFECT_RESIST -> on ? "Resistencia pasiva [ON]" : "Resistencia pasiva [OFF]";
         case EFFECT_SPEED -> on ? "Velocidad pasiva [ON]" : "Velocidad pasiva [OFF]";
         case ALLOW_FLIGHT -> on ? "Vuelo en zona: ACTIVO [ON]" : "Vuelo en zona: inactivo [OFF]";
         case BUILDING -> on ? "Construir: BLOQUEADO [ON]" : "Construir: permitido [OFF]";
         case BREAKING -> on ? "Romper: BLOQUEADO [ON]" : "Romper: permitido [OFF]";
         case EXPLOSIONS -> on ? "Explosiones: BLOQUEADAS [ON]" : "Explosiones: permitidas [OFF]";
         case FIRE -> on ? "Fuego: BLOQUEADO [ON]" : "Fuego: permitido [OFF]";
         case MOB_SPAWN -> on ? "Mobs hostiles: BLOQUEADOS [ON]" : "Mobs hostiles: permit. [OFF]";
         case PVP -> on ? "PVP: BLOQUEADO [ON]" : "PVP: permitido [OFF]";
         case MOB_DAMAGE -> on ? "Daño de mobs: BLOQUEADO [ON]" : "Daño de mobs: permit. [OFF]";
         case ALERTS -> on ? "Alertas intrusos: ON [ON]" : "Alertas intrusos: OFF [OFF]";
         case ITEM_USE -> on ? "Usar items: BLOQUEADO [ON]" : "Usar items: permitido [OFF]";
         case ENTITY_INTERACT -> on ? "Entidades: BLOQUEADAS [ON]" : "Entidades: libres [OFF]";
         case TRAMPLING -> on ? "Cultivos: PROTEGIDOS [ON]" : "Cultivos: sin protec. [OFF]";
         case FLUIDS -> on ? "Fluidos: BLOQUEADOS [ON]" : "Fluidos: permitidos [OFF]";
         case PVP_ALL -> on ? "Zona PVP libre: ACTIVA [ON]" : "Zona PVP libre: inact. [OFF]";
         case TREE_CHOPPING -> on ? "Árboles: PROTEGIDOS [ON]" : "Árboles: se talan [OFF]";
         case PUBLIC_MODE -> on ? "Modo visita: ACTIVO [ON]" : "Modo visita: inactivo [OFF]";
         case SHOW_WELCOME -> on ? "Bienvenida custom: ON [ON]" : "Bienvenida custom: OFF [OFF]";
         case SHOW_LEAVE -> on ? "Mensaje de salida: ON [ON]" : "Mensaje de salida: OFF [OFF]";
         case SHOW_BORDER -> on ? "Ver contorno: ON [ON]" : "Ver contorno: OFF [OFF]";
         case SHOW_PARTICLES -> on ? "Ver partículas: ON [ON]" : "Ver partículas: OFF [OFF]";
         case BURN_HOSTILES -> on ? "Repeler hostiles: ON [ON]" : "Repeler hostiles: OFF [OFF]";
         case ANIMAL_KILLING -> on ? "Animales: PROTEGIDOS [ON]" : "Animales: se matan [OFF]";
         case CHEST_ACCESS -> on ? "Cofres: BLOQUEADOS [ON]" : "Cofres: acceso libre [OFF]";
         case CROP_HARVEST -> on ? "Cosecha: PROTEGIDA [ON]" : "Cosecha: libre [OFF]";
         case ANVIL_USE -> on ? "Yunques: BLOQUEADOS [ON]" : "Yunques: uso libre [OFF]";
         case ENDER_PEARL -> on ? "Ender pearl: BLOQUEADA [ON]" : "Ender pearl: permitida [OFF]";
         case SIGN_EDITING -> on ? "Letreros: BLOQUEADOS [ON]" : "Letreros: editables [OFF]";
         case DOORS_ACCESS -> on ? "Puertas/Botones: BLOQ [ON]" : "Puertas/Botones: libres [OFF]";
         case ALL_MOB_SPAWN -> on ? "Spawn de mobs: BLOQUEADO [ON]" : "Spawn de mobs: permitido [OFF]";
         case PASSIVE_MOB_SPAWN -> on ? "Animales: NO spawnean [ON]" : "Animales: spawnean [OFF]";
         case BLOCK_ALL_INTERACT -> on ? "Interacción total: BLOQ [ON]" : "Interacción total: libre [OFF]";
      };
   }

   private static String[] flagLore(ClaimFlags.FlagId id) {
      String desc = switch (id) {
         case EFFECT_REGEN -> "Regenera vida a dueño y miembros";
         case EFFECT_RESIST -> "Reduce daño a dueño y miembros";
         case EFFECT_SPEED -> "Da velocidad a dueño y miembros";
         case ALLOW_FLIGHT -> "Dueño y miembros pueden volar";
         case BUILDING -> "Intrusos no pueden colocar bloques";
         case BREAKING -> "Intrusos no pueden romper nada";
         case EXPLOSIONS -> "TNT y creepers no destruyen";
         case FIRE -> "El fuego no se propaga aquí";
         case MOB_SPAWN -> "Zombies, skeletons no spawnean";
         case PVP -> "Jugadores no pueden atacarse";
         case MOB_DAMAGE -> "Los mobs no dañan a jugadores";
         case ALERTS -> "Avisa al dueño cuando entran";
         case ITEM_USE -> "Intrusos no pueden usar items";
         case ENTITY_INTERACT -> "Intrusos no usan mobs/aldeanos";
         case TRAMPLING -> "Intrusos no destruyen la tierra";
         case FLUIDS -> "Nadie coloca agua ni lava aquí";
         case PVP_ALL -> "Todos se pueden atacar aquí";
         case TREE_CHOPPING -> "Intrusos no pueden talar árboles";
         case PUBLIC_MODE -> "Todos entran pero no modifican";
         case SHOW_WELCOME -> "Mensaje personalizado al entrar";
         case SHOW_LEAVE -> "Mensaje personalizado al salir";
         case SHOW_BORDER -> "Aristas de polvo + pared que parpadea al acercarte al limite";
         case SHOW_PARTICLES -> "Llena tu protección con partículas";
         case BURN_HOSTILES -> "Quema a los mobs hostiles que entren (día o noche)";
         case ANIMAL_KILLING -> "Intrusos no pueden matar animales";
         case CHEST_ACCESS -> "Intrusos no abren cofres ni barriles";
         case CROP_HARVEST -> "Intrusos no cosechan cultivos";
         case ANVIL_USE -> "Intrusos no pueden usar yunques";
         case ENDER_PEARL -> "Intrusos no se teletransportan";
         case SIGN_EDITING -> "Intrusos no editan letreros";
         case DOORS_ACCESS -> "Intrusos no usan puertas, botones ni placas";
         case ALL_MOB_SPAWN -> "Nada spawnea aquí: hostiles, animales y mobs de otros mods";
         case PASSIVE_MOB_SPAWN -> "Animales, peces y murciélagos dejan de spawnear (aldeanos no)";
         case BLOCK_ALL_INTERACT -> "Intrusos no pueden interactuar con NADA en la zona";
      };
      String hint = id != ClaimFlags.FlagId.SHOW_WELCOME && id != ClaimFlags.FlagId.SHOW_LEAVE
         ? (id == ClaimFlags.FlagId.SHOW_PARTICLES ? "Clic para elegir partícula y densidad" : "Clic para cambiar")
         : "Clic izq: editar | Clic der: on/off";
      return new String[]{desc, hint};
   }

   private static ItemStack withName(ItemStack stack, Text name) {
      stack.set(DataComponentTypes.CUSTOM_NAME, name);
      return stack;
   }

   private static ItemStack withLore(ItemStack stack, List<Text> lore) {
      stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
      return stack;
   }

   private static String truncate(String s, int max) {
      if (s == null) {
         return "";
      } else {
         return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
      }
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      if (ClaimManager.getInstance().findClaimById(this.claim.getClaimId()) == null) {
         this.viewer.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
         this.viewer.closeHandledScreen();
      } else if (slotIndex >= 0 && slotIndex < 54) {
         if (slotIndex == SLOT_PREV && this.page > 0) {
            this.page--;
            this.awaitingDeleteConfirm = false;
            this.rebuild();
         } else if (slotIndex == SLOT_NEXT && this.page < LAST_PAGE) {
            this.page++;
            this.awaitingDeleteConfirm = false;
            this.rebuild();
         } else if (slotIndex == SLOT_DELETE) {
            if (!this.awaitingDeleteConfirm) {
               this.awaitingDeleteConfirm = true;
               this.rebuild();
               this.viewer.sendMessage(Text.literal("[!] Haz clic de nuevo para confirmar.").formatted(Formatting.YELLOW), true);
            } else {
               this.performDelete();
            }
         } else if (slotIndex == SLOT_CANCEL_DEL && this.awaitingDeleteConfirm) {
            this.awaitingDeleteConfirm = false;
            this.rebuild();
            this.viewer.sendMessage(Text.literal("[i] Eliminación cancelada.").formatted(Formatting.AQUA), true);
         } else {
            if (this.awaitingDeleteConfirm) {
               this.awaitingDeleteConfirm = false;
            }

            ClaimFlags.FlagId clicked = this.slotToFlag(slotIndex);
            if (clicked != null) {
               int reqLevel = requiredPaidLevel(clicked);
               if (reqLevel > 0 && paidLevelOf(this.claim.getTier()) < reqLevel) {
                  this.viewer.sendMessage(Text.literal("[x] Requiere zona " + requiredTierLabel(reqLevel) + " o superior.").formatted(Formatting.RED), true);
                  return;
               }

               if (clicked == ClaimFlags.FlagId.SHOW_WELCOME) {
                  if (button == 1) {
                     this.claim.getFlags().showWelcome = !this.claim.getFlags().showWelcome;
                     ClaimManager.getInstance().save();
                     this.refreshFlagSlot(slotIndex, clicked);
                  } else {
                     requestEditWelcome(this.viewer, this.claim, this.page);
                     this.viewer.closeHandledScreen();
                  }
               } else if (clicked == ClaimFlags.FlagId.SHOW_LEAVE) {
                  if (button == 1) {
                     this.claim.getFlags().showLeave = !this.claim.getFlags().showLeave;
                     ClaimManager.getInstance().save();
                     this.refreshFlagSlot(slotIndex, clicked);
                  } else {
                     requestEditLeave(this.viewer, this.claim, this.page);
                     this.viewer.closeHandledScreen();
                  }
               } else if (clicked == ClaimFlags.FlagId.SHOW_BORDER) {
                  this.claim.getFlags().showBorder = !this.claim.getFlags().showBorder;
                  ClaimManager.getInstance().save();
                  this.refreshFlagSlot(slotIndex, clicked);
               } else if (clicked == ClaimFlags.FlagId.SHOW_PARTICLES) {
                  ClaimParticleMenuHandler.open(this.viewer, this.claim, this.page);
               } else {
                  this.claim.getFlags().toggle(clicked);
                  ClaimManager.getInstance().save();
                  this.refreshFlagSlot(slotIndex, clicked);
               }
            } else if (slotIndex == SLOT_VIEW_MEMBERS) {
               this.viewer.sendMessage(Text.literal("[Claim] Miembros de la zona:").formatted(Formatting.GRAY), false);
               if (this.claim.getMembers().isEmpty()) {
                  this.viewer.sendMessage(Text.literal("  (sin miembros)").formatted(Formatting.DARK_GRAY), false);
               } else {
                  for (int i = 0; i < this.claim.getMembers().size(); i++) {
                     String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
                     this.viewer.sendMessage(Text.literal("  - " + n).formatted(Formatting.WHITE), false);
                  }
               }
            } else if (slotIndex == SLOT_ADD_MEMBER) {
               if (button == 1) {
                  requestAddMember(this.viewer, this.claim, this.page);
                  this.viewer.closeHandledScreen();
               } else {
                  MemberSelectMenu.open(this.viewer, this.claim, this.page, 0);
               }
            } else if (slotIndex == SLOT_REMOVE_MEMBER) {
               if (this.claim.getMembers().isEmpty()) {
                  this.viewer.sendMessage(Text.literal("[i] Esta zona no tiene miembros que quitar.").formatted(Formatting.YELLOW), true);
               } else {
                  requestRemoveMember(this.viewer, this.claim, this.page);
                  this.viewer.closeHandledScreen();
               }
            } else if (slotIndex == SLOT_BAN) {
               requestBanPlayer(this.viewer, this.claim, this.page);
               this.viewer.closeHandledScreen();
            } else if (slotIndex == SLOT_UNBAN) {
               if (this.claim.getBannedPlayers().isEmpty()) {
                  this.viewer.sendMessage(Text.literal("[i] No hay jugadores baneados.").formatted(Formatting.YELLOW), true);
               } else {
                  requestUnbanPlayer(this.viewer, this.claim, this.page);
                  this.viewer.closeHandledScreen();
               }
            } else if (slotIndex == SLOT_MERGE) {
               ClaimGroup group = ClaimManager.getInstance().getGroupOf(this.claim);
               if (group == null) {
                  requestMergeName(this.viewer, this.claim, this.page);
                  this.viewer.closeHandledScreen();
               } else if (this.claim.isGroupMother()) {
                  requestMergeUsers(this.viewer, this.claim, this.page);
                  this.viewer.closeHandledScreen();
               }
            } else if (slotIndex == SLOT_DISSOLVE) {
               ClaimGroup group = ClaimManager.getInstance().getGroupOf(this.claim);
               if (group != null && this.claim.isGroupMother()) {
                  ClaimManager.getInstance().dissolveGroupBreaking(group.getGroupId());
                  this.viewer
                     .sendMessage(Text.literal("✔ Grupo disuelto. Las piedras solapadas se devolvieron a sus duenos.").formatted(Formatting.GREEN), false);
                  this.rebuild();
               }
            } else if (slotIndex == SLOT_CLOSE) {
               this.viewer.closeHandledScreen();
            } else if (slotIndex == SLOT_LIST) {
               this.viewer.closeHandledScreen();
               this.viewer.getServer().getCommandManager().executeWithPrefix(this.viewer.getCommandSource(), "claim list");
            }
         }
      } else if (actionType != SlotActionType.QUICK_MOVE) {
         super.onSlotClick(slotIndex, button, actionType, player);
      }
   }

   private void performDelete() {
      World world = this.viewer.getWorld();
      ClaimTier tier = this.claim.getTier();
      BlockPos centre = this.claim.getCenter();
      if (tier != null && ClaimBlocks.isClaimConcreteForTier(world.getBlockState(centre).getBlock(), tier)) {
         world.breakBlock(centre, false, this.viewer);
      }

      world.playSound(null, centre, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 2.0F, 1.0F);
      ClaimManager.getInstance().removeClaim(world, centre);
      if (tier != null) {
         ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
         if (!this.viewer.getInventory().insertStack(stack)) {
            this.viewer.dropItem(stack, false);
         }
      }

      this.viewer.sendMessage(Text.literal("✔ Zona eliminada. Protección devuelta a tu inventario.").formatted(Formatting.GREEN), false);
      this.viewer.closeHandledScreen();
   }

   /** Refresca solo la casilla de la flag pulsada, para no reenviar el menu completo. */
   private void refreshFlagSlot(int slotIndex, ClaimFlags.FlagId id) {
      int reqLevel = requiredPaidLevel(id);
      if (reqLevel > 0 && paidLevelOf(this.claim.getTier()) < reqLevel) {
         this.chest.setStack(slotIndex, this.lockedEffectButton(id, reqLevel));
      } else {
         this.chest.setStack(slotIndex, this.flagButton(id, this.claim.getFlags().get(id)));
      }

      this.sendContentUpdates();
   }

   private int pageIndex() {
      return Math.max(0, Math.min(LAST_PAGE, this.page));
   }

   private ClaimFlags.FlagId slotToFlag(int slotIndex) {
      ClaimFlags.FlagId[] ids = PAGES[this.pageIndex()];
      int[] slots = PAGE_SLOTS[this.pageIndex()];

      for (int i = 0; i < slots.length; i++) {
         if (slots[i] == slotIndex) {
            return ids[i];
         }
      }

      return null;
   }

   public ItemStack quickMove(PlayerEntity player, int slot) {
      return ItemStack.EMPTY;
   }

   public boolean canUse(PlayerEntity player) {
      return true;
   }

   public static void open(ServerPlayerEntity player, Claim claim, int page) {
      open(player, claim, page, null);
   }

   public static void open(ServerPlayerEntity player, Claim claim, int page, String customTitle) {
      if (claim.getGroupId() != null && !claim.isGroupMother()) {
         Claim mother = claim.getMother();
         String motherName = mother != null ? mother.getOwnerName() : "?";
         player.sendMessage(
            Text.literal(
                  "[!] Esta piedra pertenece al grupo de "
                     + motherName
                     + ". Solo la piedra nodriza gestiona el grupo. Puedes romperla para recuperarla."
               )
               .formatted(Formatting.YELLOW),
            false
         );
      } else {
         int p = Math.max(0, Math.min(LAST_PAGE, page));
         ClaimGroup group = ClaimManager.getInstance().getGroupOf(claim);
         String title = customTitle != null
            ? truncate(customTitle, 40)
            : (group != null ? truncate("Grupo: " + group.getName(), 40) : truncate("Zona " + claim.sizeLabel() + " - " + claim.getOwnerName(), 40));
         player.openHandledScreen(
            new SimpleNamedScreenHandlerFactory(
               (syncId, pInv, plr) -> new ClaimMenuHandler(syncId, pInv, claim, p),
               Text.literal(title).formatted(new Formatting[]{Formatting.GOLD, Formatting.BOLD})
            )
         );
      }
   }

   public static void requestAddMember(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.ADD_MEMBER, claim.getClaimId(), returnPage));
      player.sendMessage(Text.literal("[Claim] Escribe el nombre del jugador a añadir (o 'cancelar'):").formatted(Formatting.YELLOW), false);
      player.sendMessage(
         Text.literal("    No hace falta que esté conectado. Alternativa: /claim addmember <jugador>").formatted(Formatting.DARK_GRAY), false
      );
   }

   public static void requestRemoveMember(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.REMOVE_MEMBER, claim.getClaimId(), returnPage));
      StringBuilder sb = new StringBuilder();
      List<String> names = claim.getMemberNames();

      for (int i = 0; i < names.size(); i++) {
         if (i > 0) {
            sb.append(", ");
         }

         sb.append(names.get(i));
      }

      player.sendMessage(Text.literal("[Claim] Miembros: ").formatted(Formatting.GRAY).append(Text.literal(sb.toString()).formatted(Formatting.WHITE)), false);
      player.sendMessage(Text.literal("[Claim] Escribe el nombre del invitado a quitar (o 'cancelar'):").formatted(Formatting.YELLOW), false);
   }

   public static void requestEditWelcome(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.EDIT_WELCOME, claim.getClaimId(), returnPage));
      player.sendMessage(
         Text.literal("[Claim] Escribe tu bienvenida (max " + ClaimConfig.get().maxWelcomeLength + " chars) o 'cancelar':").formatted(Formatting.YELLOW),
         false
      );
   }

   public static void requestEditLeave(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.EDIT_LEAVE, claim.getClaimId(), returnPage));
      player.sendMessage(
         Text.literal("[Claim] Escribe tu mensaje de salida (max " + ClaimConfig.get().maxWelcomeLength + " chars) o 'cancelar':").formatted(Formatting.YELLOW),
         false
      );
   }

   public static void requestBanPlayer(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.BAN_PLAYER, claim.getClaimId(), returnPage));
      player.sendMessage(Text.literal("[Claim] Escribe el nombre del jugador a BANEAR (o 'cancelar'):").formatted(Formatting.YELLOW), false);
   }

   public static void requestUnbanPlayer(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.UNBAN_PLAYER, claim.getClaimId(), returnPage));
      player.sendMessage(Text.literal("[Claim] Escribe el nombre del jugador a DESBANEAR (o 'cancelar'):").formatted(Formatting.YELLOW), false);
   }

   public static void requestMergeName(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.MERGE_NAME, claim.getClaimId(), returnPage));
      player.sendMessage(Text.literal("[Grupo] Escribe el NOMBRE de la zona unida (o 'cancelar'):").formatted(Formatting.LIGHT_PURPLE), false);
   }

   public static void requestMergeUsers(ServerPlayerEntity player, Claim claim, int returnPage) {
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.MERGE_USERS, claim.getClaimId(), returnPage));
      player.sendMessage(
         Text.literal("[Grupo] Escribe el/los jugadores a invitar (separados por espacio) o 'cancelar':").formatted(Formatting.LIGHT_PURPLE), false
      );
   }

   public static boolean hasPrompt(UUID id) {
      if (id == null) {
         return false;
      } else {
         ClaimMenuHandler.PendingChat p = pending.get(id);
         if (p == null) {
            return false;
         } else if (p.isExpired()) {
            pending.remove(id, p);
            return false;
         } else {
            return true;
         }
      }
   }

   public static ClaimMenuHandler.PendingChat popPrompt(UUID id) {
      if (id == null) {
         return null;
      } else {
         ClaimMenuHandler.PendingChat p = pending.remove(id);
         return p != null && !p.isExpired() ? p : null;
      }
   }

   public static void clearPrompt(UUID id) {
      if (id != null) {
         pending.remove(id);
         pendingMergeName.remove(id);
      }
   }

   /**
    * Fallback por si el mixin de paquete no llega a inyectar: tambien escuchamos el evento
    * de chat de Fabric API.
    */
   public static void registerChatListener() {
      ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((AllowChatMessage)(message, sender, params) -> {
         if (sender == null) {
            return true;
         } else {
            String raw = message.getContent().getString();
            if (ChatPromptRouter.consume(sender, raw)) {
               return false;
            } else {
               return !ChatPromptRouter.shouldSuppress(sender.getUuid(), raw);
            }
         }
      });
   }

   public static void dispatchPrompt(ServerPlayerEntity player, ClaimMenuHandler.PendingChat prompt, String text) {
      if (player != null && prompt != null && !player.isDisconnected()) {
         if (ChatPromptRouter.isCancel(text)) {
            player.sendMessage(Text.literal("[Claim] Cancelado.").formatted(Formatting.GRAY), false);
         } else {
            Claim claim = findClaimById(prompt.claimId());
            if (claim == null) {
               player.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
            } else {
               switch (prompt.type()) {
                  case ADD_MEMBER -> handleAddMember(player, claim, text, prompt.returnPage());
                  case REMOVE_MEMBER -> handleRemoveMember(player, claim, text, prompt.returnPage());
                  case EDIT_WELCOME -> handleEditWelcome(player, claim, text, prompt.returnPage());
                  case EDIT_LEAVE -> handleEditLeave(player, claim, text, prompt.returnPage());
                  case BAN_PLAYER -> handleBanPlayer(player, claim, text, prompt.returnPage());
                  case UNBAN_PLAYER -> handleUnbanPlayer(player, claim, text, prompt.returnPage());
                  case MERGE_NAME -> handleMergeName(player, claim, text, prompt.returnPage());
                  case MERGE_USERS -> handleMergeUsers(player, claim, text, prompt.returnPage());
               }
            }
         }
      }
   }

   public static void dispatchAdminTransfer(ServerPlayerEntity player, UUID claimId, String text) {
      if (player != null && !player.isDisconnected()) {
         if (ChatPromptRouter.isCancel(text)) {
            player.sendMessage(Text.literal("[Claim] Cancelado.").formatted(Formatting.GRAY), false);
         } else {
            String name = ChatPromptRouter.extractPlayerName(text);
            PlayerLookup.resolveAsync(player.getServer(), name, resolved -> {
               if (!player.isDisconnected()) {
                  applyAdminTransfer(player, claimId, name, resolved);
               }
            });
         }
      }
   }

   private static void applyAdminTransfer(ServerPlayerEntity op, UUID claimId, String name, PlayerLookup.Resolved resolved) {
      Claim claim = findClaimById(claimId);
      if (claim == null) {
         op.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
      } else if (resolved == null) {
         op.sendMessage(Text.literal("[x] Jugador no encontrado: " + name).formatted(Formatting.RED), false);
      } else {
         claim.setOwner(resolved.id(), resolved.name());
         claim.getMembers().clear();
         claim.getMemberNames().clear();
         ClaimManager.getInstance().save();
         op.sendMessage(Text.literal("✔ Zona transferida a " + resolved.name() + ".").formatted(Formatting.GREEN), false);
         MutableText msg = Text.literal("[!] Un administrador te transfirió una zona ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(claim.sizeLabel()).formatted(new Formatting[]{Formatting.WHITE, Formatting.BOLD}))
            .append(Text.literal(" en X:" + claim.getX() + " Z:" + claim.getZ()).formatted(Formatting.YELLOW));
         if (resolved.isOnline()) {
            resolved.online().sendMessage(msg, false);
         } else {
            ClaimManager.getInstance().queueMessage(resolved.id(), msg);
         }
      }
   }

   private static void handleAddMember(ServerPlayerEntity player, Claim claim, String text, int page) {
      addMemberByName(player, claim, text, page, true);
   }

   public static boolean addMemberByName(ServerPlayerEntity player, Claim claim, String text, int page, boolean reopen) {
      String name = ChatPromptRouter.extractPlayerName(text);
      UUID claimId = claim.getClaimId();
      PlayerLookup.resolveAsync(player.getServer(), name, resolved -> {
         if (!player.isDisconnected()) {
            Claim current = findClaimById(claimId);
            if (current == null) {
               player.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
            } else {
               if (resolved == null) {
                  player.sendMessage(
                     Text.literal("[x] No encuentro al jugador \"" + name + "\". Revisa el nombre; si nunca ha entrado al servidor, no puedo resolverlo.")
                        .formatted(Formatting.RED),
                     false
                  );
               } else {
                  addMemberResolved(player, current, resolved);
               }

               if (reopen) {
                  open(player, current, page);
               }
            }
         }
      });
      return true;
   }

   public static boolean addMemberResolved(ServerPlayerEntity player, Claim claim, PlayerLookup.Resolved resolved) {
      if (claim.isOwner(resolved.id())) {
         player.sendMessage(Text.literal("[x] Ese jugador ya es el dueño.").formatted(Formatting.RED), false);
         return false;
      } else if (claim.isMember(resolved.id())) {
         player.sendMessage(Text.literal("[i] " + resolved.name() + " ya es miembro de esta zona.").formatted(Formatting.YELLOW), false);
         return false;
      } else {
         int max = ClaimConfig.get().maxMembersPerClaim;
         if (max > 0 && claim.getMembers().size() >= max) {
            player.sendMessage(Text.literal("[x] Esta zona ya tiene el maximo de miembros (" + max + ").").formatted(Formatting.RED), false);
            return false;
         } else {
            if (claim.isBanned(resolved.id())) {
               claim.unbanPlayer(resolved.id());
               player.sendMessage(
                  Text.literal("[i] " + resolved.name() + " estaba baneado de la zona; se le quitó el baneo.").formatted(Formatting.YELLOW), false
               );
            }

            claim.addMember(resolved.id(), resolved.name());
            ClaimManager.getInstance().save();
            player.sendMessage(Text.literal("✔ " + resolved.name() + " agregado como miembro de la zona.").formatted(Formatting.GREEN), false);
            MutableText msg = Text.literal("[Claim] Eres miembro de la zona de " + player.getName().getString()).formatted(Formatting.AQUA);
            if (resolved.isOnline()) {
               resolved.online().sendMessage(msg, false);
            } else {
               ClaimManager.getInstance().queueMessage(resolved.id(), msg);
            }

            return true;
         }
      }
   }

   private static void handleRemoveMember(ServerPlayerEntity player, Claim claim, String text, int page) {
      removeMemberByName(player, claim, text, page, true);
   }

   public static boolean removeMemberByName(ServerPlayerEntity player, Claim claim, String text, int page, boolean reopen) {
      String name = ChatPromptRouter.extractPlayerName(text);
      UUID target = null;
      String shownName = name;

      for (int i = 0; i < claim.getMemberNames().size() && i < claim.getMembers().size(); i++) {
         if (claim.getMemberNames().get(i).equalsIgnoreCase(name)) {
            target = claim.getMembers().get(i);
            shownName = claim.getMemberNames().get(i);
            break;
         }
      }

      if (target == null) {
         PlayerLookup.Resolved resolved = PlayerLookup.resolve(player.getServer(), name);
         if (resolved != null && claim.isMember(resolved.id())) {
            target = resolved.id();
            shownName = resolved.name();
         }
      }

      if (target == null) {
         player.sendMessage(Text.literal("[x] " + name + " no es miembro de esta zona.").formatted(Formatting.RED), false);
         if (reopen) {
            open(player, claim, page);
         }

         return false;
      } else {
         claim.removeMember(target);
         ClaimManager.getInstance().save();
         player.sendMessage(Text.literal("✔ " + shownName + " fue eliminado de la zona.").formatted(Formatting.GREEN), false);
         MutableText msg = Text.literal("[Claim] Ya no eres miembro de la zona de " + player.getName().getString()).formatted(Formatting.YELLOW);
         ServerPlayerEntity online = player.getServer() == null ? null : player.getServer().getPlayerManager().getPlayer(target);
         if (online != null) {
            online.sendMessage(msg, false);
         } else {
            ClaimManager.getInstance().queueMessage(target, msg);
         }

         if (reopen) {
            open(player, claim, page);
         }

         return true;
      }
   }

   private static void handleEditWelcome(ServerPlayerEntity player, Claim claim, String text, int page) {
      int max = ClaimConfig.get().maxWelcomeLength;
      if (text.length() > max) {
         text = text.substring(0, max);
      }

      claim.getFlags().welcomeMessage = text;
      claim.getFlags().showWelcome = !text.isBlank();
      ClaimManager.getInstance().save();
      player.sendMessage(Text.literal("✔ Bienvenida guardada.").formatted(Formatting.GREEN), false);
      open(player, claim, page);
   }

   private static void handleEditLeave(ServerPlayerEntity player, Claim claim, String text, int page) {
      int max = ClaimConfig.get().maxWelcomeLength;
      if (text.length() > max) {
         text = text.substring(0, max);
      }

      claim.getFlags().leaveMessage = text;
      claim.getFlags().showLeave = !text.isBlank();
      ClaimManager.getInstance().save();
      player.sendMessage(Text.literal("✔ Mensaje de salida guardado.").formatted(Formatting.GREEN), false);
      open(player, claim, page);
   }

   private static void handleBanPlayer(ServerPlayerEntity player, Claim claim, String text, int page) {
      String name = ChatPromptRouter.extractPlayerName(text);
      UUID claimId = claim.getClaimId();
      PlayerLookup.resolveAsync(player.getServer(), name, resolved -> {
         if (!player.isDisconnected()) {
            Claim current = findClaimById(claimId);
            if (current == null) {
               player.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
            } else if (resolved == null) {
               player.sendMessage(Text.literal("[x] Jugador no encontrado: " + name).formatted(Formatting.RED), false);
               open(player, current, page);
            } else if (current.isOwner(resolved.id())) {
               player.sendMessage(Text.literal("[x] No puedes banear al dueño.").formatted(Formatting.RED), false);
               open(player, current, page);
            } else {
               current.banPlayer(resolved.id());
               ClaimManager.getInstance().save();
               player.sendMessage(Text.literal("✔ " + resolved.name() + " baneado de la zona.").formatted(Formatting.GREEN), false);
               MutableText msg = Text.literal("[!] Has sido baneado de una zona de " + player.getName().getString())
                  .formatted(new Formatting[]{Formatting.RED, Formatting.BOLD});
               if (resolved.isOnline()) {
                  resolved.online().sendMessage(msg, false);
               } else {
                  ClaimManager.getInstance().queueMessage(resolved.id(), msg);
               }

               open(player, current, page);
            }
         }
      });
   }

   private static void handleUnbanPlayer(ServerPlayerEntity player, Claim claim, String text, int page) {
      String name = ChatPromptRouter.extractPlayerName(text);
      UUID claimId = claim.getClaimId();
      PlayerLookup.resolveAsync(player.getServer(), name, resolved -> {
         if (!player.isDisconnected()) {
            Claim current = findClaimById(claimId);
            if (current == null) {
               player.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
            } else {
               if (resolved != null && current.isBanned(resolved.id())) {
                  current.unbanPlayer(resolved.id());
                  ClaimManager.getInstance().save();
                  player.sendMessage(Text.literal("✔ " + resolved.name() + " desbaneado.").formatted(Formatting.GREEN), false);
               } else {
                  player.sendMessage(Text.literal("[x] Ese jugador no está baneado.").formatted(Formatting.RED), false);
               }

               open(player, current, page);
            }
         }
      });
   }

   private static void handleMergeName(ServerPlayerEntity player, Claim claim, String text, int page) {
      String name = text.length() > 32 ? text.substring(0, 32) : text;
      pendingMergeName.put(player.getUuid(), name);
      pending.put(player.getUuid(), new ClaimMenuHandler.PendingChat(ClaimMenuHandler.PendingType.MERGE_USERS, claim.getClaimId(), page));
      player.sendMessage(
         Text.literal("[Grupo] Nombre: \"" + name + "\". Ahora escribe el/los jugadores a invitar (separados por espacio):")
            .formatted(Formatting.LIGHT_PURPLE),
         false
      );
   }

   private static void handleMergeUsers(ServerPlayerEntity player, Claim claim, String text, int page) {
      ClaimManager mgr = ClaimManager.getInstance();
      ClaimGroup group = mgr.getGroupOf(claim);
      if (group == null) {
         String name = pendingMergeName.getOrDefault(player.getUuid(), "Grupo");
         group = mgr.createGroup(claim, name);
      }

      pendingMergeName.remove(player.getUuid());
      String[] parts = ChatPromptRouter.sanitize(text).split("[ ,]+");
      int sent = 0;

      for (String part : parts) {
         String target = part.trim();
         if (!target.isEmpty()) {
            ServerPlayerEntity online = player.getServer().getPlayerManager().getPlayer(target);
            if (online == null) {
               player.sendMessage(Text.literal("[x] " + target + " no esta en linea (debe estar conectado para invitarlo).").formatted(Formatting.RED), false);
            } else if (!online.getUuid().equals(player.getUuid())) {
               if (group.isRegistered(online.getUuid())) {
                  player.sendMessage(Text.literal("[i] " + online.getName().getString() + " ya esta en el grupo.").formatted(Formatting.GRAY), false);
               } else {
                  String code = genCode();
                  invites.put(code, new ClaimMenuHandler.MergeInvite(code, group.getGroupId(), online.getUuid(), player.getName().getString(), group.getName()));
                  sendInvite(online, player.getName().getString(), group.getName(), code);
                  sent++;
               }
            }
         }
      }

      if (sent > 0) {
         player.sendMessage(
            Text.literal("✔ Invitacion enviada a " + sent + " jugador(es). Grupo: \"" + group.getName() + "\".").formatted(Formatting.GREEN), false
         );
      }

      open(player, claim, page);
   }

   private static void sendInvite(ServerPlayerEntity target, String inviter, String groupName, String code) {
      target.sendMessage(Text.literal("[Grupo] " + inviter + " te invita a unir tu proteccion al grupo \"" + groupName + "\".").formatted(Formatting.AQUA), false);
      MutableText accept = Text.literal(" [✔ ACEPTAR] ")
         .setStyle(
            Style.EMPTY.withColor(Formatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claimmerge accept " + code))
         );
      MutableText reject = Text.literal("[✘ RECHAZAR]")
         .setStyle(
            Style.EMPTY.withColor(Formatting.RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claimmerge reject " + code))
         );
      target.sendMessage(Text.literal("").append(accept).append(reject), false);
   }

   public static void acceptMerge(ServerPlayerEntity player, String code) {
      ClaimMenuHandler.MergeInvite invite = invites.remove(code);
      if (invite != null && player.getUuid().equals(invite.targetId())) {
         ClaimManager mgr = ClaimManager.getInstance();
         ClaimGroup group = mgr.getGroup(invite.groupId());
         if (group == null) {
            player.sendMessage(Text.literal("[x] El grupo ya no existe.").formatted(Formatting.RED), false);
         } else {
            mgr.registerPlayer(group.getGroupId(), player.getUuid());
            player.sendMessage(
               Text.literal("✔ Te uniste al grupo \"" + group.getName() + "\". Ahora tus piedras colocadas dentro de esa zona se uniran.")
                  .formatted(Formatting.GREEN),
               false
            );
            MutableText msg = Text.literal(player.getName().getString() + " acepto unirse al grupo \"" + group.getName() + "\".").formatted(Formatting.GREEN);
            ServerPlayerEntity mother = group.getMotherOwnerId() == null ? null : player.getServer().getPlayerManager().getPlayer(group.getMotherOwnerId());
            if (mother != null) {
               mother.sendMessage(msg, false);
            } else if (group.getMotherOwnerId() != null) {
               mgr.queueMessage(group.getMotherOwnerId(), msg);
            }
         }
      } else {
         player.sendMessage(Text.literal("[x] Invitacion no valida o expirada.").formatted(Formatting.RED), false);
      }
   }

   public static void rejectMerge(ServerPlayerEntity player, String code) {
      ClaimMenuHandler.MergeInvite invite = invites.remove(code);
      if (invite != null && player.getUuid().equals(invite.targetId())) {
         player.sendMessage(Text.literal("[i] Rechazaste la invitacion de union.").formatted(Formatting.GRAY), false);
         ClaimGroup group = ClaimManager.getInstance().getGroup(invite.groupId());
         if (group != null && group.getMotherOwnerId() != null) {
            MutableText msg = Text.literal(player.getName().getString() + " rechazo unirse al grupo \"" + group.getName() + "\".")
               .formatted(Formatting.YELLOW);
            ServerPlayerEntity mother = player.getServer().getPlayerManager().getPlayer(group.getMotherOwnerId());
            if (mother != null) {
               mother.sendMessage(msg, false);
            } else {
               ClaimManager.getInstance().queueMessage(group.getMotherOwnerId(), msg);
            }
         }
      } else {
         player.sendMessage(Text.literal("[x] Invitacion no valida o expirada.").formatted(Formatting.RED), false);
      }
   }

   public static void leaveMerge(ServerPlayerEntity player) {
      ClaimManager mgr = ClaimManager.getInstance();
      ClaimGroup group = mgr.getGroupByRegistered(player.getUuid());
      if (group == null) {
         player.sendMessage(Text.literal("[!] No estas en ningun grupo.").formatted(Formatting.YELLOW), false);
      } else {
         boolean isMother = player.getUuid().equals(group.getMotherOwnerId());
         String name = group.getName();
         mgr.leaveGroupBreaking(group.getGroupId(), player.getUuid());
         player.sendMessage(
            Text.literal(isMother ? "✔ Disolviste el grupo \"" + name + "\"." : "✔ Saliste del grupo \"" + name + "\". Tus piedras vuelven a ser independientes.")
               .formatted(Formatting.GREEN),
            false
         );
      }
   }

   private static String genCode() {
      return UUID.randomUUID().toString().substring(0, 8);
   }

   private static Claim findClaimById(UUID id) {
      Claim indexed = ClaimManager.getInstance().findClaimById(id);
      if (indexed != null) {
         return indexed;
      } else {
         for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) {
               return c;
            }
         }

         return null;
      }
   }

   public static record MergeInvite(String code, UUID groupId, UUID targetId, String inviterName, String groupName) {
   }

   public static record PendingChat(ClaimMenuHandler.PendingType type, UUID claimId, int returnPage, long createdAtMillis) {
      public PendingChat(ClaimMenuHandler.PendingType type, UUID claimId, int returnPage) {
         this(type, claimId, returnPage, System.currentTimeMillis());
      }

      public boolean isExpired() {
         return System.currentTimeMillis() - this.createdAtMillis > ClaimConfig.get().chatPromptMillis();
      }
   }

   public static enum PendingType {
      ADD_MEMBER,
      EDIT_WELCOME,
      EDIT_LEAVE,
      BAN_PLAYER,
      UNBAN_PLAYER,
      REMOVE_MEMBER,
      MERGE_NAME,
      MERGE_USERS;
   }
}
