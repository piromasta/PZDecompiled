package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.IsoWorld;

public final class ParameterPowerSupply extends FMODGlobalParameter {
   public ParameterPowerSupply() {
      super("Electricity");
   }

   public float calculateCurrentValue() {
      return IsoWorld.instance != null && IsoWorld.instance.isHydroPowerOn() ? 1.0F : 0.0F;
   }
}
