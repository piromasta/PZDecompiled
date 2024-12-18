package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.MetaEntity;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.weather.ClimateManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;

public class CraftUtil {
   private static final ConcurrentLinkedDeque<ArrayList<Resource>> resource_list_pool = new ConcurrentLinkedDeque();

   public CraftUtil() {
   }

   public static ArrayList<Resource> AllocResourceList() {
      ArrayList var0 = (ArrayList)resource_list_pool.poll();
      if (var0 == null) {
         var0 = new ArrayList();
      }

      return var0;
   }

   public static void ReleaseResourceList(ArrayList<Resource> var0) {
      if (var0 != null) {
         var0.clear();

         assert !Core.bDebug || !resource_list_pool.contains(var0) : "Object already in pool.";

         resource_list_pool.offer(var0);
      }
   }

   public static boolean canItemsStack(InventoryItem var0, InventoryItem var1) {
      return canItemsStack(var0, var1, false);
   }

   public static boolean canItemsStack(InventoryItem var0, InventoryItem var1, boolean var2) {
      if (var0 != null && var1 != null) {
         if (var0 == var1) {
            return false;
         } else {
            return var0.getRegistry_id() == var1.getRegistry_id();
         }
      } else {
         return var2;
      }
   }

   public static boolean canItemsStack(Item var0, Item var1, boolean var2) {
      if (var0 != null && var1 != null) {
         if (var0 == var1) {
            return true;
         } else {
            return var0.getRegistry_id() == var1.getRegistry_id();
         }
      } else {
         return var2;
      }
   }

   public static Resource findResourceOrEmpty(ResourceIO var0, List<Resource> var1, InventoryItem var2, int var3, Resource var4, HashSet<Resource> var5) {
      return findResourceOrEmpty(var0, var1, var2.getScriptItem(), var3, var4, var5);
   }

   public static Resource findResourceOrEmpty(ResourceIO var0, List<Resource> var1, Item var2, int var3, Resource var4, HashSet<Resource> var5) {
      Resource var6 = null;
      if (var1 != null && var1.size() > 0) {
         for(int var8 = 0; var8 < var1.size(); ++var8) {
            Resource var7 = (Resource)var1.get(var8);
            if ((var4 == null || var4 != var7) && (var5 == null || !var5.contains(var7))) {
               if (var0 != ResourceIO.Any && var7.getIO() != ResourceIO.Output) {
                  if (Core.bDebug) {
                     DebugLog.General.warn("resource passed does not match selected IO type.");
                  }
               } else if (var7.getType() == ResourceType.Item && !var7.isFull() && (var3 <= 0 || var7.getFreeItemCapacity() >= var3)) {
                  if (var7.isEmpty() && var6 == null) {
                     var6 = var7;
                  }

                  if (var7.canStackItem(var2) && (var6 == null || var6.isEmpty() || var6.getFreeItemCapacity() >= var7.getFreeItemCapacity())) {
                     var6 = var7;
                  }
               }
            }
         }
      }

      return var6;
   }

   public static boolean canResourceFitItem(Resource var0, InventoryItem var1) {
      return canResourceFitItem(var0, (InventoryItem)var1, 1, (Resource)null, (HashSet)null);
   }

   public static boolean canResourceFitItem(Resource var0, InventoryItem var1, int var2) {
      return canResourceFitItem(var0, (InventoryItem)var1, var2, (Resource)null, (HashSet)null);
   }

   public static boolean canResourceFitItem(Resource var0, InventoryItem var1, int var2, Resource var3, HashSet<Resource> var4) {
      if (var0 != null && var0.getType() == ResourceType.Item && !var0.isFull()) {
         if (var3 != null && var3 == var0) {
            return false;
         } else if (var4 != null && var4.contains(var0)) {
            return false;
         } else {
            return var2 > 0 && var0.getFreeItemCapacity() < var2 ? false : var0.canStackItem(var1);
         }
      } else {
         return false;
      }
   }

