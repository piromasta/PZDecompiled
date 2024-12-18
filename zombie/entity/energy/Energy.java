package zombie.entity.energy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EnergyDefinitionScript;

@DebugClassFields
public class Energy {
   private static boolean HAS_INITIALIZED = false;
   private static final HashMap<EnergyType, Energy> energyEnumMap = new HashMap();
   private static final HashMap<String, Energy> energyStringMap = new HashMap();
   private static final HashMap<String, Energy> cacheStringMap = new HashMap();
   private static final ArrayList<Energy> allEnergies = new ArrayList();
   public static final Energy Electric;
   public static final Energy Mechanical;
   public static final Energy Thermal;
   public static final Energy Steam;
   public static final Energy VoidEnergy;
   private EnergyDefinitionScript script;
   private final EnergyType energyType;
   private final String energyTypeString;
   private final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);

   private static Energy addEnergy(EnergyType var0) {
      if (energyEnumMap.containsKey(var0)) {
         throw new RuntimeException("Energy defined twice: " + var0);
      } else {
         Energy var1 = new Energy(var0);
         energyEnumMap.put(var0, var1);
         return var1;
      }
   }

   public static Energy Get(EnergyType var0) {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Energies have not yet been initialized!");
      } else {
         return (Energy)energyEnumMap.get(var0);
      }
   }

   public static Energy Get(String var0) {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Energies have not yet been initialized!");
      } else {
         return (Energy)energyStringMap.get(var0);
      }
   }

   public static ArrayList<Energy> getAllEnergies() {
      if (Core.bDebug && !HAS_INITIALIZED) {
         throw new RuntimeException("Energies have not yet been initialized!");
      } else {
         return allEnergies;
      }
   }

   public static void Init(ScriptLoadMode var0) throws Exception {
      DebugLog.Energy.println("*************************************");
      DebugLog.Energy.println("* Energy: initialize Energies.      *");
      DebugLog.Energy.println("*************************************");
      ArrayList var1 = ScriptManager.instance.getAllEnergyDefinitionScripts();
      cacheStringMap.clear();
      allEnergies.clear();
      Iterator var2;
      Map.Entry var3;
      if (var0 == ScriptLoadMode.Reload) {
         var2 = energyStringMap.entrySet().iterator();

         while(var2.hasNext()) {
            var3 = (Map.Entry)var2.next();
            cacheStringMap.put((String)var3.getKey(), (Energy)var3.getValue());
         }

         energyStringMap.clear();
      }

      var2 = var1.iterator();

      while(var2.hasNext()) {
         EnergyDefinitionScript var5 = (EnergyDefinitionScript)var2.next();
         DebugLogStream var10000;
         String var10001;
         Energy var4;
         if (var5.getEnergyType() == EnergyType.Modded) {
            var10000 = DebugLog.Energy;
            var10001 = var5.getModID();
            var10000.println(var10001 + " = " + var5.getEnergyTypeString());
            var4 = (Energy)cacheStringMap.get(var5.getEnergyTypeString());
            if (var4 == null) {
               var4 = new Energy(var5.getEnergyTypeString());
            }

            var4.setScript(var5);
            energyStringMap.put(var5.getEnergyTypeString(), var4);
            allEnergies.add(var4);
         } else {
            var10000 = DebugLog.Energy;
            var10001 = var5.getModID();
            var10000.println(var10001 + " = " + var5.getEnergyType());
            var4 = (Energy)energyEnumMap.get(var5.getEnergyType());
            if (var4 == null) {
               if (Core.bDebug) {
                  throw new Exception("Energy not found: " + var5.getEnergyType());
               }
            } else {
               var4.setScript(var5);
               allEnergies.add(var4);
            }
         }
      }

      var2 = energyEnumMap.entrySet().iterator();

      while(var2.hasNext()) {
         var3 = (Map.Entry)var2.next();
         if (Core.bDebug && ((Energy)var3.getValue()).script == null) {
            throw new Exception("Energy has no script set: " + var3.getKey());
         }

         energyStringMap.put(((EnergyType)var3.getKey()).toString(), (Energy)var3.getValue());
      }

      cacheStringMap.clear();
      HAS_INITIALIZED = true;
      DebugLog.Energy.println("*************************************");
   }

   public static void PreReloadScripts() {
      HAS_INITIALIZED = false;
   }

   public static void Reset() {
      energyStringMap.clear();
      HAS_INITIALIZED = false;
   }

   public static void saveEnergy(Energy var0, ByteBuffer var1) {
      var1.put((byte)(var0 != null ? 1 : 0));
      if (var0 != null) {
         if (var0.energyType == EnergyType.Modded) {
            var1.put((byte)1);
            GameWindow.WriteString(var1, var0.energyTypeString);
         } else {
            var1.put((byte)0);
            var1.put(var0.energyType.getId());
         }

      }
   }

   public static Energy loadEnergy(ByteBuffer var0, int var1) {
      if (var0.get() == 0) {
         return null;
      } else {
         Energy var2;
         if (var0.get() == 1) {
            String var3 = GameWindow.ReadString(var0);
            var2 = Get(var3);
         } else {
            EnergyType var4 = EnergyType.FromId(var0.get());
            var2 = Get(var4);
         }

         return var2;
      }
   }

   private Energy(EnergyType var1) {
      this.energyType = var1;
      this.energyTypeString = null;
   }

   private Energy(String var1) {
      this.energyType = EnergyType.Modded;
      this.energyTypeString = (String)Objects.requireNonNull(var1);
   }

   private void setScript(EnergyDefinitionScript var1) {
      this.script = (EnergyDefinitionScript)Objects.requireNonNull(var1);
      this.color.set(var1.getColor());
   }

   public boolean isVanilla() {
      return this.script != null && this.script.isVanilla();
   }

   public String getDisplayName() {
      return this.script != null ? this.script.getDisplayName() : Translator.getEntityText("EC_Energy");
   }

   public Color getColor() {
      return this.color;
   }

   public Texture getIconTexture() {
      return this.script != null ? this.script.getIconTexture() : EnergyDefinitionScript.getDefaultIconTexture();
   }

   public Texture getHorizontalBarTexture() {
      return this.script != null ? this.script.getHorizontalBarTexture() : EnergyDefinitionScript.getDefaultHorizontalBarTexture();
   }

   public Texture getVerticalBarTexture() {
      return this.script != null ? this.script.getVerticalBarTexture() : EnergyDefinitionScript.getDefaultVerticalBarTexture();
   }

   public String getEnergyTypeString() {
      return this.energyType == EnergyType.Modded ? this.energyTypeString : this.energyType.toString();
   }

   static {
      Electric = addEnergy(EnergyType.Electric);
      Mechanical = addEnergy(EnergyType.Mechanical);
      Thermal = addEnergy(EnergyType.Thermal);
      Steam = addEnergy(EnergyType.Steam);
      VoidEnergy = addEnergy(EnergyType.VoidEnergy);
   }
}
