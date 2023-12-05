package zombie.radio.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.radio.globals.RadioGlobal;

public final class RadioScriptInfo {
   private final Map<String, RadioGlobal> onStartSetters = new HashMap();
   private final List<ExitOptionOld> exitOptions = new ArrayList();

   public RadioScriptInfo() {
   }

   public RadioScriptEntry getNextScript() {
      RadioScriptEntry var1 = null;
      Iterator var2 = this.exitOptions.iterator();

      while(var2.hasNext()) {
         ExitOptionOld var3 = (ExitOptionOld)var2.next();
         if (var3 != null) {
            var1 = var3.evaluate();
            if (var1 != null) {
               break;
            }
         }
      }

      return var1;
   }

   public void addExitOption(ExitOptionOld var1) {
      if (var1 != null) {
         this.exitOptions.add(var1);
      }

   }
}
