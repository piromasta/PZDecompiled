package zombie.debug;

import zombie.config.BooleanConfigOption;
import zombie.core.Core;
import zombie.debug.options.IDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.debug.options.OptionGroup;

public class BooleanDebugOption extends BooleanConfigOption implements IDebugOption {
   private IDebugOptionGroup m_parent;
   private final boolean m_debugOnly;
   private String m_fullPath;

   public BooleanDebugOption(String var1, boolean var2, boolean var3) {
      super(var1, var3);
      this.m_fullPath = var1;
      this.m_debugOnly = var2;
   }

   public String getName() {
      return this.m_fullPath;
   }

   public boolean getValue() {
      return !Core.bDebug && this.isDebugOnly() ? super.getDefaultValue() : super.getValue();
   }

   public boolean isDebugOnly() {
      return this.m_debugOnly;
   }

   public IDebugOptionGroup getParent() {
      return this.m_parent;
   }

   public void setParent(IDebugOptionGroup var1) {
      this.m_parent = var1;
      this.m_fullPath = OptionGroup.getCombinedName(this.m_parent, this.name);
   }

   public void onFullPathChanged() {
      this.m_fullPath = OptionGroup.getCombinedName(this.m_parent, this.name);
   }

   public static BooleanDebugOption newOption(IDebugOptionGroup var0, String var1, boolean var2) {
      return newOptionInternal(var0, var1, false, var2);
   }

   public static BooleanDebugOption newDebugOnlyOption(IDebugOptionGroup var0, String var1, boolean var2) {
      return newOptionInternal(var0, var1, true, var2);
   }

   private static BooleanDebugOption newOptionInternal(IDebugOptionGroup var0, String var1, boolean var2, boolean var3) {
      BooleanDebugOption var4 = new BooleanDebugOption(var1, var2, var3);
      if (var0 != null) {
         var0.addChild(var4);
      }

      return var4;
   }
}
