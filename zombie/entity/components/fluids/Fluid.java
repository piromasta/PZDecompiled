package zombie.entity.components.fluids;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.objects.DebugClass;
import zombie.debug.objects.DebugField;
import zombie.debug.objects.DebugMethod;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.Item;

@DebugClass
public class Fluid {
   private static boolean HAS_INITIALIZED = false;
   private static final HashMap<FluidType, Fluid> fluidEnumMap = new HashMap();
   private static final HashMap<String, Fluid> fluidStringMap = new HashMap();
   private static final HashMap<String, Fluid> cacheStringMap = new HashMap();
   private static final HashMap<FluidDefinitionScript, Fluid> scriptToFluidMap = new HashMap();
   private static final ArrayList<Fluid> allFluids = new ArrayList();
   public static final Fluid Water;
   public static final Fluid TaintedWater;
   public static final Fluid Petrol;
   public static final Fluid Alcohol;
   public static final Fluid PoisonPotent;
   public static final Fluid Beer;
   public static final Fluid Whiskey;
   public static final Fluid SodaPop;
   public static final Fluid Coffee;
   public static final Fluid Tea;
   public static final Fluid Wine;
   public static final Fluid Bleach;
   public static final Fluid Blood;
   public static final Fluid Honey;
   public static final Fluid Mead;
   public static final Fluid Acid;
   public static final Fluid SpiffoJuice;
   public static final Fluid SecretFlavoring;
   public static final Fluid CarbonatedWater;
   public static final Fluid CleaningLiquid;
   public static final Fluid CowMilk;
   public static final Fluid SheepMilk;
   public static final Fluid AnimalBlood;
   public static final Fluid AnimalGrease;
   public static final Fluid Dye;
   public static final Fluid HairDye;
   @DebugNonRecursive
   private FluidDefinitionScript script;
   @DebugField
   private final FluidType fluidType;
   @DebugField
   private final String fluidTypeStr;
   @DebugField
   private final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   @DebugField
   private ImmutableSet<FluidCategory> categories;
   private String categoriesCacheStr;
   @DebugField
   private FluidFilter blendWhitelist;
   @DebugField
   private FluidFilter blendBlacklist;
   @DebugField
   private PoisonInfo poisonInfo;
   @DebugField
   private SealedFluidProperties properties;

   private static Fluid addFluid(FluidType var0) {
      if (fluidEnumMap.containsKey(var0)) {
         throw new RuntimeException("Fluid defined twice: " + var0);
      } else {
         Fluid var1 = new Fluid(var0);
         fluidEnumMap.put(var0, var1);
         return var1;
      }
   }

