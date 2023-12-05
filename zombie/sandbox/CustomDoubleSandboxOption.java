package zombie.sandbox;

import zombie.scripting.ScriptParser;

public final class CustomDoubleSandboxOption extends CustomSandboxOption {
   public final double min;
   public final double max;
   public final double defaultValue;

   CustomDoubleSandboxOption(String var1, double var2, double var4, double var6) {
      super(var1);
      this.min = var2;
      this.max = var4;
      this.defaultValue = var6;
   }

   static CustomDoubleSandboxOption parse(ScriptParser.Block var0) {
      double var1 = getValueDouble(var0, "min", 0.0 / 0.0);
      double var3 = getValueDouble(var0, "max", 0.0 / 0.0);
      double var5 = getValueDouble(var0, "default", 0.0 / 0.0);
      if (!Double.isNaN(var1) && !Double.isNaN(var3) && !Double.isNaN(var5)) {
         CustomDoubleSandboxOption var7 = new CustomDoubleSandboxOption(var0.id, var1, var3, var5);
         return !var7.parseCommon(var0) ? null : var7;
      } else {
         return null;
      }
   }
}
