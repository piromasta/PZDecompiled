package zombie.popman.animal;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.ai.states.animals.AnimalPathFindState;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.NetworkTeleport;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IsoObjectID;
import zombie.network.fields.AnimalStateVariables;
import zombie.network.packets.character.AnimalPacket;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.util.StringUtils;
import zombie.util.Type;

public class AnimalInstanceManager {
   private static final AnimalInstanceManager instance = new AnimalInstanceManager();
   private static final Vector2 movement = new Vector2();
   private static final IsoObjectID<IsoAnimal> AnimalMap = new IsoObjectID(IsoAnimal.class);

   public AnimalInstanceManager() {
   }

   public static AnimalInstanceManager getInstance() {
      return instance;
   }

   public short allocateID() {
      return AnimalMap.allocateID();
   }

   public void add(IsoAnimal var1, short var2) {
      DebugLog.Animal.debugln("Animal add id=%d", var2);
      AnimalMap.remove(var1);
      var1.setOnlineID(var2);
      AnimalMap.put(var2, var1);
   }

   public void remove(IsoAnimal var1) {
      DebugLog.Animal.debugln("Animal remove id=%d", var1.getOnlineID());
      AnimalMap.remove(var1.getOnlineID());
   }

   public IsoAnimal get(short var1) {
      return (IsoAnimal)AnimalMap.get(var1);
   }

   public IsoObjectID<IsoAnimal> getAnimals() {
      return AnimalMap;
   }

   private void updateLocal(IsoAnimal var1) {
      AnimalPacket var2 = var1.getNetworkCharacterAI().getAnimalPacket();
      var2.realX = var1.getX();
      var2.realY = var1.getY();
      var2.realZ = var1.getZ();
      var2.realDirection = (byte)var1.getDir().index();
      boolean var3 = !var1.isCurrentState(AnimalIdleState.instance());
      if (var3) {
         if (var1.isCurrentState(AnimalPathFindState.instance())) {
            var2.x = var1.getPathFindBehavior2().pathNextX;
            var2.y = var1.getPathFindBehavior2().pathNextY;
         } else {
            var1.getDeferredMovement(movement);
            var2.x = var1.getX() + movement.x * 0.03F * 1000.0F;
            var2.y = var1.getY() + movement.y * 0.03F * 1000.0F;
         }
      } else {
         var2.x = var1.getX();
         var2.y = var1.getY();
      }

      var2.z = var1.getZ();
      var2.direction = var1.getForwardDirection().getDirection();
      var2.stateVariables = AnimalStateVariables.getInstance().getVariables(var1);
      var2.pfbData.set(var1);
      var2.idle = var1.getVariableString("idleAction");
   }

   private void updateRemote(IsoAnimal var1) {
      AnimalPacket var2 = var1.getNetworkCharacterAI().getAnimalPacket();
      PathFindBehavior2 var3 = var1.getPathFindBehavior2();
      NetworkCharacterAI var4 = var1.getNetworkCharacterAI();
      var1.setVariable("idleAction", var2.idle);
      if (StringUtils.isNullOrEmpty(var2.idle) || !var2.idle.startsWith("happy")) {
         var4.targetX = var2.x;
         var4.targetY = var2.y;
         var4.targetZ = (byte)((int)var2.z);
         var4.direction.set((float)Math.cos((double)var2.direction), (float)Math.sin((double)var2.direction));
         var1.realx = var2.realX;
         var1.realy = var2.realY;
         var1.realz = (byte)((int)var2.realZ);
         var1.realState = var2.pfbData.realState;
         var1.setHealth(var2.health);
         if (var2.existanceType == AnimalPacket.Existences.isInHutch) {
            if (var1.hutch != null) {
               IsoHutch var5 = IsoHutch.getHutch(PZMath.fastfloor(var2.realX), PZMath.fastfloor(var2.realY), PZMath.fastfloor(var2.realZ));
               var1.hutch = var5;
            }

            if (var1.hutch != null) {
               if (var1.nestBox != var2.hutchNestBox) {
                  if (var1.nestBox != -1) {
                     var1.hutch.getNestBox(var1.nestBox).animal = null;
                  }

                  if (var2.hutchNestBox != -1) {
                     var1.hutch.getNestBox(Integer.valueOf(var2.hutchNestBox)).animal = var1;
                  }

                  var1.nestBox = var2.hutchNestBox;
               }

               if (var1.getData().getHutchPosition() != var2.hutchPosition) {
                  if (var1.getData().getHutchPosition() != -1) {
                     var1.hutch.animalInside.put(var1.getData().getHutchPosition(), (Object)null);
                  }

                  if (var2.hutchPosition != -1) {
                     var1.hutch.animalInside.put(Integer.valueOf(var2.hutchPosition), var1);
                  }

                  var1.getData().setHutchPosition(var2.hutchPosition);
               }

               if (var2.health == 0.0F && var1.hutch.deadBodiesInside.get(Integer.valueOf(var2.hutchPosition)) == null) {
                  IsoDeadBody var8 = new IsoDeadBody(var1, false);
                  var1.hutch.deadBodiesInside.put(Integer.valueOf(var2.hutchPosition), var8);
               }
            }
         }

         float var9 = IsoUtils.DistanceManhatten(var4.targetX, var4.targetY, var1.getX(), var1.getY());
         if (var9 > 3.0F) {
            var4.forcePathFinder = true;
         }

         if (var4.forcePathFinder && var9 < 2.0F && !PolygonalMap2.instance.lineClearCollide(var1.getX(), var1.getY(), var4.targetX, var4.targetY, (int)var1.getZ(), (IsoMovingObject)null, false, true)) {
            var4.forcePathFinder = false;
         }

         if (!var4.forcePathFinder) {
            var1.getDeferredMovement(movement);
            float var6 = IsoUtils.DistanceTo(var1.getX(), var1.getY(), var4.targetX, var4.targetY) / IsoUtils.DistanceTo(var1.realx, var1.realy, var4.targetX, var4.targetY);
            float var7 = 0.8F + 0.4F * IsoUtils.smoothstep(0.8F, 1.2F, var6);
            if (var9 > 0.2F) {
               var1.setVariable("bMoving", true);
            }

            var3.moveToPoint(var4.targetX, var4.targetY, var7);
         } else {
            if (!var4.usePathFind || var4.targetX != var3.getTargetX() || var4.targetY != var3.getTargetY()) {
               var3.pathToLocationF(var4.targetX, var4.targetY, (float)var4.targetZ);
               var3.walkingOnTheSpot.reset(var1.getX(), var1.getY());
               var4.usePathFind = true;
            }

            PathFindBehavior2.BehaviorResult var10 = var3.update();
            if (var10 == PathFindBehavior2.BehaviorResult.Failed) {
               var1.setPathFindIndex(-1);
               if (var4.forcePathFinder) {
                  var4.forcePathFinder = false;
               } else {
                  NetworkTeleport.teleport(var1, NetworkTeleport.Type.teleportation, var4.targetX, var4.targetY, (byte)var4.targetZ, 1.0F);
               }
            } else if (var10 == PathFindBehavior2.BehaviorResult.Succeeded) {
               IsoWorld.instance.CurrentCell.getChunkForGridSquare((int)var3.getTargetX(), (int)var3.getTargetY(), 0);
            }
         }

         if (var9 < 0.5F) {
            var1.DirectionFromVector(var4.direction);
            var1.setForwardDirection(var4.direction);
         }

         AnimalStateVariables.getInstance().setVariables(var1, var2.stateVariables);
      }
   }

