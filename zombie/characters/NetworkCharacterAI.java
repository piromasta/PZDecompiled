package zombie.characters;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import zombie.GameTime;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.PacketValidator;
import zombie.network.packets.DeadCharacterPacket;
import zombie.network.packets.hit.IMovable;
import zombie.network.packets.hit.VehicleHitPacket;

public abstract class NetworkCharacterAI {
   private static final short VEHICLE_HIT_DELAY_MS = 500;
   private final SpeedChecker speedChecker = new SpeedChecker();
   public NetworkVariables.PredictionTypes predictionType;
   protected DeadCharacterPacket deadBody;
   protected VehicleHitPacket vehicleHit;
   protected float timestamp;
   protected BaseAction action;
   protected String performingAction;
   protected long noCollisionTime;
   protected boolean wasLocal;
   protected final HitReactionNetworkAI hitReaction;
   private final IsoGameCharacter character;
   public NetworkTeleport.NetworkTeleportDebug teleportDebug;
   public final HashMap<Integer, String> debugData = new LinkedHashMap<Integer, String>() {
      protected boolean removeEldestEntry(Map.Entry<Integer, String> var1) {
         return this.size() > 10;
      }
   };

   public NetworkCharacterAI(IsoGameCharacter var1) {
      this.character = var1;
      this.deadBody = null;
      this.wasLocal = false;
      this.vehicleHit = null;
      this.noCollisionTime = 0L;
      this.hitReaction = new HitReactionNetworkAI(var1);
      this.predictionType = NetworkVariables.PredictionTypes.None;
      this.clearTeleportDebug();
      this.speedChecker.reset();
   }

   public void reset() {
      this.deadBody = null;
      this.wasLocal = false;
      this.vehicleHit = null;
      this.noCollisionTime = 0L;
      this.hitReaction.finish();
      this.predictionType = NetworkVariables.PredictionTypes.None;
      this.clearTeleportDebug();
      this.speedChecker.reset();
   }

   public void setLocal(boolean var1) {
      this.wasLocal = var1;
   }

   public boolean wasLocal() {
      return this.wasLocal;
   }

   public NetworkTeleport.NetworkTeleportDebug getTeleportDebug() {
      return this.teleportDebug;
   }

   public void clearTeleportDebug() {
      this.teleportDebug = null;
      this.debugData.clear();
   }

