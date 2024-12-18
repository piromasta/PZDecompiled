package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.components.resources.Resource;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.Item;

public class CraftRecipeMonitor {
   private final ArrayList<String> lines = new ArrayList();
   private final ArrayList<String> _tempStrings = new ArrayList();
   private int openedBlocks = 0;
   private boolean sealed = false;
   private boolean printToConsole = false;
   private CraftRecipe recipe;

   public static CraftRecipeMonitor Create() {
      CraftRecipeMonitor var0 = new CraftRecipeMonitor();
      var0.log("[root]");
      var0.open();
      return var0;
   }

   private CraftRecipeMonitor() {
   }

   public void setPrintToConsole(boolean var1) {
      this.printToConsole = var1;
   }

   public void reset() {
      this.lines.clear();
      this.log("[root]");
      this.open();
   }

   public void setRecipe(CraftRecipe var1) {
      this.recipe = var1;
   }

   public CraftRecipe getRecipe() {
      return this.recipe;
   }

   public ArrayList<String> GetLines() {
      if (!this.sealed) {
         this.seal();
      }

      return this.lines;
   }

   public CraftRecipeMonitor seal() {
      if (!this.sealed) {
         this.close();
         if (this.openedBlocks > 0) {
            DebugLog.General.warn("seal called but '" + this.openedBlocks + "' open blocks remain, auto resolving...");

            while(this.openedBlocks > 0) {
               this.log("}");
               --this.openedBlocks;
            }
         }

         this.sealed = true;
      }

      return this;
   }

   public void open() {
      if (this.canLog()) {
         ++this.openedBlocks;
         this.log("{");
      }
   }

   public void close() {
      if (this.canLog()) {
         if (this.openedBlocks > 0) {
            --this.openedBlocks;
            this.log("}");
         } else {
            DebugLog.General.warn("close called but no more opened blocks");
         }

      }
   }

   public boolean canLog() {
      return Core.bDebug && !this.sealed;
   }

   public void warn(String var1) {
      if (this.canLog()) {
         this.lines.add("<WARNING> " + var1);
         if (this.printToConsole) {
            DebugLog.General.debugln(this.lines.get(this.lines.size() - 1));
         }

      }
   }

   public void success(String var1) {
      if (this.canLog()) {
         this.lines.add("<SUCCESS> " + var1);
         if (this.printToConsole) {
            DebugLog.General.debugln(this.lines.get(this.lines.size() - 1));
         }

      }
   }

   public void log(String var1) {
      if (this.canLog()) {
         this.lines.add(var1);
         if (this.printToConsole) {
            DebugLog.General.debugln(this.lines.get(this.lines.size() - 1));
         }

      }
   }

   public <T> void logList(String var1, ArrayList<T> var2) {
      if (this.canLog()) {
         this.log(var1);
         this.open();
         if (var2 != null && !var2.isEmpty()) {
            Iterator var3 = var2.iterator();

            while(var3.hasNext()) {
               Object var4 = var3.next();
               this.log(var4.toString());
            }
         }

         this.close();
      }
   }

   public void logCraftLogic(CraftLogic var1) {
      if (this.canLog()) {
         this.log("[CraftLogic]");
         this.open();
         this.log("isValid = " + var1.isValid());
         this.log("StartMode = " + var1.getStartMode());
         this.log("Query = " + var1.getRecipeTagQuery());
         this.log("InputGroup = " + var1.getInputsGroupName());
         this.log("OutputGroup = " + var1.getOutputsGroupName());
         this.log("[Recipes]");
         this.open();

         for(int var2 = 0; var2 < var1.getRecipes().size(); ++var2) {
            this.log("[" + var2 + "] " + ((CraftRecipe)var1.getRecipes().get(var2)).getScriptObjectFullType());
         }

         this.close();
         this.close();
      }
   }

