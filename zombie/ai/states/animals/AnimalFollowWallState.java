package zombie.ai.states.animals;

import java.util.HashMap;
import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;
import zombie.network.GameServer;

public class AnimalFollowWallState extends State {
   private static final AnimalFollowWallState _instance = new AnimalFollowWallState();
   private final Vector2 temp = new Vector2();
   private static final Integer PARAM_REPATHDELAY = 0;
   private static final Integer PARAM_TIMETOSTOP_FOLLOWING_WALL = 1;
   private static final Integer PARAM_CW = 2;
   private static final Integer PARAM_CURRENTDIR = 3;

   public AnimalFollowWallState() {
   }

   public static AnimalFollowWallState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      if (var1.getCurrentSquare() != null) {
         HashMap var2 = var1.getStateMachineParams(this);
         var2.put(PARAM_REPATHDELAY, 0.0F);
         float var3 = var1.getCurrentSquare().DistToProper(var1.getPathTargetX(), var1.getPathTargetY()) * 80.0F;
         if (var3 < 500.0F) {
            var3 = 500.0F;
         }

         var2.put(PARAM_TIMETOSTOP_FOLLOWING_WALL, var3);
         var2.put(PARAM_CW, (Object)null);
         var2.put(PARAM_CURRENTDIR, (Object)null);
         var1.setVariable("bMoving", true);
      }
   }

   public boolean decideRotation(IsoAnimal var1, IsoDirections var2) {
      HashMap var3 = var1.getStateMachineParams(this);
      Boolean var4 = (Boolean)var3.get(PARAM_CW);
      if (var4 != null) {
         return var4;
      } else if (var1.getDir() == var2) {
         var3.put(PARAM_CW, Rand.NextBool(2));
         return (Boolean)var3.get(PARAM_CW);
      } else if (var2 == IsoDirections.N) {
         if (var1.getDir() != IsoDirections.NE && var1.getDir() != IsoDirections.E) {
            var3.put(PARAM_CW, false);
            return (Boolean)var3.get(PARAM_CW);
         } else {
            var3.put(PARAM_CW, true);
            return (Boolean)var3.get(PARAM_CW);
         }
      } else if (var2 == IsoDirections.E) {
         if (var1.getDir() != IsoDirections.SE && var1.getDir() != IsoDirections.S) {
            var3.put(PARAM_CW, false);
            return (Boolean)var3.get(PARAM_CW);
         } else {
            var3.put(PARAM_CW, true);
            return (Boolean)var3.get(PARAM_CW);
         }
      } else if (var2 == IsoDirections.S) {
         if (var1.getDir() != IsoDirections.SW && var1.getDir() != IsoDirections.W) {
            var3.put(PARAM_CW, false);
            return (Boolean)var3.get(PARAM_CW);
         } else {
            var3.put(PARAM_CW, true);
            return (Boolean)var3.get(PARAM_CW);
         }
      } else if (var2 == IsoDirections.W) {
         if (var1.getDir() != IsoDirections.NW && var1.getDir() != IsoDirections.N) {
            var3.put(PARAM_CW, false);
            return (Boolean)var3.get(PARAM_CW);
         } else {
            var3.put(PARAM_CW, true);
            return (Boolean)var3.get(PARAM_CW);
         }
      } else {
         var3.put(PARAM_CW, false);
         return (Boolean)var3.get(PARAM_CW);
      }
   }

   public void execute(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      HashMap var3 = var1.getStateMachineParams(this);
      this.updateParams(var2);
      float var4 = (Float)var3.get(PARAM_REPATHDELAY);
      if (var4 > 0.0F) {
         var4 -= GameTime.getInstance().getMultiplier();
         var3.put(PARAM_REPATHDELAY, var4);
      } else {
         var3.put(PARAM_REPATHDELAY, 10.0F);
         this.temp.x = var2.getPathFindBehavior2().getTargetX();
         this.temp.y = var2.getPathFindBehavior2().getTargetY();
         Vector2 var10000 = this.temp;
         var10000.x -= var2.getX();
         var10000 = this.temp;
         var10000.y -= var2.getY();
         float var5 = this.temp.getLength();
         boolean var6 = var1.isCollidedThisFrame();
         if (var6) {
            this.followWall(var2);
         }

         this.checkNoCollide(var2);
         if (!var6) {
         }

         if (!GameServer.bServer) {
            float var7 = Math.min(var5 / 2.0F, 4.0F);
            float var8 = (float)((var1.getID() + var2.animalID) % 20) / 10.0F - 1.0F;
            float var9 = (float)((var2.getID() + var2.animalID) % 20) / 10.0F - 1.0F;
            var10000 = this.temp;
            var10000.x += var2.getX();
            var10000 = this.temp;
            var10000.y += var2.getY();
            var10000 = this.temp;
            var10000.x += var8 * var7;
            var10000 = this.temp;
            var10000.y += var9 * var7;
            var10000 = this.temp;
            var10000.x -= var2.getX();
            var10000 = this.temp;
            var10000.y -= var2.getY();
         }

         this.temp.normalize();
         var2.setDir(IsoDirections.fromAngle(this.temp));
         var2.setForwardDirection(this.temp);
      }
   }

   public void checkNoCollide(IsoAnimal var1) {
      if (Rand.NextBool(5)) {
         byte var2 = 7;
         HashMap var3 = var1.getStateMachineParams(this);
         IsoDirections var4 = (IsoDirections)var3.get(PARAM_CURRENTDIR);
         if (var1.getDir().dy() != 0 || (var1.getDir().dx() != 1 || var4 != IsoDirections.E) && (var1.getDir().dx() != -1 || var4 != IsoDirections.W)) {
            if (var1.getDir().dx() == 0 && (var1.getDir().dy() == 1 && var4 == IsoDirections.S || var1.getDir().dy() == -1 && var4 == IsoDirections.N)) {
               if (!var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0) && !var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
                  if (Rand.NextBool(2) && !var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
                     var1.setShouldFollowWall(false);
                     var1.pathToLocation((int)var1.getX() + var2, (int)var1.getY(), (int)var1.getZ());
                     var1.setVariable("bMoving", true);
                  } else if (!var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0)) {
                     var1.setShouldFollowWall(false);
                     var1.pathToLocation((int)var1.getX() - var2, (int)var1.getY(), (int)var1.getZ());
                     var1.setVariable("bMoving", true);
                  }

               }
            }
         } else if (!var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0) && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, 1, 0)) {
            if (Rand.NextBool(2) && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, 1, 0)) {
               var1.setShouldFollowWall(false);
               var1.pathToLocation((int)var1.getX(), (int)var1.getY() + var2, (int)var1.getZ());
               var1.setVariable("bMoving", true);
            } else if (!var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0)) {
               var1.setShouldFollowWall(false);
               var1.pathToLocation((int)var1.getX(), (int)var1.getY() - var2, (int)var1.getZ());
               var1.setVariable("bMoving", true);
            }

         }
      }
   }

   public void noCollide(IsoAnimal var1) {
      if (Rand.NextBool(7)) {
         byte var2 = 7;
         if (var1.getDir().dy() == 0 && (var1.getDir().dx() == 1 || var1.getDir().dx() == -1)) {
            if (!var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0) && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, 1, 0)) {
               if (Rand.NextBool(2) && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, 1, 0)) {
                  this.go((int)var1.getX(), (int)var1.getY() + var2, var1);
               } else if (!var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0)) {
                  this.go((int)var1.getX(), (int)var1.getY() - var2, var1);
               }

            }
         } else if (var1.getDir().dx() == 0 && (var1.getDir().dy() == 1 || var1.getDir().dy() == -1)) {
            if (!var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0) && !var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
               if (Rand.NextBool(2) && !var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
                  this.go((int)var1.getX() + var2, (int)var1.getY(), var1);
               } else if (!var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0)) {
                  this.go((int)var1.getX() - var2, (int)var1.getY(), var1);
               }

            }
         }
      }
   }

   public boolean continueFollowingWall(IsoAnimal var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      Boolean var3 = (Boolean)var2.get(PARAM_CW);
      IsoDirections var4 = (IsoDirections)var2.get(PARAM_CURRENTDIR);
      return false;
   }

   public void followWall(IsoAnimal var1) {
      var1.followingWall = true;
      HashMap var2 = var1.getStateMachineParams(this);
      Boolean var3 = (Boolean)var2.get(PARAM_CW);
      IsoDirections var4 = (IsoDirections)var2.get(PARAM_CURRENTDIR);
      if (!this.continueFollowingWall(var1)) {
         byte var5 = 40;
         byte var6 = 4;
         if (var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0)) {
            var3 = this.decideRotation(var1, IsoDirections.N);
            if (var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
               if (var4 != IsoDirections.E) {
                  this.go((int)var1.getX() + var5, (int)var1.getY() + var6, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.E);
               }

            } else if (!var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0)) {
               if (var4 != IsoDirections.W) {
                  this.go((int)var1.getX() - var5, (int)var1.getY() + var6, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.W);
               }

            } else {
               this.go((int)var1.getX(), (int)var1.getY() + var5, var1);
            }
         } else if (var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
            var3 = this.decideRotation(var1, IsoDirections.E);
            if (var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, -1, 1, 0)) {
               if (var4 != IsoDirections.S) {
                  this.go((int)var1.getX() - var6, (int)var1.getY() + var5, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.S);
               }

            } else if (!var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0)) {
               if (var4 != IsoDirections.N) {
                  this.go((int)var1.getX() - var6, (int)var1.getY() - var5, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.N);
               }

            } else {
               this.go((int)var1.getX() - var5, (int)var1.getY(), var1);
            }
         } else if (var1.getCurrentSquare().testCollideAdjacent(var1, 0, 1, 0)) {
            var3 = this.decideRotation(var1, IsoDirections.S);
            if (var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0)) {
               if (var4 != IsoDirections.W) {
                  this.go((int)var1.getX() - var5, (int)var1.getY() - var6, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.W);
               }

            } else if (!var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, 1, 0, 0)) {
               if (var4 != IsoDirections.E) {
                  this.go((int)var1.getX() + var5, (int)var1.getY() - var6, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.E);
               }

            } else {
               this.go((int)var1.getX(), (int)var1.getY() - var5, var1);
            }
         } else if (var1.getCurrentSquare().testCollideAdjacent(var1, -1, 0, 0)) {
            var3 = this.decideRotation(var1, IsoDirections.W);
            if (var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, -1, 0)) {
               if (var4 != IsoDirections.N) {
                  this.go((int)var1.getX() + var6, (int)var1.getY() - var5, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.N);
               }

            } else if (!var3 && !var1.getCurrentSquare().testCollideAdjacent(var1, 0, 1, 0)) {
               if (var4 != IsoDirections.S) {
                  this.go((int)var1.getX() + var6, (int)var1.getY() + var5, var1);
                  var2.put(PARAM_CURRENTDIR, IsoDirections.S);
               }

            } else {
               this.go((int)var1.getX() + var5, (int)var1.getY(), var1);
            }
         }
      }
   }

   public void go(int var1, int var2, IsoAnimal var3) {
      var3.getStateMachineParams(this);
      var3.getPathFindBehavior2().reset();
      var3.getPathFindBehavior2().pathToLocation(var1, var2, (int)var3.getZ());
   }

   public void updateParams(IsoAnimal var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      float var3 = (Float)var2.get(PARAM_TIMETOSTOP_FOLLOWING_WALL);
      var3 -= GameTime.getInstance().getMultiplier();
      if (var3 <= 0.0F) {
         var1.getPathFindBehavior2().reset();
         var1.setVariable("bMoving", false);
         var1.followingWall = false;
         var1.setShouldFollowWall(false);
      } else {
         var2.put(PARAM_TIMETOSTOP_FOLLOWING_WALL, var3);
      }
   }

   public void exit(IsoGameCharacter var1) {
      var1.setVariable("bMoving", false);
      ((IsoAnimal)var1).followingWall = false;
      ((IsoAnimal)var1).setShouldFollowWall(false);
      ((IsoAnimal)var1).getBehavior().doBehaviorAction();
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
      }

      super.animEvent(var1, var2);
   }

   public boolean isMoving(IsoGameCharacter var1) {
      return true;
   }
}