   public static boolean canResourceFitItem(Resource var0, Item var1) {
      return canResourceFitItem(var0, (Item)var1, 1, (Resource)null, (HashSet)null);
   }

   public static boolean canResourceFitItem(Resource var0, Item var1, int var2) {
      return canResourceFitItem(var0, (Item)var1, var2, (Resource)null, (HashSet)null);
   }

   public static boolean canResourceFitItem(Resource var0, Item var1, int var2, Resource var3, HashSet<Resource> var4) {
      if (var0 != null && var0.getType() == ResourceType.Item && !var0.isFull()) {
         if (var3 != null && var3 == var0) {
            return false;
         } else if (var4 != null && var4.contains(var0)) {
            return false;
         } else {
            return var2 > 0 && var0.getFreeItemCapacity() < var2 ? false : var0.canStackItem(var1);
         }
      } else {
         return false;
      }
   }

   public static Resource findResourceOrEmpty(ResourceIO var0, List<Resource> var1, Fluid var2, float var3, Resource var4, HashSet<Resource> var5) {
      float var6 = 0.0F;
      Resource var7 = null;
      Resource var8 = null;
      if (var1 != null && var1.size() > 0) {
         for(int var10 = 0; var10 < var1.size(); ++var10) {
            Resource var9 = (Resource)var1.get(var10);
            if ((var4 == null || var4 != var9) && (var5 == null || !var5.contains(var9))) {
               if (var0 != ResourceIO.Any && var9.getIO() != ResourceIO.Output) {
                  if (Core.bDebug) {
                     DebugLog.General.warn("resource passed does not match selected IO type.");
                  }
               } else if (var9.getType() == ResourceType.Fluid && !var9.isFull() && (!(var3 > 0.0F) || !(var9.getFreeFluidCapacity() < var3))) {
                  if (var9.isEmpty() && var7 == null) {
                     var7 = var9;
                  }

                  ResourceFluid var11 = (ResourceFluid)var9;
                  if (var11.getFluidContainer().canAddFluid(var2)) {
                     if (var11.getFluidContainer().isPureFluid(var2)) {
                        if (var8 != null && var8.getFreeFluidCapacity() < var9.getFreeFluidCapacity()) {
                           continue;
                        }

                        var8 = var9;
                     }

                     if (var8 == null && var11.getFluidContainer().contains(var2)) {
                        float var12 = var11.getFluidContainer().getRatioForFluid(var2);
                        if (!(var12 < var6)) {
                           var6 = var12;
                           var7 = var9;
                        }
                     }
                  }
               }
            }
         }
      }

      return var8 != null ? var8 : var7;
   }

   public static Resource findResourceOrEmpty(ResourceIO var0, List<Resource> var1, Energy var2, float var3, Resource var4, HashSet<Resource> var5) {
      Resource var6 = null;
      if (var1 != null && var1.size() > 0) {
         for(int var8 = 0; var8 < var1.size(); ++var8) {
            Resource var7 = (Resource)var1.get(var8);
            if ((var4 == null || var4 != var7) && (var5 == null || !var5.contains(var7))) {
               if (var0 != ResourceIO.Any && var7.getIO() != ResourceIO.Output) {
                  if (Core.bDebug) {
                     DebugLog.General.warn("resource passed does not match selected IO type.");
                  }
               } else if (var7.getType() == ResourceType.Energy && !var7.isFull() && (!(var3 > 0.0F) || !(var7.getFreeEnergyCapacity() < var3))) {
                  ResourceEnergy var9 = (ResourceEnergy)var7;
                  if (var9.getEnergy() == var2) {
                     if (var7.isEmpty() && var6 == null) {
                        var6 = var7;
                     }

                     if (var6 == null || var6.isEmpty() || !(var6.getFreeEnergyCapacity() < var7.getFreeEnergyCapacity())) {
                        var6 = var7;
                     }
                  }
               }
            }
         }
      }

      return var6;
   }

