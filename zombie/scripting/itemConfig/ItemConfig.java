package zombie.scripting.itemConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeUtil;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.ItemConfigurator;
import zombie.inventory.ItemPickInfo;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.generators.GeneratorBoolAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumSetAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumStringSetAttribute;
import zombie.scripting.itemConfig.generators.GeneratorFluidContainer;
import zombie.scripting.itemConfig.generators.GeneratorLuaFunc;
import zombie.scripting.itemConfig.generators.GeneratorNumericAttribute;
import zombie.scripting.itemConfig.generators.GeneratorStringAttribute;
import zombie.scripting.itemConfig.script.BucketRootScript;
import zombie.scripting.itemConfig.script.SelectorBucketScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class ItemConfig extends BaseScriptObject {
   public static String error_line = null;
   public static String error_bucket = null;
   public static String error_root = null;
   public static String error_item_config = null;
   public static final String VARIABLE_PREFIX = "$";
   private final ArrayList<String> includes = new ArrayList();
   private final HashMap<String, String> variables = new HashMap();
   private final HashMap<String, BucketRootScript> rootScripts = new HashMap();
   private final ArrayList<BucketRoot> roots = new ArrayList();
   private String name;
   private boolean hasBeenParsed = false;
   private boolean isValid = true;
   private static ArrayList<RandomGenerator> tempGenerators = new ArrayList();

   private static String createErrorString() {
      String var0 = "[";
      var0 = var0 + "itemConfig=" + (error_item_config != null ? error_item_config : "unknown") + ", ";
      if (error_bucket != null) {
         var0 = var0 + "bucket=" + error_bucket + ", ";
      }

      if (error_root != null) {
         var0 = var0 + "attribute=" + error_root + ", ";
      }

      var0 = var0 + "line=\"" + (error_line != null ? error_line : "null") + "\"]";
      var0 = var0 + "]";
      return var0;
   }

   private static void WarnOrError(String var0) throws ItemConfigException {
      if (Core.bDebug) {
         throw new ItemConfigException(var0);
      } else {
         DebugLog.log("RecipeAttributes -> " + var0 + " \n" + createErrorString());
      }
   }

   public ItemConfig() {
      super(ScriptType.ItemConfig);
   }

   public String getName() {
      return this.name;
   }

   public boolean isValid() {
      return this.isValid;
   }

   public void ConfigureEntitySpawned(GameEntity var1, ItemPickInfo var2) {
      if (this.roots.size() != 0 && this.isValid) {
         for(int var4 = 0; var4 < this.roots.size(); ++var4) {
            BucketRoot var3 = (BucketRoot)this.roots.get(var4);
            if (var3.getBucketSpawn() != null) {
               var3.getBucketSpawn().Resolve(var1, var2);
            }
         }

      }
   }

   public void ConfigureEntityOnCreate(GameEntity var1) {
      if (this.roots.size() != 0 && this.isValid) {
         for(int var3 = 0; var3 < this.roots.size(); ++var3) {
            BucketRoot var2 = (BucketRoot)this.roots.get(var3);
            if (var2.getBucketOnCreate() != null) {
               var2.getBucketOnCreate().ResolveOnCreate(var1);
            }
         }

      }
   }

   public void Load(String var1, String var2) throws ItemConfigException {
      this.name = var1;
      error_line = null;
      error_bucket = null;
      error_root = null;
      error_item_config = this.name;

      try {
         ScriptParser.Block var3 = ScriptParser.parse(var2);
         var3 = (ScriptParser.Block)var3.children.get(0);
         super.LoadCommonBlock(var3);
         Iterator var4 = var3.elements.iterator();

         label72:
         while(true) {
            while(true) {
               if (!var4.hasNext()) {
                  break label72;
               }

               ScriptParser.BlockElement var5 = (ScriptParser.BlockElement)var4.next();
               String var8;
               String var9;
               if (var5.asValue() != null) {
                  String var11 = var5.asValue().string;
                  error_line = var11;
                  if (!StringUtils.isNullOrWhitespace(var11)) {
                     if (var11.contains("=")) {
                        String[] var13 = var11.split("=");
                        var8 = var13[0].trim();
                        var9 = var13[1].trim();
                        if (var8.equalsIgnoreCase("include")) {
                           this.includes.add(var9);
                        }
                     }

                     error_line = null;
                  }
               } else {
                  ScriptParser.Block var6 = var5.asBlock();
                  Iterator var12;
                  ScriptParser.Value var14;
                  if ("includes".equalsIgnoreCase(var6.type)) {
                     var12 = var6.values.iterator();

                     while(var12.hasNext()) {
                        var14 = (ScriptParser.Value)var12.next();
                        var9 = var14.string;
                        if (!StringUtils.isNullOrWhitespace(var9)) {
                           this.includes.add(var9.trim());
                        }
                     }
                  } else if ("variables".equalsIgnoreCase(var6.type)) {
                     var12 = var6.values.iterator();

                     while(var12.hasNext()) {
                        var14 = (ScriptParser.Value)var12.next();
                        var9 = var14.string;
                        if (!StringUtils.isNullOrWhitespace(var9)) {
                           this.variables.put(var14.getKey(), var14.getValue());
                        }
                     }
                  } else {
                     BucketRootScript var7 = BucketRootScript.TryLoad(var6);
                     if (var7 != null) {
                        var8 = var7.getType().toString();
                        if (var7.getId() != null) {
                           var8 = var8 + ":" + var7.getId();
                        }

                        this.rootScripts.put(var8, var7);
                     }
                  }
               }
            }
         }
      } catch (Exception var10) {
         if (!(var10 instanceof ItemConfigException)) {
            throw new ItemConfigException(var10.getMessage(), var10);
         }

         throw new ItemConfigException(var10.getMessage(), var10, false);
      }

      error_line = null;
      error_bucket = null;
      error_root = null;
      error_item_config = null;
   }

   public void PreReload() {
      this.hasBeenParsed = false;
      this.includes.clear();
      this.variables.clear();
      this.rootScripts.clear();
      this.roots.clear();
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      error_item_config = this.name;

      try {
         this.Parse((HashSet)null);
      } catch (Exception var3) {
         ExceptionLogger.logException(var3);
         throw new Exception(var3);
      }

      error_item_config = null;
   }

   private void Parse(HashSet<String> var1) throws ItemConfigException {
      if (var1 != null) {
         if (var1.contains(this.name)) {
            throw new ItemConfigException("Circular includes detected.");
         }

         var1.add(this.name);
      }

      if (!this.hasBeenParsed) {
         HashMap var2 = null;
         HashMap var3 = new HashMap();
         Iterator var4;
         if (this.includes != null) {
            var4 = this.includes.iterator();

            while(var4.hasNext()) {
               String var5 = (String)var4.next();
               HashSet var6 = var1 != null ? var1 : new HashSet();
               var6.add(this.name);
               ItemConfig var7 = ScriptManager.instance.getItemConfig(var5);
               if (!var7.hasBeenParsed) {
                  var7.Parse(var6);
               }

               var2 = MergeRoots(var2, var7.rootScripts, true);
               Iterator var8 = var7.variables.entrySet().iterator();

               while(var8.hasNext()) {
                  Map.Entry var9 = (Map.Entry)var8.next();
                  var3.put((String)var9.getKey(), (String)var9.getValue());
               }
            }
         }

         if (var2 != null) {
            MergeRoots(var2, this.rootScripts, true);
         }

         var4 = var3.entrySet().iterator();

         while(var4.hasNext()) {
            Map.Entry var10 = (Map.Entry)var4.next();
            if (!this.variables.containsKey(var10.getKey())) {
               var3.put((String)var10.getKey(), (String)var10.getValue());
            }
         }

         this.hasBeenParsed = true;
      }
   }

   private static HashMap<String, BucketRootScript> MergeRoots(HashMap<String, BucketRootScript> var0, HashMap<String, BucketRootScript> var1, boolean var2) {
      HashMap var3 = new HashMap();
      if (var0 == null && var1 == null) {
         return var3;
      } else {
         if (var0 != null && var1 != null) {
            Iterator var7 = var0.entrySet().iterator();

            Map.Entry var8;
            while(var7.hasNext()) {
               var8 = (Map.Entry)var7.next();
               if (!var1.containsKey(var8.getKey())) {
                  var3.put((String)var8.getKey(), var2 ? ((BucketRootScript)var8.getValue()).copy() : (BucketRootScript)var8.getValue());
               }
            }

            var7 = var1.entrySet().iterator();

            while(var7.hasNext()) {
               var8 = (Map.Entry)var7.next();
               var3.put((String)var8.getKey(), var2 ? ((BucketRootScript)var8.getValue()).copy() : (BucketRootScript)var8.getValue());
            }
         } else {
            HashMap var4 = var0 != null ? var0 : var1;
            Iterator var5 = var4.entrySet().iterator();

            while(var5.hasNext()) {
               Map.Entry var6 = (Map.Entry)var5.next();
               var3.put((String)var6.getKey(), var2 ? ((BucketRootScript)var6.getValue()).copy() : (BucketRootScript)var6.getValue());
            }
         }

         return var3;
      }
   }

   public void BuildBuckets() {
      error_item_config = this.name;

      try {
         this.roots.clear();
         Iterator var1 = this.rootScripts.entrySet().iterator();

         while(var1.hasNext()) {
            Map.Entry var2 = (Map.Entry)var1.next();
            BucketRoot var3 = this.buildBucketRoot((BucketRootScript)var2.getValue());
            this.roots.add(var3);
         }
      } catch (Exception var4) {
         DebugLog.log(var4.getMessage());
         var4.printStackTrace();
         this.isValid = false;
      }

      VariableBuilder.clear();
      error_item_config = null;
   }

   private BucketRoot buildBucketRoot(BucketRootScript var1) throws ItemConfigException {
      VariableBuilder.setKeys(this.variables);
      if (var1.idIsVariable()) {
         String var2 = VariableBuilder.Build(var1.getId());
         var1.setId(var2);
      }

      BucketRoot var5 = new BucketRoot(var1.getType(), var1.getId());
      SelectorBucket var3 = this.buildBucket(var1, var1.getDefaultBucket());
      var5.setBucketSpawn(var3);
      if (var1.getOnCreateBucket() != null) {
         SelectorBucket var4 = this.buildBucket(var1, var1.getOnCreateBucket());
         var5.setBucketOnCreate(var4);
      }

      return var5;
   }

   private SelectorBucket buildBucket(BucketRootScript var1, SelectorBucketScript var2) throws ItemConfigException {
      SelectorBucket[] var3 = null;
      if (var2.getChildren().size() > 0) {
         var3 = new SelectorBucket[var2.getChildren().size()];

         for(int var4 = 0; var4 < var2.getChildren().size(); ++var4) {
            var3[var4] = this.buildBucket(var1, (SelectorBucketScript)var2.getChildren().get(var4));
         }
      }

      int[] var11 = null;
      if (var2.getSelectorString() != null) {
         String var5 = var2.getSelectorString();
         if (var5.contains("$")) {
            var5 = VariableBuilder.Build(var5);
         }

         String[] var6 = null;
         int var7;
         if (var2.getSelectorType().isAllowChaining() && var5.contains("/")) {
            var6 = var5.split("/");

            for(var7 = 0; var7 < var6.length; ++var7) {
               var6[var7] = var6[var7].trim();
            }
         } else {
            var6 = new String[]{var5};
         }

         var11 = new int[var6.length];

         for(var7 = 0; var7 < var6.length; ++var7) {
            if (var2.getSelectorType() == SelectorType.Tile) {
               var11[var7] = ItemConfigurator.GetIdForSprite(var6[var7]);
            } else {
               var11[var7] = ItemConfigurator.GetIdForString(var6[var7]);
            }

            if (var11[var7] == -1) {
               throw new ItemConfigException("Could not find selectorID for: " + var6[var7] + ", in: " + var2.getSelectorString());
            }
         }
      }

      Randomizer var12 = null;
      if (var2.getRandomizers().size() > 0) {
         tempGenerators.clear();
         Iterator var13 = var2.getRandomizers().iterator();

         while(var13.hasNext()) {
            String var15 = (String)var13.next();
            String var8 = var15;
            if (!StringUtils.isNullOrWhitespace(var15)) {
               if (var15.contains("$")) {
                  var8 = VariableBuilder.Build(var15);
               }

               if (!StringUtils.isNullOrWhitespace(var8)) {
                  error_line = var8;
                  RandomGenerator var9 = null;
                  switch (var1.getType()) {
                     case Attribute:
                        AttributeType var10 = Attribute.TypeFromName(var1.getId());
                        if (var10 == null) {
                           String var10002 = this.name;
                           throw new ItemConfigException("Invalid attribute! [itemConfig=" + var10002 + ", attribute=" + (var10 != null ? var10 : "null") + ", attributeString = " + (var1.getId() != null ? var1.getId() : "null") + "]");
                        }

                        if (AttributeValueType.IsNumeric(var10.getValueType())) {
                           var9 = this.buildNumericGenerator(var10, var8);
                        } else if (var10.getValueType() == AttributeValueType.Boolean) {
                           var9 = this.buildBoolGenerator(var10, var8);
                        } else if (var10.getValueType() == AttributeValueType.String) {
                           var9 = this.buildStringGenerator(var10, var8);
                        } else if (var10.getValueType() == AttributeValueType.Enum) {
                           var9 = this.buildEnumGenerator(var10, var8);
                        } else if (var10.getValueType() == AttributeValueType.EnumSet) {
                           var9 = this.buildEnumSetGenerator(var10, var8);
                        } else if (var10.getValueType() == AttributeValueType.EnumStringSet) {
                           var9 = this.buildEnumStringSetGenerator(var10, var8);
                        }
                        break;
                     case FluidContainer:
                        var9 = this.buildFluidContainerGenerator(var1.getId(), var8);
                        break;
                     case LuaFunc:
                        var9 = this.buildLuaFuncGenerator(var8);
                  }

                  if (var9 != null) {
                     tempGenerators.add(var9);
                  }

                  error_line = null;
               }
            }
         }

         var12 = new Randomizer((RandomGenerator[])PZArrayUtil.toArray(tempGenerators));
      }

      SelectorBucket var14 = new SelectorBucket(var11, var2, var3, var12);
      return var14;
   }

   private RandomGenerator buildLuaFuncGenerator(String var1) throws ItemConfigException {
      String[] var2 = var1.split("\\s+");
      float var3 = 1.0F;
      String var4 = null;
      String[] var5 = var2;
      int var6 = var2.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         String var8 = var5[var7];
         String[] var9 = var8.split("=");
         String var10 = var9[0];
         if (var10.equalsIgnoreCase("chance")) {
            var3 = Float.parseFloat(var9[1]);
         } else if (var10.equalsIgnoreCase("func")) {
            var4 = var9[1];
         }
      }

      if (var4 == null) {
         throw new ItemConfigException("At least parameter 'func' has to be defined.");
      } else {
         return new GeneratorLuaFunc(var4, var3);
      }
   }

   private RandomGenerator buildFluidContainerGenerator(String var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      float var5 = 0.0F;
      float var6 = 0.0F;
      boolean var7 = false;
      ArrayList var8 = new ArrayList();
      ArrayList var9 = new ArrayList();
      float var10 = 0.0F;
      boolean var11 = false;
      String[] var12 = var3;
      int var13 = var3.length;

      for(int var14 = 0; var14 < var13; ++var14) {
         String var15 = var12[var14];
         String[] var16 = var15.split("=");
         String var17 = var16[0];
         if (var17.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var16[1]);
         } else if (var17.equalsIgnoreCase("min")) {
            var5 = Float.parseFloat(var16[1]);
         } else if (var17.equalsIgnoreCase("max")) {
            var7 = true;
            var6 = Float.parseFloat(var16[1]);
         } else if (var17.equalsIgnoreCase("value")) {
            var11 = true;
            var10 = Float.parseFloat(var16[1]);
         } else if (var17.equalsIgnoreCase("fluid")) {
            String[] var18 = var16[1].split(":");
            Fluid var19 = Fluid.Get(var18[0]);
            float var20 = Float.parseFloat(var18[1]);
            if (var19 == null) {
               throw new ItemConfigException("Could not find fluid: '" + var18[0] + "'.");
            }

            var8.add(var19);
            var9.add(var20);
         }
      }

      if (!var7 && !var11) {
         throw new ItemConfigException("At least one of these parameters: 'max' or 'value', has to be defined.");
      } else {
         if (var11) {
            var5 = var10;
            var6 = var10;
         }

         if (var8.size() <= 0) {
            return new GeneratorFluidContainer(var1, (Fluid[])null, (float[])null, var4, var5, var6);
         } else {
            float[] var21 = new float[var9.size()];

            for(var13 = 0; var13 < var9.size(); ++var13) {
               var21[var13] = (Float)var9.get(var13);
            }

            return new GeneratorFluidContainer(var1, (Fluid[])PZArrayUtil.toArray(var8), var21, var4, var5, var6);
         }
      }
   }

   private RandomGenerator buildNumericGenerator(AttributeType var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      float var5 = 0.0F;
      float var6 = 0.0F;
      boolean var7 = false;
      float var8 = 0.0F;
      boolean var9 = false;
      String[] var10 = var3;
      int var11 = var3.length;

      for(int var12 = 0; var12 < var11; ++var12) {
         String var13 = var10[var12];
         String[] var14 = var13.split("=");
         String var15 = var14[0];
         if (var15.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var14[1]);
         } else if (var15.equalsIgnoreCase("min")) {
            var5 = Float.parseFloat(var14[1]);
         } else if (var15.equalsIgnoreCase("max")) {
            var7 = true;
            var6 = Float.parseFloat(var14[1]);
         } else if (var15.equalsIgnoreCase("value")) {
            var9 = true;
            var8 = Float.parseFloat(var14[1]);
         }
      }

      if (!var7 && !var9) {
         throw new ItemConfigException("At least one of these parameters: 'max' or 'value', has to be defined.");
      } else {
         if (var9) {
            var5 = var8;
            var6 = var8;
         }

         return new GeneratorNumericAttribute(var1, var4, var5, var6);
      }
   }

   private RandomGenerator buildStringGenerator(AttributeType var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      String var5 = null;
      String[] var6 = var3;
      int var7 = var3.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         String var9 = var6[var8];
         String[] var10 = var9.split("=");
         String var11 = var10[0];
         if (var11.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var10[1]);
         } else if (var11.equalsIgnoreCase("value")) {
            var5 = var10[1];
         }
      }

      if (var5 == null) {
         throw new ItemConfigException("At least parameter 'value' has to be defined.");
      } else {
         return new GeneratorStringAttribute(var1, var4, var5);
      }
   }

   private RandomGenerator buildBoolGenerator(AttributeType var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      boolean var5 = false;
      boolean var6 = false;
      String[] var7 = var3;
      int var8 = var3.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         String var10 = var7[var9];
         String[] var11 = var10.split("=");
         String var12 = var11[0];
         if (var12.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var11[1]);
         } else if (var12.equalsIgnoreCase("value")) {
            var6 = Boolean.parseBoolean(var11[1]);
            var5 = true;
         }
      }

      if (!var5) {
         throw new ItemConfigException("At least parameter 'value' has to be defined.");
      } else {
         return new GeneratorBoolAttribute(var1, var4, var6);
      }
   }

   private RandomGenerator buildEnumGenerator(AttributeType var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      String var5 = null;
      String[] var6 = var3;
      int var7 = var3.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         String var9 = var6[var8];
         String[] var10 = var9.split("=");
         String var11 = var10[0];
         if (var11.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var10[1]);
         } else if (var11.equalsIgnoreCase("value")) {
            var5 = var10[1];
         }
      }

      if (var5 == null) {
         throw new ItemConfigException("At least parameter 'value' has to be defined.");
      } else {
         return new GeneratorEnumAttribute(var1, var4, var5);
      }
   }

   private RandomGenerator buildEnumSetGenerator(AttributeType var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      String var5 = null;
      GeneratorEnumSetAttribute.Mode var6 = GeneratorEnumSetAttribute.Mode.Set;
      String[] var7 = var3;
      int var8 = var3.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         String var10 = var7[var9];
         String[] var11 = var10.split("=");
         String var12 = var11[0];
         if (var12.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var11[1]);
         } else if (var12.equalsIgnoreCase("value")) {
            var5 = var11[1];
         } else if (var12.equalsIgnoreCase("mode")) {
            if (var11[1].equalsIgnoreCase("add")) {
               var6 = GeneratorEnumSetAttribute.Mode.Add;
            } else if (var11[1].equalsIgnoreCase("remove")) {
               var6 = GeneratorEnumSetAttribute.Mode.Remove;
            }
         }
      }

      if (var5 == null) {
         throw new ItemConfigException("At least parameter 'value' has to be defined.");
      } else {
         if (var5.contains(";")) {
            var7 = var5.split(";");
         } else {
            var7 = new String[]{var5};
         }

         return new GeneratorEnumSetAttribute(var1, var6, var4, var7);
      }
   }

   private RandomGenerator buildEnumStringSetGenerator(AttributeType var1, String var2) throws ItemConfigException {
      String[] var3 = var2.split("\\s+");
      float var4 = 1.0F;
      String var5 = null;
      GeneratorEnumStringSetAttribute.Mode var6 = GeneratorEnumStringSetAttribute.Mode.Set;
      String[] var7 = var3;
      int var8 = var3.length;

      String[] var11;
      for(int var9 = 0; var9 < var8; ++var9) {
         String var10 = var7[var9];
         var11 = var10.split("=");
         String var12 = var11[0];
         if (var12.equalsIgnoreCase("chance")) {
            var4 = Float.parseFloat(var11[1]);
         } else if (var12.equalsIgnoreCase("value")) {
            var5 = var11[1];
         } else if (var12.equalsIgnoreCase("mode")) {
            if (var11[1].equalsIgnoreCase("add")) {
               var6 = GeneratorEnumStringSetAttribute.Mode.Add;
            } else if (var11[1].equalsIgnoreCase("remove")) {
               var6 = GeneratorEnumStringSetAttribute.Mode.Remove;
            }
         }
      }

      if (var5 == null) {
         throw new ItemConfigException("At least parameter 'value' has to be defined.");
      } else {
         var7 = null;
         String[] var16 = null;
         ArrayList var17 = new ArrayList();
         ArrayList var18 = new ArrayList();
         if (var5.contains(";")) {
            var11 = var5.split(";");
            String[] var19 = var11;
            int var13 = var11.length;

            for(int var14 = 0; var14 < var13; ++var14) {
               String var15 = var19[var14];
               if (AttributeUtil.isEnumString(var15)) {
                  var17.add(var15);
               } else {
                  var18.add(var15);
               }
            }
         } else if (AttributeUtil.isEnumString(var5)) {
            var17.add(var5);
         } else {
            var18.add(var5);
         }

         if (var17.size() > 0) {
            var7 = (String[])var17.toArray(new String[0]);
         }

         if (var18.size() > 0) {
            var16 = (String[])var18.toArray(new String[0]);
         }

         return new GeneratorEnumStringSetAttribute(var1, var6, var4, var7, var16);
      }
   }

   public static class ItemConfigException extends Exception {
      public ItemConfigException(String var1) {
         super("RecipeAttributes -> " + var1 + " \n" + ItemConfig.createErrorString());
      }

      public ItemConfigException(String var1, Throwable var2) {
         super("RecipeAttributes -> " + var1 + " \n" + ItemConfig.createErrorString(), var2);
      }

      public ItemConfigException(String var1, Throwable var2, boolean var3) {
         super(var3 ? "RecipeAttributes -> " + var1 + " \n" + ItemConfig.createErrorString() : var1, var2);
      }
   }
}
