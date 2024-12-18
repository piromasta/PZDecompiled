package zombie.core.profiling;

import zombie.GameProfiler;

public interface IPerformanceProbe {
   default boolean isProbeEnabled() {
      return this.isEnabled() && GameProfiler.isRunning();
   }

   boolean isEnabled();

   void setEnabled(boolean var1);
}
