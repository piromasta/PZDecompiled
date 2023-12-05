package zombie.text.templating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.debug.DebugLog;

public class ReplaceProvider implements IReplaceProvider {
   protected final Map<String, IReplace> m_keys = new HashMap();

   public ReplaceProvider() {
   }

   public void addKey(String var1, final String var2) {
      this.addReplacer(var1, new IReplace() {
         public String getString() {
            return var2;
         }
      });
   }

   public void addKey(String var1, KahluaTableImpl var2) {
      try {
         ArrayList var3 = new ArrayList();

         for(int var4 = 1; var4 < var2.len() + 1; ++var4) {
            var3.add((String)var2.rawget(var4));
         }

         if (var3.size() > 0) {
            this.addReplacer(var1, new ReplaceList(var3));
         } else {
            DebugLog.log("ReplaceProvider -> key '" + var1 + "' contains no entries, ignoring.");
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }

   public void addReplacer(String var1, IReplace var2) {
      if (this.m_keys.containsKey(var1.toLowerCase())) {
         DebugLog.log("ReplaceProvider -> Warning: key '" + var1 + "' replaces an existing key.");
      }

      this.m_keys.put(var1.toLowerCase(), var2);
   }

   public boolean hasReplacer(String var1) {
      return this.m_keys.containsKey(var1);
   }

   public IReplace getReplacer(String var1) {
      return (IReplace)this.m_keys.get(var1);
   }
}
