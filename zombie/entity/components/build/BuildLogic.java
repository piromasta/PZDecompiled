package zombie.entity.components.build;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.characters.IsoGameCharacter;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoObject;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.Item;

public class BuildLogic extends BaseCraftingLogic {
   private CraftRecipe selectedRecipe = null;
   private final CraftBench craftBench;
   private final IsoObject isoObject;
   private final CraftRecipeData recipeData;
   private final CraftRecipeData recipeDataInProgress;
   private final ArrayList<ItemContainer> containers = new ArrayList();
   private boolean craftActionInProgress = false;
   private Dictionary<CraftRecipe, CraftRecipeComponentScript> recipeComponentScriptLookup = new Hashtable();

   public BuildLogic(IsoGameCharacter var1, CraftBench var2, IsoObject var3) {
      super(var1, var2);
      this.craftBench = var2;
      this.isoObject = var3;
      this.recipeData = new CraftRecipeData(CraftMode.Handcraft, var2 != null, true, false, true);
      this.recipeDataInProgress = new CraftRecipeData(CraftMode.Handcraft, var2 != null, true, false, true);
      if (this.craftBench != null) {
         this.sourceResources.addAll(var2.getResources());
      }

      this.registerEvent("onUpdateContainers");
      this.registerEvent("onRecipeChanged");
      this.registerEvent("onSetRecipeList");
      this.registerEvent("onUpdateRecipeList");
      this.registerEvent("onRebuildInputItemNodes");
      this.registerEvent("onManualSelectChanged");
      this.registerEvent("onStartCraft");
      this.registerEvent("onStopCraft");
   }

   public List<Item> getSatisfiedInputItems(InputScript var1) {
      ArrayList var2 = new ArrayList();
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         CraftRecipeData.InputScriptData var3 = this.recipeData.getDataForInputScript(var1);

         for(int var4 = 0; var4 < var3.getAppliedItemsCount(); ++var4) {
            var2.add(var3.getAppliedItem(var4).getScriptItem());
         }

         return var2;
      } else {
         return var2;
      }
   }

   public boolean shouldShowManualSelectInputs() {
      return false;
   }

   private void registerEvent(String var1) {
      this.events.put(var1, new ArrayList());
   }

   public ArrayList<CraftRecipe> getRecipeList() {
      return this.filteredRecipeList;
   }

   public CraftRecipe getRecipe() {
      return this.selectedRecipe;
   }

   public SpriteConfigManager.ObjectInfo getSelectedBuildObject() {
      if (this.selectedRecipe != null) {
         CraftRecipeComponentScript var1 = (CraftRecipeComponentScript)this.recipeComponentScriptLookup.get(this.selectedRecipe);
         GameEntityScript var2 = (GameEntityScript)var1.getParent();
         if (var2 != null) {
            SpriteConfigScript var3 = (SpriteConfigScript)var2.getComponentScriptFor(ComponentType.SpriteConfig);
            if (var3 != null) {
               return SpriteConfigManager.GetObjectInfo(var3.getName());
            }
         }
      }

      return null;
   }

   public ArrayList<CraftRecipe> getAllBuildableRecipes() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = ScriptManager.instance.getAllGameEntities();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         ArrayList var4 = ((GameEntityScript)var2.get(var3)).getComponentScripts();

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            if (((ComponentScript)var4.get(var5)).type == ComponentType.CraftRecipe) {
               CraftRecipeComponentScript var6 = (CraftRecipeComponentScript)var4.get(var5);
               CraftRecipe var7 = var6 != null ? var6.getCraftRecipe() : null;
               if (var7 != null && var7.hasTag("EntityRecipe".toLowerCase())) {
                  var1.add(var7);
                  this.recipeComponentScriptLookup.put(var7, var6);
                  break;
               }
            }
         }
      }

      return var1;
   }

   public void setRecipe(CraftRecipe var1) {
      if (this.selectedRecipe != var1) {
         this.selectedRecipe = var1;
         this.recipeData.setRecipe(var1);
         this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true);
         this.triggerEvent("onRecipeChanged", new Object[]{var1});
         this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
      }

   }

   public boolean isManualSelectInputs() {
      return false;
   }

   public boolean isCraftActionInProgress() {
      return this.craftActionInProgress;
   }

   public boolean canPerformCurrentRecipe() {
      return this.recipeData.canPerform(this.player, this.sourceResources, this.allItems);
   }

   public boolean isInputSatisfied(InputScript var1) {
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         CraftRecipeData.InputScriptData var2 = this.recipeData.getDataForInputScript(var1);
         return var2.isCachedCanConsume();
      } else {
         return false;
      }
   }

   public void startCraftAction(KahluaTableImpl var1) {
      this.craftActionInProgress = true;
      this.recipeDataInProgress.setRecipe(this.recipeData.getRecipe());
      this.triggerEvent("onStartCraft", new Object[]{var1});
   }

   public boolean performCurrentRecipe() {
      return this.recipeDataInProgress.perform(this.player, this.sourceResources, this.allItems);
   }

   public void stopCraftAction() {
      this.craftActionInProgress = false;
      this.recipeDataInProgress.setRecipe((CraftRecipe)null);
      this.triggerEvent("onStopCraft", new Object[0]);
   }

   public void refresh() {
      if (this.getRecipe() != null) {
         for(int var1 = 0; var1 < this.getRecipe().getInputs().size(); ++var1) {
            InputScript var2 = (InputScript)this.getRecipe().getInputs().get(var1);
            CraftRecipeData.InputScriptData var3 = this.recipeData.getDataForInputScript(var2);
            var3.verifyInputItems(this.allItems);
         }

      }
   }

   public void setContainers(ArrayList<ItemContainer> var1) {
      this.containers.clear();
      this.containers.addAll(var1);
      this.allItems.clear();
      CraftRecipeManager.getAllItemsFromContainers(this.containers, this.allItems);
      this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true);
      this.cachedRecipeInfosDirty = true;
      this.triggerEvent("onUpdateContainers", new Object[0]);
      this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
   }

   public boolean isCraftCheat() {
      return this.player.isBuildCheat();
   }
}
