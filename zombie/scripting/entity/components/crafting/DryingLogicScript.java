package zombie.scripting.entity.components.crafting;

import java.util.Iterator;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

public class DryingLogicScript extends ComponentScript {
   private String dryingRecipeTagQuery = null;
   private String fuelRecipeTagQuery = null;
   private StartMode startMode;
   private String inputsGroupName;
   private String outputsGroupName;
   private String fuelInputsGroupName;
   private String fuelOutputsGroupName;

   private DryingLogicScript() {
      super(ComponentType.DryingLogic);
      this.startMode = StartMode.Manual;
   }

   public String getDryingRecipeTagQuery() {
      return this.dryingRecipeTagQuery;
   }

   public String getFuelRecipeTagQuery() {
      return this.fuelRecipeTagQuery;
   }

   public StartMode getStartMode() {
      return this.startMode;
   }

   public String getInputsGroupName() {
      return this.inputsGroupName;
   }

   public String getOutputsGroupName() {
      return this.outputsGroupName;
   }

   public String getFuelInputsGroupName() {
      return this.fuelInputsGroupName;
   }

   public String getFuelOutputsGroupName() {
      return this.fuelOutputsGroupName;
   }

   public boolean isUsesFuel() {
      return this.fuelRecipeTagQuery != null;
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
      this.dryingRecipeTagQuery = null;
      this.fuelRecipeTagQuery = null;
      this.startMode = StartMode.Manual;
      this.inputsGroupName = null;
      this.outputsGroupName = null;
      this.fuelInputsGroupName = null;
      this.fuelOutputsGroupName = null;
   }

   public void reset() {
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      if (!StringUtils.isNullOrWhitespace(this.dryingRecipeTagQuery)) {
         this.dryingRecipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.dryingRecipeTagQuery);
         if ((!Core.bDebug || !StringUtils.isNullOrWhitespace(this.inputsGroupName)) && StringUtils.isNullOrWhitespace(this.outputsGroupName)) {
         }

         if (!StringUtils.isNullOrWhitespace(this.fuelRecipeTagQuery)) {
            this.fuelRecipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.fuelRecipeTagQuery);
            if ((!Core.bDebug || !StringUtils.isNullOrWhitespace(this.fuelInputsGroupName)) && StringUtils.isNullOrWhitespace(this.fuelOutputsGroupName)) {
            }

         }
      }
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   protected void load(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            if (var4.equalsIgnoreCase("dryingRecipes")) {
               this.dryingRecipeTagQuery = var5;
            }

            if (var4.equalsIgnoreCase("fuelRecipes")) {
               this.fuelRecipeTagQuery = var5;
            } else if (var4.equalsIgnoreCase("startMode")) {
               this.startMode = StartMode.valueOf(var5);
            } else if (var4.equalsIgnoreCase("inputGroup")) {
               this.inputsGroupName = var5;
            } else if (var4.equalsIgnoreCase("outputGroup")) {
               this.outputsGroupName = var5;
            } else if (var4.equalsIgnoreCase("fuelInputGroup")) {
               this.fuelInputsGroupName = var5;
            } else if (var4.equalsIgnoreCase("fuelOutputGroup")) {
               this.fuelOutputsGroupName = var5;
            }
         }
      }

      var2 = var1.children.iterator();

      while(var2.hasNext()) {
         ScriptParser.Block var6 = (ScriptParser.Block)var2.next();
         String var10001 = var6.type;
         DebugLog.General.error("Unknown block '" + var10001 + "' in entity script: " + this.getName());
      }

   }
}