   public void update(IsoAnimal var1) {
      if (GameServer.bServer) {
         AnimalPacket var2 = var1.getNetworkCharacterAI().getAnimalPacket();
         var1.setX(var2.realX);
         var1.setY(var2.realY);
         var1.setZ(var2.realZ);
         var1.setLastX(var2.realX);
         var1.setLastY(var2.realY);
         var1.setLastZ(var2.realZ);
      } else if (GameClient.bClient) {
         if (var1.getNetworkCharacterAI().isRemote()) {
            this.updateRemote(var1);
         } else {
            this.updateLocal(var1);
         }
      }

   }

   public void remove(UdpConnection var1) {
      int var2 = (IsoChunkMap.ChunkGridWidth / 2 + 2) * 8;
      IsoPlayer[] var3 = var1.players;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoPlayer var6 = var3[var5];
         if (var6 != null) {
            int var7 = (int)var6.getX();
            int var8 = (int)var6.getY();
            ArrayList var9 = new ArrayList();
            Iterator var10 = getInstance().getAnimals().iterator();

            while(var10.hasNext()) {
               IsoAnimal var11 = (IsoAnimal)var10.next();
               if (var11 != null && IsoUtils.DistanceTo((float)var7, (float)var8, var11.getX(), var11.getY()) < (float)var2) {
                  var9.add(var11);
               }
            }

            var10 = AnimalOwnershipManager.getInstance().getOwnership(var1).iterator();

            int var17;
            while(var10.hasNext()) {
               var17 = (Short)var10.next();
               IsoAnimal var12 = getInstance().get((short)var17);
               if (var12 != null) {
                  var9.add(var12);
               }
            }

            var9.forEach(IsoAnimal::remove);

            for(int var16 = 0; var16 < 64; ++var16) {
               for(var17 = var8 - var2; var17 <= var8 + var2; ++var17) {
                  for(int var18 = var7 - var2; var18 <= var7 + var2; ++var18) {
                     IsoGridSquare var13 = IsoWorld.instance.CurrentCell.getGridSquare(var18, var17, var16);
                     if (var13 != null) {
                        int var14;
                        for(var14 = var13.getStaticMovingObjects().size() - 1; var14 >= 0; --var14) {
                           IsoDeadBody var15 = (IsoDeadBody)Type.tryCastTo((IsoMovingObject)var13.getStaticMovingObjects().get(var14), IsoDeadBody.class);
                           if (var15 != null && var15.isAnimal()) {
                              GameServer.sendRemoveCorpseFromMap(var15);
                              var15.removeFromWorld();
                              var15.removeFromSquare();
                           }
                        }

                        for(var14 = var13.getMovingObjects().size() - 1; var14 >= 0; --var14) {
                           IsoAnimal var19 = (IsoAnimal)Type.tryCastTo((IsoMovingObject)var13.getMovingObjects().get(var14), IsoAnimal.class);
                           if (var19 != null) {
                              var19.remove();
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public void stop() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = getInstance().getAnimals().iterator();

      while(var2.hasNext()) {
         IsoAnimal var3 = (IsoAnimal)var2.next();
         var1.add(var3);
      }

      var1.forEach(IsoAnimal::remove);
   }
}
