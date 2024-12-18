package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.characters.CharacterTimedActions.BaseAction;

public final class ParameterActionProgressPercent extends FMODLocalParameter {
   private final IsoPlayer character;
   private boolean bWasAction = false;

   public ParameterActionProgressPercent(IsoPlayer var1) {
      super("ActionProgressPercent");
      this.character = var1;
   }

   public float calculateCurrentValue() {
      if (this.character.getCharacterActions().isEmpty()) {
         return this.checkWasAction();
      } else {
         BaseAction var1 = (BaseAction)this.character.getCharacterActions().get(0);
         if (var1 == null) {
            return this.checkWasAction();
         } else if (!var1.bStarted) {
            return this.checkWasAction();
         } else if (var1.MaxTime == 0) {
            return this.checkWasAction();
         } else if (var1.finished()) {
            return 100.0F;
         } else {
            this.bWasAction = var1.delta > 0.0F;
            return var1.delta * 100.0F;
         }
      }
   }

   private float checkWasAction() {
      if (this.bWasAction) {
         this.bWasAction = false;
         return 100.0F;
      } else {
         return 0.0F;
      }
   }
}
