package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.util.StringUtils;

public interface IDebugOptionGroup extends IDebugOption {
   Iterable<IDebugOption> getChildren();

   void addChild(IDebugOption var1);

   void removeChild(IDebugOption var1);

   void onChildAdded(IDebugOption var1);

   void onDescendantAdded(IDebugOption var1);

   default <E extends IDebugOptionGroup> E newOptionGroup(E var1) {
      this.addChild(var1);
      return var1;
   }

   default BooleanDebugOption newOption(String var1, boolean var2) {
      return BooleanDebugOption.newOption(this, var1, var2);
   }

   default BooleanDebugOption newDebugOnlyOption(String var1, boolean var2) {
      return BooleanDebugOption.newDebugOnlyOption(this, var1, var2);
   }

   default String getCombinedName(String var1) {
      String var2 = this.getName();
      if (StringUtils.isNullOrWhitespace(var2)) {
         return var1;
      } else {
         String var3 = String.format("%s.%s", var2, var1);
         return var3;
      }
   }
}
