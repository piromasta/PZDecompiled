package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterMusicActionStyle extends FMODGlobalParameter {
   public ParameterMusicActionStyle() {
      super("MusicActionStyle");
   }

   public float calculateCurrentValue() {
      return Core.getInstance().getOptionMusicActionStyle() == 2 ? (float)ParameterMusicActionStyle.State.Legacy.label : (float)ParameterMusicActionStyle.State.Official.label;
   }

   public static enum State {
      Official(0),
      Legacy(1);

      final int label;

      private State(int var3) {
         this.label = var3;
      }
   }
}
