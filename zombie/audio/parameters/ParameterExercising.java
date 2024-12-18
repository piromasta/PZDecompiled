package zombie.audio.parameters;

import zombie.ai.states.FitnessState;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterExercising extends FMODLocalParameter {
   private final IsoGameCharacter character;

   public ParameterExercising(IsoGameCharacter var1) {
      super("Exercising");
      this.character = var1;
   }

   public float calculateCurrentValue() {
      if (!this.character.isCurrentState(FitnessState.instance())) {
         return 0.0F;
      } else {
         return !this.character.getVariableBoolean("ExerciseStarted") ? 0.0F : 1.0F;
      }
   }
}
