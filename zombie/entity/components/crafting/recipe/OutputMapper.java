package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

public class OutputMapper {
   private static final ArrayList<Item> _emptyItems = new ArrayList();
   private final ArrayList<Item> resultItems = new ArrayList();
   private final ArrayList<OutputEntree> entrees = new ArrayList();
   private final HashMap<Item, OutputEntree> entreeMap = new HashMap();
   private OutputEntree defaultOutputEntree;
   private final ArrayList<InputScript> inputScripts = new ArrayList();
   private final HashSet<InputScript> matchedInputs = new HashSet();
   private final String name;

   public OutputMapper(String var1) {
      this.name = var1;
   }

   public boolean isEmpty() {
      return this.defaultOutputEntree == null && this.entrees.size() == 0;
   }

   private void clear() {
      this.resultItems.clear();
      this.entrees.clear();
      this.entreeMap.clear();
      this.defaultOutputEntree = null;
      this.inputScripts.clear();
   }

   public ArrayList<Item> getResultItems() {
      return this.resultItems;
   }

   public ArrayList<Item> getPatternForResult(Item var1) {
      return this.entreeMap.containsKey(var1) ? ((OutputEntree)this.entreeMap.get(var1)).pattern : _emptyItems;
   }

   public void registerInputScript(InputScript var1) {
      this.inputScripts.add(var1);
   }

   public void setDefaultOutputEntree(String var1) {
      this.defaultOutputEntree = new OutputEntree();
      this.defaultOutputEntree.fullType = var1;
   }

   public void addOutputEntree(String var1, String[] var2) {
      OutputEntree var3 = new OutputEntree();
      var3.fullType = var1;
      var3.patternFullTypes.addAll(Arrays.asList(var2));
      this.entrees.add(var3);
   }

   public void addOutputEntree(String var1, ArrayList<String> var2) {
      OutputEntree var3 = new OutputEntree();
      var3.fullType = var1;
      var3.patternFullTypes.addAll(var2);
      this.entrees.add(var3);
   }

   private Item getItem(String var1) throws Exception {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new Exception("Item not found, full type invalid: " + var1);
      } else {
         Item var2 = ScriptManager.instance.getItem(var1);
         if (var2 == null) {
            throw new Exception("Item not found: " + var1);
         } else {
            return var2;
         }
      }
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      if (this.defaultOutputEntree != null) {
         this.defaultOutputEntree.result = this.getItem(this.defaultOutputEntree.fullType);
         this.resultItems.add(this.defaultOutputEntree.result);
      }

      for(int var2 = 0; var2 < this.entrees.size(); ++var2) {
         OutputEntree var1 = (OutputEntree)this.entrees.get(var2);
         Item var3 = this.getItem(var1.fullType);
         var1.result = var3;
         this.resultItems.add(var1.result);
         this.entreeMap.put(var3, var1);

         for(int var4 = 0; var4 < var1.patternFullTypes.size(); ++var4) {
            Item var5 = this.getItem((String)var1.patternFullTypes.get(var4));
            var1.pattern.add(var5);
         }
      }

      assert this.resultItems.size() != 0;

   }

   public Item getOutputItem(CraftRecipeData var1) {
      return this.getOutputItem(var1, false);
   }

   public Item getOutputItem(CraftRecipeData var1, boolean var2) {
      int var4;
      if (this.entrees.size() > 0 && var1 != null) {
         for(var4 = 0; var4 < this.entrees.size(); ++var4) {
            OutputEntree var3 = (OutputEntree)this.entrees.get(var4);
            if (var3.pattern.size() != 0) {
               this.matchedInputs.clear();
               boolean var5 = true;

               for(int var7 = 0; var7 < var3.pattern.size(); ++var7) {
                  Item var6 = (Item)var3.pattern.get(var7);
                  boolean var8 = false;

                  for(int var10 = 0; var10 < this.inputScripts.size(); ++var10) {
                     InputScript var9 = (InputScript)this.inputScripts.get(var10);
                     if (!this.matchedInputs.contains(var9) && this.matchItem(var1, var9, var6, var2)) {
                        this.matchedInputs.add(var9);
                        var8 = true;
                        break;
                     }
                  }

                  if (!var8) {
                     var5 = false;
                     break;
                  }
               }

               if (var5) {
                  return var3.result;
               }
            }
         }
      }

      if (this.defaultOutputEntree == null) {
         return null;
      } else {
         if (var2) {
            for(var4 = 0; var4 < this.inputScripts.size(); ++var4) {
               InputScript var11 = (InputScript)this.inputScripts.get(var4);
               if (var1.getFirstManualInputFor(var11) == null) {
                  return null;
               }
            }
         }

         return this.defaultOutputEntree.result;
      }
   }

   private boolean matchItem(CraftRecipeData var1, InputScript var2, Item var3, boolean var4) {
      CraftRecipeData.InputScriptData var5 = var1.getDataForInputScript(var2);
      if (var5 != null) {
         if (var4) {
            if (var5.getFirstInputItem() != null && var5.getFirstInputItem().getScriptItem() != null) {
               return var5.getFirstInputItem().getScriptItem() != null && var5.getFirstInputItem().getScriptItem().equals(var3);
            }
         } else if (var5.getMostRecentItem() != null) {
            return var5.getMostRecentItem().getScriptItem() != null && var5.getMostRecentItem().getScriptItem().equals(var3);
         }
      }

      return false;
   }

   public ArrayList<OutputEntree> getEntrees() {
      return this.entrees;
   }

   public static class OutputEntree {
      private String fullType;
      public Item result;
      private final ArrayList<String> patternFullTypes = new ArrayList();
      public final ArrayList<Item> pattern = new ArrayList();

      public OutputEntree() {
      }
   }
}
