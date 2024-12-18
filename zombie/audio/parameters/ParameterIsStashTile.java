package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoObject;

public final class ParameterIsStashTile extends FMODLocalParameter {
   private final IsoGameCharacter character;

   public ParameterIsStashTile(IsoGameCharacter var1) {
      super("IsStashTile");
      this.character = var1;
   }

   public float calculateCurrentValue() {
      return this.isCharacterOnStashTile() ? 1.0F : 0.0F;
   }

   private boolean isCharacterOnStashTile() {
      if (this.character.getCurrentSquare() == null) {
         return false;
      } else {
         IsoObject var1 = this.character.getCurrentSquare().getHiddenStash();
         return var1 != null;
      }
   }
}
