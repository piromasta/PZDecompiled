package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;

public final class ParameterMusicState extends FMODGlobalParameter {
   private State state;

   public ParameterMusicState() {
      super("MusicState");
      this.state = ParameterMusicState.State.MainMenu;
   }

   public float calculateCurrentValue() {
      return (float)this.state.label;
   }

   public void setState(State var1) {
      this.state = var1;
   }

   public static enum State {
      MainMenu(0),
      Loading(1),
      InGame(2),
      PauseMenu(3),
      Tutorial(4);

      final int label;

      private State(int var3) {
         this.label = var3;
      }
   }
}
