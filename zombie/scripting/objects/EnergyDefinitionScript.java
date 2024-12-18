package zombie.scripting.objects;

import java.util.Iterator;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.entity.energy.EnergyType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public class EnergyDefinitionScript extends BaseScriptObject {
   private static Texture defaultIconTexture;
   private static Texture defaultHorizontalBarTexture;
   private static Texture defaultVerticalBarTexture;
   private boolean existsAsVanilla = false;
   private String modID;
   private EnergyType energyType;
   private String energyTypeString;
   private String displayName;
   private final Color color;
   private String iconTextureName;
   private String horizontalBarTextureName;
   private String verticalBarTextureName;
   private Texture iconTexture;
   private Texture horizontalBarTexture;
   private Texture verticalBarTexture;

   public static Texture getDefaultIconTexture() {
      if (defaultIconTexture == null) {
         defaultIconTexture = Texture.getSharedTexture("media/inventory/Question_On.png");
      }

      return defaultIconTexture;
   }

   public static Texture getDefaultHorizontalBarTexture() {
      if (defaultHorizontalBarTexture == null) {
         defaultHorizontalBarTexture = Texture.getSharedTexture("media/ui/Entity/Bars/bars_horz_yellow.png");
      }

      return defaultHorizontalBarTexture;
   }

   public static Texture getDefaultVerticalBarTexture() {
      if (defaultVerticalBarTexture == null) {
         defaultVerticalBarTexture = Texture.getSharedTexture("media/ui/Entity/Bars/bars_vert_yellow.png");
      }

      return defaultVerticalBarTexture;
   }

   protected EnergyDefinitionScript() {
      super(ScriptType.EnergyDefinition);
      this.energyType = EnergyType.None;
      this.energyTypeString = null;
      this.displayName = null;
      this.color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
      this.iconTextureName = null;
      this.horizontalBarTextureName = null;
      this.verticalBarTextureName = null;
      this.iconTexture = null;
      this.horizontalBarTexture = null;
      this.verticalBarTexture = null;
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

   public EnergyType getEnergyType() {
      return this.energyType;
   }

   public String getEnergyTypeString() {
      return this.energyTypeString;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Color getColor() {
      return this.color;
   }

   public Texture getIconTexture() {
      return this.iconTexture;
   }

   public Texture getHorizontalBarTexture() {
      return this.horizontalBarTexture;
   }

   public Texture getVerticalBarTexture() {
      return this.verticalBarTexture;
   }

   public void PreReload() {
      super.PreReload();
      this.enabled = true;
      this.existsAsVanilla = false;
      this.modID = null;
      this.energyType = EnergyType.None;
      this.energyTypeString = null;
      this.displayName = null;
      this.color.set(1.0F, 1.0F, 1.0F, 1.0F);
      this.iconTextureName = null;
      this.horizontalBarTextureName = null;
      this.verticalBarTextureName = null;
      this.iconTexture = null;
      this.horizontalBarTexture = null;
      this.verticalBarTexture = null;
   }

   public void reset() {
      super.reset();
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      if (this.energyType == EnergyType.None && this.energyTypeString == null) {
         throw new Exception("No energy type set.");
      } else {
         if (this.iconTextureName != null) {
            this.iconTexture = Texture.trygetTexture(this.iconTextureName);
         }

         if (this.iconTexture == null) {
            this.iconTexture = getDefaultIconTexture();
         }

         if (this.horizontalBarTextureName != null) {
            this.horizontalBarTexture = Texture.trygetTexture(this.horizontalBarTextureName);
         }

         if (this.horizontalBarTexture == null) {
            this.horizontalBarTexture = getDefaultHorizontalBarTexture();
         }

         if (this.verticalBarTextureName != null) {
            this.verticalBarTexture = Texture.trygetTexture(this.verticalBarTextureName);
         }

         if (this.verticalBarTexture == null) {
            this.verticalBarTexture = getDefaultVerticalBarTexture();
         }

      }
   }

   public void OnLoadedAfterLua() throws Exception {
      super.OnLoadedAfterLua();
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      super.OnPostWorldDictionaryInit();
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
      if (EnergyType.containsNameLowercase(var1)) {
         this.energyType = EnergyType.FromNameLower(var1);
      } else {
         this.energyType = EnergyType.Modded;
         this.energyTypeString = var1;
      }

      Iterator var4 = var3.values.iterator();

      while(var4.hasNext()) {
         ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
         String var6 = var5.getKey().trim();
         String var7 = var5.getValue().trim();
         if (!var6.isEmpty() && !var7.isEmpty()) {
            if (var6.equalsIgnoreCase("displayName")) {
               this.displayName = Translator.getEntityText(var7);
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
            } else if (var6.equalsIgnoreCase("iconTexture")) {
               this.iconTextureName = var7;
            } else if (var6.equalsIgnoreCase("horizontalBarTexture")) {
               this.horizontalBarTextureName = var7;
            } else if (var6.equalsIgnoreCase("verticalBarTexture")) {
               this.verticalBarTextureName = var7;
            } else {
               DebugLog.General.error("Unknown key '" + var6 + "' val(" + var7 + ") in energy definition: " + var1);
               if (Core.bDebug) {
                  throw new Exception("EnergyDefinition error.");
               }
            }
         }
      }

   }
}
