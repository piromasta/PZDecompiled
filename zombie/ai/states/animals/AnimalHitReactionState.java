package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public final class AnimalHitReactionState extends State {
   private static final AnimalHitReactionState _instance = new AnimalHitReactionState();

   public AnimalHitReactionState() {
   }

   public static AnimalHitReactionState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
   }

   public void exit(IsoGameCharacter var1) {
      var1.setHitReaction((String)null);
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      if (var2.m_EventName.equalsIgnoreCase("ActiveAnimFinishing")) {
      }

      if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
      }

   }
}
