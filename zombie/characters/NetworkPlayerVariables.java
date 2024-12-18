package zombie.characters;

import zombie.ai.states.FishingState;
import zombie.core.Core;
import zombie.iso.Vector2;

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
      var1 |= var0.isDoShove() ? 512 : 0;
      var1 |= var0.isDoGrapple() ? 1048576 : 0;
      var0.getDeferredMovement(deferredMovement);
      var1 |= deferredMovement.getLength() > 0.0F ? 1024 : 0;
      var1 |= var0.isOnFloor() ? 2048 : 0;
      var1 |= var0.isGodMod() ? 4096 : 0;
      var1 |= Core.bDebug ? 8192 : 0;
      var1 |= var0.getVariableBoolean("petanimal") ? 65536 : 0;
      var1 |= var0.isSitOnGround() ? 131072 : 0;
      var1 |= "fall".equals(var0.getVariableString("ClimbFenceOutcome")) ? 262144 : 0;
      var1 |= var0.getVariableBoolean("shearanimal") ? 524288 : 0;
      var1 |= var0.getVariableBoolean("milkanimal") ? 2097152 : 0;
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
      if (!var0.isDoShove() && (var1 & 512) != 0) {
         var0.setDoShove((var1 & 512) != 0);
      }

      var0.setDoGrapple((var1 & 1048576) != 0);
      var0.networkAI.moving = (var1 & 1024) != 0;
      var0.setOnFloor((var1 & 2048) != 0);
      var0.setVariable("petanimal", (var1 & 65536) != 0);
      var0.setSitOnGround((var1 & 131072) != 0);
      var0.networkAI.climbFenceOutcomeFall = (var1 & 262144) != 0;
      var0.setVariable("shearanimal", (var1 & 524288) != 0);
      var0.setVariable("milkanimal", (var1 & 2097152) != 0);
   }

   public static class Flags {
      public static final int isSneaking = 1;
      public static final int isOnFire = 2;
      public static final int isAsleep = 4;
      public static final int isFishing = 8;
      public static final int isRunning = 16;
      public static final int isSprinting = 32;
      public static final int isAiming = 64;
      public static final int isCharging = 128;
      public static final int isChargingLT = 256;
      public static final int isDoShove = 512;
      public static final int hasDeferredMovement = 1024;
      public static final int isOnFloor = 2048;
      public static final int isCheatMode = 4096;
      public static final int isDebugMode = 8192;
      public static final int isPetAnimal = 65536;
      public static final int isSitOnGround = 131072;
      public static final int hasFallenAfterClimbingFence = 262144;
      public static final int isShearAnimal = 524288;
      public static final int isDoGrapple = 1048576;
      public static final int isMilkAnimal = 2097152;

      public Flags() {
      }
   }
}
