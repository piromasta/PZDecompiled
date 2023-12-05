package zombie.characters;

import java.util.Iterator;
import java.util.LinkedHashMap;
import zombie.GameTime;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.areas.NonPvpZone;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.util.Type;

public class SafetySystemManager {
   private static final LinkedHashMap<String, Float> playerCooldown = new LinkedHashMap();
   private static final LinkedHashMap<String, Boolean> playerSafety = new LinkedHashMap();
   private static final LinkedHashMap<String, Long> playerDelay = new LinkedHashMap();
   private static final long safetyDelay = 1500L;

   public SafetySystemManager() {
   }

   private static void updateTimers(Safety var0) {
      float var1 = GameTime.instance.getRealworldSecondsSinceLastUpdate();
      if (var0.getToggle() > 0.0F) {
         var0.setToggle(var0.getToggle() - var1);
         if (var0.getToggle() <= 0.0F) {
            var0.setToggle(0.0F);
            if (!var0.isLast()) {
               var0.setEnabled(!var0.isEnabled());
            }
         }
      } else if (var0.getCooldown() > 0.0F) {
         var0.setCooldown(var0.getCooldown() - var1);
      } else {
         var0.setCooldown(0.0F);
      }

   }

   private static void updateNonPvpZone(IsoPlayer var0, boolean var1) {
      if (var1 && !var0.networkAI.wasNonPvpZone) {
         storeSafety(var0);
         GameServer.sendChangeSafety(var0.getSafety());
      } else if (!var1 && var0.networkAI.wasNonPvpZone) {
         restoreSafety(var0);
         GameServer.sendChangeSafety(var0.getSafety());
      }

      var0.networkAI.wasNonPvpZone = var1;
   }

   static void update(IsoPlayer var0) {
      boolean var1 = NonPvpZone.getNonPvpZone(PZMath.fastfloor(var0.getX()), PZMath.fastfloor(var0.getY())) != null;
      if (!var1) {
         updateTimers(var0.getSafety());
      }

      if (GameServer.bServer) {
         updateNonPvpZone(var0, var1);
      }

   }

   public static void clear() {
      playerCooldown.clear();
      playerSafety.clear();
      playerDelay.clear();
   }

   public static void clearSafety(IsoPlayer var0) {
      if (var0 != null) {
         Safety var1 = var0.getSafety();
         LoggerManager.getLogger("pvp").write(String.format("user \"%s\" clear safety %s", var0.getUsername(), var1.getDescription()), "INFO");
         playerCooldown.remove(var0.getUsername());
         playerSafety.remove(var0.getUsername());
         playerDelay.remove(var0.getUsername());
      } else if (Core.bDebug) {
         DebugLog.Combat.debugln("ClearSafety: player not found");
      }

   }

   public static void storeSafety(IsoPlayer var0) {
      try {
         if (var0 != null && var0.isAlive()) {
            Safety var1 = var0.getSafety();
            LoggerManager.getLogger("pvp").write(String.format("user \"%s\" store safety %s", var0.getUsername(), var1.getDescription()), "INFO");
            playerSafety.put(var0.getUsername(), var1.isEnabled());
            playerCooldown.put(var0.getUsername(), var1.getCooldown());
            playerDelay.put(var0.getUsername(), System.currentTimeMillis());
            Iterator var2;
            if (playerCooldown.size() > ServerOptions.instance.MaxPlayers.getValue() * 1000) {
               var2 = playerCooldown.entrySet().iterator();
               if (var2.hasNext()) {
                  var2.next();
                  var2.remove();
               }
            }

            if (playerSafety.size() > ServerOptions.instance.MaxPlayers.getValue() * 1000) {
               var2 = playerSafety.entrySet().iterator();
               if (var2.hasNext()) {
                  var2.next();
                  var2.remove();
               }
            }

            if (playerDelay.size() > ServerOptions.instance.MaxPlayers.getValue() * 1000) {
               var2 = playerDelay.entrySet().iterator();
               if (var2.hasNext()) {
                  var2.next();
                  var2.remove();
               }
            }
         } else if (Core.bDebug) {
            DebugLog.Combat.debugln("StoreSafety: player not found");
         }
      } catch (Exception var3) {
         DebugLog.Multiplayer.printException(var3, "StoreSafety failed", LogSeverity.Error);
      }

   }

   public static void restoreSafety(IsoPlayer var0) {
      try {
         if (var0 != null) {
            Safety var1 = var0.getSafety();
            if (playerSafety.containsKey(var0.getUsername())) {
               var1.setEnabled((Boolean)playerSafety.remove(var0.getUsername()));
            }

            if (playerCooldown.containsKey(var0.getUsername())) {
               var1.setCooldown((Float)playerCooldown.remove(var0.getUsername()));
            }

            playerDelay.put(var0.getUsername(), System.currentTimeMillis());
            LoggerManager.getLogger("pvp").write(String.format("user \"%s\" restore safety %s", var0.getUsername(), var1.getDescription()), "INFO");
         } else if (Core.bDebug) {
            DebugLog.Combat.debugln("RestoreSafety: player not found");
         }
      } catch (Exception var2) {
         DebugLog.Multiplayer.printException(var2, "RestoreSafety failed", LogSeverity.Error);
      }

   }

   public static void updateOptions() {
      boolean var0 = ServerOptions.instance.PVP.getValue();
      boolean var1 = ServerOptions.instance.SafetySystem.getValue();
      Iterator var2;
      IsoPlayer var3;
      if (!var0) {
         clear();
         var2 = GameServer.IDToPlayerMap.values().iterator();

         while(var2.hasNext()) {
            var3 = (IsoPlayer)var2.next();
            if (var3 != null) {
               var3.getSafety().setEnabled(true);
               var3.getSafety().setLast(false);
               var3.getSafety().setCooldown(0.0F);
               var3.getSafety().setToggle(0.0F);
               GameServer.sendChangeSafety(var3.getSafety());
            }
         }
      } else if (!var1) {
         clear();
         var2 = GameServer.IDToPlayerMap.values().iterator();

         while(var2.hasNext()) {
            var3 = (IsoPlayer)var2.next();
            if (var3 != null) {
               var3.getSafety().setEnabled(false);
               var3.getSafety().setLast(false);
               var3.getSafety().setCooldown(0.0F);
               var3.getSafety().setToggle(0.0F);
               GameServer.sendChangeSafety(var3.getSafety());
            }
         }
      }

   }

   public static boolean checkUpdateDelay(IsoGameCharacter var0, IsoGameCharacter var1) {
      boolean var2 = false;
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var0, IsoPlayer.class);
      IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var3 != null && var4 != null) {
         long var5 = System.currentTimeMillis();
         long var7;
         boolean var9;
         if (playerDelay.containsKey(var3.getUsername())) {
            var7 = var5 - (Long)playerDelay.getOrDefault(var3.getUsername(), 0L);
            var9 = var7 < 1500L;
            var2 = var9;
            if (!var9) {
               playerDelay.remove(var3.getUsername());
            }
         }

         if (playerDelay.containsKey(var4.getUsername())) {
            var7 = var5 - (Long)playerDelay.getOrDefault(var4.getUsername(), 0L);
            var9 = var7 < 1500L;
            if (!var2) {
               var2 = var9;
            }

            if (!var9) {
               playerDelay.remove(var4.getUsername());
            }
         }
      }

      return var2;
   }
}
