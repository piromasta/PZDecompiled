package zombie.characters;

import zombie.GameTime;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.LungeState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardState;
import zombie.debug.DebugOptions;
import zombie.debug.options.Multiplayer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.character.ZombiePacket;
import zombie.pathfind.PathFindBehavior2;
import zombie.popman.NetworkZombieSimulator;
import zombie.util.Type;

public class NetworkZombieAI extends NetworkCharacterAI {
   private final PathFindBehavior2 pfb2;
   public final IsoZombie zombie;
   public float targetX = 0.0F;
   public float targetY = 0.0F;
   public int targetZ = 0;
   public boolean isClimbing;
   private byte flags;
   private byte direction;
   public final NetworkZombieMind mindSync;
   public final ObjectID reanimatedBodyID;
   public boolean DebugInterfaceActive;

   public NetworkZombieAI(IsoGameCharacter var1) {
      super(var1);
      this.reanimatedBodyID = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
      this.DebugInterfaceActive = false;
      this.zombie = (IsoZombie)var1;
      this.isClimbing = false;
      this.flags = 0;
      this.pfb2 = this.zombie.getPathFindBehavior2();
      this.mindSync = new NetworkZombieMind(this.zombie);
      var1.ulBeatenVehicle.Reset(400L);
      this.reanimatedBodyID.reset();
   }

   public void reset() {
      super.reset();
      this.usePathFind = true;
      this.targetX = this.zombie.getX();
      this.targetY = this.zombie.getY();
      this.targetZ = (byte)((int)this.zombie.getZ());
      this.isClimbing = false;
      this.flags = 0;
      this.zombie.getHitDir().set(0.0F, 0.0F);
      this.reanimatedBodyID.reset();
   }

   public IsoPlayer getRelatedPlayer() {
      return (IsoPlayer)Type.tryCastTo(this.zombie.target, IsoPlayer.class);
   }

   public boolean isRemote() {
      return this.zombie.isRemoteZombie();
   }

   public Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions() {
      return DebugOptions.instance.Multiplayer.DebugFlags.Zombie;
   }

   public void extraUpdate() {
      NetworkZombieSimulator.getInstance().addExtraUpdate(this.zombie);
   }

   private void setUsingExtrapolation(ZombiePacket var1, int var2) {
      if (this.zombie.isMoving()) {
         Vector2 var3 = this.zombie.dir.ToVector();
         this.zombie.networkCharacter.checkReset(var2);
         NetworkCharacter.Transform var4 = this.zombie.networkCharacter.predict(500, var2, this.zombie.getX(), this.zombie.getY(), var3.x, var3.y);
         var1.x = var4.position.x;
         var1.y = var4.position.y;
         var1.z = (byte)((int)this.zombie.getZ());
         var1.moveType = NetworkVariables.PredictionTypes.Moving;
      } else {
         var1.x = this.zombie.getX();
         var1.y = this.zombie.getY();
         var1.z = (byte)((int)this.zombie.getZ());
         var1.moveType = NetworkVariables.PredictionTypes.Static;
      }

   }

   private void setUsingThump(ZombiePacket var1) {
      var1.x = ((IsoObject)this.zombie.getThumpTarget()).getX();
      var1.y = ((IsoObject)this.zombie.getThumpTarget()).getY();
      var1.z = (byte)((int)((IsoObject)this.zombie.getThumpTarget()).getZ());
      var1.moveType = NetworkVariables.PredictionTypes.Thump;
   }

   private void setUsingClimb(ZombiePacket var1) {
      var1.x = this.zombie.getTarget().getX();
      var1.y = this.zombie.getTarget().getY();
      var1.z = (byte)((int)this.zombie.getTarget().getZ());
      var1.moveType = NetworkVariables.PredictionTypes.Climb;
   }

