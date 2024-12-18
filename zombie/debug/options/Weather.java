package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Weather extends OptionGroup {
   public final BooleanDebugOption Fog = this.newDebugOnlyOption("Fog", true);
   public final BooleanDebugOption Fx = this.newDebugOnlyOption("Fx", true);
   public final BooleanDebugOption Snow = this.newDebugOnlyOption("Snow", true);
   public final BooleanDebugOption WaterPuddles = this.newDebugOnlyOption("WaterPuddles", true);

   public Weather() {
   }
}
