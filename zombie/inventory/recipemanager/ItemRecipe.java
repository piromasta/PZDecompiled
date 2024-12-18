package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.core.Colors;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.RecipeManager;
import zombie.scripting.objects.Recipe;

public class ItemRecipe {
   public static final String FLUID_PREFIX = "Fluid.";
   private static final ArrayDeque<ItemRecipe> pool = new ArrayDeque();
   private Recipe recipe;
   private IsoGameCharacter character;
   private InventoryItem selectedItem;
   private boolean allItems;
   private boolean valid = false;
   private boolean hasCollectedSources = false;
   private final UsedItemProperties usedItemProperties = new UsedItemProperties();
   private final ArrayList<SourceRecord> sourceRecords = new ArrayList();
   private final ArrayList<InventoryItem> allSourceItems = new ArrayList();
   private final ArrayList<InventoryItem> allResultItems = new ArrayList();
   private ArrayList<InventoryItem>[] resultsPerType;

   public static int getNumberOfTimesRecipeCanBeDone(Recipe var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, InventoryItem var3) {
      RecipeMonitor.suspend();
      ItemRecipe var4 = Alloc(var0, var1, var2, var3, (ArrayList)null, true);
      int var5 = var4.getNumberOfTimesRecipeCanBeDone();
      Release(var4);
      RecipeMonitor.resume();
      return var5;
   }

   public static ItemRecipe Alloc(Recipe var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, InventoryItem var3, ArrayList<InventoryItem> var4, boolean var5) {
      if (var3 != null && (var3.getContainer() == null || !var3.getContainer().contains(var3))) {
         DebugLog.Recipe.warn("recipe: item appears to have been used already, ignoring " + var3.getFullType());
         var3 = null;
      }

      if (var2 == null) {
         var2 = new ArrayList();
         var2.add(var1.getInventory());
      }

      if (var3 != null && !RecipeManager.validateRecipeContainsSourceItem(var0, var3)) {
         String var10002 = var3.getFullType();
         throw new RuntimeException("item " + var10002 + " isn't used in recipe " + var0.getOriginalname());
      } else {
         RecipeMonitor.LogInit(var0, var1, var2, var3, var4, var5);
         ItemRecipe var6 = pool.isEmpty() ? new ItemRecipe() : (ItemRecipe)pool.pop();
         var6.init(var0, var1, var2, var3, var4, var5);
         return var6;
      }
   }

   public static void Release(ItemRecipe var0) {
      assert !pool.contains(var0);

      pool.push(var0.reset());
   }

   private ItemRecipe() {
      this.ensureResultsPerType(20);
   }

   protected Recipe getRecipe() {
      return this.recipe;
   }

   protected IsoGameCharacter getCharacter() {
      return this.character;
   }

   protected InventoryItem getSelectedItem() {
      return this.selectedItem;
   }

   protected boolean isValid() {
      return this.valid;
   }

   private void ensureResultsPerType(int var1) {
      if (this.resultsPerType == null || this.resultsPerType.length < var1) {
         this.resultsPerType = new ArrayList[var1];

         for(int var2 = 0; var2 < this.resultsPerType.length; ++var2) {
            this.resultsPerType[var2] = new ArrayList();
         }
      }

   }

   protected String getRecipeName() {
      return this.recipe != null ? this.recipe.getName() : "Recipe.Null";
   }

   private ItemRecipe reset() {
      this.recipe = null;
      this.character = null;
      this.selectedItem = null;
      this.allItems = false;
      this.valid = false;
      this.hasCollectedSources = false;
      this.usedItemProperties.reset();
      this.allSourceItems.clear();

      int var1;
      for(var1 = 0; var1 < this.sourceRecords.size(); ++var1) {
         SourceRecord.release((SourceRecord)this.sourceRecords.get(var1));
      }

      this.sourceRecords.clear();
      this.allResultItems.clear();
      if (this.resultsPerType != null) {
         for(var1 = 0; var1 < this.resultsPerType.length; ++var1) {
            this.resultsPerType[var1].clear();
         }
      }

      return this;
   }

