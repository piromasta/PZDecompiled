package zombie.ai.states;

import zombie.GameTime;
import zombie.SoundManager;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.ZombieThumpManager;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ThumpState extends State {
   private static final ThumpState _instance = new ThumpState();

   public ThumpState() {
   }

   public static ThumpState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      // $FF: Couldn't be decompiled
   }

   public void execute(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)var1;
      Thumpable var3 = var1.getThumpTarget();
      if (var3 instanceof IsoObject) {
         var1.faceThisObject((IsoObject)var3);
      }

      this.slideAwayFromEdge(var1, var3);
      boolean var4 = GameServer.bServer && GameServer.bFastForward || !GameServer.bServer && IsoPlayer.allPlayersAsleep();
      if (var4 || var1.getActionContext().hasEventOccurred("thumpframe")) {
         var1.getActionContext().clearEvent("thumpframe");
         var1.setTimeThumping(var1.getTimeThumping() + 1);
         if (var2.TimeSinceSeenFlesh < 5.0F) {
            var1.setTimeThumping(0);
         }

         int var5 = 1;
         if (var1.getCurrentSquare() != null) {
            var5 = var1.getCurrentSquare().getMovingObjects().size();
         }

         for(int var6 = 0; var6 < var5 && this.isThumpTargetValid(var1, var1.getThumpTarget()); ++var6) {
            var1.getThumpTarget().Thump(var1);
         }

         Thumpable var12 = var1.getThumpTarget() == null ? null : var1.getThumpTarget().getThumpableFor(var1);
         boolean var7 = GameServer.bServer || SoundManager.instance.isListenerInRange(var1.getX(), var1.getY(), 20.0F);
         if (var7 && !IsoPlayer.allPlayersAsleep()) {
            if (var12 instanceof IsoWindow) {
               var2.setThumpFlag(Rand.Next(3) == 0 ? 2 : 3);
               var2.setThumpCondition(var12.getThumpCondition());
               if (!GameServer.bServer) {
                  ZombieThumpManager.instance.addCharacter(var2);
               }
            } else if (var12 != null) {
               String var8 = "ZombieThumpGeneric";
               IsoBarricade var9 = (IsoBarricade)Type.tryCastTo(var12, IsoBarricade.class);
               if (var9 != null && (var9.isMetal() || var9.isMetalBar())) {
                  var8 = "ZombieThumpMetal";
               } else if (var12 instanceof IsoDoor) {
                  var8 = ((IsoDoor)var12).getThumpSound();
               } else if (var12 instanceof IsoThumpable) {
                  var8 = ((IsoThumpable)var12).getThumpSound();
               }

               if ("ZombieThumpGeneric".equals(var8)) {
                  var2.setThumpFlag(1);
               } else if ("ZombieThumpWindow".equals(var8)) {
                  var2.setThumpFlag(3);
               } else if ("ZombieThumpMetal".equals(var8)) {
                  var2.setThumpFlag(4);
               } else if ("ZombieThumpGarageDoor".equals(var8)) {
                  var2.setThumpFlag(5);
               } else if ("ZombieThumpWireFence".equals(var8)) {
                  var2.setThumpFlag(6);
               } else if ("ZombieThumpMetalPoleGate".equals(var8)) {
                  var2.setThumpFlag(7);
               } else {
                  var2.setThumpFlag(1);
               }

               var2.setThumpCondition(var12.getThumpCondition());
               if (!GameServer.bServer) {
                  ZombieThumpManager.instance.addCharacter(var2);
               }
            }
         }
      }

      if (!this.isThumpTargetValid(var1, var1.getThumpTarget())) {
         var1.setThumpTarget((Thumpable)null);
         var1.setTimeThumping(0);
         if (var3 instanceof IsoWindow && ((IsoWindow)var3).canClimbThrough(var1)) {
            var1.climbThroughWindow((IsoWindow)var3);
         } else {
            IsoGridSquare var13;
            IsoGridSquare var14;
            if (var3 instanceof IsoDoor && (((IsoDoor)var3).open || var3.isDestroyed())) {
               IsoDoor var10 = (IsoDoor)var3;
               var13 = var10.getSquare();
               var14 = var10.getOppositeSquare();
               if (this.lungeThroughDoor(var2, var13, var14)) {
                  return;
               }
            }

            if (var3 instanceof IsoThumpable && ((IsoThumpable)var3).isDoor && (((IsoThumpable)var3).open || var3.isDestroyed())) {
               IsoThumpable var11 = (IsoThumpable)var3;
               var13 = var11.getSquare();
               var14 = var11.getInsideSquare();
               if (this.lungeThroughDoor(var2, var13, var14)) {
                  return;
               }
            }

            if (var2.LastTargetSeenX != -1) {
               var1.pathToLocation(var2.LastTargetSeenX, var2.LastTargetSeenY, var2.LastTargetSeenZ);
            }

         }
      }
   }

   public void exit(IsoGameCharacter var1) {
      var1.setThumpTarget((Thumpable)null);
      ((IsoZombie)var1).setThumpTimer(200);
      if (GameClient.bClient && var1.isLocal()) {
         GameClient.sendThump(var1, var1.getThumpTarget());
      }

   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      if (var2.m_EventName.equalsIgnoreCase("ThumpFrame")) {
      }

   }

   private void slideAwayFromEdge(IsoGameCharacter var1, Thumpable var2) {
      boolean var3 = false;
      boolean var4 = false;
      if (!(var2 instanceof BaseVehicle)) {
         if (var2 instanceof IsoObject) {
            IsoObject var5 = (IsoObject)var2;
            IsoGridSquare var6 = var5.getSquare();
            if (!(var2 instanceof IsoBarricade)) {
               if (var2 instanceof IsoDoor) {
                  IsoDoor var8 = (IsoDoor)var2;
                  var4 = var8.getNorth();
               } else if (var2 instanceof IsoThumpable) {
                  IsoThumpable var9 = (IsoThumpable)var2;
                  var3 = var9.isBlockAllTheSquare();
                  var4 = var9.getNorth();
               } else if (var2 instanceof IsoWindow) {
                  IsoWindow var10 = (IsoWindow)var2;
                  var4 = var10.getNorth();
               } else if (var2 instanceof IsoWindowFrame) {
                  IsoWindowFrame var11 = (IsoWindowFrame)var2;
                  var4 = var11.getNorth();
               }
            } else {
               IsoBarricade var7 = (IsoBarricade)var2;
               var4 = var7.getDir() == IsoDirections.N || var7.getDir() == IsoDirections.S;
            }

            float var12 = 0.4F;
            Thumpable var13 = var2.getThumpableFor(var1);
            if (var13 instanceof IsoBarricade) {
               IsoBarricade var14 = (IsoBarricade)var13;
               if (var2 instanceof BarricadeAble) {
                  BarricadeAble var15 = (BarricadeAble)var2;
                  if (IsoBarricade.GetBarricadeForCharacter(var15, var1) == var13) {
                     var12 = 0.47F;
                  }
               }
            }

            if (var3) {
               if (var1.getY() < (float)var6.y) {
                  this.slideAwayFromEdgeN(var1, var6.y, var12);
               } else if (var1.getY() > (float)(var6.y + 1)) {
                  this.slideAwayFromEdgeS(var1, var6.y + 1, var12);
               }

               if (var1.getX() < (float)var6.x) {
                  this.slideAwayFromEdgeW(var1, var6.x, var12);
               } else if (var1.getX() > (float)(var6.x + 1)) {
                  this.slideAwayFromEdgeE(var1, var6.x + 1, var12);
               }
            } else if (var4) {
               if (var1.getY() < (float)var6.y) {
                  this.slideAwayFromEdgeN(var1, var6.y, var12);
               } else {
                  this.slideAwayFromEdgeS(var1, var6.y, var12);
               }
            } else if (var1.getX() < (float)var6.x) {
               this.slideAwayFromEdgeW(var1, var6.x, var12);
            } else {
               this.slideAwayFromEdgeE(var1, var6.x, var12);
            }
         }

      }
   }

   private void slideAwayFromEdgeN(IsoGameCharacter var1, int var2, float var3) {
      if (var1.getY() > (float)var2 - var3) {
         var1.setNextY((float)var2 - var3);
      }

   }

   private void slideAwayFromEdgeS(IsoGameCharacter var1, int var2, float var3) {
      if (var1.getY() < (float)var2 + var3) {
         var1.setNextY((float)var2 + var3);
      }

   }

   private void slideAwayFromEdgeW(IsoGameCharacter var1, int var2, float var3) {
      if (var1.getX() > (float)var2 - var3) {
         var1.setNextX((float)var2 - var3);
      }

   }

   private void slideAwayFromEdgeE(IsoGameCharacter var1, int var2, float var3) {
      if (var1.getX() < (float)var2 + var3) {
         var1.setNextX((float)var2 + var3);
      }

   }

   private IsoPlayer findPlayer(int var1, int var2, int var3, int var4, int var5) {
      for(int var6 = var3; var6 <= var4; ++var6) {
         for(int var7 = var1; var7 <= var2; ++var7) {
            IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var7, var6, var5);
            if (var8 != null) {
               for(int var9 = 0; var9 < var8.getMovingObjects().size(); ++var9) {
                  IsoMovingObject var10 = (IsoMovingObject)var8.getMovingObjects().get(var9);
                  if (var10 instanceof IsoPlayer && !((IsoPlayer)var10).isGhostMode()) {
                     return (IsoPlayer)var10;
                  }
               }
            }
         }
      }

      return null;
   }

   private boolean lungeThroughDoor(IsoZombie var1, IsoGridSquare var2, IsoGridSquare var3) {
      if (var2 != null && var3 != null) {
         boolean var4 = var2.getY() > var3.getY();
         IsoGridSquare var5 = null;
         IsoPlayer var6 = null;
         if (var1.getCurrentSquare() == var2) {
            var5 = var3;
            if (var4) {
               var6 = this.findPlayer(var3.getX() - 1, var3.getX() + 1, var3.getY() - 1, var3.getY(), var3.getZ());
            } else {
               var6 = this.findPlayer(var3.getX() - 1, var3.getX(), var3.getY() - 1, var3.getY() + 1, var3.getZ());
            }
         } else if (var1.getCurrentSquare() == var3) {
            var5 = var2;
            if (var4) {
               var6 = this.findPlayer(var2.getX() - 1, var2.getX() + 1, var2.getY(), var2.getY() + 1, var2.getZ());
            } else {
               var6 = this.findPlayer(var2.getX(), var2.getX() + 1, var2.getY() - 1, var2.getY() + 1, var2.getZ());
            }
         }

         if (var6 != null && !LosUtil.lineClearCollide(var5.getX(), var5.getY(), var5.getZ(), PZMath.fastfloor(var6.getX()), PZMath.fastfloor(var6.getY()), PZMath.fastfloor(var6.getZ()), false)) {
            var1.setTarget(var6);
            var1.vectorToTarget.x = var6.getX();
            var1.vectorToTarget.y = var6.getY();
            Vector2 var10000 = var1.vectorToTarget;
            var10000.x -= var1.getX();
            var10000 = var1.vectorToTarget;
            var10000.y -= var1.getY();
            var1.TimeSinceSeenFlesh = 0.0F;
            var1.setThumpTarget((Thumpable)null);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int getFastForwardDamageMultiplier() {
      GameTime var0 = GameTime.getInstance();
      if (GameServer.bServer) {
         return (int)(GameServer.bFastForward ? ServerOptions.instance.FastForwardMultiplier.getValue() / (double)var0.getDeltaMinutesPerDay() : 1.0);
      } else if (GameClient.bClient) {
         return (int)(GameClient.bFastForward ? ServerOptions.instance.FastForwardMultiplier.getValue() / (double)var0.getDeltaMinutesPerDay() : 1.0);
      } else {
         return IsoPlayer.allPlayersAsleep() ? (int)(200.0F * (30.0F / (float)PerformanceSettings.getLockFPS()) / 1.6F) : (int)var0.getTrueMultiplier();
      }
   }

   private boolean isThumpTargetValid(IsoGameCharacter var1, Thumpable var2) {
      if (var2 == null) {
         return false;
      } else if (var2.isDestroyed()) {
         return false;
      } else {
         IsoObject var3 = (IsoObject)Type.tryCastTo(var2, IsoObject.class);
         if (var3 == null) {
            return false;
         } else if (var2 instanceof BaseVehicle) {
            return var3.getMovingObjectIndex() != -1;
         } else if (var3.getObjectIndex() == -1) {
            return false;
         } else {
            int var4 = var3.getSquare().getX() / 8;
            int var5 = var3.getSquare().getY() / 8;
            IsoChunk var6 = GameServer.bServer ? ServerMap.instance.getChunk(var4, var5) : IsoWorld.instance.CurrentCell.getChunk(var4, var5);
            if (var6 == null) {
               return false;
            } else {
               return var2.getThumpableFor(var1) != null;
            }
         }
      }
   }
}
