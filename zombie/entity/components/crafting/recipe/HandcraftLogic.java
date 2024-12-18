package zombie.entity.components.crafting.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.Item;

public class HandcraftLogic extends BaseCraftingLogic {
   private static final String EXCLUDE_TAG = "RightClickOnly";
   private static final int workstationInteractDistance = 3;
   private static final Comparator<InputItemNode> inputItemNodeComparator = new Comparator<InputItemNode>() {
      public int compare(InputItemNode var1, InputItemNode var2) {
         return var1.name.compareTo(var2.name);
      }
   };
   private final CraftBench craftBench;
   private IsoObject isoObject;
   private final CraftRecipeData recipeData;
   private int cachedPossibleCraftCount = 0;
   private final boolean limitAutoFillToCurrentlyFilledItems = true;
   private final ArrayList<Resource> multicraftConsumedResources = new ArrayList();
   private final ArrayList<InventoryItem> multicraftConsumedItems = new ArrayList();
   private final ArrayList<ItemContainer> containers = new ArrayList();
   private final ArrayList<InputItemNode> inputItemNodes = new ArrayList();
   private boolean inputItemNodesDirty = false;
   private boolean cachedCanPerform = false;
   private boolean cachedCanPerformDirty = true;
   private boolean manualSelectInputs = false;
   private boolean showManualSelectInputs = false;
   private InputScript manualSelectInputScriptFilter = null;
   private boolean craftActionInProgress = false;
   private KahluaTableImpl craftActionTable = null;
   private final ArrayList<InputItemNode> oldInputItemNodes = new ArrayList();
   private final HashMap<Item, InputItemNode> oldInputItemNodeMap = new HashMap();
   private final ArrayList<CachedRecipeInfo> cachedRecipeInfos = new ArrayList();
   private final HashMap<CraftRecipe, CachedRecipeInfo> cachedRecipeInfoMap = new HashMap();

   public HandcraftLogic(IsoGameCharacter var1, CraftBench var2, IsoObject var3) {
      super(var1, var2);
      this.craftBench = var2;
      this.isoObject = var3;
      this.recipeData = new CraftRecipeData(CraftMode.Handcraft, var2 != null, true, false, true);
      if (this.craftBench != null) {
         this.sourceResources.addAll(var2.getResources());
      }

      this.registerEvent("onUpdateContainers");
      this.registerEvent("onRecipeChanged");
      this.registerEvent("onSetRecipeList");
      this.registerEvent("onUpdateRecipeList");
      this.registerEvent("onRebuildInputItemNodes");
      this.registerEvent("onManualSelectChanged");
      this.registerEvent("onShowManualSelectChanged");
      this.registerEvent("onStartCraft");
      this.registerEvent("onStopCraft");
      this.registerEvent("onInputsChanged");
   }

   private void registerEvent(String var1) {
      this.events.put(var1, new ArrayList());
   }

   public IsoGameCharacter getPlayer() {
      return this.player;
   }

   public CraftBench getCraftBench() {
      return this.craftBench;
   }

   public IsoObject getIsoObject() {
      return this.isoObject;
   }

   public CraftRecipeData getRecipeData() {
      return this.recipeData;
   }

   public ArrayList<Resource> getSourceResources() {
      return this.sourceResources;
   }

   public ArrayList<CraftRecipe> getRecipeList() {
      return this.filteredRecipeList;
   }

   public ArrayList<InventoryItem> getAllItems() {
      return this.allItems;
   }

   public ArrayList<InputItemNode> getInputItemNodes() {
      if (this.inputItemNodesDirty) {
         this.rebuildInputItemNodes();
      }

      return this.inputItemNodes;
   }

   public ArrayList<InputItemNode> getInputItemNodesForInput(InputScript var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = this.getInputItemNodes();

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         if (((InputItemNode)var3.get(var4)).getFirstMatchedInputScript() == var1) {
            var2.add((InputItemNode)this.getInputItemNodes().get(var4));
         }
      }

