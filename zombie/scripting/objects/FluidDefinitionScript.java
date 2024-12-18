package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugIgnoreField;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.fluids.PoisonEffect;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@DebugClassFields
public class FluidDefinitionScript extends BaseScriptObject {
   @DebugIgnoreField
   private final HashMap<String, PropertyValue> propertiesMap = new HashMap();
   @DebugIgnoreField
   private final ArrayList<PropertyValue> properties = new ArrayList();
   private boolean existsAsVanilla = false;
   private String modID;
   private FluidType fluidType;
   private String fluidTypeString;
   private String colorReference;
   private final Color color;
   private final EnumSet<FluidCategory> categories;
   private String displayName;
   private FluidFilterScript blendWhitelist;
   private FluidFilterScript blendBlacklist;
   private boolean hasPoison;
   private PoisonEffect poisonMaxEffect;
   private float poisonMinAmount;
   private float poisonDiluteRatio;
   private final PropertyValue fatigueChange;
   private final PropertyValue hungerChange;
   private final PropertyValue stressChange;
   private final PropertyValue thirstChange;
   private final PropertyValue unhappyChange;
   private final PropertyValue calories;
   private final PropertyValue carbohydrates;
   private final PropertyValue lipids;
   private final PropertyValue proteins;
   private final PropertyValue alcohol;
   private final PropertyValue fluReduction;
   private final PropertyValue painReduction;
   private final PropertyValue enduranceChange;
   private final PropertyValue foodSicknessReduction;

   private PropertyValue addProperty(String var1, float var2) {
      if (this.propertiesMap.containsKey(var1)) {
         throw new RuntimeException("Name defined twice");
      } else {
         PropertyValue var3 = new PropertyValue(var1, var2);
         this.propertiesMap.put(var1, var3);
         this.properties.add(var3);
         return var3;
      }
   }

   protected FluidDefinitionScript() {
      super(ScriptType.FluidDefinition);
      this.fluidType = FluidType.None;
      this.fluidTypeString = null;
      this.color = new Color(0.0F, 0.0F, 1.0F, 1.0F);
      this.categories = EnumSet.noneOf(FluidCategory.class);
      this.displayName = "Fluid";
      this.hasPoison = false;
      this.poisonMaxEffect = PoisonEffect.None;
      this.poisonMinAmount = 1.0F;
      this.poisonDiluteRatio = 0.0F;
      this.fatigueChange = this.addProperty("fatigueChange", 0.0F);
      this.hungerChange = this.addProperty("hungerChange", 0.0F);
      this.stressChange = this.addProperty("stressChange", 0.0F);
      this.thirstChange = this.addProperty("thirstChange", 0.0F);
      this.unhappyChange = this.addProperty("unhappyChange", 0.0F);
      this.calories = this.addProperty("calories", 0.0F);
      this.carbohydrates = this.addProperty("carbohydrates", 0.0F);
      this.lipids = this.addProperty("lipids", 0.0F);
      this.proteins = this.addProperty("proteins", 0.0F);
      this.alcohol = this.addProperty("alcohol", 0.0F);
      this.fluReduction = this.addProperty("fluReduction", 0.0F);
      this.painReduction = this.addProperty("painReduction", 0.0F);
      this.enduranceChange = this.addProperty("enduranceChange", 0.0F);
      this.foodSicknessReduction = this.addProperty("foodSicknessReduction", 0.0F);
   }

   public boolean getExistsAsVanilla() {
      return this.existsAsVanilla;
   }

   public boolean isVanilla() {
      return this.modID != null && this.modID.equals("pz-vanilla");
   }

   public String getModID() {
      return this.modID;
   }

   public FluidType getFluidType() {
      return this.fluidType;
   }