   private void setUsingLungeState(ZombiePacket var1, long var2) {
      if (this.zombie.target == null) {
         this.setUsingExtrapolation(var1, (int)var2);
      } else {
         float var4 = IsoUtils.DistanceTo(this.zombie.target.getX(), this.zombie.target.getY(), this.zombie.getX(), this.zombie.getY());
         float var5;
         if (var4 > 5.0F) {
            var1.x = (this.zombie.getX() + this.zombie.target.getX()) * 0.5F;
            var1.y = (this.zombie.getY() + this.zombie.target.getY()) * 0.5F;
            var1.z = (byte)((int)this.zombie.target.getZ());
            var5 = var4 * 0.5F / 5.0E-4F * this.zombie.speedMod;
            var1.moveType = NetworkVariables.PredictionTypes.LungeHalf;
         } else {
            var1.x = this.zombie.target.getX();
            var1.y = this.zombie.target.getY();
            var1.z = (byte)((int)this.zombie.target.getZ());
            var5 = var4 / 5.0E-4F * this.zombie.speedMod;
            var1.moveType = NetworkVariables.PredictionTypes.Lunge;
         }

      }
   }

   private void setUsingWalkTowardState(ZombiePacket var1) {
      float var2;
      if (this.zombie.getPath2() == null) {
         float var3 = this.pfb2.getPathLength();
         if (var3 > 5.0F) {
            var1.x = (this.zombie.getX() + this.pfb2.getTargetX()) * 0.5F;
            var1.y = (this.zombie.getY() + this.pfb2.getTargetY()) * 0.5F;
            var1.z = (byte)((int)this.pfb2.getTargetZ());
            var2 = var3 * 0.5F / 5.0E-4F * this.zombie.speedMod;
            var1.moveType = NetworkVariables.PredictionTypes.WalkHalf;
         } else {
            var1.x = this.pfb2.getTargetX();
            var1.y = this.pfb2.getTargetY();
            var1.z = (byte)((int)this.pfb2.getTargetZ());
            var2 = var3 / 5.0E-4F * this.zombie.speedMod;
            var1.moveType = NetworkVariables.PredictionTypes.Walk;
         }
      } else {
         var1.x = this.pfb2.pathNextX;
         var1.y = this.pfb2.pathNextY;
         var1.z = (byte)((int)this.zombie.getZ());
         var2 = IsoUtils.DistanceTo(this.zombie.getX(), this.zombie.getY(), this.pfb2.pathNextX, this.pfb2.pathNextY) / 5.0E-4F * this.zombie.speedMod;
         var1.moveType = NetworkVariables.PredictionTypes.Walk;
      }

   }

   private void setUsingPathFindState(ZombiePacket var1) {
      var1.x = this.pfb2.pathNextX;
      var1.y = this.pfb2.pathNextY;
      var1.z = (byte)((int)this.zombie.getZ());
      float var2 = IsoUtils.DistanceTo(this.zombie.getX(), this.zombie.getY(), this.pfb2.pathNextX, this.pfb2.pathNextY) / 5.0E-4F * this.zombie.speedMod;
      var1.moveType = NetworkVariables.PredictionTypes.PathFind;
   }