   public void logFurnaceLogic(FurnaceLogic var1) {
      if (this.canLog()) {
         this.log("[FurnaceLogic]");
         this.open();
         this.log("isValid = " + var1.isValid());
         this.log("StartMode = " + var1.getStartMode());
         this.log("FuelQuery = " + var1.getFuelRecipeTagQuery());
         this.log("FurnaceQuery = " + var1.getFurnaceRecipeTagQuery());
         this.log("FuelInputGroup = " + var1.getFuelInputsGroupName());
         this.log("FuelOutputGroup = " + var1.getFuelOutputsGroupName());
         this.log("FurnaceInputGroup = " + var1.getFurnaceInputsGroupName());
         this.log("FurnaceOutputGroup = " + var1.getFurnaceOutputsGroupName());
         this.log("[FuelRecipes]");
         this.open();

         int var2;
         for(var2 = 0; var2 < var1.getFuelRecipes().size(); ++var2) {
            this.log("[" + var2 + "] " + ((CraftRecipe)var1.getFuelRecipes().get(var2)).getScriptObjectFullType());
         }

         this.close();
         this.log("[FurnaceRecipes]");
         this.open();

         for(var2 = 0; var2 < var1.getFurnaceRecipes().size(); ++var2) {
            this.log("[" + var2 + "] " + ((CraftRecipe)var1.getFurnaceRecipes().get(var2)).getScriptObjectFullType());
         }

         this.close();
         this.close();
      }
   }

   public void logDryingLogic(DryingLogic var1) {
      if (this.canLog()) {
         this.log("[CraftLogic]");
         this.open();
         this.log("isValid = " + var1.isValid());
         this.log("StartMode = " + var1.getStartMode());
         this.log("UsesFuel = " + var1.isUsesFuel());
         this.log("FuelQuery = " + var1.getFuelRecipeTagQuery());
         this.log("FurnaceQuery = " + var1.getDryingRecipeTagQuery());
         this.log("FuelInputGroup = " + var1.getFuelInputsGroupName());
         this.log("FuelOutputGroup = " + var1.getFuelOutputsGroupName());
         this.log("DryingInputGroup = " + var1.getDryingInputsGroupName());
         this.log("DryingOutputGroup = " + var1.getDryingOutputsGroupName());
         this.log("[FuelRecipes]");
         this.open();

         int var2;
         for(var2 = 0; var2 < var1.getFuelRecipes().size(); ++var2) {
            this.log("[" + var2 + "] " + ((CraftRecipe)var1.getFuelRecipes().get(var2)).getScriptObjectFullType());
         }

         this.close();
         this.log("[DryingRecipes]");
         this.open();

         for(var2 = 0; var2 < var1.getDryingRecipes().size(); ++var2) {
            this.log("[" + var2 + "] " + ((CraftRecipe)var1.getDryingRecipes().get(var2)).getScriptObjectFullType());
         }

         this.close();
         this.close();
      }
   }

   public void logMashingLogic(MashingLogic var1) {
      if (this.canLog()) {
         this.log("[MashingLogic]");
         this.open();
         this.log("isValid = " + var1.isValid());
         this.log("Query = " + var1.getRecipeTagQuery());
         this.log("InputGroup = " + var1.getInputsGroupName());
         this.log("[Recipes]");
         this.open();

         for(int var2 = 0; var2 < var1.getRecipes().size(); ++var2) {
            this.log("[" + var2 + "] " + ((CraftRecipe)var1.getRecipes().get(var2)).getScriptObjectFullType());
         }

         this.close();
         this.close();
      }
   }

   public void logResources(List<Resource> var1, List<Resource> var2) {
      if (this.canLog()) {
         this.log("[resources]");
         this.open();
         if (var1 != null && !var1.isEmpty()) {
            this.logResourcesList("[Inputs]", var1);
         }

         if (var2 != null && !var2.isEmpty()) {
            this.logResourcesList("[Outputs]", var2);
         }

         this.close();
      }
   }

   public void logResourcesList(String var1, List<Resource> var2) {
      if (this.canLog()) {
         this.log(var1);
         this.open();

         for(int var4 = 0; var4 < var2.size(); ++var4) {
            Resource var3 = (Resource)var2.get(var4);
            this.log("[" + var4 + "]");
            this.open();
            if (var3.getId() != null) {
               this.log("Id = \"" + var3.getId() + "\"");
            } else {
               this.log("Id = " + var3.getId());
            }

            this.log("Type = " + var3.getType());
            this.log("Io = " + var3.getIO());
            this.log("Channel = " + var3.getChannel());
            this.log("Flags = " + var3.getDebugFlagsString());
            this.close();
         }

         this.close();
      }
   }