   public String getFluidTypeString() {
      return this.fluidTypeString;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Color getColor() {
      if (this.colorReference != null) {
         Color var1 = Colors.GetColorByName(this.colorReference);
         if (var1 == null) {
            throw new RuntimeException("Cannot find color: " + this.colorReference);
         }

         this.color.set(var1);
      }

      return this.color;
   }

   public EnumSet<FluidCategory> getCategories() {
      return this.categories;
   }

   public FluidFilterScript getBlendWhitelist() {
      return this.blendWhitelist;
   }

   public FluidFilterScript getBlendBlackList() {
      return this.blendBlacklist;
   }

   public PoisonEffect getPoisonMaxEffect() {
      return this.poisonMaxEffect;
   }

   public float getPoisonMinAmount() {
      return this.poisonMinAmount;
   }

   public float getPoisonDiluteRatio() {
      return this.poisonDiluteRatio;
   }

   public float getFatigueChange() {
      return this.fatigueChange.get();
   }

   public float getHungerChange() {
      return this.hungerChange.get();
   }

   public float getStressChange() {
      return this.stressChange.get();
   }

   public float getThirstChange() {
      return this.thirstChange.get();
   }

   public float getUnhappyChange() {
      return this.unhappyChange.get();
   }

   public float getCalories() {
      return this.calories.get();
   }

   public float getCarbohydrates() {
      return this.carbohydrates.get();
   }

   public float getLipids() {
      return this.lipids.get();
   }

   public float getProteins() {
      return this.proteins.get();
   }

   public float getAlcohol() {
      return this.alcohol.get();
   }

   public float getFluReduction() {
      return this.fluReduction.get();
   }

   public float getPainReduction() {
      return this.painReduction.get();
   }

   public float getEnduranceChange() {
      return this.enduranceChange.get();
   }

   public float getFoodSicknessReduction() {
      return this.foodSicknessReduction.get();
   }

   public boolean hasPropertiesSet() {
      for(int var1 = 0; var1 < this.properties.size(); ++var1) {
         if (((PropertyValue)this.properties.get(var1)).isSet()) {
            return true;
         }
      }

      return false;
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
      this.modID = ScriptManager.getCurrentLoadFileMod();
      if (this.modID.equals("pz-vanilla")) {
         this.existsAsVanilla = true;
      }

   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      if (FluidType.containsNameLowercase(var1)) {
         this.fluidType = FluidType.FromNameLower(var1);
      } else {
         this.fluidType = FluidType.Modded;
         this.fluidTypeString = var1;
      }

      Iterator var4 = var3.values.iterator();

      while(var4.hasNext()) {
         ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
         String var6 = var5.getKey().trim();
         String var7 = var5.getValue().trim();
         if (!var6.isEmpty() && !var7.isEmpty()) {
            if (var6.equalsIgnoreCase("displayName")) {
               this.displayName = Translator.getFluidText(var7);
            } else if (var6.equalsIgnoreCase("colorReference")) {
               this.colorReference = var7;
            } else if ("r".equalsIgnoreCase(var6)) {
               this.color.r = Float.parseFloat(var7);
            } else if ("g".equalsIgnoreCase(var6)) {
               this.color.g = Float.parseFloat(var7);
            } else if ("b".equalsIgnoreCase(var6)) {
               this.color.b = Float.parseFloat(var7);
            } else if (var6.equalsIgnoreCase("color")) {
               String[] var8 = var7.split(":");
               if (var8.length == 3) {
                  this.color.r = Float.parseFloat(var8[0]);
                  this.color.g = Float.parseFloat(var8[1]);
                  this.color.b = Float.parseFloat(var8[2]);
               }
            } else {
               DebugLog.General.error("Unknown key '" + var6 + "' val(" + var7 + ") in fluid definition: " + this.getScriptObjectFullType());
               if (Core.bDebug) {
                  throw new Exception("FluidDefinition error.");
               }
            }
         }
      }

      var4 = var3.children.iterator();

      while(var4.hasNext()) {
         ScriptParser.Block var9 = (ScriptParser.Block)var4.next();
         if ("blendWhiteList".equalsIgnoreCase(var9.type)) {
            this.blendWhitelist = FluidFilterScript.GetAnonymous(true);
            this.blendWhitelist.LoadAnonymousFromBlock(var9);
         } else if ("blendBlackList".equalsIgnoreCase(var9.type)) {
            this.blendBlacklist = FluidFilterScript.GetAnonymous(false);
            this.blendBlacklist.LoadAnonymousFromBlock(var9);
         } else if ("categories".equalsIgnoreCase(var9.type)) {
            this.LoadCategories(var9);
         } else if ("properties".equalsIgnoreCase(var9.type)) {
            this.LoadProperties(var9);
         } else if ("poison".equalsIgnoreCase(var9.type)) {
            this.LoadPoison(var9);
         } else {
            String var10001 = var3.type;
            DebugLog.General.error("Unknown block '" + var10001 + "' val(" + var3.id + ") in fluid definition: " + this.getScriptObjectFullType());
            if (Core.bDebug) {
               throw new Exception("FluidDefinition error.");
            }
         }
      }

   }

   private void LoadCategories(ScriptParser.Block var1) {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         if (!StringUtils.isNullOrWhitespace(var3.string)) {
            FluidCategory var4 = FluidCategory.valueOf(var3.string.trim());
            this.categories.add(var4);
         }
      }

   }

