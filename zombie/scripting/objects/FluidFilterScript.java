package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.Core;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidFilter;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.world.StringDictionary;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
public class FluidFilterScript extends BaseScriptObject {
   private final ArrayList<String> fluids = new ArrayList();
   private final ArrayList<String> categories = new ArrayList();
   private boolean isWhitelist = true;
   private FluidFilter filter = null;
   private String name;
   private final boolean anonymous;

   public FluidFilterScript() {
      super(ScriptType.FluidFilter);
      this.anonymous = false;
   }

   private FluidFilterScript(boolean var1) {
      super(ScriptType.FluidFilter);
      this.anonymous = var1;
   }

   public static FluidFilterScript GetAnonymous() {
      return new FluidFilterScript(true);
   }

   public static FluidFilterScript GetAnonymous(boolean var0) {
      FluidFilterScript var1 = new FluidFilterScript(true);
      var1.isWhitelist = var0;
      return var1;
   }

   public FluidFilterScript copy() {
      FluidFilterScript var1 = new FluidFilterScript(this.anonymous);
      var1.fluids.addAll(this.fluids);
      var1.categories.addAll(this.categories);
      var1.isWhitelist = this.isWhitelist;
      var1.name = this.name;
      return var1;
   }

   public boolean isSingleFluid() {
      return this.fluids.size() == 1 && this.categories.size() == 0;
   }

   private void addFluid(String var1) {
      if (!this.fluids.contains(var1)) {
         this.fluids.add(var1);
      }

   }

   private void addCategory(String var1) {
      if (!this.categories.contains(var1)) {
         this.categories.add(var1);
      }

   }

   public FluidFilter getFilter() {
      return this.filter;
   }

   public FluidFilter createFilter() throws Exception {
      if (!Fluid.FluidsInitialized() && Core.bDebug) {
         throw new RuntimeException("Fluids not yet initialized.");
      } else {
         FluidFilter.FilterType var1 = this.isWhitelist ? FluidFilter.FilterType.Whitelist : FluidFilter.FilterType.Blacklist;
         this.filter = new FluidFilter();
         this.filter.setFilterType(var1);
         Iterator var2 = this.categories.iterator();

         String var3;
         while(var2.hasNext()) {
            var3 = (String)var2.next();
            FluidCategory var4 = FluidCategory.valueOf(var3);
            this.filter.add(var4);
         }

         var2 = this.fluids.iterator();

         while(var2.hasNext()) {
            var3 = (String)var2.next();
            Fluid var5 = Fluid.Get(var3);
            if (var5 == null) {
               throw new Exception("Cannot add fluid '" + var3 + "' in filter script.");
            }

            this.filter.add(var5);
         }

         return this.filter;
      }
   }

   public void getVersion(IVersionHash var1) {
      if (this.name != null) {
         var1.add(this.name);
      }

   }

   public void PreReload() {
      this.fluids.clear();
      this.categories.clear();
      this.filter = null;
      this.isWhitelist = true;
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      if (var1 == ScriptLoadMode.Init && !this.anonymous) {
         StringDictionary.Generic.register(this.getScriptObjectFullType());
      }

   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      this.createFilter();
   }

   public void Load(String var1, String var2) throws Exception {
      if (this.anonymous) {
         throw new Exception("Cannot load, is anonymous");
      } else {
         ScriptParser.Block var3 = ScriptParser.parse(var2);
         var3 = (ScriptParser.Block)var3.children.get(0);
         this.name = var1;
         super.LoadCommonBlock(var3);
         this.readBlock(var3);
      }
   }

   public void LoadAnonymousFromBlock(ScriptParser.Block var1) throws Exception {
      if (!this.anonymous) {
         throw new Exception("Cannot load, is not anonymous");
      } else {
         this.name = "anonymous";
         this.readBlock(var1);
      }
   }

   public void LoadAnonymousSingleFluid(String var1) throws Exception {
      if (!this.anonymous) {
         throw new Exception("Cannot load, is not anonymous");
      } else {
         this.name = "anonymous";
         this.fluids.add(var1);
      }
   }

   private void readBlock(ScriptParser.Block var1) {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            if (var4.equalsIgnoreCase("fluid")) {
               this.parseInputString(this.fluids, var5);
            } else if (var4.equalsIgnoreCase("category")) {
               this.parseInputString(this.categories, var5);
            } else if (var4.equalsIgnoreCase("filterType")) {
               this.isWhitelist = var5.equalsIgnoreCase("whitelist");
            } else if (var4.equalsIgnoreCase("whitelist")) {
               this.isWhitelist = Boolean.parseBoolean(var5);
            } else if (var4.equalsIgnoreCase("blacklist")) {
               this.isWhitelist = !Boolean.parseBoolean(var5);
            }
         }
      }

      var2 = var1.children.iterator();

      while(var2.hasNext()) {
         ScriptParser.Block var6 = (ScriptParser.Block)var2.next();
         if ("fluids".equalsIgnoreCase(var6.type)) {
            this.readFilterBlock(var6, this.fluids);
         } else if ("categories".equalsIgnoreCase(var6.type)) {
            this.readFilterBlock(var6, this.categories);
         }
      }

   }

   private void readFilterBlock(ScriptParser.Block var1, ArrayList<String> var2) {
      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         if (var4.string != null && !var4.string.trim().isEmpty()) {
            String var5 = var4.string.trim();
            if (!var5.contains("=")) {
               this.parseInputString(var2, var5);
            }
         }
      }

   }

   private void parseInputString(ArrayList<String> var1, String var2) {
      String[] var3 = var2.split("/");
      String[] var4 = var3;
      int var5 = var3.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String var7 = var4[var6];
         var7 = var7.trim();
         if (!var1.contains(var7)) {
            var1.add(var7);
         }
      }

   }
}
