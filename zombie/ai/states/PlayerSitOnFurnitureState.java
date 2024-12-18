package zombie.ai.states;

import org.joml.Vector3f;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.seating.SeatingManager;
import zombie.util.StringUtils;

public final class PlayerSitOnFurnitureState extends State {
   private static final PlayerSitOnFurnitureState _instance = new PlayerSitOnFurnitureState();

   public PlayerSitOnFurnitureState() {
   }

   public static PlayerSitOnFurnitureState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setIgnoreAimingInput(true);
      var1.setSittingOnFurniture(true);
      if (!(var1.getPrimaryHandItem() instanceof HandWeapon) && !(var1.getSecondaryHandItem() instanceof HandWeapon)) {
         var1.setHideWeaponModel(true);
      }

      if (var1.getStateMachine().getPrevious() == IdleState.instance()) {
         var1.clearVariable("forceGetUp");
         var1.clearVariable("SitOnFurnitureAnim");
         var1.clearVariable("SitOnFurnitureStarted");
      }

      IsoObject var2 = var1.getSitOnFurnitureObject();
      Vector3f var3 = new Vector3f();
      String var4 = var1.getSitOnFurnitureDirection().name();
      String var5 = var1.getVariableString("SitOnFurnitureDirection");
      String var6 = "SitOnFurniture" + var5;
      boolean var7 = SeatingManager.getInstance().getAdjacentPosition(var1, var2, var4, var5, "sitonfurniture", var6, var3);
      if (var7) {
         var1.setX(var3.x);
         var1.setY(var3.y);
         IsoDirections var8 = IsoDirections.fromString(var4);
         IsoDirections var10000;
         switch (var5) {
            case "Front":
               var10000 = var8;
               break;
            case "Left":
               var10000 = var8.RotRight(2);
               break;
            case "Right":
               var10000 = var8.RotLeft(2);
               break;
            default:
               var10000 = var8;
         }

         IsoDirections var9 = var10000;
         var1.getAnimationPlayer().setTargetAndCurrentDirection(var9.ToVector());
      }

   }

   public void execute(IsoGameCharacter var1) {
      var1.getStateMachineParams(this);
      IsoPlayer var3 = (IsoPlayer)var1;
      if (var3.pressedMovement(false)) {
         var1.StopAllActionQueue();
         var1.setVariable("forceGetUp", true);
      } else {
         IsoObject var4 = var1.getSitOnFurnitureObject();
         if (var4 != null && var4.getObjectIndex() != -1) {
            if (!var1.isInvisible() && this.isVisibleZombieNearby(var1)) {
               var1.StopAllActionQueue();
               var1.setVariable("forceGetUp", true);
               var1.setVariable("pressedRunButton", true);
               var1.setVariable("getUpQuick", true);
            } else {
               if (var1.getVariableBoolean("SitOnFurnitureStarted")) {
                  var1.setVariable("SitOnFurnitureAnim", "Idle");
               }

               IsoObject var5 = var3.getSitOnFurnitureObject();
               IsoDirections var6 = var3.getSitOnFurnitureDirection();
               var3.setInitiateAttack(false);
               var3.setAttackStarted(false);
               var3.setAttackType((String)null);
            }
         } else {
            var1.StopAllActionQueue();
            var1.setVariable("forceGetUp", true);
            var1.setVariable("pressedRunButton", true);
            var1.setVariable("getUpQuick", true);
         }
      }
   }

   public void exit(IsoGameCharacter var1) {
      var1.setHideWeaponModel(false);
      if (StringUtils.isNullOrEmpty(var1.getVariableString("HitReaction"))) {
         var1.clearVariable("forceGetUp");
         var1.clearVariable("SitOnFurnitureAnim");
         var1.clearVariable("SitOnFurnitureStarted");
         var1.setIgnoreMovement(false);
      } else if ("hitreaction".equalsIgnoreCase(var1.getCurrentActionContextStateName())) {
         this.abortSitting(var1);
      } else if ("hitreactionpvp".equalsIgnoreCase(var1.getCurrentActionContextStateName())) {
         this.abortSitting(var1);
      }

      var1.setIgnoreAimingInput(false);
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      if (var2.m_EventName.equalsIgnoreCase("SitOnFurnitureStarted")) {
         var1.setVariable("SitOnFurnitureStarted", true);
      }

   }

   public void abortSitting(IsoGameCharacter var1) {
      var1.setHideWeaponModel(false);
      var1.setIgnoreAimingInput(false);
      var1.setIgnoreMovement(false);
      var1.clearVariable("forceGetUp");
      var1.clearVariable("SitOnFurnitureAnim");
      var1.clearVariable("SitOnFurnitureStarted");
      IsoObject var2 = var1.getSitOnFurnitureObject();
      if (var2 != null) {
         var2.setSatChair(false);
      }

      var1.setOnBed(false);
      var1.setSittingOnFurniture(false);
      var1.setSitOnFurnitureObject((IsoObject)null);
      var1.setSitOnFurnitureDirection((IsoDirections)null);
   }

   private boolean isVisibleZombieNearby(IsoGameCharacter var1) {
      if (!IsoPlayer.isLocalPlayer(var1)) {
         return false;
      } else {
         int var2 = PZMath.fastfloor(var1.getX());
         int var3 = PZMath.fastfloor(var1.getY());
         int var4 = PZMath.fastfloor(var1.getZ());
         int var5 = ((IsoPlayer)var1).getIndex();

         for(int var6 = 0; var6 < 8; ++var6) {
            IsoDirections var7 = IsoDirections.fromIndex(var6);
            IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare(var2 + var7.dx(), var3 + var7.dy(), var4);
            if (var8 != null) {
               int var9 = 0;

               for(int var10 = var8.getMovingObjects().size(); var9 < var10; ++var9) {
                  Object var12 = var8.getMovingObjects().get(var9);
                  if (var12 instanceof IsoZombie) {
                     IsoZombie var11 = (IsoZombie)var12;
                     if (!var11.isReanimatedForGrappleOnly() && var8.isCanSee(var5) && var11.getTargetAlpha(var5) > 0.0F) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }
}
