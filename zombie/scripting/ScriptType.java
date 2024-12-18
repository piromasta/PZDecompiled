package zombie.scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

public enum ScriptType {
   EntityComponent("entityComponent"),
   VehicleTemplate(true, "vehicle"),
   EntityTemplate(true, "entity"),
   Item("item"),
   Recipe("recipe"),
   UniqueRecipe("uniquerecipe"),
   EvolvedRecipe("evolvedrecipe"),
   Fixing("fixing"),
   AnimationMesh("animationsMesh"),
   Mannequin("mannequin"),
   Model("model"),
   SpriteModel("spriteModel"),
   Sound("sound"),
   SoundTimeline("soundTimeline"),
   Vehicle("vehicle"),
   RuntimeAnimation("animation"),
   VehicleEngineRPM("vehicleEngineRPM"),
   ItemConfig("itemConfig"),
   Entity("entity"),
   XuiLayout("xuiLayout"),
   XuiStyle("xuiStyle"),
   XuiDefaultStyle("xuiDefaultStyle"),
   XuiColor("xuiGlobalColors"),
   XuiSkin("xuiSkin"),
   XuiConfig("xuiConfig"),
   ItemFilter("itemFilter"),
   CraftRecipe("craftRecipe"),
   FluidFilter("fluidFilter"),
   StringList("stringList"),
   EnergyDefinition("energy"),
   FluidDefinition("fluid"),
   PhysicsShape("physicsShape"),
   TimedAction("timedAction"),
   Ragdoll("ragdoll");

   private static final ArrayList<ScriptType> sortedList;
   private static final Comparator<ScriptType> typeComparator = (var0x, var1x) -> {
      if (var0x.isTemplate && !var1x.isTemplate) {
         return 1;
      } else {
         return !var0x.isTemplate && var1x.isTemplate ? -1 : var0x.toString().compareTo(var1x.toString());
      }
   };
   private final boolean isTemplate;
   private final String scriptTag;
   private boolean isCritical;
   private Set<Flags> flags;
   private boolean verbose;

   private ScriptType(String var3) {
      this(false, var3);
   }

   private ScriptType(boolean var3, String var4) {
      this.isCritical = false;
      this.verbose = false;
      this.isTemplate = var3;
      this.scriptTag = var4;
   }

   public boolean isTemplate() {
      return this.isTemplate;
   }

   public boolean isCritical() {
      return this.isCritical;
   }

   public String getScriptTag() {
      return this.scriptTag;
   }

   public boolean hasFlag(Flags var1) {
      return this.flags.contains(var1);
   }

   public boolean hasFlags(EnumSet<Flags> var1) {
      return this.flags.containsAll(var1);
   }

   public boolean isVerbose() {
      return this.verbose;
   }

   public void setVerbose(boolean var1) {
      this.verbose = var1;
   }

   public static ArrayList<ScriptType> GetEnumListLua() {
      return sortedList;
   }

   static {
      EnumSet var0 = EnumSet.of(ScriptType.Flags.Clear, ScriptType.Flags.CacheFullType, ScriptType.Flags.ResetExisting, ScriptType.Flags.RemoveLoadError, ScriptType.Flags.SeekImports, ScriptType.Flags.AllowNewScriptDiscoveryOnReload);
      EnumSet var1 = EnumSet.copyOf(var0);
      var1.remove(ScriptType.Flags.AllowNewScriptDiscoveryOnReload);
      Item.flags = Collections.unmodifiableSet(var1);
      Entity.flags = Collections.unmodifiableSet(var1);
      EnumSet var2 = EnumSet.copyOf(var0);
      var2.remove(ScriptType.Flags.ResetExisting);
      var2.add(ScriptType.Flags.NewInstanceOnReload);
      Vehicle.flags = Collections.unmodifiableSet(var2);
      XuiSkin.flags = Collections.unmodifiableSet(EnumSet.of(ScriptType.Flags.Clear, ScriptType.Flags.CacheFullType, ScriptType.Flags.RemoveLoadError, ScriptType.Flags.SeekImports, ScriptType.Flags.ResetOnceOnReload));
      XuiSkin.setVerbose(true);
      ArrayList var3 = new ArrayList();
      ScriptType[] var4 = values();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ScriptType var7 = var4[var6];
         if (var7.flags == null) {
            var7.flags = Collections.unmodifiableSet(var0);
         }

         if (var7 != EntityComponent) {
            var3.add(var7);
         }
      }

      var3.sort(typeComparator);
      sortedList = var3;
   }

   public static enum Flags {
      Clear,
      FromList,
      CacheFullType,
      ResetExisting,
      RemoveLoadError,
      SeekImports,
      ResetOnceOnReload,
      AllowNewScriptDiscoveryOnReload,
      NewInstanceOnReload;

      private Flags() {
      }
   }
}
