package com.claimblocks.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Un grupo de zonas fusionadas. La "zona madre" manda: aporta flags, bans y altura. */
public class ClaimGroup {
   private final UUID groupId;
   private String name;
   private UUID motherClaimId;
   private UUID motherOwnerId;
   private final Set<UUID> registeredPlayers = new HashSet<>();

   public ClaimGroup(UUID groupId, String name, UUID motherClaimId, UUID motherOwnerId) {
      this.groupId = groupId;
      this.name = name;
      this.motherClaimId = motherClaimId;
      this.motherOwnerId = motherOwnerId;
      if (motherOwnerId != null) {
         this.registeredPlayers.add(motherOwnerId);
      }
   }

   public UUID getGroupId() {
      return this.groupId;
   }

   public String getName() {
      return this.name == null ? "Grupo" : this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public UUID getMotherClaimId() {
      return this.motherClaimId;
   }

   public void setMotherClaimId(UUID id) {
      this.motherClaimId = id;
   }

   public UUID getMotherOwnerId() {
      return this.motherOwnerId;
   }

   public Set<UUID> getRegisteredPlayers() {
      return this.registeredPlayers;
   }

   public boolean isRegistered(UUID id) {
      return id != null && this.registeredPlayers.contains(id);
   }

   public void register(UUID id) {
      if (id != null) {
         this.registeredPlayers.add(id);
      }
   }

   public void unregister(UUID id) {
      this.registeredPlayers.remove(id);
   }

   public JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("groupId", this.groupId.toString());
      o.addProperty("name", this.name == null ? "" : this.name);
      o.addProperty("motherClaimId", this.motherClaimId == null ? "" : this.motherClaimId.toString());
      o.addProperty("motherOwnerId", this.motherOwnerId == null ? "" : this.motherOwnerId.toString());
      JsonArray reg = new JsonArray();

      for (UUID id : this.registeredPlayers) {
         reg.add(id.toString());
      }

      o.add("registered", reg);
      return o;
   }

   public static ClaimGroup fromJson(JsonObject o) {
      UUID gid = UUID.fromString(o.get("groupId").getAsString());
      String name = o.has("name") ? o.get("name").getAsString() : "Grupo";
      UUID mother = o.has("motherClaimId") && !o.get("motherClaimId").getAsString().isEmpty()
         ? UUID.fromString(o.get("motherClaimId").getAsString())
         : null;
      UUID owner = o.has("motherOwnerId") && !o.get("motherOwnerId").getAsString().isEmpty()
         ? UUID.fromString(o.get("motherOwnerId").getAsString())
         : null;
      ClaimGroup g = new ClaimGroup(gid, name, mother, owner);
      if (o.has("registered")) {
         JsonArray arr = o.getAsJsonArray("registered");

         for (int i = 0; i < arr.size(); i++) {
            g.registeredPlayers.add(UUID.fromString(arr.get(i).getAsString()));
         }
      }

      return g;
   }
}
