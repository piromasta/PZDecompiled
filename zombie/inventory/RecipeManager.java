package zombie.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.recipemanager.ItemRecipe;
import zombie.inventory.recipemanager.RecipeMonitor;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.Moveable;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.MovableRecipe;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.util.StringUtils;

public class RecipeManager {
   private static final ArrayList<Recipe> RecipeList = new ArrayList();

   public RecipeManager() {
   }

   public static void ScriptsLoaded() {
      ArrayList var0 = ScriptManager.instance.getAllRecipes();
      HashSet var1 = new HashSet();
      Iterator var2 = var0.iterator();

      while(true) {
         Recipe var3;
         Iterator var4;
         do {
            if (!var2.hasNext()) {
               return;
            }

            var3 = (Recipe)var2.next();
            var4 = var3.getSource().iterator();

            while(var4.hasNext()) {
               Recipe.Source var5 = (Recipe.Source)var4.next();

               for(int var6 = 0; var6 < var5.getItems().size(); ++var6) {
                  String var7 = (String)var5.getItems().get(var6);
                  if (var7.startsWith("Fluid.")) {
                     var5.getItems().set(var6, var7);
                  } else if (!"Water".equals(var7) && !var7.contains(".") && !var7.startsWith("[")) {
                     Item var8 = resolveItemModuleDotType(var3, var7, var1, "recipe source");
                     if (var8 == null) {
                        var5.getItems().set(var6, "???." + var7);
                     } else {
                        var5.getItems().set(var6, var8.getFullName());
                     }
                  }
               }
            }
         } while(var3.getResults().size() <= 0);

         var4 = var3.getResults().iterator();

         while(var4.hasNext()) {
            Recipe.Result var9 = (Recipe.Result)var4.next();
            if (var9.getModule() == null) {
               Item var10 = resolveItemModuleDotType(var3, var9.getType(), var1, "recipe result");
               if (var10 == null) {
                  var9.module = "???";
               } else {
                  var9.module = var10.getModule().getName();
               }
            }
         }
      }
   }

   private static Item resolveItemModuleDotType(Recipe var0, String var1, Set<String> var2, String var3) {
      ScriptModule var4 = var0.getModule();
      Item var5 = var4.getItem(var1);
      if (var5 != null && !var5.getObsolete()) {
         return var5;
      } else {
         for(int var6 = 0; var6 < ScriptManager.instance.ModuleList.size(); ++var6) {
            ScriptModule var7 = (ScriptModule)ScriptManager.instance.ModuleList.get(var6);
            var5 = var7.getItem(var1);
            if (var5 != null && !var5.getObsolete()) {
               String var8 = var0.getModule().getName();
               if (!var2.contains(var8)) {
                  var2.add(var8);
                  DebugLog.Recipe.warn("WARNING: module \"%s\" may have forgot to import module Base", var8);
               }

               return var5;
            }
         }

         DebugLog.Recipe.warn("ERROR: can't find %s \"%s\" in recipe \"%s\"", var3, var1, var0.getOriginalname());
         return null;
      }
   }

   public static void LoadedAfterLua() {
      ArrayList var0 = new ArrayList();
      ArrayList var1 = ScriptManager.instance.getAllRecipes();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         Recipe var3 = (Recipe)var1.get(var2);
         DebugLog.Recipe.debugln("Checking Recipe " + var3.name);
         Iterator var4 = var3.getSource().iterator();

         while(var4.hasNext()) {
            Recipe.Source var5 = (Recipe.Source)var4.next();
            ScriptManager.resolveGetItemTypes(var5.getItems(), var0);
         }
      }