   public static CraftRecipeMonitor debugCanStart(IsoPlayer var0, CraftRecipeData var1, List<CraftRecipe> var2, List<Resource> var3, List<Resource> var4, CraftRecipeMonitor var5) {
      try {
         var1.setMonitor(var5);
         canStart(var1, var2, var3, var4, var5);
         var1.setMonitor((CraftRecipeMonitor)null);
         CraftRecipeMonitor var6 = var5.seal();
         return var6;
      } catch (Exception var10) {
         var10.printStackTrace();
      } finally {
         var1.setMonitor((CraftRecipeMonitor)null);
      }

      return null;
   }

   public static boolean canStart(CraftRecipeData var0, List<CraftRecipe> var1, List<Resource> var2, List<Resource> var3) {
      return canStart(var0, var1, var2, var3, (CraftRecipeMonitor)null);
   }

   public static boolean canStart(CraftRecipeData var0, List<CraftRecipe> var1, List<Resource> var2, List<Resource> var3, CraftRecipeMonitor var4) {
      if (var4 != null) {
         var4.log("starting craftProcessor 'can start test'...");
      }

      CraftRecipe var5 = getPossibleRecipe(var0, var1, var2, var3, var4);
      if (var4 != null) {
         if (var5 != null) {
            var4.success("selected recipe: " + var5.getScriptObjectFullType());
            var4.setRecipe(var5);
            var4.logRecipe(var5, false);
         } else {
            var4.warn("no recipe can be performed for this craftProcessor");
         }
      }

      return var5 != null;
   }

   public static boolean canPerformRecipe(CraftRecipe var0, CraftRecipeData var1, List<Resource> var2, List<Resource> var3) {
      return canPerformRecipe(var0, var1, var2, var3, (CraftRecipeMonitor)null);
   }

   public static boolean canPerformRecipe(CraftRecipe var0, CraftRecipeData var1, List<Resource> var2, List<Resource> var3, CraftRecipeMonitor var4) {
      if (var0 != null && var1 != null && var2 != null) {
         var1.setRecipe(var0);
         if (!var1.canConsumeInputs(var2) || var3 != null && !var1.canCreateOutputs(var3)) {
            return false;
         } else {
            if (var4 != null) {
               var4.log("Input and Output passed, Calling LuaTest...");
            }

            boolean var5 = var1.luaCallOnTest();
            if (var4 != null) {
               if (var5) {
                  var4.success("LuaTest: OK");
               } else {
                  var4.warn("LuaTest: FAILED");
               }
            }

            return var5;
         }
      } else {
         return false;
      }
   }

   public static CraftRecipe getPossibleRecipe(CraftRecipeData var0, List<CraftRecipe> var1, List<Resource> var2, List<Resource> var3) {
      return getPossibleRecipe(var0, var1, var2, var3, (CraftRecipeMonitor)null);
   }

   public static CraftRecipe getPossibleRecipe(CraftRecipeData var0, List<CraftRecipe> var1, List<Resource> var2, List<Resource> var3, CraftRecipeMonitor var4) {
      if (var0 != null && var1 != null && var2 != null) {
         if (var4 != null) {
            var4.log("Get possible recipe...");
            var4.open();
         }

         for(int var5 = 0; var5 < var1.size(); ++var5) {
            CraftRecipe var6 = (CraftRecipe)var1.get(var5);
            if (var4 != null) {
               var4.log("[" + var5 + "] test recipe = " + var6.getScriptObjectFullType());
            }

            if (canPerformRecipe(var6, var0, var2, var3, var4)) {
               if (var4 != null) {
                  var4.close();
               }

               return var6;
            }
         }

         if (var4 != null) {
            var4.close();
         }

         return null;
      } else {
         return null;
      }
   }

   public static float getEntityTemperature(GameEntity var0) {
      float var1 = ClimateManager.getInstance().getTemperature();
      if (var0 instanceof MetaEntity) {
         return var1;
      } else {
         IsoGridSquare var2 = var0.getSquare();
         return var2 == null ? var1 : ClimateManager.getInstance().getAirTemperatureForSquare(var2);
      }
   }
}
