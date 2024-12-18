package zombie.core.profiling;

import zombie.GameProfiler;

public class PerformanceProfileFrameProbe extends PerformanceProfileProbe {
   public PerformanceProfileFrameProbe(String var1) {
      super(var1);
   }

   public void start() {
      GameProfiler.getInstance().startFrame(this.Name);
      super.start();
   }

   public void end() {
      super.end();
      GameProfiler.getInstance().endFrame();
   }
}
