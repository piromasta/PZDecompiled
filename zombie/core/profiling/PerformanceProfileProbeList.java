package zombie.core.profiling;

import zombie.util.list.PZArrayUtil;

public class PerformanceProfileProbeList<Probe extends PerformanceProfileProbe> {
   final String m_prefix;
   final Probe[] layers;

   public static PerformanceProfileProbeList<PerformanceProfileProbe> construct(String var0, int var1) {
      return new PerformanceProfileProbeList(var0, var1, PerformanceProfileProbe.class, PerformanceProfileProbe::new);
   }

   public static <Probe extends PerformanceProfileProbe> PerformanceProfileProbeList<Probe> construct(String var0, int var1, Class<Probe> var2, Constructor<Probe> var3) {
      return new PerformanceProfileProbeList(var0, var1, var2, var3);
   }

   protected PerformanceProfileProbeList(String var1, int var2, Class<Probe> var3, Constructor<Probe> var4) {
      this.m_prefix = var1;
      this.layers = (PerformanceProfileProbe[])PZArrayUtil.newInstance(var3, var2 + 1);

      for(int var5 = 0; var5 < var2; ++var5) {
         this.layers[var5] = var4.get(var1 + "_" + var5);
      }

      this.layers[var2] = var4.get(var1 + "_etc");
   }

   public int count() {
      return this.layers.length;
   }

   public Probe at(int var1) {
      return var1 < this.count() ? this.layers[var1] : this.layers[this.count() - 1];
   }

   public Probe start(int var1) {
      PerformanceProfileProbe var2 = this.at(var1);
      var2.start();
      return var2;
   }

   public interface Constructor<Probe extends PerformanceProfileProbe> {
      Probe get(String var1);
   }
}