      var0.clear();
   }

   private static void testLuaFunction(Recipe var0, String var1, String var2) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         Object var3 = LuaManager.getFunctionObject(var1);
         if (var3 == null) {
            DebugLog.Recipe.error("no such function %s = \"%s\" in recipe \"%s\"", var2, var1, var0.name);
         }

      }
   }

   public static int getKnownRecipesNumber(IsoGameCharacter var0) {
      int var1 = 0;
      ArrayList var2 = ScriptManager.instance.getAllRecipes();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         Recipe var4 = (Recipe)var2.get(var3);
         if (var0.isRecipeKnown(var4)) {
            ++var1;
         }
      }

      return var1;
   }

   public static ArrayList<Recipe> getUniqueRecipeItems(InventoryItem var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2) {
      RecipeList.clear();
      ArrayList var3 = ScriptManager.instance.getAllRecipes();

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         Recipe var5 = (Recipe)var3.get(var4);
         if (IsRecipeValid(var5, var1, var0, var2)) {
            RecipeList.add(var5);
         }
      }

      if (var0 instanceof Moveable && RecipeList.size() == 0 && ((Moveable)var0).getWorldSprite() != null) {
         if (var0.type != null && var0.type.equalsIgnoreCase(((Moveable)var0).getWorldSprite())) {
            MovableRecipe var6 = new MovableRecipe();
            LuaEventManager.triggerEvent("OnDynamicMovableRecipe", ((Moveable)var0).getWorldSprite(), var6, var0, var1);
            if (var6.isValid() && IsRecipeValid(var6, var1, var0, var2)) {
               RecipeList.add(var6);
            }
         } else {
            DebugLog.Recipe.warn("RecipeManager -> Cannot create recipe for this movable item: " + var0.getFullType());
         }
      }

      return RecipeList;
   }

   public static boolean IsRecipeValid(Recipe var0, IsoGameCharacter var1, InventoryItem var2, ArrayList<ItemContainer> var3) {
      if (Core.bDebug) {
      }

      if (var0.Result == null) {
         return false;
      } else if (!var1.isRecipeKnown(var0)) {
         return false;
      } else if (var2 != null && !validateRecipeContainsSourceItem(var0, var2)) {
         return false;
      } else if (!validateHasAllRequiredItems(var0, var1, var2, var3)) {
         return false;
      } else if (!validateHasRequiredSkill(var0, var1)) {
         return false;
      } else if (!validateNearIsoObject(var0, var1)) {
         return false;
      } else if (!validateHasHeat(var0, var2, var3, var1)) {
         return false;
      } else {
         Iterator var4 = var0.getSource().iterator();

         boolean var6;
         do {
            Recipe.Source var5;
            do {
               if (!var4.hasNext()) {
                  return true;
               }

               var5 = (Recipe.Source)var4.next();
            } while(var5.keep);

            var6 = false;
            Iterator var7 = var5.getItems().iterator();

            while(var7.hasNext()) {
               String var8 = (String)var7.next();
               Iterator var9;
               if (var3 == null) {
                  var9 = var1.getInventory().getItems().iterator();

                  label113:
                  while(true) {
                     while(true) {
                        if (!var9.hasNext()) {
                           break label113;
                        }

                        InventoryItem var15 = (InventoryItem)var9.next();
                        if (var15.getFullType().equals(var8) && validateCanPerform(var0, var1, var15)) {
                           var6 = true;
                        } else if (var8.startsWith("Fluid.") && validateCanPerform(var0, var1, var15)) {
                           String var16 = var8.substring(6);
                           Fluid var17 = Fluid.Get(var16);
                           if (var15.hasComponent(ComponentType.FluidContainer) && var15.getFluidContainer().contains(var17) && var15.getFluidContainer().getAmount() >= var5.use) {
                              var6 = true;
                           }
                        }
                     }
                  }
               } else {
                  var9 = var3.iterator();

                  while(var9.hasNext()) {
                     ItemContainer var10 = (ItemContainer)var9.next();
                     Iterator var11 = var10.getItems().iterator();

                     while(true) {
                        while(var11.hasNext()) {
                           InventoryItem var12 = (InventoryItem)var11.next();
                           if (var12.getFullType().equals(var8) && validateCanPerform(var0, var1, var12)) {
                              var6 = true;
                           } else if (var8.startsWith("Fluid.") && validateCanPerform(var0, var1, var12)) {
                              String var13 = var8.substring(6);
                              Fluid var14 = Fluid.Get(var13);
                              if (var12.hasComponent(ComponentType.FluidContainer) && var12.getFluidContainer().contains(var14) && var12.getFluidContainer().getAmount() >= var5.use) {
                                 var6 = true;
                              }
                           }
                        }

                        if (var6) {
                        }
                        break;
                     }
                  }
               }

               if (var6) {
               }
            }
         } while(var6);

         return false;
      }
   }

   public static void printDebugRecipeValid(Recipe var0, IsoGameCharacter var1, InventoryItem var2, ArrayList<ItemContainer> var3) {
      if (RecipeMonitor.canLog()) {
         RecipeMonitor.LogBlanc();
      }

      if (RecipeMonitor.canLog()) {
         RecipeMonitor.Log("[DebugTestRecipeValid]", RecipeMonitor.colHeader);
      }

      if (RecipeMonitor.canLog()) {
         RecipeMonitor.IncTab();
      }

      boolean var4 = true;
      String var5;
      if (var0.Result == null) {
         var5 = "invalid: recipe result is null.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (!var1.isRecipeKnown(var0)) {
         var5 = "invalid: recipe not known.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (var2 != null && !validateRecipeContainsSourceItem(var0, var2)) {
         var5 = "invalid: recipe does not contain source item.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (!validateHasAllRequiredItems(var0, var1, var2, var3)) {
         var5 = "invalid: recipe does not have all required items.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (!validateHasRequiredSkill(var0, var1)) {
         var5 = "invalid: character does not have required skill.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (!validateNearIsoObject(var0, var1)) {
         var5 = "invalid: recipe is not near required IsoObject.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (!validateHasHeat(var0, var2, var3, var1)) {
         var5 = "invalid: recipe heat validation failed.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      if (!validateCanPerform(var0, var1, var2)) {
         var5 = "invalid: recipe can perform failed.";
         DebugLog.Recipe.warn(var5);
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(var5, RecipeMonitor.colNeg);
         }

         var4 = false;
      }

      var5 = "recipe overall valid: " + var4;
      DebugLog.Recipe.println(var5);
      if (RecipeMonitor.canLog()) {
         RecipeMonitor.DecTab();
      }

      if (RecipeMonitor.canLog()) {
         RecipeMonitor.Log(var5, var4 ? RecipeMonitor.colPos : RecipeMonitor.colNeg);
      }

   }

   private static boolean validateNearIsoObject(Recipe var0, IsoGameCharacter var1) {
      return false;
   }

   private static boolean validateCanPerform(Recipe var0, IsoGameCharacter var1, InventoryItem var2) {
      return false;
   }

   private static boolean validateHasRequiredSkill(Recipe var0, IsoGameCharacter var1) {
      if (var0.getRequiredSkillCount() > 0) {
         for(int var2 = 0; var2 < var0.getRequiredSkillCount(); ++var2) {
            Recipe.RequiredSkill var3 = var0.getRequiredSkill(var2);
            if (var1.getPerkLevel(var3.getPerk()) < var3.getLevel()) {
               return false;
            }
         }
      }

      return true;
   }

   public static boolean validateRecipeContainsSourceItem(Recipe var0, InventoryItem var1) {
      for(int var2 = 0; var2 < var0.Source.size(); ++var2) {
         Recipe.Source var3 = (Recipe.Source)var0.getSource().get(var2);

         for(int var4 = 0; var4 < var3.getItems().size(); ++var4) {
            String var5 = (String)var3.getItems().get(var4);
            if (var5.startsWith("Fluid.") && var1.hasComponent(ComponentType.FluidContainer)) {
               return true;
            }

            if (var5.equals(var1.getFullType())) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean validateHasAllRequiredItems(Recipe var0, IsoGameCharacter var1, InventoryItem var2, ArrayList<ItemContainer> var3) {
      ArrayList var4 = getAvailableItemsNeeded(var0, var1, var3, var2, (ArrayList)null);
      return !var4.isEmpty();
   }

   public static boolean validateHasHeat(Recipe var0, InventoryItem var1, ArrayList<ItemContainer> var2, IsoGameCharacter var3) {
      if (var0.getHeat() == 0.0F) {
         return true;
      } else {
         InventoryItem var4 = null;
         Iterator var5 = getAvailableItemsNeeded(var0, var3, var2, var1, (ArrayList)null).iterator();

         while(var5.hasNext()) {
            InventoryItem var6 = (InventoryItem)var5.next();
            if (var6 instanceof DrainableComboItem) {
               var4 = var6;
               break;
            }
         }

         if (var4 != null) {
            var5 = var2.iterator();

            while(var5.hasNext()) {
               ItemContainer var9 = (ItemContainer)var5.next();
               Iterator var7 = var9.getItems().iterator();

               while(var7.hasNext()) {
                  InventoryItem var8 = (InventoryItem)var7.next();
                  if (var8.getName().equals(var4.getName())) {
                     if (var0.getHeat() < 0.0F) {
                        if (var8.getInvHeat() <= var0.getHeat()) {
                           return true;
                        }
                     } else if (var0.getHeat() > 0.0F && var8.getInvHeat() + 1.0F >= var0.getHeat()) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public static ArrayList<InventoryItem> getAvailableItemsAll(Recipe var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, InventoryItem var3, ArrayList<InventoryItem> var4) {
      RecipeMonitor.suspend();
      ItemRecipe var5 = ItemRecipe.Alloc(var0, var1, var2, var3, var4, true);
      ArrayList var6 = var5.getSourceItems();
      ItemRecipe.Release(var5);
      RecipeMonitor.resume();
      return var6;
   }

   public static ArrayList<InventoryItem> getAvailableItemsNeeded(Recipe var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, InventoryItem var3, ArrayList<InventoryItem> var4) {
      RecipeMonitor.suspend();
      ItemRecipe var5 = ItemRecipe.Alloc(var0, var1, var2, var3, var4, false);
      ArrayList var6 = var5.getSourceItems();
      ItemRecipe.Release(var5);
      RecipeMonitor.resume();
      return var6;
   }

   public static ArrayList<InventoryItem> getSourceItemsAll(Recipe var0, int var1, IsoGameCharacter var2, ArrayList<ItemContainer> var3, InventoryItem var4, ArrayList<InventoryItem> var5) {
      RecipeMonitor.suspend();
      ItemRecipe var6 = ItemRecipe.Alloc(var0, var2, var3, var4, var5, true);
      ArrayList var7 = var6.getSourceItems(var1);
      ItemRecipe.Release(var6);
      RecipeMonitor.resume();
      return var7;
   }

   public static ArrayList<InventoryItem> getSourceItemsNeeded(Recipe var0, int var1, IsoGameCharacter var2, ArrayList<ItemContainer> var3, InventoryItem var4, ArrayList<InventoryItem> var5) {
      RecipeMonitor.suspend();
      ItemRecipe var6 = ItemRecipe.Alloc(var0, var2, var3, var4, var5, false);
      ArrayList var7 = var6.getSourceItems(var1);
      ItemRecipe.Release(var6);
      RecipeMonitor.resume();
      return var7;
   }

   public static int getNumberOfTimesRecipeCanBeDone(Recipe var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, InventoryItem var3) {
      return ItemRecipe.getNumberOfTimesRecipeCanBeDone(var0, var1, var2, var3);
   }

   public static ArrayList<InventoryItem> PerformMakeItem(Recipe var0, InventoryItem var1, IsoGameCharacter var2, ArrayList<ItemContainer> var3) {
      RecipeMonitor.StartMonitor();
      RecipeMonitor.setRecipe(var0);
      ItemRecipe var4 = ItemRecipe.Alloc(var0, var2, var3, var1, (ArrayList)null, false);
      ArrayList var5 = var4.perform();
      ItemRecipe.Release(var4);
      return var5;
   }

   public static ArrayList<EvolvedRecipe> getAllEvolvedRecipes() {
      Stack var0 = ScriptManager.instance.getAllEvolvedRecipes();
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < var0.size(); ++var2) {
         var1.add((EvolvedRecipe)var0.get(var2));
      }

      return var1;
   }

   public static ArrayList<EvolvedRecipe> getEvolvedRecipe(InventoryItem var0, IsoGameCharacter var1, ArrayList<ItemContainer> var2, boolean var3) {
      ArrayList var4 = new ArrayList();
      if (var0 instanceof Food && ((Food)var0).isRotten() && var1.getPerkLevel(PerkFactory.Perks.Cooking) < 7) {
         return var4;
      } else {
         Stack var5 = ScriptManager.instance.getAllEvolvedRecipes();

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            EvolvedRecipe var7 = (EvolvedRecipe)var5.get(var6);
            if ((var0.isCooked() && var7.addIngredientIfCooked || !var0.isCooked()) && (var0.getType().equals(var7.baseItem) || var0.getType().equals(var7.getResultItem())) && (!var0.getType().equals("WaterPot") || !((double)var0.getCurrentUsesFloat() < 0.75))) {
               if (var3) {
                  ArrayList var8 = var7.getItemsCanBeUse(var1, var0, var2);
                  if (!var8.isEmpty()) {
                     if (var0 instanceof Food && ((Food)var0).isFrozen()) {
                        if (var7.isAllowFrozenItem()) {
                           var4.add(var7);
                        }
                     } else {
                        var4.add(var7);
                     }
                  }
               } else {
                  var4.add(var7);
               }
            }
         }

         return var4;
      }
   }

   public static Recipe getDismantleRecipeFor(String var0) {
      RecipeList.clear();
      ArrayList var1 = ScriptManager.instance.getAllRecipes();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         Recipe var3 = (Recipe)var1.get(var2);
         ArrayList var4 = var3.getSource();
         if (var4.size() > 0) {
            for(int var5 = 0; var5 < var4.size(); ++var5) {
               Recipe.Source var6 = (Recipe.Source)var4.get(var5);

               for(int var7 = 0; var7 < var6.getItems().size(); ++var7) {
                  if (((String)var6.getItems().get(var7)).equalsIgnoreCase(var0) && var3.name.toLowerCase().startsWith("dismantle ")) {
                     return var3;
                  }
               }
            }
         }
      }

      return null;
   }

   public static InventoryItem GetMovableRecipeTool(boolean var0, Recipe var1, InventoryItem var2, IsoGameCharacter var3, ArrayList<ItemContainer> var4) {
      if (!(var1 instanceof MovableRecipe var5)) {
         return null;
      } else {
         Recipe.Source var6 = var0 ? var5.getPrimaryTools() : var5.getSecondaryTools();
         if (var6 != null && var6.getItems() != null && var6.getItems().size() != 0) {
            RecipeMonitor.suspend();
            ItemRecipe var7 = ItemRecipe.Alloc(var1, var3, var4, var2, (ArrayList)null, false);
            if (var7.getSourceItems() != null && var7.getSourceItems().size() != 0) {
               ArrayList var8 = var7.getSourceItems();
               ItemRecipe.Release(var7);
               RecipeMonitor.resume();

               for(int var9 = 0; var9 < var8.size(); ++var9) {
                  InventoryItem var10 = (InventoryItem)var8.get(var9);

                  for(int var11 = 0; var11 < var6.getItems().size(); ++var11) {
                     if (var10.getFullType().equalsIgnoreCase((String)var6.getItems().get(var11))) {
                        return var10;
                     }
                  }
               }

               return null;
            } else {
               ItemRecipe.Release(var7);
               RecipeMonitor.resume();
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static boolean HasAllRequiredItems(Recipe var0, IsoGameCharacter var1, InventoryItem var2, ArrayList<ItemContainer> var3) {
      return validateHasAllRequiredItems(var0, var1, var2, var3);
   }

   public static boolean isAllItemsUsableRotten(Recipe var0, IsoGameCharacter var1, InventoryItem var2, ArrayList<ItemContainer> var3) {
      if (var1.getPerkLevel(PerkFactory.Perks.Cooking) >= 7) {
         return true;
      } else {
         ArrayList var4 = getAvailableItemsNeeded(var0, var1, var3, var2, (ArrayList)null);
         Iterator var5 = var4.iterator();

         InventoryItem var6;
         do {
            if (!var5.hasNext()) {
               return true;
            }

            var6 = (InventoryItem)var5.next();
         } while(!(var6 instanceof Food) || !((Food)var6).isRotten());

         return false;
      }
   }

   public static boolean hasHeat(Recipe var0, InventoryItem var1, ArrayList<ItemContainer> var2, IsoGameCharacter var3) {
      return validateHasHeat(var0, var1, var2, var3);
   }

   private static void DebugPrintAllRecipes() {
   }

   /** @deprecated */
   @Deprecated
   public static boolean IsItemDestroyed(String var0, Recipe var1) {
      DebugLog.Recipe.error("Method is deprecated.");
      return false;
   }

   /** @deprecated */
   @Deprecated
   public static float UseAmount(String var0, Recipe var1, IsoGameCharacter var2) {
      DebugLog.Recipe.error("Method is deprecated.");
      return 0.0F;
   }

   /** @deprecated */
   @Deprecated
   public static boolean DoesWipeUseDelta(String var0, String var1) {
      DebugLog.Recipe.error("Method is deprecated.");
      return true;
   }

   /** @deprecated */
   @Deprecated
   public static boolean DoesUseItemUp(String var0, Recipe var1) {
      DebugLog.Recipe.error("Method is deprecated.");
      return false;
   }
}
