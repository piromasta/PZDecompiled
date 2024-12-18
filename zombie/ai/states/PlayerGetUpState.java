package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.util.Type;

public final class PlayerGetUpState extends State {
   private static final PlayerGetUpState _instance = new PlayerGetUpState();

   public PlayerGetUpState() {
   }

   public static PlayerGetUpState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      var1.setIgnoreMovement(true);
      IsoPlayer var3 = (IsoPlayer)var1;
      var3.setInitiateAttack(false);
      var3.setAttackStarted(false);
      var3.setAttackType((String)null);
      var3.setBlockMovement(true);
      var3.setForceRun(false);
      var3.setForceSprint(false);
      var1.setVariable("getUpQuick", var1.getVariableBoolean("pressedRunButton"));
      if (var1.getMoodles().getMoodleLevel(MoodleType.Panic) > 1) {
         var1.setVariable("getUpQuick", true);
      }

      if (var1.getVariableBoolean("pressedMovement")) {
         var1.setVariable("getUpWalk", true);
      }

      var2.put(0, var1.getDir());
      if (var1.getVariableBoolean("SittingOnFurniture")) {
         String var4 = var1.getVariableString("SitOnFurnitureDirection");
         if (var4 != null) {
            float var5 = 0.0F;
            var1.faceDirection(var1.getDir());
         }
      }

      if (GameClient.bClient) {
         var1.setKnockedDown(false);
      }

   }

   public void execute(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      String var3;
      float var4;
      IsoDirections var5;
      if (var1.isOnBed()) {
         var3 = var1.getVariableString("OnBedDirection");
         if (var3 != null) {
            var4 = 0.0F;
            var5 = (IsoDirections)Type.tryCastTo(var2.get(0), IsoDirections.class);
            switch (var3) {
               case "Foot":
                  return;
               case "FootLeft":
               case "HeadLeft":
                  var4 = 0.0F;
                  if (var5 == IsoDirections.N) {
                     var4 = 180.0F;
                  }

                  if (var5 == IsoDirections.W) {
                     var4 = 90.0F;
                  }

                  if (var5 == IsoDirections.E) {
                     var4 = 270.0F;
                  }
                  break;
               case "FootRight":
               case "HeadRight":
                  var4 = 180.0F;
                  if (var5 == IsoDirections.N) {
                     var4 = 0.0F;
                  }

                  if (var5 == IsoDirections.W) {
                     var4 = 270.0F;
                  }

                  if (var5 == IsoDirections.E) {
                     var4 = 90.0F;
                  }
            }

            var1.blockTurning = true;
            Vector2 var6 = Vector2.fromLengthDirection(1.0F, var4 * 0.017453292F);
            var1.faceLocationF(var1.getX() + var6.x, var1.getY() + var6.y);
         }
      }

      if (var1.getVariableBoolean("SittingOnFurniture")) {
         var3 = var1.getVariableString("SitOnFurnitureDirection");
         if (var3 != null) {
            var4 = 0.0F;
            var5 = (IsoDirections)Type.tryCastTo(var2.get(0), IsoDirections.class);
            switch (var3) {
               case "Front":
                  if (var5 == IsoDirections.N) {
                     var4 = 270.0F;
                  }

                  if (var5 == IsoDirections.S) {
                     var4 = 90.0F;
                  }

                  if (var5 == IsoDirections.W) {
                     var4 = 180.0F;
                  }
                  break;
               case "Left":
                  var4 = 0.0F;
                  if (var5 == IsoDirections.N) {
                     var4 = 180.0F;
                  }

                  if (var5 == IsoDirections.W) {
                     var4 = 90.0F;
                  }

                  if (var5 == IsoDirections.E) {
                     var4 = 270.0F;
                  }
                  break;
               case "Right":
                  var4 = 180.0F;
                  if (var5 == IsoDirections.N) {
                     var4 = 0.0F;
                  }

                  if (var5 == IsoDirections.W) {
                     var4 = 270.0F;
                  }

                  if (var5 == IsoDirections.E) {
                     var4 = 90.0F;
                  }
            }

            var1.getAnimationPlayer().setTargetAngle(var4 * 0.017453292F);
         }
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.clearVariable("getUpWalk");
      if (var1.isOnBed()) {
         var1.blockTurning = false;
         var1.setHideWeaponModel(false);
      }

      if (var1.isSittingOnFurniture()) {
         var1.setHideWeaponModel(false);
      }

      if (var1.getVariableBoolean("sitonground")) {
         var1.setHideWeaponModel(false);
      }

      var1.setIgnoreMovement(false);
      var1.setFallOnFront(false);
      var1.setOnFloor(false);
      ((IsoPlayer)var1).setBlockMovement(false);
      IsoObject var2 = var1.getSitOnFurnitureObject();
      if (var2 != null) {
         var2.setSatChair(false);
         this.ejectFromSolidFurniture(var1, var2);
      }

      var1.setOnBed(false);
      var1.setSittingOnFurniture(false);
      var1.setSitOnFurnitureObject((IsoObject)null);
      var1.setSitOnFurnitureDirection((IsoDirections)null);
      var1.setSitOnGround(false);
   }

   private void ejectFromSolidFurniture(IsoGameCharacter var1, IsoObject var2) {
      IsoGridSquare var3 = var2.getSquare();
      if (var3 != null) {
         if (var3.isSolid() || var3.isSolidTrans()) {
            if (var3 == var1.getCurrentSquare()) {
               IsoGridSquare var4 = var3.getAdjacentSquare(var1.getDir());
               if (var4 != null) {
                  int var5 = var4.getX() - var3.getX();
                  int var6 = var4.getY() - var3.getY();
                  if (!var3.testCollideAdjacent(var1, var5, var6, 0)) {
                     if (var1.getDir() == IsoDirections.N) {
                        var1.setY((float)var3.getY() - 0.05F);
                     } else if (var1.getDir() == IsoDirections.S) {
                        var1.setY((float)var4.getY() + 0.05F);
                     } else if (var1.getDir() == IsoDirections.W) {
                        var1.setX((float)var3.getX() - 0.05F);
                     } else if (var1.getDir() == IsoDirections.E) {
                        var1.setX((float)var4.getX() + 0.05F);
                     }

                  }
               }
            }
         }
      }
   }
}