   public void set(ZombiePacket var1) {
      int var2 = (int)(GameTime.getServerTime() / 1000000L);
      var1.booleanVariables = NetworkZombieVariables.getBooleanVariables(this.zombie);
      var1.realHealth = (short)NetworkZombieVariables.getInt(this.zombie, (short)0);
      var1.target = (short)NetworkZombieVariables.getInt(this.zombie, (short)1);
      var1.speedMod = (short)NetworkZombieVariables.getInt(this.zombie, (short)2);
      var1.timeSinceSeenFlesh = NetworkZombieVariables.getInt(this.zombie, (short)3);
      var1.smParamTargetAngle = NetworkZombieVariables.getInt(this.zombie, (short)4);
      var1.walkType = NetworkVariables.WalkType.fromString(this.zombie.getVariableString("zombieWalkType"));
      var1.realX = this.zombie.getX();
      var1.realY = this.zombie.getY();
      var1.realZ = (byte)((int)this.zombie.getZ());
      this.zombie.realState = NetworkVariables.ZombieState.fromString(this.zombie.getAdvancedAnimator().getCurrentStateName());
      var1.realState = this.zombie.realState;
      var1.reanimatedBodyID.set(this.reanimatedBodyID);
      if (this.zombie.getThumpTarget() != null && this.zombie.getCurrentState() == ThumpState.instance()) {
         this.setUsingThump(var1);
      } else if (this.zombie.getTarget() == null || this.isClimbing || this.zombie.getCurrentState() != ClimbOverFenceState.instance() && this.zombie.getCurrentState() != ClimbOverWallState.instance() && this.zombie.getCurrentState() != ClimbThroughWindowState.instance()) {
         if (this.zombie.getCurrentState() == WalkTowardState.instance()) {
            this.setUsingWalkTowardState(var1);
         } else if (this.zombie.getCurrentState() == LungeState.instance()) {
            this.setUsingLungeState(var1, (long)var2);
         } else if (this.zombie.getCurrentState() == PathFindState.instance() && this.zombie.isMoving()) {
            this.setUsingPathFindState(var1);
         } else {
            this.setUsingExtrapolation(var1, var2);
         }
      } else {
         this.setUsingClimb(var1);
         this.isClimbing = true;
      }

      Vector2 var3 = this.zombie.dir.ToVector();
      this.zombie.networkCharacter.updateExtrapolationPoint(var2, this.zombie.getX(), this.zombie.getY(), var3.x, var3.y);
   }

   public void parse(ZombiePacket var1) {
      if (this.usePathFind) {
         this.pfb2.pathToLocationF(var1.x, var1.y, (float)var1.z);
         this.pfb2.walkingOnTheSpot.reset(this.zombie.getX(), this.zombie.getY());
      }

      this.targetX = var1.x;
      this.targetY = var1.y;
      this.targetZ = var1.z;
      this.predictionType = var1.moveType;
      NetworkZombieVariables.setInt(this.zombie, (short)1, var1.target);
      NetworkZombieVariables.setInt(this.zombie, (short)3, var1.timeSinceSeenFlesh);
      if (this.zombie.isRemoteZombie()) {
         NetworkZombieVariables.setInt(this.zombie, (short)2, var1.speedMod);
         NetworkZombieVariables.setInt(this.zombie, (short)4, var1.smParamTargetAngle);
         NetworkZombieVariables.setBooleanVariables(this.zombie, var1.booleanVariables);
         this.zombie.setWalkType(var1.walkType.toString());
         this.zombie.realState = var1.realState;
      }

      this.zombie.realx = var1.realX;
      this.zombie.realy = var1.realY;
      this.zombie.realz = var1.realZ;
      if ((IsoUtils.DistanceToSquared(this.zombie.getX(), this.zombie.getY(), this.zombie.realx, this.zombie.realy) > 9.0F || this.zombie.getZ() != (float)this.zombie.realz) && (this.zombie.isRemoteZombie() || IsoPlayer.getInstance() != null && IsoUtils.DistanceToSquared(this.zombie.getX(), this.zombie.getY(), IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY()) > 2.0F)) {
         NetworkTeleport.teleport(this.zombie, NetworkTeleport.Type.teleportation, this.zombie.realx, this.zombie.realy, this.zombie.realz, 1.0F);
      }

   }

   public void preupdate() {
      if (GameClient.bClient) {
         if (this.zombie.target != null) {
            this.zombie.setTargetSeenTime(this.zombie.getTargetSeenTime() + GameTime.getInstance().getRealworldSecondsSinceLastUpdate());
         }
      } else if (GameServer.bServer) {
         byte var1 = (byte)((this.zombie.getVariableBoolean("bMoving") ? 1 : 0) | (this.zombie.getVariableBoolean("bPathfind") ? 2 : 0));
         if (this.flags != var1) {
            this.flags = var1;
            this.extraUpdate();
         }

         byte var2 = (byte)IsoDirections.fromAngleActual(this.zombie.getForwardDirection()).index();
         if (this.direction != var2) {
            this.direction = var2;
            this.extraUpdate();
         }
      }

   }
}
