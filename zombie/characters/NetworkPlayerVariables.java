package zombie.characters;

import zombie.ai.states.FishingState;
import zombie.core.Core;
import zombie.iso.Vector2;
import zombie.network.GameClient;

public class NetworkPlayerVariables {
   static Vector2 deferredMovement = new Vector2();

   public NetworkPlayerVariables() {
   }

   public static int getBooleanVariables(IsoPlayer var0) {
      int var1 = 0;
      var1 |= var0.isSneaking() ? 1 : 0;
      var1 |= var0.isOnFire() ? 2 : 0;
      var1 |= var0.isAsleep() ? 4 : 0;
      var1 |= FishingState.instance().equals(var0.getCurrentState()) ? 8 : 0;
      var1 |= var0.isRunning() ? 16 : 0;
      var1 |= var0.isSprinting() ? 32 : 0;
      var1 |= var0.isAiming() ? 64 : 0;
      var1 |= var0.isCharging ? 128 : 0;
      var1 |= var0.isChargingLT ? 256 : 0;
      var1 |= var0.bDoShove ? 512 : 0;
      var0.getDeferredMovement(deferredMovement);
      var1 |= deferredMovement.getLength() > 0.0F ? 1024 : 0;
      var1 |= var0.isOnFloor() ? 2048 : 0;
      var1 |= var0.isGhostMode() ? 4096 : 0;
      var1 |= Core.bDebug ? 8192 : 0;
      var1 |= var0.isNoClip() ? 16384 : 0;
      if (GameClient.bClient && GameClient.connection.accessLevel != 1 || !var0.isAccessLevel("None")) {
         var1 |= 32768;
      }

      var1 |= var0.isSitOnGround() ? 131072 : 0;
      var1 |= "fall".equals(var0.getVariableString("ClimbFenceOutcome")) ? 262144 : 0;
      return var1;
   }

   public static void setBooleanVariables(IsoPlayer var0, int var1) {
      var0.setSneaking((var1 & 1) != 0);
      if ((var1 & 2) != 0) {
         var0.SetOnFire();
      } else {
         var0.StopBurning();
      }

      var0.setAsleep((var1 & 4) != 0);
      boolean var2 = (var1 & 8) != 0;
      if (FishingState.instance().equals(var0.getCurrentState()) && !var2) {
         var0.SetVariable("FishingFinished", "true");
      }

      var0.setRunning((var1 & 16) != 0);
      var0.setSprinting((var1 & 32) != 0);
      var0.setIsAiming((var1 & 64) != 0);
      var0.isCharging = (var1 & 128) != 0;
      var0.isChargingLT = (var1 & 256) != 0;
      if (!var0.bDoShove && (var1 & 512) != 0) {
         var0.setDoShove((var1 & 512) != 0);
      }

      var0.networkAI.moving = (var1 & 1024) != 0;
      var0.setOnFloor((var1 & 2048) != 0);
      var0.setSitOnGround((var1 & 131072) != 0);
      var0.networkAI.climbFenceOutcomeFall = (var1 & 262144) != 0;
   }
}
