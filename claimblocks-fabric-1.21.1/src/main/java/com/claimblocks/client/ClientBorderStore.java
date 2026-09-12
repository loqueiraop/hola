package com.claimblocks.client;

import java.util.Collections;
import java.util.List;

/** Guarda los contornos recibidos del servidor. Se descartan si dejan de llegar. */
public final class ClientBorderStore {
   private static final long STALE_AFTER_MS = 60000L;
   private static volatile List<double[]> boxes = Collections.emptyList();
   private static volatile long lastUpdate = 0L;

   private ClientBorderStore() {
   }

   public static void receive(List<double[]> incoming) {
      boxes = incoming == null ? Collections.emptyList() : incoming;
      lastUpdate = System.currentTimeMillis();
   }

   public static void clear() {
      boxes = Collections.emptyList();
      lastUpdate = 0L;
   }

   public static List<double[]> current() {
      List<double[]> snapshot = boxes;
      if (snapshot.isEmpty()) {
         return Collections.emptyList();
      } else {
         return System.currentTimeMillis() - lastUpdate > STALE_AFTER_MS ? Collections.emptyList() : snapshot;
      }
   }
}
