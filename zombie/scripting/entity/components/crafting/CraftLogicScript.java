package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
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

public class CraftLogicScript extends ComponentScript {
   private String recipeTagQuery = null;
   private StartMode startMode;
   private String inputsGroupName;
   private String outputsGroupName;

   private CraftLogicScript() {
      super(ComponentType.CraftLogic);
      this.startMode = StartMode.Manual;
   }

   public String getRecipeTagQuery() {
      return this.recipeTagQuery;
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

   /** @deprecated */
   @Deprecated
   public ArrayList<Object> getCraftProcessorScripts() {
      return new ArrayList();
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
      this.recipeTagQuery = null;
      this.startMode = StartMode.Manual;
      this.inputsGroupName = null;
      this.outputsGroupName = null;
   }

   public void reset() {
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      if (!StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
         this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
         if ((!Core.bDebug || !StringUtils.isNullOrWhitespace(this.inputsGroupName)) && StringUtils.isNullOrWhitespace(this.outputsGroupName)) {
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
            if (var4.equalsIgnoreCase("recipes")) {
               this.recipeTagQuery = var5;
            } else if (var4.equalsIgnoreCase("startMode")) {
               this.startMode = StartMode.valueOf(var5);
            } else if (var4.equalsIgnoreCase("inputGroup")) {
               this.inputsGroupName = var5;
            } else if (var4.equalsIgnoreCase("outputGroup")) {
               this.outputsGroupName = var5;
            }
         }
      }

      var2 = var1.children.iterator();

      while(var2.hasNext()) {
         ScriptParser.Block var6 = (ScriptParser.Block)var2.next();
         if (var6.type.equalsIgnoreCase("craftProcessor")) {
            DebugLog.General.warn("Block craft processor is deprecated.");
         } else {
            String var10001 = var6.type;
            DebugLog.General.error("Unknown block '" + var10001 + "' in entity script: " + this.getName());
         }
      }

   }
}
