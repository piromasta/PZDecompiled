package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@DebugClassFields
public class ItemFilterScript extends BaseScriptObject {
   private final FilterTypeInfo whitelist = new FilterTypeInfo();
   private final FilterTypeInfo blacklist = new FilterTypeInfo();
   private boolean hasParsed = false;
   private String name;
   private final ArrayList<Item> tempScriptItems = new ArrayList();

   public ItemFilterScript() {
      super(ScriptType.ItemFilter);
   }

   public String getName() {
      return this.name;
   }

   public void PreReload() {
      this.hasParsed = false;
      this.whitelist.reset();
      this.blacklist.reset();
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) {
   }

   public void OnLoadedAfterLua() {
      this.parseFilter();
   }

   private void parseFilter() {
      if (!this.hasParsed) {
         this.resolveItemTypes(this.whitelist);
         this.resolveItemTypes(this.blacklist);
         this.whitelist.items.clear();
         this.whitelist.items.addAll(this.whitelist.loadedItems);
         if (!this.whitelist.items.isEmpty()) {
            ScriptManager.resolveGetItemTypes(this.whitelist.items, this.tempScriptItems);
         }

         this.blacklist.items.clear();
         this.blacklist.items.addAll(this.blacklist.loadedItems);
         if (!this.blacklist.items.isEmpty()) {
            ScriptManager.resolveGetItemTypes(this.blacklist.items, this.tempScriptItems);
         }

         this.hasParsed = true;
      } else {
         DebugLog.General.warn("Already parsed filter: " + this.name);
      }

   }

   private void resolveItemTypes(FilterTypeInfo var1) {
      if (!var1.loadedTypes.isEmpty()) {
         Iterator var2 = var1.loadedTypes.iterator();

         while(var2.hasNext()) {
            String var3 = (String)var2.next();
            Item.Type var4 = Item.Type.valueOf(var3);
            if (!var1.types.contains(var4)) {
               var1.types.add(var4);
            }
         }
      }

   }

   public void OnPostWorldDictionaryInit() {
   }

   public boolean allowsItem(InventoryItem var1) {
      if (this.blacklist.containsItem(var1)) {
         return false;
      } else {
         return !this.whitelist.hasEntries() || this.whitelist.containsItem(var1);
      }
   }

   public boolean allowsItem(Item var1) {
      if (this.blacklist.containsItem(var1)) {
         return false;
      } else {
         return !this.whitelist.hasEntries() || this.whitelist.containsItem(var1);
      }
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      this.name = var1;
      super.LoadCommonBlock(var3);
      this.readBlock(var3, this.whitelist);
   }

   private void readBlock(ScriptParser.Block var1, FilterTypeInfo var2) {
      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (!var5.isEmpty() && !var6.isEmpty()) {
            if (var5.equalsIgnoreCase("items")) {
               this.parseInputString(var2.loadedItems, var6);
            } else if (var5.equalsIgnoreCase("types")) {
               this.parseInputString(var2.loadedTypes, var6);
            } else if (var5.equalsIgnoreCase("tags")) {
               this.parseInputString(var2.tags, var6);
            }
         }
      }

      var3 = var1.children.iterator();

      while(var3.hasNext()) {
         ScriptParser.Block var7 = (ScriptParser.Block)var3.next();
         if ("items".equalsIgnoreCase(var7.type)) {
            this.readFilterBlock(var7, var2.loadedItems);
         } else if ("types".equalsIgnoreCase(var7.type)) {
            this.readFilterBlock(var7, var2.loadedTypes);
         } else if ("tags".equalsIgnoreCase(var7.type)) {
            this.readFilterBlock(var7, var2.tags);
         } else if ("blacklist".equalsIgnoreCase(var7.type)) {
            this.readBlock(var7, this.blacklist);
         }
      }

   }

   private void readFilterBlock(ScriptParser.Block var1, ArrayList<String> var2) {
      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         if (var4.string != null && !var4.string.trim().isEmpty()) {
            String var5 = var4.string.trim();
            if (!var5.contains("=")) {
               this.parseInputString(var2, var5);
            }
         }
      }

   }

   private void parseInputString(ArrayList<String> var1, String var2) {
      String[] var3 = var2.split("/");
      String[] var4 = var3;
      int var5 = var3.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String var7 = var4[var6];
         var7 = var7.trim();
         if (!var1.contains(var7)) {
            var1.add(var7);
         }
      }

   }

   @DebugClassFields
   private static class FilterTypeInfo {
      private final ArrayList<String> loadedItems = new ArrayList();
      private final ArrayList<String> loadedTypes = new ArrayList();
      private final ArrayList<String> items = new ArrayList();
      private final ArrayList<Item.Type> types = new ArrayList();
      private final ArrayList<String> tags = new ArrayList();

      private FilterTypeInfo() {
      }

      private void reset() {
         this.loadedItems.clear();
         this.loadedTypes.clear();
         this.items.clear();
         this.types.clear();
         this.tags.clear();
      }

      private boolean hasEntries() {
         return !this.items.isEmpty() || !this.types.isEmpty() || !this.tags.isEmpty();
      }

      private boolean containsItem(InventoryItem var1) {
         if (var1 == null) {
            return false;
         } else {
            return var1.getScriptItem() != null ? this.containsItem(var1.getFullType(), var1.getScriptItem().getType(), var1.getTags()) : this.containsItem(var1.getFullType(), Item.Type.Normal, var1.getTags());
         }
      }

      private boolean containsItem(Item var1) {
         return var1 == null ? false : this.containsItem(var1.getFullName(), var1.getType(), var1.getTags());
      }

      private boolean containsItem(String var1, Item.Type var2, ArrayList<String> var3) {
         if (var1 != null && this.hasEntries()) {
            if (!this.items.isEmpty() && this.items.contains(var1)) {
               return true;
            } else if (!this.types.isEmpty() && this.types.contains(var2)) {
               return true;
            } else {
               if (!this.tags.isEmpty() && var3 != null && !var3.isEmpty()) {
                  for(int var5 = 0; var5 < var3.size(); ++var5) {
                     String var4 = (String)var3.get(var5);
                     if (this.tags.contains(var4)) {
                        return true;
                     }
                  }
               }

               return false;
            }
         } else {
            return false;
         }
      }
   }
}
