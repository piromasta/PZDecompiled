package zombie.sandbox;

import zombie.scripting.ScriptParser;

public final class CustomIntegerSandboxOption extends CustomSandboxOption {
   public final int min;
   public final int max;
   public final int defaultValue;

   CustomIntegerSandboxOption(String var1, int var2, int var3, int var4) {
      super(var1);
      this.min = var2;
      this.max = var3;
      this.defaultValue = var4;
   }

   static CustomIntegerSandboxOption parse(ScriptParser.Block var0) {
      int var1 = getValueInt(var0, "min", -2147483648);
      int var2 = getValueInt(var0, "max", -2147483648);
      int var3 = getValueInt(var0, "default", -2147483648);
      if (var1 != -2147483648 && var2 != -2147483648 && var3 != -2147483648) {
         CustomIntegerSandboxOption var4 = new CustomIntegerSandboxOption(var0.id, var1, var2, var3);
         return !var4.parseCommon(var0) ? null : var4;
      } else {
         return null;
      }
   }
}
