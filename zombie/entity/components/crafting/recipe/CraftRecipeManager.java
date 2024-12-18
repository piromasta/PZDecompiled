package zombie.entity.components.crafting.recipe;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.TaggedObjectManager;
import zombie.util.list.PZUnmodifiableList;

public class CraftRecipeManager {
   private static boolean initialized = false;
   private static TaggedObjectManager<CraftRecipe> craftRecipeTagManager;
   private static List<CraftRecipe> unmodifiableAllRecipes;
   private static final Map<IsoPlayer, CraftRecipeData> playerCraftDataMap = new HashMap();
   private static final ArrayList<CraftRecipe> RecipeList = new ArrayList();

   public CraftRecipeManager() {
   }

   public static void Reset() {
      playerCraftDataMap.clear();
      craftRecipeTagManager.clear();
      craftRecipeTagManager = null;
      unmodifiableAllRecipes = null;
      initialized = false;
   }

   public static void Init() {
      craftRecipeTagManager = new TaggedObjectManager(new CraftRecipeListProvider());
      craftRecipeTagManager.registerObjectsFromBackingList();
      initialized = true;
      LogAllRecipesToFile();
   }

   public static String FormatAndRegisterRecipeTagsQuery(String var0) throws Exception {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         return craftRecipeTagManager.formatAndRegisterQueryString(var0);
      }
   }

   public static String sanitizeTagQuery(String var0) {
      return craftRecipeTagManager.formatQueryString(var0);
   }

   public static List<CraftRecipe> getRecipesForTag(String var0) {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         return craftRecipeTagManager.getListForTag(var0);
      }
   }

   public static List<String> getAllRecipeTags() {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         return craftRecipeTagManager.getRegisteredTags();
      }
   }

   public static List<String> getTagGroups() {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         ArrayList var0 = new ArrayList();
         craftRecipeTagManager.getRegisteredTagGroups(var0);
         return var0;
      }
   }

   public static void debugPrintTagManager() {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         craftRecipeTagManager.debugPrint();
      }
   }

   public static ArrayList<String> debugPrintTagManagerLines() {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         ArrayList var0 = new ArrayList();
         craftRecipeTagManager.debugPrint(var0);
         return var0;
      }
   }

   public static void LogAllRecipesToFile() {
      List var0 = queryRecipes("*");
      List var1 = getAllRecipeTags();

      try {
         String var2 = ZomboidFileSystem.instance.getCacheDirSub("Crafting");
         ZomboidFileSystem.ensureFolderExists(var2);
         FileWriter var3 = new FileWriter(var2 + File.separator + "AllRecipes.txt");
         var3.write("Recipe and Tag reference\n\n");
         var3.write("Available Tags:\n");

         int var4;
         for(var4 = 0; var4 < var1.size(); ++var4) {
            var3.write("" + var4 + ": \t" + (String)var1.get(var4) + "\n");
         }

         var3.write("\nAll Recipes:\n");

         for(var4 = 0; var4 < var0.size(); ++var4) {
            var3.write("" + var4 + ": \t" + ((CraftRecipe)var0.get(var4)).getName() + "\n");
         }

         var3.flush();
         var3.close();
      } catch (Exception var5) {
      }

   }

   public static List<CraftRecipe> queryRecipes(String var0) {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else if (!StringUtils.isNullOrWhitespace(var0) && var0.equals("*")) {
         if (unmodifiableAllRecipes == null) {
            unmodifiableAllRecipes = PZUnmodifiableList.wrap(ScriptManager.instance.getAllCraftRecipes());
         }

         return unmodifiableAllRecipes;
      } else {
         return craftRecipeTagManager.queryTaggedObjects(var0);
      }
   }

   public static List<CraftRecipe> populateRecipeList(String var0, List<CraftRecipe> var1, boolean var2) {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         return craftRecipeTagManager.populateList(var0, var1, (List)null, var2);
      }
   }

   public static List<CraftRecipe> populateRecipeList(String var0, List<CraftRecipe> var1, List<CraftRecipe> var2, boolean var3) {
      if (!initialized && Core.bDebug) {
         throw new RuntimeException("Not initialized.");
      } else {
         return craftRecipeTagManager.populateList(var0, var1, var2, var3);
      }
   }

   public static List<CraftRecipe> filterRecipeList(String var0, List<CraftRecipe> var1) {
      return filterRecipeList(var0, var1, ScriptManager.instance.getAllCraftRecipes());
   }

   public static List<CraftRecipe> filterRecipeList(String var0, List<CraftRecipe> var1, List<CraftRecipe> var2) {
      if (var1 != null) {
         var1.clear();
      }

      if (var2 != null && var1 != null) {
         if (StringUtils.isNullOrWhitespace(var0)) {
            var1.addAll(var2);
            return var1;
         } else {
            FilterMode var3 = CraftRecipeManager.FilterMode.Name;
            if (var0.startsWith("@")) {
               var3 = CraftRecipeManager.FilterMode.ModName;
               var0 = var0.substring(1, var0.length());
            } else if (var0.startsWith("$")) {
               var3 = CraftRecipeManager.FilterMode.Tags;
               var0 = var0.substring(1, var0.length());
            }

            var0 = var0.toLowerCase();
            CraftRecipe var4;
            int var8;
            switch (var3) {
               case Name:
                  for(var8 = 0; var8 < var2.size(); ++var8) {
                     var4 = (CraftRecipe)var2.get(var8);
                     if (var4.getTranslationName() != null && var4.getTranslationName().toLowerCase().contains(var0)) {
                        var1.add(var4);
                     }
                  }

                  return var1;
               case ModName:
                  for(var8 = 0; var8 < var2.size(); ++var8) {
                     var4 = (CraftRecipe)var2.get(var8);
                     if (var4.getModName() != null && var4.getModName().toLowerCase().contains(var0)) {
                        var1.add(var4);
                     }
                  }

                  return var1;
               case Tags:
                  StringBuilder var5 = new StringBuilder("");

                  for(int var6 = 0; var6 < craftRecipeTagManager.getRegisteredTags().size(); ++var6) {
                     String var7 = (String)craftRecipeTagManager.getRegisteredTags().get(var6);
                     if (var7.contains(var0)) {
                        if (var5.length() > 0) {
                           var5.append(";");
                        }

                        var5.append(var7);
                     }
                  }

                  String var9 = var5.toString();
                  craftRecipeTagManager.filterList(var9, var1, var2, true);
            }

            return var1;
         }
      } else {
         DebugLog.General.error("one of list parameters is null.");
         return var1;
      }
   }

   public static CraftRecipeData getCraftDataForPlayer(IsoPlayer var0) {
      CraftRecipeData var1 = (CraftRecipeData)playerCraftDataMap.get(var0);
      if (var1 == null) {
         playerCraftDataMap.put(var0, var1);
      }

      throw new RuntimeException("not implemented");
   }

   public static ArrayList<InventoryItem> getAllItemsFromContainers(ArrayList<ItemContainer> var0, ArrayList<InventoryItem> var1) {
      var1.clear();

      for(int var2 = 0; var2 < var0.size(); ++var2) {
         var1.addAll(((ItemContainer)var0.get(var2)).getItems());
      }

      return var1;
   }

   public static ArrayList<InventoryItem> getAllValidItemsForRecipe(CraftRecipe var0, ArrayList<InventoryItem> var1, ArrayList<InventoryItem> var2) {
      for(int var4 = 0; var4 < var1.size(); ++var4) {
         InventoryItem var3 = (InventoryItem)var1.get(var4);
         if (isItemValidForRecipe(var0, var3)) {
            var2.add(var3);
         }
      }

      return var2;
   }

   public static InputScript getValidInputScriptForItem(CraftRecipe var0, InventoryItem var1) {
      for(int var3 = 0; var3 < var0.getInputs().size(); ++var3) {
         InputScript var2 = (InputScript)var0.getInputs().get(var3);
         if (var2.getResourceType() == ResourceType.Item && isItemValidForInputScript(var2, var1)) {
            return var2;
         }
      }

      return null;
   }

   public static boolean isItemToolForRecipe(CraftRecipe var0, InventoryItem var1) {
      InputScript var2 = getValidInputScriptForItem(var0, var1);
      return var2 != null && (var2.hasFlag(InputFlag.ToolLeft) || var2.hasFlag(InputFlag.ToolRight));
   }

   public static boolean isItemValidForRecipe(CraftRecipe var0, InventoryItem var1) {
      InputScript var2 = getValidInputScriptForItem(var0, var1);
      return var2 != null;
   }

   public static boolean isItemValidForInputScript(InputScript var0, InventoryItem var1) {
      return var0.getResourceType() == ResourceType.Item ? consumeInputItem(var0, (InventoryItem)var1, true, (CraftRecipeData.CacheData)null) : false;
   }

   public static boolean isValidRecipeForCharacter(CraftRecipe var0, IsoGameCharacter var1, CraftRecipeMonitor var2) {
      if (var0 == null) {
         return false;
      } else if (var1 != null && !validateHasRequiredSkill(var0, var1)) {
         if (var2 != null) {
            var2.log("Player doesn't have required skill for " + var0.getScriptObjectFullType());
         }

         return false;
      } else if (var1 != null && var0.needToBeLearn() && !var1.isRecipeKnown(var0, true)) {
         if (var2 != null) {
            var2.log("Player doesn't know recipe " + var0.getScriptObjectFullType());
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean validateHasRequiredSkill(CraftRecipe var0, IsoGameCharacter var1) {
      if (var0.getRequiredSkillCount() > 0) {
         for(int var2 = 0; var2 < var0.getRequiredSkillCount(); ++var2) {
            CraftRecipe.RequiredSkill var3 = var0.getRequiredSkill(var2);
            if (var1.getPerkLevel(var3.getPerk()) < var3.getLevel()) {
               return false;
            }
         }
      }

      return true;
   }

   public static boolean hasPlayerLearnedRecipe(CraftRecipe var0, IsoGameCharacter var1) {
      if (var0 != null && var1 != null) {
         return !var0.needToBeLearn() || var1.isRecipeKnown(var0);
      } else {
         return false;
      }
   }

   public static boolean hasPlayerRequiredSkill(CraftRecipe.RequiredSkill var0, IsoGameCharacter var1) {
      if (var0 != null && var1 != null) {
         return var1.getPerkLevel(var0.getPerk()) >= var0.getLevel();
      } else {
         return false;
      }
   }

   public static int getAutoCraftCountItems(CraftRecipe var0, ArrayList<InventoryItem> var1) {
      boolean var4 = false;
      int var5 = 0;
      boolean var6 = false;

      for(int var7 = 0; var7 < var0.getInputs().size(); ++var7) {
         InputScript var2 = (InputScript)var0.getInputs().get(var7);
         if (var2.getResourceType() == ResourceType.Item) {
            int var9 = 0;
            int var10 = 0;

            int var8;
            for(var8 = 0; var8 < var1.size(); ++var8) {
               InventoryItem var3 = (InventoryItem)var1.get(var8);
               if (consumeInputItem(var2, (InventoryItem)var3, true, (CraftRecipeData.CacheData)null)) {
                  if (!var2.isKeep()) {
                     ++var10;
                  } else if (var2.hasConsumeFromItem()) {
                     switch (var2.getResourceType()) {
                        case Item:
                           var9 += var3.getCurrentUses() / (int)var2.getAmount();
                           break;
                        case Fluid:
                           var9 += PZMath.fastfloor(var3.getFluidContainer().getAmount() / var2.getAmount());
                           break;
                        case Energy:
                           var9 += var3.getCurrentUses() / (int)var2.getAmount();
                     }
                  } else {
                     ++var9;
                  }
               }
            }

            if (var10 > 0) {
               var8 = PZMath.fastfloor((float)var10 / (float)var2.getIntAmount());
               var5 = PZMath.min(var5, var8);
            } else if (var9 > 0) {
               var5 = PZMath.min(var5, var9);
            }

            if (var5 == 0) {
               break;
            }
         }
      }

      return PZMath.max(0, var5);
   }

   private static boolean validateInputScript(InputScript var0, ResourceType var1, boolean var2) {
      if (var0 != null && var0.getResourceType() == var1) {
         if (Core.bDebug && !var2 && GameClient.bClient) {
            throw new RuntimeException("Recipes can only be tested on client, input=" + var0);
         } else {
            return true;
         }
      } else if (Core.bDebug) {
         throw new RuntimeException("Wrong InputScript.ResourceType for call or null, input=" + var0);
      } else {
         return false;
      }
   }

   private static boolean validateOutputScript(OutputScript var0, ResourceType var1, boolean var2) {
      if (var0 != null && var0.getResourceType() == var1) {
         if (Core.bDebug && !var2 && GameClient.bClient) {
            throw new RuntimeException("Recipes can only be tested on client, output=" + var0);
         } else {
            return true;
         }
      } else if (Core.bDebug) {
         throw new RuntimeException("Wrong OutputScript.ResourceType for call or null, output=" + var0);
      } else {
         return false;
      }
   }

   protected static boolean consumeInputFromResource(InputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var0 != null && var1 != null && var0.getResourceType() == var1.getType()) {
         switch (var0.getResourceType()) {
            case Item:
               return consumeInputItem(var0, var1, var2, var3);
            case Fluid:
               return consumeInputFluid(var0, var1, var2, var3);
            case Energy:
               return consumeInputEnergy(var0, var1, var2, var3);
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   protected static boolean consumeInputItem(InputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateInputScript(var0, ResourceType.Item, var2)) {
         return false;
      } else if (var1 != null && !var1.isEmpty() && var1.getType() == ResourceType.Item) {
         if (var1.getItemAmount() < var0.getIntAmount()) {
            return false;
         } else {
            for(int var5 = 0; var5 < var0.getIntAmount(); ++var5) {
               InventoryItem var4;
               if (var2) {
                  var4 = var1.peekItem(var5);
               } else {
                  var4 = var1.peekItem();
               }

               if (var4 == null) {
                  return false;
               }

               if (!consumeInputItem(var0, var4, var2, var3)) {
                  return false;
               }

               if (!var2 && (!var0.isKeep() || var1.canMoveItemsToOutput())) {
                  InventoryItem var6 = var1.pollItem(true, false);
                  var1.setDirty();
                  if ((var6 == null || var6 != var4) && Core.bDebug) {
                     throw new RuntimeException("Item didnt get polled.");
                  }
               }
            }

            if (var3 != null && !var1.canMoveItemsToOutput()) {
               var3.setMoveToOutputs(false);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   protected static boolean consumeInputItem(InputScript var0, InventoryItem var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateInputScript(var0, ResourceType.Item, var2)) {
         return false;
      } else {
         boolean var4 = consumeInputItemInternal(var0, var1, var2, var3);
         boolean var5 = var0.isKeep();
         if (var4 && var0.hasConsumeFromItem()) {
            InputScript var6 = var0.getConsumeFromItemScript();
            switch (var6.getResourceType()) {
               case Item:
                  var4 = consumeInputItemUsesInternal(var6, var1, var2, var3);
                  if (var4 && !var0.isDestroy()) {
                     var5 = (float)var1.getCurrentUses() < var0.getAmount();
                  }
                  break;
               case Fluid:
                  var4 = consumeInputFluidInternal(var6, var1.getFluidContainer(), var2, var3);
                  if (var4) {
                     var5 = var1.getFluidContainer().getAmount() < var0.getAmount();
                  }
                  break;
               case Energy:
                  var4 = consumeInputEnergyFromItemInternal(var6, var1, var2, var3);
                  if (var4) {
                     var5 = (float)var1.getCurrentUses() < var0.getAmount();
                  }
            }
         }

         if (var4 && var0.hasCreateToItem()) {
            OutputScript var7 = var0.getCreateToItemScript();
            switch (var7.getResourceType()) {
               case Item:
                  var4 = createOutputItemUsesInternal(var7, var1, var2, var3);
                  break;
               case Fluid:
                  var4 = createOutputFluidInternal(var7, var1.getFluidContainer(), var2, var3);
                  break;
               case Energy:
                  var4 = createOutputEnergyToItemInternal(var7, var1, var2, var3);
            }
         }

         if (var4) {
            if (var3 != null) {
               var3.setMoveToOutputs(var5);
            }

            return true;
         } else {
            if (var3 != null) {
               var3.softReset();
            }

            return false;
         }
      }
   }

   private static boolean consumeInputItemInternal(InputScript var0, InventoryItem var1, boolean var2, CraftRecipeData.CacheData var3) {
      Item var4 = var1.getScriptItem();
      if (var4 == null) {
         return false;
      } else if (var0.containsItem(var4)) {
         if (!var0.getParentRecipe().OnTestItem(var1)) {
            return false;
         } else {
            boolean var5 = var0.hasFlag(InputFlag.AllowFavorite) || var0.isKeep();
            if (var1.isFavorite() && !var5) {
               return false;
            } else if (!var0.hasFlag(InputFlag.AllowRottenItem) && var1 instanceof Food && ((Food)var1).isRotten()) {
               return false;
            } else if (!var0.hasFlag(InputFlag.AllowFrozenItem) && var1 instanceof Food && ((Food)var1).isFrozen()) {
               return false;
            } else if (!var0.hasFlag(InputFlag.AllowDestroyedItem) && var1.isBroken()) {
               return false;
            } else if (var0.hasFlag(InputFlag.IsWorn) && (var1 instanceof Clothing || var1 instanceof InventoryContainer) && !var1.isWorn()) {
               return false;
            } else if (var0.hasFlag(InputFlag.IsNotWorn) && (var1 instanceof Clothing || var1 instanceof InventoryContainer) && var1.isWorn()) {
               return false;
            } else if (var0.hasFlag(InputFlag.IsDamaged) && !var1.isDamaged()) {
               return false;
            } else if (var0.hasFlag(InputFlag.IsUndamaged) && var1.isDamaged()) {
               return false;
            } else if (var0.hasFlag(InputFlag.IsNotDull) && var1.hasSharpness() && var1.isDull()) {
               return false;
            } else if (var0.hasFlag(InputFlag.IsSharpenable) && (!var1.hasSharpness() || !(var1.getSharpness() < var1.getMaxSharpness()))) {
               return false;
            } else {
               if (var0.hasFlag(InputFlag.IsEmptyContainer) && var1 instanceof InventoryContainer) {
                  InventoryContainer var6 = (InventoryContainer)var1;
                  if (!var6.isEmpty()) {
                     return false;
                  }
               }

               Food var7;
               if (var0.hasFlag(InputFlag.IsWholeFoodItem) && var1 instanceof Food) {
                  var7 = (Food)var1;
                  if (!var7.isWholeFoodItem()) {
                     return false;
                  }
               }

               if (var0.hasFlag(InputFlag.IsUncookedFoodItem) && var1 instanceof Food) {
                  var7 = (Food)var1;
                  if (var1.isCookable() && !var7.isUncooked()) {
                     return false;
                  }
               }

               if (var0.hasFlag(InputFlag.IsCookedFoodItem) && var1 instanceof Food) {
                  var7 = (Food)var1;
                  if (var1.isCookable() && var7.isUncooked()) {
                     return false;
                  }
               }

               DrainableComboItem var8;
               if (var1 instanceof DrainableComboItem) {
                  var8 = (DrainableComboItem)var1;
                  if (var0.hasFlag(InputFlag.IsFull) && !var8.isFullUses()) {
                     return false;
                  }

                  if (var0.hasFlag(InputFlag.IsEmpty) && !var8.isEmptyUses()) {
                     return false;
                  }

                  if (var0.hasFlag(InputFlag.NotFull) && var8.isFullUses()) {
                     return false;
                  }

                  if (var0.hasFlag(InputFlag.NotEmpty) && var8.isEmptyUses()) {
                     return false;
                  }
               }

               if (var0.hasFlag(InputFlag.ItemIsFluid)) {
                  if (var1.getFluidContainer() == null) {
                     return false;
                  }

                  FluidContainer var9 = var1.getFluidContainer();
                  if (var0.hasFlag(InputFlag.IsFull) && !var9.isFull()) {
                     return false;
                  }

                  if (var0.hasFlag(InputFlag.IsEmpty) && !var9.isEmpty()) {
                     return false;
                  }
               }

               if (var0.hasFlag(InputFlag.ItemIsEnergy)) {
                  if (!(var1 instanceof DrainableComboItem)) {
                     return false;
                  }

                  var8 = (DrainableComboItem)var1;
                  if (!var8.isEnergy()) {
                     return false;
                  }

                  if (var0.hasFlag(InputFlag.IsFull) && !var8.isFullUses()) {
                     return false;
                  }

                  if (var0.hasFlag(InputFlag.IsEmpty) && !var8.isEmptyUses()) {
                     return false;
                  }
               }

               if (var3 != null) {
                  var3.addAppliedItem(var1);
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean consumeInputItemUsesInternal(InputScript var0, InventoryItem var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var1 instanceof DrainableComboItem) {
         Item var4 = var1.getScriptItem();
         if (var4 == null || !var0.containsItem(var4)) {
            return false;
         }

         DrainableComboItem var5 = (DrainableComboItem)var1;
         if (var0.hasFlag(InputFlag.IsFull) && !var5.isFullUses()) {
            return false;
         }

         int var6 = 0;
         if (var0.getAmount() > 0.0F) {
            var6 = (int)var0.getAmount();
         }

         if (var5.getCurrentUses() >= var6) {
            if (!var2 && !var0.isKeep()) {
               var5.setCurrentUses(var5.getCurrentUses() - var6);
            }

            if (var3 != null) {
               var3.usesConsumed = (float)var6;
            }

            return true;
         }
      }

      return false;
   }

   private static boolean consumeInputFluid(InputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateInputScript(var0, ResourceType.Fluid, var2)) {
         return false;
      } else {
         return var1 != null && var1.getType() == ResourceType.Fluid && !var1.isEmpty() ? consumeInputFluidInternal(var0, ((ResourceFluid)var1).getFluidContainer(), var2, var3) : false;
      }
   }

   private static boolean consumeInputFluidInternal(InputScript var0, FluidContainer var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var1 != null && !var1.isEmpty()) {
         if (var0.hasFlag(InputFlag.IsFull) && !var1.isFull()) {
            return false;
         }

         if (var1.getAmount() >= var0.getAmount() && var0.isFluidMatch(var1)) {
            if (!var2) {
               if (var3 != null) {
                  var1.createFluidSample(var3.fluidSample, var0.getAmount());
               }

               if (!var0.isKeep()) {
                  if (var3 != null) {
                     var1.removeFluid(var0.getAmount(), false, var3.fluidConsume);
                  } else {
                     var1.removeFluid(var0.getAmount(), false);
                  }
               }
            }

            if (var3 != null) {
               var3.fluidConsumed = var0.getAmount();
            }

            return true;
         }
      }

      return false;
   }

   private static boolean consumeInputEnergy(InputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateInputScript(var0, ResourceType.Energy, var2)) {
         return false;
      } else if (var1 != null && var1.getType() == ResourceType.Energy && !var1.isEmpty()) {
         ResourceEnergy var4 = (ResourceEnergy)var1;
         if (var0.hasFlag(InputFlag.IsFull) && !var1.isFull()) {
            return false;
         } else if (var4.getEnergyAmount() >= var0.getAmount() && var0.isEnergyMatch(var4.getEnergy())) {
            if (!var0.isKeep() && !var2) {
               var4.setEnergyAmount(var1.getEnergyAmount() - var0.getAmount());
            }

            if (var3 != null) {
               var3.energyConsumed = var0.getAmount();
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean consumeInputEnergyFromItemInternal(InputScript var0, InventoryItem var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var1 instanceof DrainableComboItem var4) {
         if (var0.hasFlag(InputFlag.IsFull) && !var4.isFullUses()) {
            return false;
         }

         int var5 = var0.getAmount() > 0.0F ? (int)var0.getAmount() : 1;
         if (var4.getCurrentUses() >= var5 && var0.isEnergyMatch(var4)) {
            if (!var2 && !var0.isKeep()) {
               var4.setCurrentUses(var4.getCurrentUses() - var5);
            }

            if (var3 != null) {
               var3.energyConsumed = (float)var5;
            }

            return true;
         }
      }

      return false;
   }

   protected static boolean createOutputToResource(OutputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var0 != null && var1 != null && var0.getResourceType() == var1.getType()) {
         switch (var0.getResourceType()) {
            case Fluid:
               return createOutputFluid(var0, var1, var2, var3);
            case Energy:
               return createOutputEnergy(var0, var1, var2, var3);
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   protected static boolean createOutputItem(OutputScript var0, Item var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateOutputScript(var0, ResourceType.Item, var2)) {
         return false;
      } else {
         boolean var4 = createOutputItemInternal(var0, var1, var2, var3);
         if (var4 && !var2 && var0.hasCreateToItem() && var3.getMostRecentItem() != null) {
            OutputScript var5 = var0.getCreateToItemScript();
            boolean var6 = false;
            InventoryItem var7 = var3.getMostRecentItem();

            assert var7 != null;

            switch (var5.getResourceType()) {
               case Item:
                  var6 = createOutputItemUsesInternal(var5, var7, var2, var3);
                  break;
               case Fluid:
                  var6 = createOutputFluidInternal(var5, var7.getFluidContainer(), var2, var3);
                  break;
               case Energy:
                  var6 = createOutputEnergyToItemInternal(var5, var7, var2, var3);
            }

            if (!var6) {
               String var10001 = var1 != null ? var1.getFullName() : "unknown";
               DebugLog.General.warn("unable to create uses/fluid/energy to item: " + var10001);
            }
         }

         if (var4) {
            return true;
         } else {
            var3.softReset();
            return false;
         }
      }
   }

   private static boolean createOutputItemInternal(OutputScript var0, Item var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!var2 && var0.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > var0.getChance()) {
         return true;
      } else if (var1 != null && var1.getFullName() != null) {
         if (!var2) {
            InventoryItem var4 = InventoryItemFactory.CreateItem(var1.getFullName());
            if (var4 != null) {
               if (var3 != null) {
                  var3.addAppliedItem(var4);
               }
            } else {
               DebugLog.General.warn("Failed to create item: " + var1.getFullName());
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean createOutputItemUsesInternal(OutputScript var0, InventoryItem var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var1 instanceof DrainableComboItem var4) {
         int var5 = var1.getCurrentUses();
         if (var5 > 0 && var0.hasFlag(OutputFlag.ForceEmpty)) {
            var5 = 0;
            if (!var2) {
               var4.setCurrentUses(0);
            }
         }

         if (var5 > 0 && var0.hasFlag(OutputFlag.IsEmpty)) {
            return false;
         } else {
            int var6 = var0.getAmount() > 0.0F ? (int)var0.getAmount() : 1;
            int var7 = PZMath.min(var6, var4.getMaxUses() - var5);
            if (var0.hasFlag(OutputFlag.AlwaysFill)) {
               var7 = var4.getMaxUses() - var5;
            }

            if (var7 <= 0) {
               return false;
            } else if (var7 < var6 && var0.hasFlag(OutputFlag.RespectCapacity)) {
               return false;
            } else if (!var2 && var0.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > var0.getChance()) {
               return true;
            } else {
               if (!var2) {
                  var4.setCurrentUses(PZMath.min(var4.getCurrentUses() + var7, var4.getMaxUses()));
               }

               if (var3 != null) {
                  var3.usesCreated = (float)var7;
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean createOutputFluid(OutputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateOutputScript(var0, ResourceType.Fluid, var2)) {
         return false;
      } else {
         return var1 != null && var1.getType() == ResourceType.Fluid && !var1.isFull() ? createOutputFluidInternal(var0, ((ResourceFluid)var1).getFluidContainer(), var2, var3) : false;
      }
   }

   private static boolean createOutputFluidInternal(OutputScript var0, FluidContainer var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var0.getFluid() == null) {
         return false;
      } else if (var1 != null && !var1.isFull()) {
         float var4 = var1.getAmount();
         if (var4 > 0.0F && var0.hasFlag(OutputFlag.ForceEmpty)) {
            var4 = 0.0F;
            if (!var2) {
               var1.Empty();
            }
         }

         if (var4 > 0.0F && var0.hasFlag(OutputFlag.IsEmpty)) {
            return false;
         } else {
            float var5 = PZMath.min(var0.getAmount(), var1.getCapacity() - var4);
            if (var0.hasFlag(OutputFlag.AlwaysFill)) {
               var5 = var1.getCapacity() - var4;
            }

            if (var5 <= 0.0F) {
               return false;
            } else if (var5 < var0.getAmount() && var0.hasFlag(OutputFlag.RespectCapacity)) {
               return false;
            } else if (!var1.canAddFluid(var0.getFluid())) {
               return false;
            } else if (!var2 && var0.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > var0.getChance()) {
               return true;
            } else {
               if (!var2) {
                  var1.addFluid(var0.getFluid(), var5);
               }

               if (var3 != null) {
                  var3.fluidCreated = var5;
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean createOutputEnergy(OutputScript var0, Resource var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (!validateOutputScript(var0, ResourceType.Energy, var2)) {
         return false;
      } else if (var1 != null && !var1.isFull()) {
         ResourceEnergy var4 = (ResourceEnergy)var1;
         if (var4.getEnergy() != null && var4.getEnergy().equals(var0.getEnergy())) {
            float var5 = var1.getEnergyAmount();
            if (var5 > 0.0F && var0.hasFlag(OutputFlag.ForceEmpty)) {
               var5 = 0.0F;
               if (!var2) {
                  var4.setEnergyAmount(0.0F);
               }
            }

            if (var5 > 0.0F && var0.hasFlag(OutputFlag.IsEmpty)) {
               return false;
            } else {
               float var6 = PZMath.min(var0.getAmount(), var4.getEnergyCapacity() - var5);
               if (var0.hasFlag(OutputFlag.AlwaysFill)) {
                  var6 = var4.getEnergyCapacity() - var5;
               }

               if (var6 < 0.0F) {
                  return false;
               } else if (var6 < var0.getAmount() && var0.hasFlag(OutputFlag.RespectCapacity)) {
                  return false;
               } else if (!var2 && var0.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > var0.getChance()) {
                  return true;
               } else {
                  if (!var2) {
                     var4.setEnergyAmount(var1.getEnergyAmount() + var6);
                  }

                  if (var3 != null) {
                     var3.energyCreated = var6;
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean createOutputEnergyToItemInternal(OutputScript var0, InventoryItem var1, boolean var2, CraftRecipeData.CacheData var3) {
      if (var1 instanceof DrainableComboItem var4) {
         if (var4.isEnergy() && var4.getEnergy() != null && var4.getEnergy().equals(var0.getEnergy())) {
            int var5 = var4.getCurrentUses();
            if (var5 > 0 && var0.hasFlag(OutputFlag.ForceEmpty)) {
               var5 = 0;
               if (!var2) {
                  var4.setCurrentUses(0);
               }
            }

            if (var5 > 0 && var0.hasFlag(OutputFlag.IsEmpty)) {
               return false;
            } else {
               int var6 = var0.getAmount() > 0.0F ? (int)var0.getAmount() : 1;
               int var7 = PZMath.min(var6, var4.getMaxUses() - var5);
               if (var0.hasFlag(OutputFlag.AlwaysFill)) {
                  var7 = var4.getMaxUses() - var5;
               }

               if (var7 <= 0) {
                  return false;
               } else if (var7 < var6 && var0.hasFlag(OutputFlag.RespectCapacity)) {
                  return false;
               } else if (!var2 && var0.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > var0.getChance()) {
                  return true;
               } else {
                  if (!var2) {
                     var4.setCurrentUses(PZMath.min(var4.getCurrentUses() + var7, var4.getMaxUses()));
                  }

                  if (var3 != null) {
                     var3.energyCreated = (float)var7;
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static ArrayList<CraftRecipe> getUniqueRecipeItems(InventoryItem var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2) {
      RecipeList.clear();
      List var3 = queryRecipes("InHandCraft");
      HandcraftLogic var4 = new HandcraftLogic(var1, (CraftBench)null, (IsoObject)null);
      var4.setContainers(var2);

      for(int var5 = 0; var5 < var3.size(); ++var5) {
         CraftRecipe var6 = (CraftRecipe)var3.get(var5);
         if (!var6.hasTag("NoRightContextClick") && isValidRecipeForCharacter(var6, var1, (CraftRecipeMonitor)null) && getValidInputScriptForItem(var6, var0) != null && var6.OnTestItem(var0)) {
            var4.setRecipe(var6);
            var4.getRecipeData().offerInputItem(var0);
            if (var4.canPerformCurrentRecipe()) {
               RecipeList.add(var6);
            }
         }
      }

      return RecipeList;
   }

   private static class CraftRecipeListProvider implements TaggedObjectManager.BackingListProvider<CraftRecipe> {
      private CraftRecipeListProvider() {
      }

      public ArrayList<CraftRecipe> getTaggedObjectList() {
         return ScriptManager.instance.getAllCraftRecipes();
      }
   }

   public static enum FilterMode {
      Name,
      ModName,
      Tags,
      InputName,
      OutputName;

      private FilterMode() {
      }
   }
}