   public void setTeleportDebug(NetworkTeleport.NetworkTeleportDebug var1) {
      this.teleportDebug = var1;
      this.debugData.entrySet().stream().sorted(Entry.comparingByKey(Comparator.naturalOrder())).forEach((var0) -> {
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, "==> " + (String)var0.getValue());
         }

      });
      if (Core.bDebug) {
         DebugLog.log(DebugType.Multiplayer, String.format("NetworkTeleport %s id=%d distance=%.3f prediction=%s", this.character.getClass().getSimpleName(), this.character.getOnlineID(), var1.getDistance(), this.predictionType));
      }

   }

   public void addTeleportData(int var1, String var2) {
      this.debugData.put(var1, var2);
   }

   public void processDeadBody() {
      if (this.isSetDeadBody() && !this.hitReaction.isSetup() && !this.hitReaction.isStarted()) {
         this.deadBody.process();
         this.setDeadBody((DeadCharacterPacket)null);
      }

   }

   public void setDeadBody(DeadCharacterPacket var1) {
      this.deadBody = var1;
      DebugLog.Death.trace(var1 == null ? "processed" : "postpone");
   }

   public boolean isSetDeadBody() {
      return this.deadBody != null && this.deadBody.isConsistent();
   }

   public void setPerformingAction(String var1) {
      this.performingAction = var1;
   }

   public String getPerformingAction() {
      return this.performingAction;
   }

   public void setAction(BaseAction var1) {
      this.action = var1;
   }

   public BaseAction getAction() {
      return this.action;
   }

   public void startAction() {
      if (this.action != null) {
         this.action.start();
      }

   }

   public void stopAction() {
      if (this.action != null) {
         this.setOverride(false, (String)null, (String)null);
         this.action.stop();
      }

   }

   public void setOverride(boolean var1, String var2, String var3) {
      if (this.action != null) {
         this.action.chr.forceNullOverride = var1;
         this.action.chr.overridePrimaryHandModel = var2;
         this.action.chr.overrideSecondaryHandModel = var3;
         this.action.chr.resetModelNextFrame();
      }

   }

   public void processVehicleHit() {
      this.vehicleHit.tryProcessInternal();
      this.setVehicleHit((VehicleHitPacket)null);
   }

   public void setVehicleHit(VehicleHitPacket var1) {
      this.vehicleHit = var1;
      this.timestamp = (float)TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime());
      DebugLog.Damage.noise(var1 == null ? "processed" : "postpone");
   }

   public boolean isSetVehicleHit() {
      return this.vehicleHit != null && this.vehicleHit.isConsistent();
   }

   public void resetVehicleHitTimeout() {
      this.timestamp = (float)(TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime()) - 500L);
      if (this.vehicleHit == null) {
         DebugLog.Damage.noise("VehicleHit is not set");
      }

   }

   public boolean isVehicleHitTimeout() {
      boolean var1 = (float)TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime()) - this.timestamp >= 500.0F;
      if (var1) {
         DebugLog.Damage.noise("VehicleHit timeout");
      }

      return var1;
   }

   public void updateHitVehicle() {
      if (this.isSetVehicleHit() && this.isVehicleHitTimeout()) {
         this.processVehicleHit();
      }

   }

   public boolean isCollisionEnabled() {
      return this.noCollisionTime == 0L;
   }

   public boolean isNoCollisionTimeout() {
      boolean var1 = GameTime.getServerTimeMills() > this.noCollisionTime;
      if (var1) {
         this.setNoCollision(0L);
      }

      return var1;
   }

   public void setNoCollision(long var1) {
      if (var1 == 0L) {
         this.noCollisionTime = 0L;
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, "SetNoCollision: disabled");
         }
      } else {
         this.noCollisionTime = GameTime.getServerTimeMills() + var1;
         if (Core.bDebug) {
            DebugLog.log(DebugType.Multiplayer, "SetNoCollision: enabled for " + var1 + " ms");
         }
      }

   }

   public boolean checkPosition(UdpConnection var1, IsoGameCharacter var2, float var3, float var4) {
      if (GameServer.bServer && var2.isAlive()) {
         this.speedChecker.set(var3, var4, var2.isSeatedInVehicle());
         SpeedChecker var10001 = this.speedChecker;
         String var10002 = var2.getClass().getSimpleName();
         boolean var5 = PacketValidator.checkSpeed(var1, var10001, var10002 + SpeedChecker.class.getSimpleName());
         if (32 == var1.accessLevel) {
            var5 = true;
         }

         return var5;
      } else {
         return true;
      }
   }

   public void resetSpeedLimiter() {
      this.speedChecker.reset();
   }

   private static class SpeedChecker implements IMovable {
      private static final int checkDelay = 5000;
      private static final int checkInterval = 1000;
      private final UpdateLimit updateLimit = new UpdateLimit(5000L);
      private final Vector2 position = new Vector2();
      private boolean isInVehicle;
      private float speed;

      private SpeedChecker() {
      }

      public float getSpeed() {
         return this.speed;
      }

      public boolean isVehicle() {
         return this.isInVehicle;
      }

      private void set(float var1, float var2, boolean var3) {
         if (this.updateLimit.Check()) {
            if (5000L == this.updateLimit.getDelay()) {
               this.updateLimit.Reset(1000L);
               this.position.set(0.0F, 0.0F);
               this.speed = 0.0F;
            }

            this.isInVehicle = var3;
            if (this.position.getLength() != 0.0F) {
               this.speed = IsoUtils.DistanceTo(this.position.x, this.position.y, var1, var2);
            }

            this.position.set(var1, var2);
         }

      }

      private void reset() {
         this.updateLimit.Reset(5000L);
         this.isInVehicle = false;
         this.position.set(0.0F, 0.0F);
         this.speed = 0.0F;
      }

      public String getDescription() {
         return "SpeedChecker: speed=" + this.speed + " x=" + this.position.x + " y=" + this.position.y + " vehicle=" + this.isInVehicle;
      }
   }
}
