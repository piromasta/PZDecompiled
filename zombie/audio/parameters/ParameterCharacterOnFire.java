package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterOnFire extends FMODLocalParameter {
   private final IsoGameCharacter character;

   public ParameterCharacterOnFire(IsoGameCharacter var1) {
      super("CharacterOnFire");
      this.character = var1;
   }

   public float calculateCurrentValue() {
      return this.character.isOnFire() ? 1.0F : 0.0F;
   }
}
