package zombie.scripting.entity.components.ui;

import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

public class UiConfigScript extends ComponentScript {
   private String xuiSkinName;
   private String entityStyle = null;
   private boolean uiEnabled = true;

   private UiConfigScript() {
      super(ComponentType.UiConfig);
   }

   public String getXuiSkinName() {
      return this.xuiSkinName;
   }

   public String getEntityStyle() {
      return this.entityStyle;
   }

   public boolean isUiEnabled() {
      return this.uiEnabled;
   }

   public String getDisplayNameDebug() {
      XuiSkin var1 = XuiManager.GetSkin(this.xuiSkinName);
      return var1 != null ? var1.getEntityDisplayName(this.entityStyle) : GameEntity.getDefaultEntityDisplayName();
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
      this.xuiSkinName = null;
      this.entityStyle = null;
      this.uiEnabled = true;
   }

   public void reset() {
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   protected void load(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            if (var4.equalsIgnoreCase("xuiSkin")) {
               this.xuiSkinName = var5;
            } else if (var4.equalsIgnoreCase("entityStyle")) {
               this.entityStyle = var5;
            } else if (var4.equalsIgnoreCase("uiEnabled")) {
               this.uiEnabled = Boolean.parseBoolean(var5);
            }
         }
      }

      var2 = var1.children.iterator();

      while(var2.hasNext()) {
         ScriptParser.Block var6 = (ScriptParser.Block)var2.next();
         if (!var6.type.equalsIgnoreCase("someType")) {
            String var10001 = var6.type;
            DebugLog.General.error("Unknown block '" + var10001 + "' in entity script: " + this.getName());
         }
      }

   }
}