   private void LoadPoison(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            this.hasPoison = true;
            if ("maxEffect".equalsIgnoreCase(var4)) {
               this.poisonMaxEffect = PoisonEffect.valueOf(var5);
            } else if ("minAmount".equalsIgnoreCase(var4)) {
               this.poisonMinAmount = Float.parseFloat(var5);
            } else if ("diluteRatio".equalsIgnoreCase(var4)) {
               this.poisonDiluteRatio = Float.parseFloat(var5);
            } else {
               DebugLog.General.error("Unknown key '" + var4 + "' val(" + var5 + ") in fluid poison definition: " + this.getScriptObjectFullType());
               if (Core.bDebug) {
                  throw new Exception("FluidDefinition error.");
               }
            }
         }
      }

   }

   private void LoadProperties(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            if ("fatigueChange".equalsIgnoreCase(var4)) {
               this.fatigueChange.set(Float.parseFloat(var5));
            } else if ("hungerChange".equalsIgnoreCase(var4)) {
               this.hungerChange.set(Float.parseFloat(var5));
            } else if ("stressChange".equalsIgnoreCase(var4)) {
               this.stressChange.set(Float.parseFloat(var5));
            } else if ("thirstChange".equalsIgnoreCase(var4)) {
               this.thirstChange.set(Float.parseFloat(var5));
            } else if ("unhappyChange".equalsIgnoreCase(var4)) {
               this.unhappyChange.set(Float.parseFloat(var5));
            } else if ("calories".equalsIgnoreCase(var4)) {
               this.calories.set(Float.parseFloat(var5));
            } else if ("carbohydrates".equalsIgnoreCase(var4)) {
               this.carbohydrates.set(Float.parseFloat(var5));
            } else if ("lipids".equalsIgnoreCase(var4)) {
               this.lipids.set(Float.parseFloat(var5));
            } else if ("proteins".equalsIgnoreCase(var4)) {
               this.proteins.set(Float.parseFloat(var5));
            } else if ("alcohol".equalsIgnoreCase(var4)) {
               this.alcohol.set(Float.parseFloat(var5));
            } else if ("fluReduction".equalsIgnoreCase(var4)) {
               this.fluReduction.set(Float.parseFloat(var5));
            } else if ("painReduction".equalsIgnoreCase(var4)) {
               this.painReduction.set(Float.parseFloat(var5));
            } else if ("enduranceChange".equalsIgnoreCase(var4)) {
               this.enduranceChange.set(Float.parseFloat(var5));
            } else if ("foodSicknessReduction".equalsIgnoreCase(var4)) {
               this.foodSicknessReduction.set(Float.parseFloat(var5));
            } else {
               DebugLog.General.error("Unknown key '" + var4 + "' val(" + var5 + ") in fluid properties definition: " + this.getScriptObjectFullType());
               if (Core.bDebug) {
                  throw new Exception("FluidDefinition error.");
               }
            }
         }
      }

   }

   public void PreReload() {
      this.existsAsVanilla = false;
      this.modID = null;
      this.fluidType = FluidType.None;
      this.fluidTypeString = null;
      this.colorReference = null;
      this.color.set(0.0F, 0.0F, 1.0F, 1.0F);
      this.categories.clear();
      this.displayName = "Fluid";
      this.blendWhitelist = null;
      this.blendBlacklist = null;
      this.hasPoison = false;
      this.poisonMaxEffect = PoisonEffect.None;
      this.poisonMinAmount = 1.0F;
      this.poisonDiluteRatio = 0.0F;
      this.fatigueChange.reset();
      this.hungerChange.reset();
      this.stressChange.reset();
      this.thirstChange.reset();
      this.unhappyChange.reset();
      this.calories.reset();
      this.carbohydrates.reset();
      this.lipids.reset();
      this.proteins.reset();
      this.alcohol.reset();
      this.fluReduction.reset();
      this.painReduction.reset();
      this.enduranceChange.reset();
      this.foodSicknessReduction.reset();
   }

   public void reset() {
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      if (this.fluidType == FluidType.None && this.fluidTypeString == null) {
         throw new Exception("No fluid type set.");
      } else if (this.hasPoison && this.poisonMaxEffect == PoisonEffect.None) {
         throw new Exception("Poison block defined but poison effect is 'None'");
      }
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   private static class PropertyValue {
      private final String name;
      private final float defaultValue;
      private float value = 0.0F;

      public PropertyValue(String var1, float var2) {
         this.name = (String)Objects.requireNonNull(var1);
         this.defaultValue = var2;
      }

      public boolean matchesKey(String var1) {
         return var1 != null ? this.name.equalsIgnoreCase(var1) : false;
      }

      public void set(float var1) {
         this.value = var1;
      }

      public float get() {
         return this.value;
      }

      public boolean isSet() {
         return this.value != this.defaultValue;
      }

      public void reset() {
         this.value = this.defaultValue;
      }

      public String toString() {
         return String.valueOf(this.defaultValue);
      }
   }
}
