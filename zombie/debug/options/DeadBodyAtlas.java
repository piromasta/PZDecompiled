package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class DeadBodyAtlas extends OptionGroup {
   public final BooleanDebugOption Render = this.newOption("Render", false);

   public DeadBodyAtlas() {
   }
}
