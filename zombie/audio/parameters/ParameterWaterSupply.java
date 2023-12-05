package zombie.audio.parameters;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.audio.FMODGlobalParameter;

public final class ParameterWaterSupply extends FMODGlobalParameter {
   public ParameterWaterSupply() {
      super("Water");
   }

   public float calculateCurrentValue() {
      return GameTime.instance.NightsSurvived < SandboxOptions.instance.getWaterShutModifier() ? 1.0F : 0.0F;
   }
}
