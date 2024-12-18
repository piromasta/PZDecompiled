package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class WorldItemAtlas extends OptionGroup {
   public final BooleanDebugOption Enable = this.newDebugOnlyOption("Enable", true);
   public final BooleanDebugOption Render = this.newDebugOnlyOption("Render", false);

   public WorldItemAtlas() {
   }
}
