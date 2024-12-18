package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;

public final class ParameterCharacterElevation extends FMODGlobalParameter {
   public ParameterCharacterElevation() {
      super("CharacterElevation");
   }

   public float calculateCurrentValue() {
      IsoGameCharacter var1 = this.getCharacter();
      return var1 == null ? 0.0F : var1.getZ();
   }

   private IsoGameCharacter getCharacter() {
      IsoPlayer var1 = null;

      for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         IsoPlayer var3 = IsoPlayer.players[var2];
         if (var3 != null && (var1 == null || var1.isDead() && var3.isAlive() || var1.Traits.Deaf.isSet() && !var3.Traits.Deaf.isSet())) {
            var1 = var3;
         }
      }

      return var1;
   }
}
