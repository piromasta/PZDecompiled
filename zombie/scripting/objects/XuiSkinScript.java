package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

public class XuiSkinScript extends BaseScriptObject {
   private static final String protectedDefaultName = "default";
   private final ArrayList<String> imports = new ArrayList();
   private final EntityUiScript defaultEntityUiScript = new EntityUiScript();
   private final Map<String, EntityUiScript> entityUiScriptMap = new HashMap();
   private final Map<String, StyleInfoScript> styleInfoMap = new HashMap();
   private final XuiColorsScript colorsScript = new XuiColorsScript();

   public XuiSkinScript() {
      super(ScriptType.XuiSkin);
   }

   public ArrayList<String> getImports() {
      return this.imports;
   }

   public EntityUiScript getDefaultEntityUiScript() {
      return this.defaultEntityUiScript;
   }

   public final Map<String, EntityUiScript> getEntityUiScriptMap() {
      return this.entityUiScriptMap;
   }

   public Map<String, StyleInfoScript> getStyleInfoMap() {
      return this.styleInfoMap;
   }

   public XuiColorsScript getColorsScript() {
      return this.colorsScript;
   }

   public void reset() {
      this.imports.clear();
      this.defaultEntityUiScript.reset();
      this.entityUiScriptMap.clear();
      this.styleInfoMap.clear();
      this.colorsScript.getColorMap().clear();
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
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
         if (!var6.isEmpty() && !var7.isEmpty()) {
            DebugLog.General.warn("Unknown line in script: " + var5.string);
         }
      }

      var4 = var3.children.iterator();

