package zombie.text.templating;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.debug.DebugLog;

public class ReplaceProviderLua extends ReplaceProvider {
   private static final ConcurrentLinkedDeque<ReplaceSingle> pool_single = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<ReplaceList> pool_list = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<ReplaceProviderLua> pool = new ConcurrentLinkedDeque();

   public ReplaceProviderLua() {
   }

   private static ReplaceSingle alloc_single() {
      ReplaceSingle var0 = (ReplaceSingle)pool_single.poll();
      if (var0 == null) {
         var0 = new ReplaceSingle();
      }

      return var0;
   }

   private static void release_single(ReplaceSingle var0) {
      pool_single.offer(var0);
   }

   private static ReplaceList alloc_list() {
      ReplaceList var0 = (ReplaceList)pool_list.poll();
      if (var0 == null) {
         var0 = new ReplaceList();
      }

      return var0;
   }

   private static void release_list(ReplaceList var0) {
      var0.getReplacements().clear();
      pool_list.offer(var0);
   }

   protected static ReplaceProviderLua Alloc() {
      ReplaceProviderLua var0 = (ReplaceProviderLua)pool.poll();
      if (var0 == null) {
         var0 = new ReplaceProviderLua();
      }

      var0.reset();
      return var0;
   }

   private void reset() {
      Iterator var1 = this.m_keys.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         if (var2.getValue() instanceof ReplaceList) {
            release_list((ReplaceList)var2.getValue());
         } else {
            release_single((ReplaceSingle)var2.getValue());
         }
      }

      this.m_keys.clear();
   }

   public void release() {
      this.reset();
      pool.offer(this);
   }

   public void fromLuaTable(KahluaTableImpl var1) {
      Iterator var2 = var1.delegate.entrySet().iterator();

      while(true) {
         while(true) {
            Map.Entry var3;
            do {
               if (!var2.hasNext()) {
                  return;
               }

               var3 = (Map.Entry)var2.next();
            } while(!(var3.getKey() instanceof String));

            if (var3.getValue() instanceof String) {
               this.addKey((String)var3.getKey(), (String)var3.getValue());
            } else if (var3.getValue() instanceof KahluaTableImpl) {
               KahluaTableImpl var4 = (KahluaTableImpl)var3.getValue();
               ReplaceList var5 = alloc_list();

               for(int var6 = 1; var6 < var4.len() + 1; ++var6) {
                  var5.getReplacements().add((String)var4.rawget(var6));
               }

               if (var5.getReplacements().size() > 0) {
                  this.addReplacer((String)var3.getKey(), var5);
               } else {
                  DebugLog.log("ReplaceProvider -> key '" + var3.getKey() + "' contains no entries, ignoring.");
                  release_list(var5);
               }
            }
         }
      }
   }

   public void addKey(String var1, String var2) {
      ReplaceSingle var3 = alloc_single();
      var3.setValue(var2);
      this.addReplacer(var1, var3);
   }
}