   public static Fluid Get(FluidType var0) {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Fluids have not yet been initialized!");
      } else {
         return (Fluid)fluidEnumMap.get(var0);
      }
   }

   public static Fluid Get(String var0) {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Fluids have not yet been initialized!");
      } else {
         return (Fluid)fluidStringMap.get(var0);
      }
   }

   public static ArrayList<Fluid> getAllFluids() {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Fluids have not yet been initialized!");
      } else {
         return allFluids;
      }
   }

   public static ArrayList<Item> getAllFluidItemsDebug() {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Fluids have not yet been initialized!");
      } else {
         ArrayList var0 = ScriptManager.instance.getAllItems();
         ArrayList var1 = new ArrayList();
         Iterator var2 = var0.iterator();

         while(var2.hasNext()) {
            Item var3 = (Item)var2.next();
            if (var3.containsComponent(ComponentType.FluidContainer)) {
               var1.add(var3);
            }
         }

         return var1;
      }
   }

   public static boolean FluidsInitialized() {
      return HAS_INITIALIZED;
   }

   public static void Init(ScriptLoadMode var0) throws Exception {
      DebugLog.Fluid.println("*************************************");
      DebugLog.Fluid.println("* Fluid: initialize Fluids.         *");
      DebugLog.Fluid.println("*************************************");
      ArrayList var1 = ScriptManager.instance.getAllFluidDefinitionScripts();
      cacheStringMap.clear();
      scriptToFluidMap.clear();
      allFluids.clear();
      Iterator var2;
      Map.Entry var3;
      if (var0 == ScriptLoadMode.Reload) {
         var2 = fluidStringMap.entrySet().iterator();

         while(var2.hasNext()) {
            var3 = (Map.Entry)var2.next();
            cacheStringMap.put((String)var3.getKey(), (Fluid)var3.getValue());
         }

         fluidStringMap.clear();
      }

      var2 = var1.iterator();

      Fluid var4;
      FluidDefinitionScript var5;
      while(var2.hasNext()) {
         var5 = (FluidDefinitionScript)var2.next();
         DebugLogStream var10000;
         String var10001;
         if (var5.getFluidType() == FluidType.Modded) {
            var10000 = DebugLog.Fluid;
            var10001 = var5.getModID();
            var10000.println(var10001 + " = " + var5.getFluidTypeString());
            var4 = (Fluid)cacheStringMap.get(var5.getFluidTypeString());
            if (var4 == null) {
               var4 = new Fluid(var5.getFluidTypeString());
            }

            var4.setScript(var5);
            fluidStringMap.put(var5.getFluidTypeString(), var4);
            scriptToFluidMap.put(var5, var4);
            allFluids.add(var4);
         } else {
            var10000 = DebugLog.Fluid;
            var10001 = var5.getModID();
            var10000.println(var10001 + " = " + var5.getFluidType());
            var4 = (Fluid)fluidEnumMap.get(var5.getFluidType());
            if (var4 == null) {
               if (Core.bDebug) {
                  throw new Exception("Fluid not found: " + var5.getFluidType());
               }
            } else {
               var4.setScript(var5);
               scriptToFluidMap.put(var5, var4);
               allFluids.add(var4);
            }
         }
      }

      var2 = fluidEnumMap.entrySet().iterator();

      while(var2.hasNext()) {
         var3 = (Map.Entry)var2.next();
         if (Core.bDebug && ((Fluid)var3.getValue()).script == null) {
            throw new Exception("Fluid has no script set: " + var3.getKey());
         }

         fluidStringMap.put(((FluidType)var3.getKey()).toString(), (Fluid)var3.getValue());
      }

      cacheStringMap.clear();
      HAS_INITIALIZED = true;
      var2 = var1.iterator();

      while(var2.hasNext()) {
         var5 = (FluidDefinitionScript)var2.next();
         var4 = (Fluid)scriptToFluidMap.get(var5);
         if (var4 != null) {
            if (var5.getBlendWhitelist() != null) {
               var4.blendWhitelist = var5.getBlendWhitelist().createFilter();
               var4.blendWhitelist.seal();
               DebugLog.Fluid.println("[Created fluid blend whitelist: " + var4.getFluidTypeString() + "]");
               DebugLog.Fluid.println((Object)var4.blendWhitelist);
            }

            if (var5.getBlendBlackList() != null) {
               var4.blendBlacklist = var5.getBlendBlackList().createFilter();
               var4.blendBlacklist.seal();
               DebugLog.Fluid.println("[Created fluid blend blacklist: " + var4.getFluidTypeString() + "]");
               DebugLog.Fluid.println((Object)var4.blendBlacklist);
            }
         }
      }

      DebugLog.Fluid.println("*************************************");
   }

   public static void PreReloadScripts() {
      HAS_INITIALIZED = false;
   }

   public static void Reset() {
      fluidStringMap.clear();
      scriptToFluidMap.clear();
      HAS_INITIALIZED = false;
   }

   public static void saveFluid(Fluid var0, ByteBuffer var1) {
      var1.put((byte)(var0 != null ? 1 : 0));
      if (var0 != null) {
         if (var0.fluidType == FluidType.Modded) {
            var1.put((byte)1);
            GameWindow.WriteString(var1, var0.fluidTypeStr);
         } else {
            var1.put((byte)0);
            var1.put(var0.fluidType.getId());
         }

      }
   }

   public static Fluid loadFluid(ByteBuffer var0, int var1) {
      if (var0.get() == 0) {
         return null;
      } else {
         Fluid var2;
         if (var0.get() == 1) {
            String var3 = GameWindow.ReadString(var0);
            var2 = Get(var3);
         } else {
            FluidType var4 = FluidType.FromId(var0.get());
            var2 = Get(var4);
         }

         return var2;
      }
   }

   private Fluid(FluidType var1) {
      this.fluidType = (FluidType)Objects.requireNonNull(var1);
      this.fluidTypeStr = var1.toString();
   }

   private Fluid(String var1) {
      this.fluidType = FluidType.Modded;
      this.fluidTypeStr = (String)Objects.requireNonNull(var1);
   }

   private void setScript(FluidDefinitionScript var1) {
      this.script = (FluidDefinitionScript)Objects.requireNonNull(var1);
      this.color.set(var1.getColor());
      this.categories = Sets.immutableEnumSet(var1.getCategories());
      this.categoriesCacheStr = this.categories.toString();
      if (var1.hasPropertiesSet()) {
         FluidProperties var2 = new FluidProperties();
         var2.setEffects(var1.getFatigueChange(), var1.getHungerChange(), var1.getStressChange(), var1.getThirstChange(), var1.getUnhappyChange(), var1.getAlcohol());
         var2.setNutrients(var1.getCalories(), var1.getCarbohydrates(), var1.getLipids(), var1.getProteins());
         var2.setAlcohol(var1.getAlcohol());
         var2.setReductions(var1.getFluReduction(), var1.getPainReduction(), var1.getEnduranceChange(), var1.getFoodSicknessReduction());
         this.properties = var2.getSealedFluidProperties();
      } else {
         this.properties = null;
      }

      if (var1.getPoisonMaxEffect() != PoisonEffect.None) {
         this.poisonInfo = new PoisonInfo(this, var1.getPoisonMinAmount(), var1.getPoisonDiluteRatio(), var1.getPoisonMaxEffect());
      } else {
         this.poisonInfo = null;
      }

   }

   @DebugMethod
   public boolean isVanilla() {
      return this.script != null && this.script.isVanilla();
   }

   public String toString() {
      return this.fluidTypeStr;
   }

   public FluidInstance getInstance() {
      return FluidInstance.Alloc(this);
   }

   public FluidType getFluidType() {
      return this.fluidType;
   }

   public String getFluidTypeString() {
      return this.fluidTypeStr;
   }

   public Color getColor() {
      return this.color;
   }

   public ImmutableSet<FluidCategory> getCategories() {
      return this.categories;
   }

   public boolean isCategory(FluidCategory var1) {
      return this.categories.contains(var1);
   }

   public String getDisplayName() {
      return this.script != null ? this.script.getDisplayName() : "<unknown_fluid>";
   }

   public String getTranslatedName() {
      return this.getDisplayName();
   }

   public String getTranslatedNameLower() {
      return this.getDisplayName().toLowerCase();
   }

   public boolean canBlendWith(Fluid var1) {
      return (this.blendWhitelist == null || this.blendWhitelist.allows(var1)) && (this.blendBlacklist == null || this.blendBlacklist.allows(var1));
   }

   public SealedFluidProperties getProperties() {
      return this.properties;
   }

   public PoisonInfo getPoisonInfo() {
      return this.poisonInfo;
   }

   public boolean isPoisonous() {
      return this.poisonInfo != null;
   }

   public FluidDefinitionScript getScript() {
      return this.script;
   }

   static {
      Water = addFluid(FluidType.Water);
      TaintedWater = addFluid(FluidType.TaintedWater);
      Petrol = addFluid(FluidType.Petrol);
      Alcohol = addFluid(FluidType.Alcohol);
      PoisonPotent = addFluid(FluidType.PoisonPotent);
      Beer = addFluid(FluidType.Beer);
      Whiskey = addFluid(FluidType.Whiskey);
      SodaPop = addFluid(FluidType.SodaPop);
      Coffee = addFluid(FluidType.Coffee);
      Tea = addFluid(FluidType.Tea);
      Wine = addFluid(FluidType.Wine);
      Bleach = addFluid(FluidType.Bleach);
      Blood = addFluid(FluidType.Blood);
      Honey = addFluid(FluidType.Honey);
      Mead = addFluid(FluidType.Mead);
      Acid = addFluid(FluidType.Acid);
      SpiffoJuice = addFluid(FluidType.SpiffoJuice);
      SecretFlavoring = addFluid(FluidType.SecretFlavoring);
      CarbonatedWater = addFluid(FluidType.CarbonatedWater);
      CleaningLiquid = addFluid(FluidType.CleaningLiquid);
      CowMilk = addFluid(FluidType.CowMilk);
      SheepMilk = addFluid(FluidType.SheepMilk);
      AnimalBlood = addFluid(FluidType.AnimalBlood);
      AnimalGrease = addFluid(FluidType.AnimalGrease);
      Dye = addFluid(FluidType.Dye);
      HairDye = addFluid(FluidType.HairDye);
   }
}
