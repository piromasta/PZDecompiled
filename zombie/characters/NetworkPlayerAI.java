package zombie.characters;

import java.util.LinkedList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SystemDisabler;
import zombie.Lua.LuaManager;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.FishingState;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.options.Multiplayer;
import zombie.input.GameKeyboard;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.ServerOptions;
import zombie.network.packets.actions.EventPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class NetworkPlayerAI extends NetworkCharacterAI {
   public final LinkedList<EventPacket> events = new LinkedList();
   public final PlayerPacket playerPacket = new PlayerPacket();
   public final UpdateLimit reliable = new UpdateLimit(2000L);
   IsoPlayer player;
   private PathFindBehavior2 pfb2 = null;
   private final UpdateLimit timerMax = new UpdateLimit(1000L);
   private final UpdateLimit timerMin = new UpdateLimit(200L);
   private boolean needUpdate = false;
   private final Vector2 tempo = new Vector2();
   private IsoGridSquare square;
   public float collidePointX;
   public float collidePointY;
   public boolean needToMovingUsingPathFinder = false;
   public boolean moving = false;
   public byte footstepSoundRadius = 0;
   public int lastBooleanVariables = 0;
   private boolean pressedMovement = false;
   private boolean pressedCancelAction = false;
   public boolean climbFenceOutcomeFall = false;
   private long accessLevelTimestamp = 0L;
   boolean wasNonPvpZone = false;
   boolean lastClimbFenceOutcomeFall = false;
   public FishingState.FishingStage fishingStage;
   public boolean disconnected;

   public NetworkPlayerAI(IsoGameCharacter var1) {
      super(var1);
      this.fishingStage = FishingState.FishingStage.None;
      this.player = (IsoPlayer)var1;
      this.pfb2 = this.player.getPathFindBehavior2();
      var1.ulBeatenVehicle.Reset(200L);
      this.collidePointX = -1.0F;
      this.collidePointY = -1.0F;
      this.wasNonPvpZone = false;
      this.disconnected = false;
   }

   public IsoPlayer getRelatedPlayer() {
      if (this.player instanceof IsoAnimal) {
         return ((IsoAnimal)this.player).atkTarget instanceof IsoPlayer ? (IsoPlayer)((IsoAnimal)this.player).atkTarget : ((IsoAnimal)this.player).getData().getAttachedPlayer();
      } else {
         return null;
      }
   }

   public Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions() {
      if (this.player instanceof IsoAnimal) {
         return DebugOptions.instance.Multiplayer.DebugFlags.Animal;
      } else {
         return this.player == IsoPlayer.getInstance() ? DebugOptions.instance.Multiplayer.DebugFlags.Self : DebugOptions.instance.Multiplayer.DebugFlags.Player;
      }
   }

   public void needToUpdate() {
      this.needUpdate = true;
   }

   private void setUsingCollide(PlayerPacket var1, int var2) {
      if (SystemDisabler.useNetworkCharacter) {
         this.player.networkCharacter.checkResetPlayer(var2);
      }

      var1.x = (float)this.player.getCurrentSquare().getX();
      var1.y = (float)this.player.getCurrentSquare().getY();
      var1.z = (byte)this.player.getCurrentSquare().getZ();
      var1.usePathFinder = false;
      var1.moveType = NetworkVariables.PredictionTypes.Thump;
   }

   private void setUsingExtrapolation(PlayerPacket var1, int var2, int var3) {
      Vector2 var4 = this.player.dir.ToVector();
      if (SystemDisabler.useNetworkCharacter) {
         this.player.networkCharacter.checkResetPlayer(var2);
      }

      if (!this.player.isPlayerMoving()) {
         var1.x = this.player.getX();
         var1.y = this.player.getY();
         var1.z = (byte)((int)this.player.getZ());
         var1.usePathFinder = false;
         var1.moveType = NetworkVariables.PredictionTypes.Static;
      } else {
         Vector2 var5 = this.tempo;
         if (SystemDisabler.useNetworkCharacter) {
            NetworkCharacter.Transform var6 = this.player.networkCharacter.predict(var3, var2, this.player.getX(), this.player.getY(), var4.x, var4.y);
            var5.x = var6.position.x;
            var5.y = var6.position.y;
         } else {
            this.player.getDeferredMovement(var5);
            var5.x = this.player.getX() + var5.x * 0.03F * (float)var3;
            var5.y = this.player.getY() + var5.y * 0.03F * (float)var3;
         }

         if (this.player.getZ() == this.pfb2.getTargetZ() && !PolygonalMap2.instance.lineClearCollide(this.player.getX(), this.player.getY(), var5.x, var5.y, PZMath.fastfloor(this.player.getZ()), (IsoMovingObject)null)) {
            var1.x = var5.x;
            var1.y = var5.y;
            var1.z = (byte)((int)this.pfb2.getTargetZ());
         } else {
            Vector2 var7 = PolygonalMap2.instance.getCollidepoint(this.player.getX(), this.player.getY(), var5.x, var5.y, PZMath.fastfloor(this.player.getZ()), (IsoMovingObject)null, 2);
            var1.collidePointX = var7.x;
            var1.collidePointY = var7.y;
            var1.x = var7.x + (this.player.dir != IsoDirections.N && this.player.dir != IsoDirections.S ? (this.player.dir.index() >= IsoDirections.NW.index() && this.player.dir.index() <= IsoDirections.SW.index() ? -1.0F : 1.0F) : 0.0F);
            var1.y = var7.y + (this.player.dir != IsoDirections.W && this.player.dir != IsoDirections.E ? (this.player.dir.index() >= IsoDirections.SW.index() && this.player.dir.index() <= IsoDirections.SE.index() ? 1.0F : -1.0F) : 0.0F);
            var1.z = (byte)((int)this.player.getZ());
         }

         var1.usePathFinder = false;
         var1.moveType = NetworkVariables.PredictionTypes.Moving;
      }
   }

   private void setUsingPathFindState(PlayerPacket var1, int var2) {
      if (SystemDisabler.useNetworkCharacter) {
         this.player.networkCharacter.checkResetPlayer(var2);
      }

      var1.x = this.pfb2.pathNextX;
      var1.y = this.pfb2.pathNextY;
      var1.z = (byte)((int)this.player.getZ());
      var1.usePathFinder = true;
      var1.moveType = NetworkVariables.PredictionTypes.PathFind;
   }

   public void set(PlayerPacket var1) {
      boolean var2 = this.square != this.player.getCurrentSquare();
      this.square = this.player.getCurrentSquare();
      var1.type = 0;
      if (this.timerMin.Check() || this.needUpdate || var2) {
         int var3 = (int)(GameTime.getServerTime() / 1000000L);
         var1.realx = this.player.getX();
         var1.realy = this.player.getY();
         var1.realz = (byte)PZMath.fastfloor(this.player.getZ());
         var1.realdir = (byte)this.player.dir.index();
         var1.realt = var3;
         var1.collidePointX = -1.0F;
         var1.collidePointY = -1.0F;
         var1.footstepSoundRadius = this.footstepSoundRadius;
         var1.disconnected = this.disconnected;
         var1.roleId = this.player.getRole().getName().hashCode();
         var1.direction = this.player.getForwardDirection().getDirection();
         if (this.player.vehicle == null) {
            var1.VehicleID = -1;
            var1.VehicleSeat = -1;
         } else {
            var1.VehicleID = this.player.vehicle.VehicleID;
            var1.VehicleSeat = (short)this.player.vehicle.getSeat(this.player);
         }

         if (this.player.getCurrentState() == CollideWithWallState.instance()) {
            this.setUsingCollide(var1, var3);
         } else if (this.pfb2.isMovingUsingPathFind()) {
            this.setUsingPathFindState(var1, var3);
         } else {
            this.setUsingExtrapolation(var1, var3, 1000);
         }

         var1.booleanVariables = NetworkPlayerVariables.getBooleanVariables(this.player);
         boolean var4 = this.lastBooleanVariables != var1.booleanVariables;
         this.lastBooleanVariables = var1.booleanVariables;
         var1.actionState = this.player.getActionContext().getCurrentStateName().hashCode();
         boolean var5 = this.timerMax.Check();
         if (var5 || var4) {
            var1.type = 1;
         }

         if (var2) {
            var1.type = 2;
         }

         if (this.needUpdate) {
            var1.type = 3;
            this.needUpdate = false;
         }

         if (var1.type > 0) {
            this.timerMax.Reset(600L);
         }
      }

   }

   public void parse(PlayerPacket var1) {
      if (!this.player.isTeleporting()) {
         this.targetX = PZMath.roundFromEdges(var1.x);
         this.targetY = PZMath.roundFromEdges(var1.y);
         this.targetZ = var1.z;
         this.predictionType = var1.moveType;
         this.needToMovingUsingPathFinder = var1.usePathFinder;
         this.direction.set((float)Math.cos((double)var1.direction), (float)Math.sin((double)var1.direction));
         this.distance.set(var1.x - this.player.getX(), var1.y - this.player.getY());
         if (this.usePathFind) {
            this.pfb2.pathToLocationF(var1.x, var1.y, (float)var1.z);
            this.pfb2.walkingOnTheSpot.reset(this.player.getX(), this.player.getY());
         }

         BaseVehicle var2 = VehicleManager.instance.getVehicleByID(var1.VehicleID);
         NetworkPlayerVariables.setBooleanVariables(this.player, var1.booleanVariables);
         this.player.setbSeenThisFrame(false);
         this.player.setbCouldBeSeenThisFrame(false);
         this.player.TimeSinceLastNetData = 0;
         this.player.ensureOnTile();
         this.player.realx = var1.realx;
         this.player.realy = var1.realy;
         this.player.realz = var1.realz;
         this.player.realdir = IsoDirections.fromIndex(var1.realdir);
         if (GameServer.bServer) {
            this.player.setForwardDirection(this.direction);
            if (this.climbFenceOutcomeFall && !this.lastClimbFenceOutcomeFall) {
               GameServer.helmetFall(this.player, false);
            }

            this.lastClimbFenceOutcomeFall = this.climbFenceOutcomeFall;
         }

         this.collidePointX = var1.collidePointX;
         this.collidePointY = var1.collidePointY;
         var1.variables.apply(this.player);
         this.footstepSoundRadius = var1.footstepSoundRadius;
         DebugLogStream var10000;
         String var10001;
         IsoGameCharacter var3;
         if (this.player.getVehicle() == null) {
            if (var2 != null) {
               if (var1.VehicleSeat >= 0 && var1.VehicleSeat < var2.getMaxPassengers()) {
                  var3 = var2.getCharacter(var1.VehicleSeat);
                  if (var3 == null) {
                     if (GameServer.bDebug) {
                        DebugLog.DetailedInfo.trace(this.player.getUsername() + " got in vehicle " + var2.VehicleID + " seat " + var1.VehicleSeat);
                     }

                     var2.enterRSync(var1.VehicleSeat, this.player, var2);
                  } else if (var3 != this.player) {
                     var10000 = DebugLog.DetailedInfo;
                     var10001 = this.player.getUsername();
                     var10000.trace(var10001 + " got in same seat as " + ((IsoPlayer)var3).getUsername());
                     this.player.sendObjectChange("exitVehicle");
                  }
               } else {
                  DebugLog.DetailedInfo.trace(this.player.getUsername() + " invalid seat vehicle " + var2.VehicleID + " seat " + var1.VehicleSeat);
               }
            }
         } else if (var2 != null) {
            if (var2 == this.player.getVehicle() && this.player.getVehicle().getSeat(this.player) != -1) {
               var3 = var2.getCharacter(var1.VehicleSeat);
               if (var3 == null) {
                  if (var2.getSeat(this.player) != var1.VehicleSeat) {
                     var2.switchSeat(this.player, var1.VehicleSeat);
                  }
               } else if (var3 != this.player) {
                  var10000 = DebugLog.DetailedInfo;
                  var10001 = this.player.getUsername();
                  var10000.trace(var10001 + " switched to same seat as " + ((IsoPlayer)var3).getUsername());
                  this.player.sendObjectChange("exitVehicle");
               }
            } else {
               var10000 = DebugLog.DetailedInfo;
               var10001 = this.player.getUsername();
               var10000.trace(var10001 + " vehicle/seat remote " + var2.VehicleID + "/" + var1.VehicleSeat + " local " + this.player.getVehicle().VehicleID + "/" + this.player.getVehicle().getSeat(this.player));
               this.player.sendObjectChange("exitVehicle");
            }
         } else {
            this.player.getVehicle().exitRSync(this.player);
            this.player.setVehicle((BaseVehicle)null);
         }

         this.setPressedMovement(false);
         this.setPressedCancelAction(false);
      }
   }

   public boolean isPressedMovement() {
      return this.pressedMovement;
   }

   public void setPressedMovement(boolean var1) {
      boolean var2 = !this.pressedMovement && var1;
      this.pressedMovement = var1;
      if (this.player.isLocal() && var2) {
         GameClient.sendEvent(this.player, "Update");
      }

   }

   public boolean isPressedCancelAction() {
      return this.pressedCancelAction;
   }

   public void setPressedCancelAction(boolean var1) {
      boolean var2 = !this.pressedCancelAction && var1;
      this.pressedCancelAction = var1;
      if (this.player.isLocal() && var2) {
         GameClient.sendEvent(this.player, "Update");
      }

   }

   public void setCheckAccessLevelDelay(long var1) {
      this.accessLevelTimestamp = System.currentTimeMillis() + var1;
   }

   public boolean doCheckAccessLevel() {
      if (this.accessLevelTimestamp == 0L) {
         return true;
      } else if (System.currentTimeMillis() > this.accessLevelTimestamp) {
         this.accessLevelTimestamp = 0L;
         return true;
      } else {
         return false;
      }
   }

   /** @deprecated */
   @Deprecated
   public void update() {
      if (!GameServer.bServer && GameClient.bClient) {
         if (!ServerOptions.getInstance().KnockedDownAllowed.getValue() && this.player.isLocalPlayer() && this.player.getVehicle() == null && this.player.isUnderVehicleRadius(0.0F)) {
            this.player.setJustMoved(true);
            this.player.setMoveDelta(1.0F);
            this.player.setForwardDirection(this.player.getForwardDirection().set((float)Rand.Next(-1, 1), (float)Rand.Next(-1, 1)));
         }

         if (Core.bDebug && this.player == IsoPlayer.getInstance() && GameKeyboard.isKeyDown(56)) {
            if (GameKeyboard.isKeyPressed(44)) {
               GameClient.SendCommandToServer(String.format("/createhorde2 -x %d -y %d -z %d -count %d -radius %d -crawler %s -isFallOnFront %s -isFakeDead %s -knockedDown %s -health %s -outfit %s ", PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ()), 1, 1, "false", "false", "false", "false", "1", ""));
            }

            if (GameKeyboard.isKeyPressed(19) && this.player.getVehicle() != null) {
               KahluaTable var1 = LuaManager.platform.newTable();
               var1.rawset("vehicle", (double)this.player.getVehicle().getId());
               GameClient.instance.sendClientCommand(this.player, "vehicle", "repair", var1);
            }
         }
      }

   }

   public boolean isDismantleAllowed() {
      return true;
   }

   public boolean isDisconnected() {
      return this.disconnected;
   }

   public void setDisconnected(boolean var1) {
      this.disconnected = var1;
   }

   public boolean isReliable() {
      return this.reliable.Check();
   }
}
