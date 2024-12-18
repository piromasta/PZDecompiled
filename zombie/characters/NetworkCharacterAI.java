package zombie.characters;

import java.util.concurrent.TimeUnit;
import zombie.GameTime;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.options.Multiplayer;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoHutch;
import zombie.network.NetworkVariables;
import zombie.network.characters.AttackRateChecker;
import zombie.network.fields.IMovable;
import zombie.network.packets.character.AnimalPacket;
import zombie.network.packets.character.DeadCharacterPacket;
import zombie.network.packets.hit.VehicleHit;
import zombie.popman.Ownership;
import zombie.vehicles.BaseVehicle;

public abstract class NetworkCharacterAI {
   private static final short VEHICLE_HIT_DELAY_MS = 500;
   public final SpeedChecker speedChecker = new SpeedChecker();
   public NetworkVariables.PredictionTypes predictionType;
   protected DeadCharacterPacket deadBody;
   protected VehicleHit vehicleHit;
   protected float timestamp;
   protected BaseAction action;
   protected String performingAction;
   protected long noCollisionTime;
   protected boolean wasLocal;
   protected final HitReactionNetworkAI hitReaction;
   private final IsoGameCharacter character;
   private final Ownership ownership = new Ownership();
   private final AnimalPacket packet = new AnimalPacket();
   public boolean usePathFind = false;
   public boolean forcePathFinder = false;
   public Vector2 direction = new Vector2();
   public Vector2 distance = new Vector2();
   public float targetX = 0.0F;
   public float targetY = 0.0F;
   public int targetZ = 0;
   public boolean moved = false;
   public final AttackRateChecker attackRateChecker = new AttackRateChecker();

   public NetworkCharacterAI(IsoGameCharacter var1) {
      this.character = var1;
      this.deadBody = null;
      this.wasLocal = false;
      this.vehicleHit = null;
      this.noCollisionTime = 0L;
      this.hitReaction = new HitReactionNetworkAI(var1);
      this.predictionType = NetworkVariables.PredictionTypes.None;
      this.speedChecker.reset();
      this.moved = false;
      this.attackRateChecker.reset();
   }

   public void reset() {
      this.deadBody = null;
      this.wasLocal = false;
      this.vehicleHit = null;
      this.noCollisionTime = 0L;
      this.hitReaction.finish();
      this.predictionType = NetworkVariables.PredictionTypes.None;
      this.speedChecker.reset();
      this.moved = false;
      this.attackRateChecker.reset();
   }

   public void setLocal(boolean var1) {
      this.wasLocal = var1;
   }

   public boolean wasLocal() {
      return this.wasLocal;
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
      return this.deadBody != null && this.deadBody.isConsistent((UdpConnection)null);
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
      this.setVehicleHit((VehicleHit)null);
   }

   public void setVehicleHit(VehicleHit var1) {
      this.vehicleHit = var1;
      this.timestamp = (float)TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime());
      DebugLog.Damage.noise(var1 == null ? "processed" : "postpone");
   }

   public boolean isSetVehicleHit() {
      return this.vehicleHit != null && this.vehicleHit.isConsistent((UdpConnection)null);
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

   public void resetSpeedLimiter() {
      this.speedChecker.reset();
   }

   public short getOnlineID() {
      return this.character.getOnlineID();
   }

   public float getX() {
      return this.character.getX();
   }

   public float getY() {
      return this.character.getY();
   }

   public float getZ() {
      return this.character.getZ();
   }

   public void setX(float var1) {
      this.character.setX(var1);
   }

   public void setY(float var1) {
      this.character.setY(var1);
   }

   public void setZ(float var1) {
      this.character.setZ(var1);
   }

   public void setOwnership(UdpConnection var1) {
      this.ownership.setOwnership(var1);
   }

   public Ownership getOwnership() {
      return this.ownership;
   }

   public AnimalPacket getAnimalPacket() {
      return this.packet;
   }

   public boolean isValid(UdpConnection var1) {
      return (this.getOwnership().getConnection() != null || this.getOwnership().getConnection() == null && this.isOwnershipOnServer()) && this.getOwnership().getConnection() != var1 && var1.RelevantTo(this.getX(), this.getY(), (float)((var1.ReleventRange - 2) * 10));
   }

   public abstract IsoPlayer getRelatedPlayer();

   public boolean isRemote() {
      return this.getOwnership().getConnection() == null;
   }

   public abstract Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions();

   public IsoHutch getHutch() {
      return this.character instanceof IsoAnimal ? ((IsoAnimal)this.character).hutch : null;
   }

   public BaseVehicle getVehile() {
      return this.character instanceof IsoAnimal ? this.character.getVehicle() : null;
   }

   public boolean isOwnershipOnServer() {
      return this.getHutch() != null || this.getVehile() != null;
   }

   public boolean isDead() {
      return this.character.isDead();
   }

   public void becomeCorpse() {
      this.character.becomeCorpse();
   }

   public static class SpeedChecker implements IMovable {
      private static final int checkDelay = 5000;
      private static final int checkInterval = 1000;
      private final UpdateLimit updateLimit = new UpdateLimit(5000L);
      private final Vector2 position = new Vector2();
      private boolean isInVehicle;
      private float speed;

      public SpeedChecker() {
      }

      public float getSpeed() {
         return this.speed;
      }

      public boolean isVehicle() {
         return this.isInVehicle;
      }

      public void set(float var1, float var2, boolean var3) {
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
