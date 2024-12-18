package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.inventory.types.HandWeapon;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.network.GameClient;

public final class AnimalAttackState extends State {
   private static final AnimalAttackState _instance = new AnimalAttackState();

   public AnimalAttackState() {
   }

   public static AnimalAttackState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      if (var2.atkTarget != null) {
         var2.faceThisObject(var2.atkTarget);
      }

      if (var2.thumpTarget != null) {
         var2.faceThisObject(var2.thumpTarget);
         if (var2.thumpTarget.getSquare().DistToProper(var2.getCurrentSquare()) >= 2.0F) {
            var2.thumpTarget = null;
            var2.setDefaultState();
         }
      }

   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      IsoAnimal var4;
      if (var2.m_EventName.equalsIgnoreCase("AttackConnect")) {
         var1.setPerformingAttackAnimation(false);
         if (var3.thumpTarget != null) {
            if (GameClient.bClient && var3.isLocalPlayer()) {
               GameClient.sendAnimalHitThumpable(var1);
            }

            if (var3.thumpTarget instanceof IsoThumpable) {
               IsoThumpable var9 = (IsoThumpable)var3.thumpTarget;
               var9.animalHit(var3);
               if ((float)var9.Health <= 0.0F) {
                  var9.destroy();
                  var3.thumpTarget = null;
                  var3.pathToLocation(var3.getPathTargetX(), var3.getPathTargetY(), var3.getPathTargetZ());
               }

               if (var9.isDoor && var9.IsOpen()) {
                  var3.thumpTarget = null;
                  var3.pathToLocation(var3.getPathTargetX(), var3.getPathTargetY(), var3.getPathTargetZ());
                  return;
               }

               return;
            }
         }

         if (var3.atkTarget instanceof IsoAnimal) {
            var4 = (IsoAnimal)var3.atkTarget;
            var4.HitByAnimal(var3, false);
            var4.getBehavior().blockMovement = false;
            if (var4.isAnimalAttacking() && var4.atkTarget == var3) {
               var3.HitByAnimal(var4, false);
            }

            var3.pathToLocation((int)var4.getX() - 3, (int)var4.getY(), (int)var4.getZ());
            var4.pathToLocation((int)var3.getX() + 3, (int)var3.getY(), (int)var3.getZ());
            var4.getBehavior().blockMovement = false;
            var4.atkTarget = null;
            var3.atkTarget = null;
            if (GameClient.bClient && var3.isLocalPlayer()) {
               GameClient.sendAnimalHitAnimal(var1, var3.atkTarget, 0.0F, false);
            }
         } else {
            float var5;
            if (var3.atkTarget instanceof IsoPlayer) {
               IsoPlayer var6 = (IsoPlayer)var3.atkTarget;
               if (var6.isInvisible() || var6.isGhostMode()) {
                  var3.atkTarget = null;
                  return;
               }

               var5 = var3.calcDamage();
               var6.hitConsequences((HandWeapon)null, var1, false, var5, false);
               if (GameClient.bClient && var3.isLocalPlayer()) {
                  GameClient.sendAnimalHitPlayer(var1, var3.atkTarget, var5, false);
               }
            } else if (var3.thumpTarget instanceof IsoDoor) {
               IsoDoor var7 = (IsoDoor)var3.thumpTarget;
               if (var7.IsOpen()) {
                  var3.thumpTarget = null;
                  var3.pathToLocation(var3.getPathTargetX(), var3.getPathTargetY(), var3.getPathTargetZ());
                  return;
               }

               var5 = 100.0F;
               var5 *= var3.calcDamage();
               var3.thumpTarget.Damage(var5);
            } else if (var3.thumpTarget != null) {
               float var8 = 100.0F;
               var8 *= var3.calcDamage();
               var3.thumpTarget.Damage(var8);
            }
         }

         var3.getBehavior().blockMovement = false;
         var3.atkTarget = null;
      } else if (var2.m_EventName.equalsIgnoreCase("ActiveAnimFinishing")) {
         var1.setPerformingAttackAnimation(false);
         var3.getBehavior().blockMovement = false;
         if (var3.atkTarget instanceof IsoAnimal) {
            var4 = (IsoAnimal)var3.atkTarget;
            var4.HitByAnimal(var3, false);
            var4.getBehavior().blockMovement = false;
            if (var4.isAnimalAttacking() && var4.atkTarget == var3) {
               var3.HitByAnimal(var4, false);
            }

            var3.pathToLocation((int)var4.getX() - 3, (int)var4.getY(), (int)var4.getZ());
            var4.pathToLocation((int)var3.getX() + 3, (int)var3.getY(), (int)var3.getZ());
            var4.getBehavior().blockMovement = false;
            var4.atkTarget = null;
            var3.atkTarget = null;
         }

         var3.atkTarget = null;
      } else if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
      }

   }
}
