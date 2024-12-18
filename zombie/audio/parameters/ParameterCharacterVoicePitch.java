package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterCharacterVoicePitch extends FMODLocalParameter {
   private final IsoPlayer player;

   public ParameterCharacterVoicePitch(IsoPlayer var1) {
      super("CharacterVoicePitch");
      this.player = var1;
   }

   public float calculateCurrentValue() {
      return this.player.getVoicePitch();
   }
}
