package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public final class AnimalFalldownState extends State {
   private static final AnimalFalldownState _instance = new AnimalFalldownState();

   public AnimalFalldownState() {
   }

   public static AnimalFalldownState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      if (var2.m_EventName.equalsIgnoreCase("ActiveAnimFinishing")) {
      }

      if (var2.m_EventName.equalsIgnoreCase("PlayDeathSound")) {
         var1.setDoDeathSound(false);
         var1.playDeadSound();
      }

      if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
      }

   }
}
