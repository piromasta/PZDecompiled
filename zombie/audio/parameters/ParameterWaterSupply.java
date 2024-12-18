package zombie.audio.parameters;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.audio.FMODGlobalParameter;

public final class ParameterWaterSupply extends FMODGlobalParameter {
   public ParameterWaterSupply() {
      super("Water");
   }

   public float calculateCurrentValue() {
      return (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) < (float)SandboxOptions.instance.getWaterShutModifier() ? 1.0F : 0.0F;
   }
}