   private void init(Recipe var1, IsoGameCharacter var2, ArrayList<ItemContainer> var3, InventoryItem var4, ArrayList<InventoryItem> var5, boolean var6) {
      this.recipe = var1;
      this.character = var2;
      this.selectedItem = var4;
      this.allItems = var6;

      for(int var7 = 0; var7 < var1.getSource().size(); ++var7) {
         SourceRecord var8 = SourceRecord.alloc(this, var7, (Recipe.Source)var1.getSource().get(var7));
         this.sourceRecords.add(var8);
      }

      RecipeMonitor.LogSources(var1.getSource());
      if (!this.testItem(var4)) {
         RecipeMonitor.Log("SelectedItem testItem() failed, aborting.", RecipeMonitor.colNeg);
      } else {
         RecipeMonitor.LogBlanc();
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[ParseItems]", RecipeMonitor.colHeader);
         }

         RecipeMonitor.IncTab();

         int var15;
         for(var15 = 0; var15 < var3.size(); ++var15) {
            ItemContainer var14 = (ItemContainer)var3.get(var15);
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log(RecipeMonitor.getContainerString(var14));
            }

            RecipeMonitor.IncTab();

            for(int var9 = 0; var9 < var14.getItems().size(); ++var9) {
               InventoryItem var10 = (InventoryItem)var14.getItems().get(var9);
               if (RecipeMonitor.canLog()) {
                  RecipeMonitor.Log("Parsing Item = " + var10, RecipeMonitor.colGray);
               }

               RecipeMonitor.IncTab();
               if (var4 != null && (var4.getContainer() == null || !var4.getContainer().contains(var4))) {
                  if (RecipeMonitor.canLog()) {
                     RecipeMonitor.Log(" -> skipping, appears to have been used already", RecipeMonitor.colNeg);
                  }
               } else if (var5 != null && var5.contains(var10)) {
                  if (RecipeMonitor.canLog()) {
                     RecipeMonitor.Log(" -> skipping ignored item", RecipeMonitor.colNeg);
                  }
               } else if ((var4 == null || var4 != var10) && var2.isEquippedClothing(var10)) {
                  if (RecipeMonitor.canLog()) {
                     RecipeMonitor.Log(" -> skipping, equipped clothing", RecipeMonitor.colNeg);
                  }

                  RecipeMonitor.DecTab();
               } else {
                  ItemRecord var11 = ItemRecord.alloc(this, var10);
                  boolean var12 = false;

                  for(int var13 = 0; var13 < this.sourceRecords.size(); ++var13) {
                     if (((SourceRecord)this.sourceRecords.get(var13)).assignItemRecord(var11)) {
                        if (RecipeMonitor.canLog()) {
                           RecipeMonitor.Log(" -> assigned to source [" + var13 + "] : " + var11.getSourceType());
                        }

                        var12 = true;
                        break;
                     }
                  }

                  if (!var12) {
                     ItemRecord.release(var11);
                  }

                  RecipeMonitor.DecTab();
               }
            }

            RecipeMonitor.DecTab();
         }

         RecipeMonitor.DecTab();
         this.valid = true;

         for(var15 = 0; var15 < this.sourceRecords.size(); ++var15) {
            SourceRecord var16 = (SourceRecord)this.sourceRecords.get(var15);
            if (!var16.isValid()) {
               this.valid = false;
               break;
            }
         }

         if (RecipeMonitor.canLog()) {
            boolean var10000 = this.valid;
            RecipeMonitor.Log("Recipe valid = " + var10000, this.valid ? RecipeMonitor.colPos : RecipeMonitor.colNeg);
         }