      return var2;
   }

   public InputScript getManualSelectInputScriptFilter() {
      return this.manualSelectInputScriptFilter;
   }

   public void setManualSelectInputScriptFilter(InputScript var1) {
      this.manualSelectInputScriptFilter = var1;
      this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
   }

   public void startCraftAction(KahluaTableImpl var1) {
      this.craftActionInProgress = true;
      this.craftActionTable = var1;
      if (this.getRecipe() == (CraftRecipe)var1.rawget("craftRecipe")) {
         List var2 = (List)var1.rawget("items");
         if (var2 != null) {
            this.populateInputs(var2, (List)null, true);
         }
      }

      this.triggerEvent("onStartCraft", new Object[]{var1});
   }

   public void stopCraftAction() {
      this.craftActionInProgress = false;
      this.craftActionTable = null;
      this.triggerEvent("onStopCraft", new Object[0]);
   }

   public void refresh() {
      if (this.getRecipe() != null) {
         for(int var1 = 0; var1 < this.getRecipe().getInputs().size(); ++var1) {
            InputScript var2 = (InputScript)this.getRecipe().getInputs().get(var1);
            CraftRecipeData.InputScriptData var3 = this.recipeData.getDataForInputScript(var2);
            var3.verifyInputItems(this.allItems);
         }

         ArrayList var4 = this.recipeData.getAllInputItems();
         this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true);
         this.populateInputs(var4, this.sourceResources, true);
         this.recipeData.canConsumeInputs(this.sourceResources);
         this.autoPopulateInputs();
      }
   }

   public boolean offerInputItem(InventoryItem var1) {
      boolean var2 = this.recipeData.offerInputItem(var1);
      if (var2) {
         if (this.canPerformCurrentRecipe()) {
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
         } else {
            this.cachedPossibleCraftCount = 0;
         }

         this.triggerEvent("onInputsChanged", new Object[0]);
      }

      return var2;
   }

   public boolean removeInputItem(InventoryItem var1) {
      boolean var2 = this.recipeData.removeInputItem(var1);
      if (var2) {
         if (this.canPerformCurrentRecipe()) {
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
         } else {
            this.cachedPossibleCraftCount = 0;
         }

         this.triggerEvent("onInputsChanged", new Object[0]);
      }

      return var2;
   }

   public void autoPopulateInputs() {
      this.populateInputs(this.getAllViableInputInventoryItems(), this.getAllViableInputResources(), false);
   }

   public void populateInputs(List<InventoryItem> var1, List<Resource> var2, boolean var3) {
      if (this.recipeData != null && this.recipeData.getRecipe() != null) {
         ArrayList var4 = this.recipeData.getRecipe().getInputs();

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            InputScript var6 = (InputScript)var4.get(var5);
            CraftRecipeData.InputScriptData var7 = this.recipeData.getDataForInputScript(var6);
            if (var3) {
               while(var7.getLastInputItem() != null) {
                  var7.removeInputItem(var7.getLastInputItem());
               }
            }

            if (!var7.isInputItemsSatisfied()) {
               for(int var8 = 0; var8 < var1.size(); ++var8) {
                  this.recipeData.offerInputItem(var6, (InventoryItem)var1.get(var8));
                  if (var7.isInputItemsSatisfied()) {
                     break;
                  }
               }
            }
         }

         this.cachedCanPerformDirty = true;
         this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
         this.triggerEvent("onInputsChanged", new Object[0]);
      }

   }

   public float getResidualFluidFromInput(InputScript var1) {
      if (var1 != null && this.recipeData != null) {
         ArrayList var2 = var1.getParentRecipe().getInputs();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            if (((InputScript)var2.get(var3)).getConsumeFromItemScript() == var1) {
               CraftRecipeData.InputScriptData var4 = this.recipeData.getDataForInputScript((InputScript)var2.get(var3));
               if (var4 != null) {
                  return var4.getInputItemFluidUses() - var1.getAmount();
               }
            }
         }

         return 0.0F;
      } else {
         return 0.0F;
      }
   }

   public void selectionSpam() {
      for(int var1 = 0; var1 < this.allItems.size(); ++var1) {
         InventoryItem var2 = (InventoryItem)this.allItems.get(var1);
         this.getRecipeData().offerInputItem(var2);
      }

   }

   public void selectionSpamWithout(ArrayList<InventoryItem> var1) {
      for(int var2 = 0; var2 < this.allItems.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.allItems.get(var2);
         if (!var1.contains(var3)) {
            this.getRecipeData().offerInputItem(var3);
         }
      }

   }

   public boolean isCraftActionInProgress() {
      return this.craftActionInProgress;
   }

   public KahluaTableImpl getCraftActionTable() {
      return this.craftActionTable;
   }

   public String getModelHandOne() {
      return this.recipeData.getModelHandOne();
   }

   public String getModelHandTwo() {
      return this.recipeData.getModelHandTwo();
   }

   public boolean cachedCanPerformCurrentRecipe() {
      return this.cachedCanPerformDirty ? this.canPerformCurrentRecipe() : this.cachedCanPerform;
   }

   public boolean canPerformCurrentRecipe() {
      this.cachedCanPerformDirty = false;
      this.cachedCanPerform = this.recipeData.canPerform(this.player, this.sourceResources, this.manualSelectInputs ? null : this.allItems);
      if (this.recipeData.getRecipe().isAnySurfaceCraft() && !this.isCharacterInRangeOfWorkbench()) {
         this.cachedCanPerform = false;
      }

      return this.cachedCanPerform;
   }

   public boolean performCurrentRecipe() {
      return this.recipeData.perform(this.player, this.sourceResources, this.manualSelectInputs ? null : this.allItems);
   }

   public CraftRecipe getRecipe() {
      return this.recipeData.getRecipe();
   }

   public void setRecipe(CraftRecipe var1) {
      if (this.recipeData.getRecipe() != var1) {
         this.recipeData.setRecipe(var1);
         this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true);
         this.inputItemNodesDirty = true;
         this.autoPopulateInputs();
         this.triggerEvent("onRecipeChanged", new Object[]{var1});
         this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
      }

   }

   public void checkValidRecipeSelected() {
      if (!this.filteredRecipeList.contains(this.recipeData.getRecipe())) {
         if (!this.filteredRecipeList.isEmpty()) {
            this.setRecipe((CraftRecipe)this.filteredRecipeList.get(0));
         } else {
            this.setRecipe((CraftRecipe)null);
         }
      }

   }

   public boolean shouldShowManualSelectInputs() {
      return this.manualSelectInputs && this.showManualSelectInputs;
   }

   public void setShowManualSelectInputs(boolean var1) {
      if (this.showManualSelectInputs != var1) {
         this.showManualSelectInputs = var1;
         this.triggerEvent("onShowManualSelectChanged", new Object[]{this.shouldShowManualSelectInputs()});
      }

   }

   public boolean isManualSelectInputs() {
      return this.manualSelectInputs;
   }

   public void setManualSelectInputs(boolean var1) {
      if (this.manualSelectInputs != var1) {
         this.manualSelectInputs = var1;
         this.triggerEvent("onManualSelectChanged", new Object[]{this.manualSelectInputs});
      }

   }

   public void setContainers(ArrayList<ItemContainer> var1) {
      this.containers.clear();
      this.containers.addAll(var1);
      this.allItems.clear();
      CraftRecipeManager.getAllItemsFromContainers(this.containers, this.allItems);
      ArrayList var2 = this.recipeData.getAllInputItems();
      this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true);

      for(int var3 = var2.size() - 1; var3 >= 0; --var3) {
         if (!this.allItems.contains(var2.get(var3))) {
            var2.remove(var3);
         }
      }

      this.populateInputs(var2, this.sourceResources, true);
      this.recipeData.canConsumeInputs(this.sourceResources);
      this.inputItemNodesDirty = true;
      this.cachedRecipeInfosDirty = true;
      this.cachedCanPerformDirty = true;
      this.triggerEvent("onUpdateContainers", new Object[0]);
      this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
   }

   public ArrayList<ItemContainer> getContainers() {
      return this.containers;
   }

   public void setRecipes(List<CraftRecipe> var1) {
      super.setRecipes(var1);
      if (this.filteredRecipeList.size() > 0 && (this.getRecipe() == null || !this.filteredRecipeList.contains(this.getRecipe()))) {
         this.setRecipe((CraftRecipe)this.filteredRecipeList.get(0));
      }

   }

   public void filterRecipeList(String var1, String var2, boolean var3, IsoPlayer var4) {
      super.filterRecipeList(var1, var2, var3, var4);

      for(int var5 = this.filteredRecipeList.size() - 1; var5 >= 0; --var5) {
         if (((CraftRecipe)this.filteredRecipeList.get(var5)).hasTag("RightClickOnly")) {
            this.filteredRecipeList.remove(var5);
         }
      }

      this.sortRecipeList();
   }

   public boolean isInputSatisfied(InputScript var1) {
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         CraftRecipeData.InputScriptData var2 = this.recipeData.getDataForInputScript(var1);
         return var1.getResourceType() == ResourceType.Item && this.manualSelectInputs ? var2.isInputItemsSatisfied() : var2.isCachedCanConsume();
      } else {
         return false;
      }
   }

   public boolean areAllInputItemsSatisfied() {
      return this.recipeData.areAllInputItemsSatisfied();
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

   public List<InventoryItem> getSatisfiedInputInventoryItems(InputScript var1) {
      ArrayList var2 = new ArrayList();
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         CraftRecipeData.InputScriptData var3 = this.recipeData.getDataForInputScript(var1);

         for(int var4 = 0; var4 < var3.getAppliedItemsCount(); ++var4) {
            var2.add(var3.getAppliedItem(var4));
         }

         return var2;
      } else {
         return var2;
      }
   }

   public List<InventoryItem> getAllViableInputInventoryItems() {
      ArrayList var1 = new ArrayList();
      if (this.recipeData.getRecipe() == null) {
         return var1;
      } else {
         for(int var2 = 0; var2 < this.recipeData.getAllViableItemsCount(); ++var2) {
            var1.add(this.recipeData.getViableItem(var2));
         }

         return var1;
      }
   }

   public List<Resource> getAllViableInputResources() {
      ArrayList var1 = new ArrayList();
      if (this.recipeData.getRecipe() == null) {
         return var1;
      } else {
         for(int var2 = 0; var2 < this.recipeData.getAllViableResourcesCount(); ++var2) {
            var1.add(this.recipeData.getViableResource(var2));
         }

         return var1;
      }
   }

   public int getInputCount(InputScript var1) {
      return this.getInputUses(var1);
   }

   public int getInputUses(InputScript var1) {
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         if (var1.getResourceType() == ResourceType.Item && this.manualSelectInputs) {
            CraftRecipeData.InputScriptData var2 = this.recipeData.getDataForInputScript(var1);
            return var2 != null ? var2.getInputItemUses() : 0;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public ArrayList<InventoryItem> getManualInputsFor(InputScript var1, ArrayList<InventoryItem> var2) {
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         CraftRecipeData.InputScriptData var3 = this.recipeData.getDataForInputScript(var1);
         if (var1.getResourceType() == ResourceType.Item && this.manualSelectInputs) {
            var3.getManualInputItems(var2);
            Iterator var4 = var2.iterator();

            while(var4.hasNext()) {
               InventoryItem var5 = (InventoryItem)var4.next();
               DebugLog.CraftLogic.println("get m-input: " + var5.getFullType());
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   public boolean setManualInputsFor(InputScript var1, ArrayList<InventoryItem> var2) {
      if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(var1)) {
         CraftRecipeData.InputScriptData var3 = this.recipeData.getDataForInputScript(var1);
         if (var1.getResourceType() == ResourceType.Item && this.manualSelectInputs) {
            while(var3.getLastInputItem() != null) {
               var3.removeInputItem(var3.getLastInputItem());
            }

            for(int var4 = 0; var4 < var2.size(); ++var4) {
               InventoryItem var5 = (InventoryItem)var2.get(var4);
               if ((var5.getContainer() != null || var5.getWorldItem() != null) && var3.addInputItem(var5)) {
                  DebugLogStream var10000 = DebugLog.CraftLogic;
                  Object var10001 = var2.get(var4);
                  var10000.println("add m-input: " + ((InventoryItem)var10001).getFullType());
               }
            }

            return var3.isInputItemsSatisfied();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void getCreatedOutputItems(ArrayList<InventoryItem> var1) {
      if (this.recipeData != null && this.recipeData.isAllowOutputItems()) {
         ItemDataList var2 = this.recipeData.getToOutputItems();
         if (var2.hasUnprocessed()) {
            var2.getUnprocessed(var1);
         }
      }

   }

   private void rebuildInputItemNodes() {
      if (this.inputItemNodesDirty) {
         this.oldInputItemNodes.clear();
         this.oldInputItemNodeMap.clear();
         int var1;
         if (this.inputItemNodes.size() > 0) {
            for(var1 = 0; var1 < this.inputItemNodes.size(); ++var1) {
               InputItemNode var2 = (InputItemNode)this.inputItemNodes.get(var1);
               this.oldInputItemNodes.add(var2);
               this.oldInputItemNodeMap.put(var2.scriptItem, var2);
            }
         }

         this.inputItemNodes.clear();
         if (this.allItems.size() > 0 && this.getRecipe() != null) {
            for(int var4 = 0; var4 < this.allItems.size(); ++var4) {
               InventoryItem var6 = (InventoryItem)this.allItems.get(var4);
               InputScript var7 = CraftRecipeManager.getValidInputScriptForItem(this.getRecipe(), var6);
               if (var7 != null) {
                  InputItemNode var3 = null;

                  for(int var5 = 0; var5 < this.inputItemNodes.size(); ++var5) {
                     var3 = (InputItemNode)this.inputItemNodes.get(var5);
                     if (var3.scriptItem == var6.getScriptItem()) {
                        break;
                     }

                     var3 = null;
                  }

                  if (var3 == null) {
                     var3 = HandcraftLogic.InputItemNode.Alloc(this.getRecipe(), var6.getScriptItem());
                     var3.firstMatchedInputScript = var7;
                     var3.isToolRight = var7.hasFlag(InputFlag.ToolRight);
                     var3.isToolLeft = var7.hasFlag(InputFlag.ToolLeft);
                     var3.isTool = var3.isToolRight || var3.isToolLeft;
                     var3.isKeep = var3.isKeep || var3.isKeep;
                     var3.isUse = var3.isUse || var3.isUse();
                     if (this.oldInputItemNodeMap.containsKey(var3.scriptItem)) {
                        var3.expanded = ((InputItemNode)this.oldInputItemNodeMap.get(var3.scriptItem)).expanded;
                     }

                     this.inputItemNodes.add(var3);
                  }

                  var3.items.add(var6);
               }
            }

            this.inputItemNodes.sort(inputItemNodeComparator);
         }

         for(var1 = 0; var1 < this.oldInputItemNodes.size(); ++var1) {
            HandcraftLogic.InputItemNode.Release((InputItemNode)this.oldInputItemNodes.get(var1));
         }

         this.inputItemNodesDirty = false;
      }
   }

   protected void rebuildCachedRecipeInfo() {
      if (this.cachedRecipeInfosDirty) {
         int var2;
         for(var2 = 0; var2 < this.cachedRecipeInfos.size(); ++var2) {
            CachedRecipeInfo var1 = (CachedRecipeInfo)this.cachedRecipeInfos.get(var2);
            HandcraftLogic.CachedRecipeInfo.Release(var1);
         }

         this.cachedRecipeInfos.clear();
         this.cachedRecipeInfoMap.clear();

         for(var2 = 0; var2 < this.completeRecipeList.size(); ++var2) {
            CraftRecipe var3 = (CraftRecipe)this.completeRecipeList.get(var2);
            this.createCachedRecipeInfo(var3);
         }

         this.cachedRecipeInfosDirty = false;
         this.cachedCanPerformDirty = true;
      }
   }

   protected BaseCraftingLogic.CachedRecipeInfo createCachedRecipeInfo(CraftRecipe var1) {
      BaseCraftingLogic.CachedRecipeInfo var2 = super.createCachedRecipeInfo(var1);
      boolean var3 = true;
      if (var1.isAnySurfaceCraft()) {
         var3 = this.isCharacterInRangeOfWorkbench();
      }

      var2.overrideCanPerform(var2.isCanPerform() && var3);
      return var2;
   }

   public boolean isCharacterInRangeOfWorkbench() {
      return this.isoObject != null && this.isoObject.getSquare().DistToProper((IsoMovingObject)this.player) < 3.0F;
   }

   public boolean isValidRecipeForCharacter(CraftRecipe var1) {
      if (this.cachedRecipeInfosDirty) {
         this.rebuildCachedRecipeInfo();
      }

      return this.getCachedRecipeInfo(var1).isValid();
   }

   public boolean canCharacterPerformRecipe(CraftRecipe var1) {
      if (this.cachedRecipeInfosDirty) {
         this.rebuildCachedRecipeInfo();
      }

      return this.getCachedRecipeInfo(var1).isCanPerform();
   }

   public boolean isRecipeAvailableForCharacter(CraftRecipe var1) {
      if (this.cachedRecipeInfosDirty) {
         this.rebuildCachedRecipeInfo();
      }

      return this.getCachedRecipeInfo(var1).isAvailable();
   }

   public Texture getResultTexture() {
      if (this.recipeData != null && this.recipeData.getFirstCreatedItem() != null && this.recipeData.getFirstCreatedItem().getIcon() != null) {
         return this.recipeData.getFirstCreatedItem().getIcon();
      } else {
         if (this.recipeData != null && this.recipeData.getRecipe() != null && this.recipeData.getRecipe().getOutputs() != null) {
            for(int var1 = 0; var1 < this.recipeData.getRecipe().getOutputs().size(); ++var1) {
               OutputScript var2 = (OutputScript)this.recipeData.getRecipe().getOutputs().get(var1);
               if (var2.getOutputMapper() != null && var2.getOutputMapper().getEntrees() != null) {
                  for(int var3 = 0; var3 < var2.getOutputMapper().getEntrees().size(); ++var3) {
                     OutputMapper.OutputEntree var4 = (OutputMapper.OutputEntree)var2.getOutputMapper().getEntrees().get(var3);
                     if (!var4.pattern.isEmpty()) {
                        for(int var5 = 0; var5 < var4.pattern.size(); ++var5) {
                           Item var6 = (Item)var4.pattern.get(var5);

                           for(int var7 = 0; var7 < this.recipeData.inputs.size(); ++var7) {
                              InventoryItem var8 = ((CraftRecipeData.InputScriptData)this.recipeData.inputs.get(var7)).getFirstInputItem();
                              if (var8.getFullType().equals(var6.moduleDotType)) {
                                 return Texture.trygetTexture("Item_" + var4.result.getIcon());
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return this.recipeData != null && this.recipeData.getRecipe() != null && this.recipeData.getRecipe().getIconTexture() != null ? this.recipeData.getRecipe().getIconTexture() : null;
      }
   }

   public boolean isCraftCheat() {
      return false;
   }

   public int getPossibleCraftCount(boolean var1) {
      if (var1) {
         if (this.recipeData != null) {
            ArrayList var2 = new ArrayList();
            ArrayList var3 = this.recipeData.getAllInputItems();
            ArrayList var4 = new ArrayList(var2);
            ArrayList var5 = new ArrayList(var3);

            int var6;
            int var7;
            for(var6 = 0; var6 < this.sourceResources.size(); ++var6) {
               for(var7 = 0; var7 < var2.size(); ++var7) {
                  if (((Resource)this.sourceResources.get(var6)).getFilterName().equalsIgnoreCase(((Resource)var2.get(var7)).getFilterName()) && !var4.contains(this.sourceResources.get(var6))) {
                     var4.add((Resource)this.sourceResources.get(var6));
                  }
               }
            }

            for(var6 = 0; var6 < this.allItems.size(); ++var6) {
               for(var7 = 0; var7 < var3.size(); ++var7) {
                  if (((InventoryItem)this.allItems.get(var6)).getFullType().equalsIgnoreCase(((InventoryItem)var3.get(var7)).getFullType()) && !var5.contains(this.allItems.get(var6))) {
                     var5.add((InventoryItem)this.allItems.get(var6));
                  }
               }
            }

            this.cachedPossibleCraftCount = this.recipeData.getPossibleCraftCount(var4, var5, this.multicraftConsumedResources, this.multicraftConsumedItems);
         } else {
            this.cachedPossibleCraftCount = this.recipeData.getPossibleCraftCount(this.sourceResources, this.allItems, this.multicraftConsumedResources, this.multicraftConsumedItems);
         }
      }

      return this.cachedPossibleCraftCount;
   }

   public ArrayList<Resource> getMulticraftConsumedResources() {
      return this.multicraftConsumedResources;
   }

   public ArrayList<InventoryItem> getMulticraftConsumedItems() {
      return this.multicraftConsumedItems;
   }

   public ArrayList<InventoryItem> getMulticraftConsumedItemsFor(InputScript var1, ArrayList<InventoryItem> var2) {
      for(int var3 = 0; var3 < this.multicraftConsumedItems.size(); ++var3) {
         if (var1.canUseItem((InventoryItem)this.multicraftConsumedItems.get(var3))) {
            var2.add((InventoryItem)this.multicraftConsumedItems.get(var3));
         }
      }

      return var2;
   }

   public void setIsoObject(IsoObject var1) {
      this.isoObject = var1;
   }

   public static class InputItemNode {
      private static final ArrayDeque<InputItemNode> pool = new ArrayDeque();
      private InputScript firstMatchedInputScript;
      private CraftRecipe recipe;
      private Item scriptItem;
      private String name;
      private boolean expanded = false;
      private boolean isToolLeft = false;
      private boolean isToolRight = false;
      private boolean isTool = false;
      private boolean isKeep = false;
      private boolean isUse = false;
      private final ArrayList<InventoryItem> items = new ArrayList();

      public InputItemNode() {
      }

      private static InputItemNode Alloc(CraftRecipe var0, Item var1) {
         InputItemNode var2 = (InputItemNode)pool.poll();
         if (var2 == null) {
            var2 = new InputItemNode();
         }

         var2.recipe = var0;
         var2.scriptItem = var1;
         var2.name = var1.getScriptObjectFullType();
         return var2;
      }

      private static void Release(InputItemNode var0) {
         var0.reset();

         assert !pool.contains(var0);

         pool.offer(var0);
      }

      public CraftRecipe getRecipe() {
         return this.recipe;
      }

      public Item getScriptItem() {
         return this.scriptItem;
      }

      public String getName() {
         return this.name;
      }

      public InputScript getFirstMatchedInputScript() {
         return this.firstMatchedInputScript;
      }

      public boolean isExpanded() {
         return this.expanded;
      }

      public void setExpanded(boolean var1) {
         this.expanded = var1;
      }

      public void toggleExpanded() {
         this.expanded = !this.expanded;
      }

      public boolean isToolRight() {
         return this.isToolRight;
      }

      public boolean isToolLeft() {
         return this.isToolLeft;
      }

      public boolean isTool() {
         return this.isTool;
      }

      public boolean isKeep() {
         return this.isKeep;
      }

      public boolean isUse() {
         return this.isUse;
      }

      public ArrayList<InventoryItem> getItems() {
         return this.items;
      }

      private void reset() {
         this.firstMatchedInputScript = null;
         this.recipe = null;
         this.scriptItem = null;
         this.expanded = false;
         this.isTool = false;
         this.isToolLeft = false;
         this.isToolRight = false;
         this.items.clear();
      }
   }

   public static class CachedRecipeInfo {
      private static final ArrayDeque<CachedRecipeInfo> pool = new ArrayDeque();
      private CraftRecipe recipe;
      private boolean isValid;
      private boolean canPerform;
      private boolean available;

      public CachedRecipeInfo() {
      }

      private static CachedRecipeInfo Alloc(CraftRecipe var0) {
         CachedRecipeInfo var1 = (CachedRecipeInfo)pool.poll();
         if (var1 == null) {
            var1 = new CachedRecipeInfo();
         }

         var1.recipe = var0;
         return var1;
      }

      private static void Release(CachedRecipeInfo var0) {
         var0.reset();

         assert !pool.contains(var0);

         pool.offer(var0);
      }

      public CraftRecipe getRecipe() {
         return this.recipe;
      }

      public boolean isValid() {
         return this.isValid;
      }

      public boolean isCanPerform() {
         return this.canPerform;
      }

      public boolean isAvailable() {
         return this.available;
      }

      private void reset() {
         this.recipe = null;
         this.isValid = false;
         this.canPerform = false;
         this.available = false;
      }
   }
}
