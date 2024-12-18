package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public final class AnimalEatState extends State {
   private static final AnimalEatState _instance = new AnimalEatState();

   public AnimalEatState() {
   }

   public static AnimalEatState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      if (var2.getData().eatingGrass) {
         var1.getEmitter().playSound("AnimalFoleyEatGrass");
      }

   }

   public void execute(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      var2.setStateEventDelayTimer(300.0F);
      if (var2.eatFromTrough != null) {
         var2.faceThisObject(var2.eatFromTrough);
      }

      if (var2.eatFromGround != null) {
         var2.faceThisObject(var2.eatFromGround);
      }

      if (var2.drinkFromTrough != null) {
         var2.faceThisObject(var2.drinkFromTrough);
      }

      var2.setVariable("bMoving", false);
   }

   public void exit(IsoGameCharacter var1) {
      var1.getEmitter().stopOrTriggerSoundByName("AnimalFoleyEatGrass");
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      if (var2.m_EventName.equalsIgnoreCase("idleActionEnd")) {
         IsoAnimal var3 = (IsoAnimal)var1;
         var1.clearVariable("eatingAnim");
         var1.clearVariable("idleAction");
         if (var3.drinkFromTrough != null) {
            var3.getData().drink();
            return;
         }

         if (var3.eatFromGround != null && var3.eatFromGround.getItem().getFluidContainer() != null) {
            var3.getData().drinkFromGround();
            return;
         }

         var3.getData().eat();
      }

   }
}