   public void logRecipe(CraftRecipe var1, boolean var2) {
      if (this.canLog()) {
         this.log("[Recipe]");
         this.open();
         this.log("CraftRecipe = " + var1.getScriptObjectFullType());
         this.log("Enabled = " + var1.isEnabled());
         this.log("DebugOnly = " + var1.isDebugOnly());
         this.log("Time = (int) " + var1.getTime());
         CraftRecipe.LuaCall[] var3 = CraftRecipe.LuaCall.values();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            CraftRecipe.LuaCall var6 = var3[var5];
            String var10001 = var6.toString();
            this.log(var10001 + " = " + var1.getLuaCallString(var6));
         }

         if (var2) {
            this.log("[Inputs]");
            this.open();

            int var7;
            for(var7 = 0; var7 < var1.getInputs().size(); ++var7) {
               this.log("[" + var7 + "]");
               this.open();
               this.logInputScript((InputScript)var1.getInputs().get(var7));
               this.close();
            }

            this.close();
            this.log("[Outputs]");
            this.open();

            for(var7 = 0; var7 < var1.getOutputs().size(); ++var7) {
               this.log("[" + var7 + "]");
               this.open();
               this.logOutputScript((OutputScript)var1.getOutputs().get(var7));
               this.close();
            }

            this.close();
         }

         this.close();
      }
   }

   public void logInputScript(InputScript var1) {
      if (this.canLog()) {
         this.log("Line = \"" + var1.getOriginalLine() + "\"");
         this.log("Type = " + var1.getResourceType());
         this.log("Amount = (float)" + var1.getAmount());
         this.log("AmountInt = (int) " + var1.getIntAmount());
         this.log("ShapedIndex = (int) " + var1.getShapedIndex());
         this.log("isKeep = " + var1.isKeep());
         this.log("isDestroy = " + var1.isDestroy());
         this.log("ItemApplyMode = " + var1.getItemApplyMode());
         this.log("FluidMatchMode = " + var1.getFluidMatchMode());
         if (var1.isReplace()) {
            this.open();
            this.log("[Replace->Output]");
            this.logOutputScript(var1.getReplaceOutputScript());
            this.close();
         }

      }
   }

   public void logOutputScript(OutputScript var1) {
      if (this.canLog()) {
         this.log("Line = \"" + var1.getOriginalLine() + "\"");
         this.log("Type = " + var1.getResourceType());
         this.log("Amount = (float) " + var1.getAmount());
         this.log("AmountInt = (int) " + var1.getIntAmount());
         this.log("Chance = (float) " + var1.getChance());
         switch (var1.getResourceType()) {
            case Item:
               if (!var1.getPossibleResultItems().isEmpty()) {
                  if (var1.getPossibleResultItems().size() > 1) {
                     this.log("Item = " + var1.getPossibleResultItems().size() + " possibilities.");

                     for(int var2 = 0; var2 < var1.getPossibleResultItems().size(); ++var2) {
                        Item var10001 = (Item)var1.getPossibleResultItems().get(var2);
                        this.log("   - " + var10001.getScriptObjectFullType());
                     }

                     return;
                  } else {
                     ArrayList var3 = var1.getPossibleResultItems();
                     this.log("Item = " + ((Item)var3.get(0)).getScriptObjectFullType());
                  }
               } else {
                  this.warn("Item = null");
               }
               break;
            case Fluid:
               if (var1.getFluid() != null) {
                  this.log("Fluid = " + var1.getFluid().getTranslatedName());
               } else {
                  this.warn("Fluid = null");
               }
               break;
            case Energy:
               if (var1.getEnergy() != null) {
                  this.log("Energy = " + var1.getEnergy().getDisplayName());
               } else {
                  this.warn("Energy = null");
               }
         }

      }
   }
}
