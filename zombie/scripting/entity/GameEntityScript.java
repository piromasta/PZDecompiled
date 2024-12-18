package zombie.scripting.entity;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugMethod;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.network.GameClient;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.world.WorldDictionary;

@DebugClassFields
public class GameEntityScript extends BaseScriptObject {
   private String name;
   private final ArrayList<ComponentScript> componentScripts = new ArrayList();
   private short registry_id = -1;
   private boolean existsAsVanilla = false;
   private String modID;
   private String fileAbsPath;

   public GameEntityScript() {
      super(ScriptType.Entity);
   }

   protected GameEntityScript(ScriptType var1) {
      super(var1);
   }

   @DebugMethod
   public String getName() {
      return this.getParent() != null ? this.getParent().getScriptObjectName() : this.name;
   }

   public String getDisplayNameDebug() {
      UiConfigScript var1 = (UiConfigScript)this.getComponentScriptFor(ComponentType.UiConfig);
      return var1 != null ? var1.getDisplayNameDebug() : GameEntity.getDefaultEntityDisplayName();
   }

   public String getModuleName() {
      return this.getModule().getName();
   }

   @DebugMethod
   public String getFullName() {
      return this.getScriptObjectFullType();
   }

   public boolean getObsolete() {
      return false;
   }

   public void PreReload() {
      this.componentScripts.clear();
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      for(int var2 = 0; var2 < this.componentScripts.size(); ++var2) {
         ((ComponentScript)this.componentScripts.get(var2)).OnScriptsLoaded(var1);
      }

   }

   public void OnLoadedAfterLua() throws Exception {
      for(int var1 = 0; var1 < this.componentScripts.size(); ++var1) {
         ((ComponentScript)this.componentScripts.get(var1)).OnLoadedAfterLua();
      }

   }

   public void OnPostWorldDictionaryInit() throws Exception {
      for(int var1 = 0; var1 < this.componentScripts.size(); ++var1) {
         ((ComponentScript)this.componentScripts.get(var1)).OnPostWorldDictionaryInit();
      }

   }

   public ArrayList<ComponentScript> getComponentScripts() {
      return this.componentScripts;
   }

   public boolean hasComponents() {
      return this.componentScripts.size() > 0;
   }

   public boolean containsComponent(ComponentType var1) {
      for(int var2 = 0; var2 < this.componentScripts.size(); ++var2) {
         if (((ComponentScript)this.componentScripts.get(var2)).type == var1) {
            return true;
         }
      }

      return false;
   }

   private ComponentScript getOrCreateComponentScript(ComponentType var1) {
      ComponentScript var2 = this.getComponentScript(var1);
      if (var2 == null) {
         var2 = var1.CreateComponentScript();
         if (var2 != null) {
            var2.setParent(this);
            this.componentScripts.add(var2);
         }
      }

      if (var2 != null) {
         var2.InitLoadPP(this.getScriptObjectName());
         var2.setModule(this.getModule());
      }

      return var2;
   }

   public <T extends ComponentScript> T getComponentScriptFor(ComponentType var1) {
      return this.containsComponent(var1) ? this.getComponentScript(var1) : null;
   }

   private ComponentScript getComponentScript(ComponentType var1) {
      for(int var2 = 0; var2 < this.componentScripts.size(); ++var2) {
         if (((ComponentScript)this.componentScripts.get(var2)).type == var1) {
            return (ComponentScript)this.componentScripts.get(var2);
         }
      }

      return null;
   }

   public void copyFrom(GameEntityScript var1) {
      Iterator var2 = var1.componentScripts.iterator();

      while(var2.hasNext()) {
         ComponentScript var3 = (ComponentScript)var2.next();
         ComponentScript var4 = this.getOrCreateComponentScript(var3.type);
         var4.copyFrom(var3);
      }

   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
      ScriptManager var2 = ScriptManager.instance;
      this.name = var1;
      this.modID = ScriptManager.getCurrentLoadFileMod();
      if (this.modID.equals("pz-vanilla")) {
         this.existsAsVanilla = true;
      }

      this.fileAbsPath = ScriptManager.getCurrentLoadFileAbsPath();
      WorldDictionary.onLoadEntity(this);
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.values.iterator();

      while(var4.hasNext()) {
         ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
         String var6 = var5.getKey().trim();
         String var7 = var5.getValue().trim();
         if (!var6.isEmpty() && !var7.isEmpty() && var6.equalsIgnoreCase("entitytemplate")) {
            GameEntityTemplate var8 = ScriptManager.instance.getGameEntityTemplate(var7);
            GameEntityScript var9 = var8.getScript();
            this.copyFrom(var9);
         }
      }

      var4 = var3.children.iterator();

      while(var4.hasNext()) {
         ScriptParser.Block var10 = (ScriptParser.Block)var4.next();
         if (var10.type.equalsIgnoreCase("component")) {
            this.LoadComponentBlock(var10);
         } else {
            String var10001 = var10.type;
            DebugLog.General.error("Unknown block '" + var10001 + "' in entity  script: " + this.getName());
         }
      }

      this.Load(var3);
   }

   protected void Load(ScriptParser.Block var1) {
   }

   public boolean LoadAttribute(String var1, String var2) {
      AttributeType var3 = Attribute.TypeFromName(var1.trim());
      if (var3 != null) {
         ComponentScript var4 = this.getOrCreateComponentScript(ComponentType.Attributes);
         return var4.parseKeyValue(var1, var2);
      } else {
         return false;
      }
   }

   public void LoadComponentBlock(ScriptParser.Block var1) throws Exception {
      ComponentType var2 = ComponentType.Undefined;
      if (var1.type.equalsIgnoreCase("component") && !StringUtils.isNullOrWhitespace(var1.id)) {
         try {
            var2 = ComponentType.valueOf(var1.id);
         } catch (Exception var4) {
            var4.printStackTrace();
            var2 = ComponentType.Undefined;
         }

         if (var2 != ComponentType.Undefined) {
            ComponentScript var3 = this.getOrCreateComponentScript(var2);
            var3.load(var1);
         } else {
            String var10001 = var1.id != null ? var1.id : "null";
            DebugLog.General.warn("Could not parse component block id = " + var10001);
         }
      }

   }

   public short getRegistry_id() {
      return this.registry_id;
   }

   public void setRegistry_id(short var1) {
      if (this.registry_id != -1) {
         WorldDictionary.DebugPrintEntity(var1);
         short var10002 = this.registry_id;
         throw new RuntimeException("Cannot override existing registry id (" + var10002 + ") to new id (" + var1 + "), item: " + (this.getFullName() != null ? this.getFullName() : "unknown"));
      } else {
         this.registry_id = var1;
      }
   }

   public String getModID() {
      return this.modID;
   }

   public boolean getExistsAsVanilla() {
      return this.existsAsVanilla;
   }

   public String getFileAbsPath() {
      return this.fileAbsPath;
   }

   public void setModID(String var1) {
      if (GameClient.bClient) {
         if (this.modID == null) {
            this.modID = var1;
         } else if (!var1.equals(this.modID) && Core.bDebug) {
            WorldDictionary.DebugPrintEntity(this);
            throw new RuntimeException("Cannot override modID. ModID=" + (var1 != null ? var1 : "null"));
         }
      }

   }
}
