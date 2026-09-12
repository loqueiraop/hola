package com.claimblocks.data;

public class ClaimFlags {
   public boolean blockBuilding = true;
   public boolean blockBreaking = true;
   public boolean blockExplosions = true;
   public boolean blockFire = true;
   public boolean blockMobSpawn = false;
   public boolean blockPVP = true;
   public boolean blockMobDamage = false;
   public boolean trespasserAlerts = false;
   public boolean blockItemUse = true;
   public boolean blockEntityInteract = true;
   public boolean blockTrampling = true;
   public boolean blockFluids = true;
   public boolean pvpAll = false;
   public boolean blockTreeChopping = true;
   public boolean publicMode = false;
   public boolean showWelcome = false;
   public String welcomeMessage = "";
   public boolean showLeave = false;
   public String leaveMessage = "";
   public boolean showBorder = false;
   public boolean showParticles = false;
   public String borderParticle = "minecraft:happy_villager";
   public int particleDensity = 10;
   public boolean burnHostiles = true;
   public boolean effectRegeneration = false;
   public boolean effectResistance = false;
   public boolean effectSpeed = false;
   public boolean blockAnimalKilling = true;
   public boolean blockChestAccess = true;
   public boolean blockCropHarvest = true;
   public boolean blockAnvilUse = true;
   public boolean blockEnderPearl = true;
   public boolean blockSignEditing = true;
   public boolean allowFlight = false;
   public boolean blockDoorsAccess = true;
   public boolean blockAllInteractions = true;
   public boolean blockAllMobSpawn = false;
   public boolean blockPassiveMobSpawn = false;

   public boolean get(ClaimFlags.FlagId id) {
      return switch (id) {
         case BUILDING -> this.blockBuilding;
         case BREAKING -> this.blockBreaking;
         case EXPLOSIONS -> this.blockExplosions;
         case FIRE -> this.blockFire;
         case MOB_SPAWN -> this.blockMobSpawn;
         case PVP -> this.blockPVP;
         case MOB_DAMAGE -> this.blockMobDamage;
         case ALERTS -> this.trespasserAlerts;
         case ITEM_USE -> this.blockItemUse;
         case ENTITY_INTERACT -> this.blockEntityInteract;
         case TRAMPLING -> this.blockTrampling;
         case FLUIDS -> this.blockFluids;
         case PVP_ALL -> this.pvpAll;
         case TREE_CHOPPING -> this.blockTreeChopping;
         case PUBLIC_MODE -> this.publicMode;
         case SHOW_WELCOME -> this.showWelcome;
         case SHOW_LEAVE -> this.showLeave;
         case SHOW_BORDER -> this.showBorder;
         case SHOW_PARTICLES -> this.showParticles;
         case BURN_HOSTILES -> this.burnHostiles;
         case EFFECT_REGEN -> this.effectRegeneration;
         case EFFECT_RESIST -> this.effectResistance;
         case EFFECT_SPEED -> this.effectSpeed;
         case ANIMAL_KILLING -> this.blockAnimalKilling;
         case CHEST_ACCESS -> this.blockChestAccess;
         case CROP_HARVEST -> this.blockCropHarvest;
         case ANVIL_USE -> this.blockAnvilUse;
         case ENDER_PEARL -> this.blockEnderPearl;
         case SIGN_EDITING -> this.blockSignEditing;
         case ALLOW_FLIGHT -> this.allowFlight;
         case DOORS_ACCESS -> this.blockDoorsAccess;
         case BLOCK_ALL_INTERACT -> this.blockAllInteractions;
         case ALL_MOB_SPAWN -> this.blockAllMobSpawn;
         case PASSIVE_MOB_SPAWN -> this.blockPassiveMobSpawn;
      };
   }

   public void set(ClaimFlags.FlagId id, boolean value) {
      switch (id) {
         case BUILDING -> this.blockBuilding = value;
         case BREAKING -> this.blockBreaking = value;
         case EXPLOSIONS -> this.blockExplosions = value;
         case FIRE -> this.blockFire = value;
         case MOB_SPAWN -> this.blockMobSpawn = value;
         case PVP -> this.blockPVP = value;
         case MOB_DAMAGE -> this.blockMobDamage = value;
         case ALERTS -> this.trespasserAlerts = value;
         case ITEM_USE -> this.blockItemUse = value;
         case ENTITY_INTERACT -> this.blockEntityInteract = value;
         case TRAMPLING -> this.blockTrampling = value;
         case FLUIDS -> this.blockFluids = value;
         case PVP_ALL -> this.pvpAll = value;
         case TREE_CHOPPING -> this.blockTreeChopping = value;
         case PUBLIC_MODE -> this.publicMode = value;
         case SHOW_WELCOME -> this.showWelcome = value;
         case SHOW_LEAVE -> this.showLeave = value;
         case SHOW_BORDER -> this.showBorder = value;
         case SHOW_PARTICLES -> this.showParticles = value;
         case BURN_HOSTILES -> this.burnHostiles = value;
         case EFFECT_REGEN -> this.effectRegeneration = value;
         case EFFECT_RESIST -> this.effectResistance = value;
         case EFFECT_SPEED -> this.effectSpeed = value;
         case ANIMAL_KILLING -> this.blockAnimalKilling = value;
         case CHEST_ACCESS -> this.blockChestAccess = value;
         case CROP_HARVEST -> this.blockCropHarvest = value;
         case ANVIL_USE -> this.blockAnvilUse = value;
         case ENDER_PEARL -> this.blockEnderPearl = value;
         case SIGN_EDITING -> this.blockSignEditing = value;
         case ALLOW_FLIGHT -> this.allowFlight = value;
         case DOORS_ACCESS -> this.blockDoorsAccess = value;
         case BLOCK_ALL_INTERACT -> this.blockAllInteractions = value;
         case ALL_MOB_SPAWN -> this.blockAllMobSpawn = value;
         case PASSIVE_MOB_SPAWN -> this.blockPassiveMobSpawn = value;
      }
   }

   public void toggle(ClaimFlags.FlagId id) {
      this.set(id, !this.get(id));
   }

   public static boolean isPaidOnly(ClaimFlags.FlagId id) {
      return id == ClaimFlags.FlagId.EFFECT_REGEN
         || id == ClaimFlags.FlagId.EFFECT_RESIST
         || id == ClaimFlags.FlagId.EFFECT_SPEED
         || id == ClaimFlags.FlagId.ALLOW_FLIGHT;
   }

   public static enum FlagId {
      BUILDING,
      BREAKING,
      EXPLOSIONS,
      FIRE,
      MOB_SPAWN,
      PVP,
      MOB_DAMAGE,
      ALERTS,
      ITEM_USE,
      ENTITY_INTERACT,
      TRAMPLING,
      FLUIDS,
      PVP_ALL,
      TREE_CHOPPING,
      PUBLIC_MODE,
      SHOW_WELCOME,
      SHOW_LEAVE,
      SHOW_BORDER,
      SHOW_PARTICLES,
      BURN_HOSTILES,
      EFFECT_REGEN,
      EFFECT_RESIST,
      EFFECT_SPEED,
      ANIMAL_KILLING,
      CHEST_ACCESS,
      CROP_HARVEST,
      ANVIL_USE,
      ENDER_PEARL,
      SIGN_EDITING,
      ALLOW_FLIGHT,
      DOORS_ACCESS,
      BLOCK_ALL_INTERACT,
      ALL_MOB_SPAWN,
      PASSIVE_MOB_SPAWN;
   }
}
