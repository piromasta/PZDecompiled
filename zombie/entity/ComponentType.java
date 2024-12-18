package zombie.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.utils.Bits;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.crafting.CraftRecipeComponent;
import zombie.entity.components.crafting.DryingLogic;
import zombie.entity.components.crafting.FurnaceLogic;
import zombie.entity.components.crafting.MashingLogic;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.lua.LuaComponent;
import zombie.entity.components.parts.Parts;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.signals.Signals;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.test.TestComponent;
import zombie.entity.components.ui.UiConfig;
import zombie.entity.meta.MetaTagComponent;
import zombie.entity.util.BitSet;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.attributes.AttributesScript;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftLogicScript;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.DryingLogicScript;
import zombie.scripting.entity.components.crafting.FurnaceLogicScript;
import zombie.scripting.entity.components.crafting.MashingLogicScript;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.entity.components.lua.LuaComponentScript;
import zombie.scripting.entity.components.parts.PartsScript;
import zombie.scripting.entity.components.resources.ResourcesScript;
import zombie.scripting.entity.components.signals.SignalsScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.entity.components.test.TestComponentScript;
import zombie.scripting.entity.components.ui.UiConfigScript;

public enum ComponentType {
   Attributes((short)1, AttributeContainer.class, AttributesScript.class, 0),
   FluidContainer((short)2, FluidContainer.class, FluidContainerScript.class, 0),
   SpriteConfig((short)3, SpriteConfig.class, SpriteConfigScript.class, 0, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   Lua((short)6, LuaComponent.class, LuaComponentScript.class, 0),
   Parts((short)7, Parts.class, PartsScript.class, 0),
   Signals((short)8, Signals.class, SignalsScript.class, 0),
   Script((short)9, EntityScriptInfo.class, (Class)null, 0),
   UiConfig((short)11, UiConfig.class, UiConfigScript.class, 0),
   CraftLogic((short)12, CraftLogic.class, CraftLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   FurnaceLogic((short)13, FurnaceLogic.class, FurnaceLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   TestComponent((short)14, TestComponent.class, TestComponentScript.class, 0),
   MashingLogic((short)15, MashingLogic.class, MashingLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   DryingLogic((short)16, DryingLogic.class, DryingLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   MetaTag((short)17, MetaTagComponent.class, (Class)null, 0, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   Resources((short)18, Resources.class, ResourcesScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, (Enum)GameEntityType.MetaEntity)),
   CraftBench((short)19, CraftBench.class, CraftBenchScript.class, 0),
   CraftRecipe((short)20, CraftRecipeComponent.class, CraftRecipeComponentScript.class, 0),
   Undefined((short)0, (Class)null, (Class)null, 0);

   private static final Map<Short, ComponentType> idMap = new HashMap();
   private static final Map<Class, ComponentType> classMap = new HashMap();
   private static final ArrayList<ComponentType> list = new ArrayList();
   private static final ComponentType[] array;
   static final BitSet bitsAddToEngine = new BitSet();
   static final BitSet bitsRunInMeta = new BitSet();
   static final BitSet bitsRenderLast = new BitSet();
   private static final ComponentFactory componentFactory = new ComponentFactory();
   private static final ComponentScriptFactory scriptFactory = new ComponentScriptFactory();
   public static final int MAX_ID_INDEX;
   final short id;
   final int flags;
   private final Class<? extends Component> componentClass;
   private final Class<? extends ComponentScript> componentScriptClass;
   private final EnumBitStore<GameEntityType> validEntityTypes;

   private ComponentType(short var3, Class var4, Class var5, int var6) {
      this(var3, var4, var5, var6, EnumBitStore.allOf(GameEntityType.class));
   }

   private ComponentType(short var3, Class var4, Class var5, int var6, EnumBitStore var7) {
      this.id = var3;
      this.flags = var6;
      this.componentClass = var4;
      this.componentScriptClass = var5;
      this.validEntityTypes = var7;
      if (this.id > 0 && var4 == null) {
         throw new IllegalArgumentException("ComponentType must have class extending 'Component' defined.");
      }
   }

   public short GetID() {
      return this.id;
   }

   public boolean isAddToEngine() {
      return Bits.hasFlags((int)this.flags, 1);
   }

   public boolean isRunInMeta() {
      return Bits.hasFlags((int)this.flags, 2);
   }

   public boolean isRenderLast() {
      return Bits.hasFlags((int)this.flags, 4);
   }

   public boolean isValidGameEntityType(GameEntityType var1) {
      return this.validEntityTypes.contains(var1);
   }

   public Class<? extends Component> GetComponentClass() {
      return this.componentClass;
   }

   public Component CreateComponent() {
      return componentFactory.alloc(this.componentClass);
   }

   public Component CreateComponentFromScript(ComponentScript var1) {
      Component var2 = this.CreateComponent();
      var2.readFromScript(var1);
      return var2;
   }

   public ComponentScript CreateComponentScript() {
      ComponentScript var1 = scriptFactory.create(this.componentScriptClass);
      if (var1 == null) {
         throw new RuntimeException("Unable to create script for component (No script class defined?): " + this);
      } else {
         return var1;
      }
   }

   public static void ReleaseComponent(Component var0) {
      componentFactory.release(var0);
   }

   public static ComponentType FromId(short var0) {
      ComponentType var1 = array[var0];
      return var1 != null ? var1 : Undefined;
   }

   public static ComponentType FromClass(Class<? extends Component> var0) {
      ComponentType var1 = (ComponentType)classMap.get(var0);
      return var1 != null ? var1 : Undefined;
   }

   public static ArrayList<ComponentType> GetList() {
      return list;
   }

   public static BitSet getBitsFor(ComponentType... var0) {
      BitSet var1 = new BitSet();
      int var2 = var0.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         var1.set(var0[var3].GetID());
      }

      return var1;
   }

   static {
      int var0 = 0;
      ComponentType[] var1 = values();
      int var2 = var1.length;

      int var3;
      ComponentType var4;
      for(var3 = 0; var3 < var2; ++var3) {
         var4 = var1[var3];
         if (idMap.containsKey(var4.id)) {
            throw new RuntimeException("ComponentType id '" + var4.id + "' already assigned.");
         }

         idMap.put(var4.id, var4);
         classMap.put(var4.componentClass, var4);
         if (var4 != Undefined) {
            list.add(var4);
         }

         var0 = PZMath.max(var0, var4.GetID());
         if (var4.isAddToEngine()) {
            bitsAddToEngine.set(var4.GetID());
         }

         if (var4.isRunInMeta()) {
            bitsRunInMeta.set(var4.GetID());
         }

         if (var4.isRenderLast()) {
            bitsRenderLast.set(var4.GetID());
         }
      }

      MAX_ID_INDEX = var0 + 1;
      if (MAX_ID_INDEX > 100 && Core.bDebug) {
         throw new RuntimeException("Warning MAX_ID_INDEX is high, increase warning threshold if this should be ignored.");
      } else {
         array = new ComponentType[MAX_ID_INDEX];
         Arrays.fill(array, Undefined);
         var1 = values();
         var2 = var1.length;

         for(var3 = 0; var3 < var2; ++var3) {
            var4 = var1[var3];
            array[var4.GetID()] = var4;
         }

      }
   }
}