         if (RecipeMonitor.canLog()) {
            RecipeManager.printDebugRecipeValid(var1, var2, (InventoryItem)null, var3);
         }

         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("debug total times recipe can be done = " + this.getNumberOfTimesRecipeCanBeDone(), Colors.Magenta);
         }

      }
   }

   private void collectSourceItems() {
      if (!this.hasCollectedSources) {
         RecipeMonitor.LogBlanc();
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[CollectingSourceItems]", RecipeMonitor.colHeader);
         }

         this.hasCollectedSources = true;
         if (this.allItems || this.isValid()) {
            boolean var1 = false;

            SourceRecord var2;
            int var3;
            for(var3 = 0; var3 < this.sourceRecords.size(); ++var3) {
               var2 = (SourceRecord)this.sourceRecords.get(var3);
               var2.collectItems();
               if (var2.getCollectedItems().isEmpty()) {
                  var1 = true;
               }
            }

            if (!this.allItems && var1) {
               if (RecipeMonitor.canLog()) {
                  RecipeMonitor.Log("allItems==true and not all sources satisfied, clearing", RecipeMonitor.colNeg);
               }

               for(var3 = 0; var3 < this.sourceRecords.size(); ++var3) {
                  var2 = (SourceRecord)this.sourceRecords.get(var3);
                  var2.clearCollectedItems();
               }
            }

            this.allSourceItems.clear();

            for(var3 = 0; var3 < this.sourceRecords.size(); ++var3) {
               this.allSourceItems.addAll(((SourceRecord)this.sourceRecords.get(var3)).getCollectedItems());
            }

            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("total items = " + this.allSourceItems.size());
            }

         }
      }
   }

   private int getNumberOfTimesRecipeCanBeDone() {
      if (!this.isValid()) {
         return 0;
      } else {
         int var1 = 2147483647;

         for(int var2 = 0; var2 < this.sourceRecords.size(); ++var2) {
            int var3 = ((SourceRecord)this.sourceRecords.get(var2)).getNumberOfTimesSourceCanBeDone();
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("times source [" + var2 + "] can be done = " + var3, Colors.Magenta);
            }

            var1 = PZMath.min(var1, var3);
            if (var1 <= 0) {
               return 0;
            }
         }

         if (var1 == 2147483647) {
            var1 = 0;
         }

         return var1;
      }
   }

   public ArrayList<InventoryItem> perform() {
      this.collectSourceItems();
      RecipeMonitor.LogBlanc();
      if (RecipeMonitor.canLog()) {
         RecipeMonitor.Log("[Perform]", RecipeMonitor.colHeader);
      }

      if (this.allSourceItems.isEmpty()) {
         throw new RuntimeException("collectSourceItems() didn't return the required number of items");
      } else if (this.allItems) {
         throw new RuntimeException("allItems = true, while attempting a recipe 'perform'");
      } else {
         this.character.removeFromHands(this.selectedItem);

         for(int var2 = 0; var2 < this.sourceRecords.size(); ++var2) {
            SourceRecord var1 = (SourceRecord)this.sourceRecords.get(var2);
            var1.applyUses(this.usedItemProperties);
         }

         return null;
      }
   }

   public ArrayList<InventoryItem> getSourceItems() {
      this.collectSourceItems();
      ArrayList var1 = new ArrayList();
      var1.addAll(this.allSourceItems);
      return var1;
   }

   public ArrayList<InventoryItem> getSourceItems(int var1) {
      this.collectSourceItems();
      if (var1 >= 0 && var1 < this.sourceRecords.size()) {
         ArrayList var2 = new ArrayList();
         var2.addAll(((SourceRecord)this.sourceRecords.get(var1)).getCollectedItems());
         return var2;
      } else {
         DebugLog.General.error("Index '" + var1 + "' is not valid for recipe '" + this.recipe.getName() + "'.");
         return null;
      }
   }

   protected boolean testItem(InventoryItem var1) {
      return true;
   }
}
