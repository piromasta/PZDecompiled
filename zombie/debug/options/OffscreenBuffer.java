package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class OffscreenBuffer extends OptionGroup {
   public final BooleanDebugOption Render = this.newDebugOnlyOption("Render", true);

   public OffscreenBuffer() {
   }
}
