package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.util.StringUtils;

public final class AnimalIdleState extends State {
   private static final AnimalIdleState _instance = new AnimalIdleState();

   public AnimalIdleState() {
   }

   public static AnimalIdleState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      var2.getBehavior().wanderIdle();
      if (var2.eatFromTrough != null) {
         var2.faceThisObject(var2.eatFromTrough);
      }

      if (var2.drinkFromTrough != null) {
         var2.faceThisObject(var2.drinkFromTrough);
      }

      if (var2.isAnimalEating()) {
         var2.getStateMachine().changeState(AnimalEatState.instance(), (Iterable)null);
      }

      if (var2.getVariableBoolean("bMoving")) {
         var2.getStateMachine().changeState(AnimalWalkState.instance(), (Iterable)null);
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.clearVariable("bPlayedPettingSound");
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      if (var2.m_EventName.equalsIgnoreCase("idleActionEnd")) {
         if (StringUtils.isNullOrEmpty(var1.getVariableString("sittingAnim")) && !"sit".equals(var1.getVariableString("idleAction"))) {
            var1.clearVariable("idleAction");
         }

         var1.clearVariable("sittingAnim");
      }

      if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         if ("petting".equalsIgnoreCase(var2.m_ParameterValue)) {
            if (var1.getVariableBoolean("bPlayedPettingSound")) {
               return;
            }

            var1.setVariable("bPlayedPettingSound", true);
         }

         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
         var3.clearVariable("PlayBreedSound");
      }

   }
}
