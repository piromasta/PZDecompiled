package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterCharacterVoiceType extends FMODLocalParameter {
   private final IsoPlayer player;

   public ParameterCharacterVoiceType(IsoPlayer var1) {
      super("CharacterVoiceType");
      this.player = var1;
   }

   public float calculateCurrentValue() {
      return (float)this.player.getVoiceType();
   }
}