      while(var4.hasNext()) {
         ScriptParser.Block var8 = (ScriptParser.Block)var4.next();
         if ("imports".equalsIgnoreCase(var8.type)) {
            this.LoadImports(var8);
         } else if ("entity".equalsIgnoreCase(var8.type)) {
            this.LoadEntityBlock(var8);
         } else if ("colors".equalsIgnoreCase(var8.type)) {
            this.colorsScript.LoadColorsBlock(var8);
         } else {
            this.LoadStyleBlock(var8);
         }
      }

   }

   private void LoadStyleBlock(ScriptParser.Block var1) throws Exception {
      if (!this.styleInfoMap.containsKey(var1.type)) {
         this.styleInfoMap.put(var1.type, new StyleInfoScript());
      }

      StyleInfoScript var2 = (StyleInfoScript)this.styleInfoMap.get(var1.type);
      Object var3;
      if (StringUtils.isNullOrWhitespace(var1.id)) {
         var3 = var2.defaultStyleBlock;
      } else {
         if (var1.id.equalsIgnoreCase("default")) {
            throw new Exception("Default is protected and cannot be used as style name.");
         }

         if (var1.id.contains(".")) {
            throw new Exception("Style name may not contain '.' (dot).");
         }

         if (!var2.styleBlocks.containsKey(var1.id)) {
            var2.styleBlocks.put(var1.id, new HashMap());
         }

         var3 = (Map)var2.styleBlocks.get(var1.id);
      }

      Iterator var4 = var1.values.iterator();

      while(true) {
         String var6;
         String var7;
         do {
            if (!var4.hasNext()) {
               return;
            }

            ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
            var6 = var5.getKey().trim();
            var7 = var5.getValue().trim();
         } while(var6.isEmpty());

         var7 = StringUtils.trimSurroundingQuotes(var7);
         ((Map)var3).put(var6, !var7.isEmpty() && !var7.equalsIgnoreCase("nil") && !var7.equalsIgnoreCase("null") ? var7 : null);
      }
   }

   private void LoadImports(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.string != null ? var3.string.trim() : "";
         if (!StringUtils.isNullOrWhitespace(var4)) {
            if (var4.equalsIgnoreCase(this.getScriptObjectName())) {
               DebugLog.General.warn("Cannot import self: " + this.getScriptObjectName());
               if (Core.bDebug) {
                  throw new Exception("Cannot import self: " + this.getScriptObjectName());
               }
            }

            this.imports.add(var4);
         }
      }

   }

   private void LoadEntityBlock(ScriptParser.Block var1) throws Exception {
      EntityUiScript var2 = null;
      if (StringUtils.isNullOrWhitespace(var1.id)) {
         var2 = this.defaultEntityUiScript;
      } else {
         if (var1.id.equalsIgnoreCase("default")) {
            throw new Exception("Default is protected and cannot be used as style name.");
         }

         var2 = (EntityUiScript)this.entityUiScriptMap.get(var1.id);
         if (var2 == null) {
            var2 = new EntityUiScript();
            this.entityUiScriptMap.put(var1.id, var2);
         }
      }

      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (!var5.isEmpty() && !var6.isEmpty()) {
            if (var5.equalsIgnoreCase("luaWindowClass")) {
               var2.luaWindowClass = var6;
            } else if (var5.equalsIgnoreCase("xuiStyle")) {
               var2.xuiStyle = var6;
            } else if (var5.equalsIgnoreCase("luaCanOpenWindow")) {
               var2.luaCanOpenWindow = var6;
            } else if (var5.equalsIgnoreCase("luaOpenWindow")) {
               var2.luaOpenWindow = var6;
            } else if (var5.equalsIgnoreCase("displayName")) {
               String var7 = var6.replace(" ", "");
               String var8 = Translator.getRecipeName(var7);
               if (!var8.equalsIgnoreCase(var7)) {
                  var6 = var8;
               }

               var2.displayName = var6;
            } else if (var5.equalsIgnoreCase("description")) {
               var2.description = var6;
            } else if (var5.equalsIgnoreCase("buildDescription")) {
               var2.buildDescription = var6;
            } else if (var5.equalsIgnoreCase("icon")) {
               var2.iconPath = var6;
            } else if (var5.equalsIgnoreCase("clearComponents")) {
               var2.clearComponents = var6.equalsIgnoreCase("true");
            } else {
               DebugLog.General.warn("Unknown line in script: " + var4.string);
            }
         }
      }

      var3 = var1.children.iterator();

      while(var3.hasNext()) {
         ScriptParser.Block var9 = (ScriptParser.Block)var3.next();
         if ("components".equalsIgnoreCase(var9.type)) {
            this.LoadComponents(var9, var2);
         }
      }

   }

   private void LoadComponents(ScriptParser.Block var1, EntityUiScript var2) throws Exception {
      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (!var5.isEmpty() && !var6.isEmpty()) {
            ComponentType var7 = ComponentType.valueOf(var5);
            String var8 = var6;
            String var9 = null;
            if (var6.contains(":")) {
               String[] var10 = var6.split(":");
               var8 = var10[0];
               var9 = var10[1];
            }

            ComponentUiScript var11 = (ComponentUiScript)var2.componentUiScriptMap.get(var7);
            if (var11 == null) {
               var11 = new ComponentUiScript();
               var2.componentUiScriptMap.put(var7, var11);
            }

            var11.luaPanelClass = var8;
            if (var9 != null) {
               var11.xuiStyle = var9;
            }
         }
      }

      var3 = var1.children.iterator();

      while(var3.hasNext()) {
         ScriptParser.Block var12 = (ScriptParser.Block)var3.next();
         this.LoadComponentBlock(var12, var2);
      }

   }

   private void LoadComponentBlock(ScriptParser.Block var1, EntityUiScript var2) throws Exception {
      ComponentType var3 = ComponentType.valueOf(var1.type);
      ComponentUiScript var4 = (ComponentUiScript)var2.componentUiScriptMap.get(var3);
      if (var4 == null) {
         var4 = new ComponentUiScript();
         var2.componentUiScriptMap.put(var3, var4);
      }

      Iterator var5 = var1.values.iterator();

      while(var5.hasNext()) {
         ScriptParser.Value var6 = (ScriptParser.Value)var5.next();
         String var7 = var6.getKey().trim();
         String var8 = var6.getValue().trim();
         if (!var7.isEmpty() && !var8.isEmpty()) {
            if (var7.equalsIgnoreCase("luaPanelClass")) {
               var4.luaPanelClass = var8;
            } else if (var7.equalsIgnoreCase("xuiStyle")) {
               var4.xuiStyle = var8;
            } else if (var7.equalsIgnoreCase("displayName")) {
               var4.displayName = var8;
            } else if (var7.equalsIgnoreCase("icon")) {
               var4.iconPath = var8;
            } else if (var7.equalsIgnoreCase("listOrderZ")) {
               var4.listOrderZ = Integer.parseInt(var8);
               var4.listOrderZset = true;
            } else if (var7.equalsIgnoreCase("enabled")) {
               var4.enabled = var8.equalsIgnoreCase("true");
               var4.enabledSet = true;
            } else {
               DebugLog.General.warn("Unknown line in script: " + var6.string);
            }
         }
      }

   }

   public void PreReload() {
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   public static class EntityUiScript {
      private String luaWindowClass;
      private String xuiStyle;
      private String luaCanOpenWindow;
      private String luaOpenWindow;
      private String displayName;
      private String description;
      private String buildDescription;
      private String iconPath;
      private boolean clearComponents = false;
      private final Map<ComponentType, ComponentUiScript> componentUiScriptMap = new HashMap();

      public EntityUiScript() {
      }

      public String getLuaWindowClass() {
         return this.luaWindowClass;
      }

      public String getXuiStyle() {
         return this.xuiStyle;
      }

      public String getLuaCanOpenWindow() {
         return this.luaCanOpenWindow;
      }

      public String getLuaOpenWindow() {
         return this.luaOpenWindow;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getDescription() {
         return this.description;
      }

      public String getBuildDescription() {
         return this.buildDescription;
      }

      public String getIconPath() {
         return this.iconPath;
      }

      public boolean isClearComponents() {
         return this.clearComponents;
      }

      public Map<ComponentType, ComponentUiScript> getComponentUiScriptMap() {
         return this.componentUiScriptMap;
      }

      protected void reset() {
         this.luaWindowClass = null;
         this.xuiStyle = null;
         this.luaCanOpenWindow = null;
         this.luaOpenWindow = null;
         this.displayName = null;
         this.description = null;
         this.buildDescription = null;
         this.iconPath = null;
         this.clearComponents = false;
         this.componentUiScriptMap.clear();
      }
   }

   public static class StyleInfoScript {
      private final HashMap<String, String> defaultStyleBlock = new HashMap();
      private final Map<String, HashMap<String, String>> styleBlocks = new HashMap();

      public StyleInfoScript() {
      }

      public HashMap<String, String> getDefaultStyleBlock() {
         return this.defaultStyleBlock;
      }

      public Map<String, HashMap<String, String>> getStyleBlocks() {
         return this.styleBlocks;
      }
   }

   public static class ComponentUiScript {
      private String luaPanelClass;
      private String xuiStyle;
      private String displayName;
      private String iconPath;
      private boolean listOrderZset = false;
      private int listOrderZ = 0;
      private boolean enabledSet = false;
      private boolean enabled = true;

      public ComponentUiScript() {
      }

      public String getLuaPanelClass() {
         return this.luaPanelClass;
      }

      public String getXuiStyle() {
         return this.xuiStyle;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getIconPath() {
         return this.iconPath;
      }

      public boolean isListOrderZ() {
         return this.listOrderZset;
      }

      public int getListOrderZ() {
         return this.listOrderZ;
      }

      public boolean isEnabledSet() {
         return this.enabledSet;
      }

      public boolean isEnabled() {
         return this.enabled;
      }
   }
}
