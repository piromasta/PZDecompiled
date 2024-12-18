package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.scripting.objects.Item;

public class ItemTags {
   private static final ArrayList<Item> emptyList = new ArrayList();
   private static final Map<String, ArrayList<Item>> tagItemMap = new HashMap();

   public ItemTags() {
   }

   public static void Init(ArrayList<Item> var0) {
      tagItemMap.clear();
      Iterator var1 = var0.iterator();

      while(true) {
         Item var2;
         do {
            if (!var1.hasNext()) {
               if (Core.bDebug) {
               }

               return;
            }

            var2 = (Item)var1.next();
         } while(var2.getTags().size() <= 0);

         Iterator var3 = var2.getTags().iterator();

         while(var3.hasNext()) {
            String var4 = (String)var3.next();
            registerItemTag(var4, var2);
         }
      }
   }

   private static void registerItemTag(String var0, Item var1) {
      if (!tagItemMap.containsKey(var0)) {
         tagItemMap.put(var0, new ArrayList());
      }

      if (!((ArrayList)tagItemMap.get(var0)).contains(var1)) {
         ((ArrayList)tagItemMap.get(var0)).add(var1);
      }

   }

   public static ArrayList<Item> getItemsForTag(String var0) {
      return tagItemMap.containsKey(var0) ? (ArrayList)tagItemMap.get(var0) : emptyList;
   }

   private static void printDebug() {
      DebugLog.log("==== ITEM TAGS ====");
      Iterator var0 = tagItemMap.entrySet().iterator();

      while(var0.hasNext()) {
         Map.Entry var1 = (Map.Entry)var0.next();
         DebugLog.log("[tag: " + (String)var1.getKey() + "]");
         Iterator var2 = ((ArrayList)var1.getValue()).iterator();

         while(var2.hasNext()) {
            Item var3 = (Item)var2.next();
            DebugLog.log("  - " + var3.getFullName());
         }
      }

      DebugLog.log("===/ ITEM TAGS /===");
   }
}
