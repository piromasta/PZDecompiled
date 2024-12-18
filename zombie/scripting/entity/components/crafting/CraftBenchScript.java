package zombie.scripting.entity.components.crafting;

import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

public class CraftBenchScript extends ComponentScript {
   private final EnumBitStore<ResourceChannel> fluidInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
   private final EnumBitStore<ResourceChannel> energyInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
   private String recipeTagQuery = null;

   private CraftBenchScript() {
      super(ComponentType.CraftBench);
   }

   public String getRecipeTagQuery() {
      return this.recipeTagQuery;
   }

   public EnumBitStore<ResourceChannel> getFluidInputChannels() {
      return this.fluidInputChannels;
   }

   public EnumBitStore<ResourceChannel> getEnergyInputChannels() {
      return this.energyInputChannels;
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
   }

   public void reset() {
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      if (StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
         throw new Exception("Recipe tag query null or whitespace.");
      } else {
         this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
      }
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   protected void load(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(true) {
         while(true) {
            String var4;
            String var5;
            do {
               do {
                  if (!var2.hasNext()) {
                     var2 = var1.children.iterator();

                     while(var2.hasNext()) {
                        ScriptParser.Block var9 = (ScriptParser.Block)var2.next();
                        if (!var9.type.equalsIgnoreCase("craftProcessor")) {
                           String var10001 = var9.type;
                           DebugLog.General.error("Unknown block '" + var10001 + "' in entity script: " + this.getName());
                        }
                     }

                     return;
                  }

                  ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
                  var4 = var3.getKey().trim();
                  var5 = var3.getValue().trim();
               } while(var4.isEmpty());
            } while(var5.isEmpty());

            if (var4.equalsIgnoreCase("recipes")) {
               this.recipeTagQuery = var5;
            } else {
               String[] var6;
               int var7;
               ResourceChannel var8;
               if (var4.equalsIgnoreCase("fluidInputChannels")) {
                  var6 = var5.split(";");

                  for(var7 = 0; var7 < var6.length; ++var7) {
                     var8 = ResourceChannel.valueOf(var6[var7]);
                     this.fluidInputChannels.add(var8);
                  }
               } else if (var4.equalsIgnoreCase("energyInputChannels")) {
                  var6 = var5.split(";");

                  for(var7 = 0; var7 < var6.length; ++var7) {
                     var8 = ResourceChannel.valueOf(var6[var7]);
                     this.energyInputChannels.add(var8);
                  }
               }
            }
         }
      }
   }
}
