package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.Lua.LuaManager;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.XuiSkinScript;

public class XuiSkin {
   private final ArrayList<XuiSkin> imports = new ArrayList();
   private final Map<String, Color> colorMap = new HashMap();
   private final EntityUiStyle defaultEntityUiStyle = new EntityUiStyle();
   private final Map<String, EntityUiStyle> entityUiStyleMap = new HashMap();
   private final Map<String, StyleInfo> styles = new HashMap();
   private final String name;
   private final XuiSkinScript script;
   private boolean hasLoaded = false;
   private boolean invalidated = false;

   public static XuiSkin Default() {
      return XuiManager.GetDefaultSkin();
   }

   public static String getDefaultSkinName() {
      return XuiManager.getDefaultSkinName();
   }

   public XuiSkin(String var1, XuiSkinScript var2) {
      this.name = var1;
      this.script = var2;
   }

   public boolean isInvalidated() {
      return this.invalidated;
   }

   protected void setInvalidated(boolean var1) {
      this.invalidated = var1;
   }

   public String getName() {
      return this.name;
   }

   public String getEntityDisplayName(String var1) {
      return this.getEntityUiStyle(var1).getDisplayName();
   }

   public EntityUiStyle getEntityUiStyle(String var1) {
      EntityUiStyle var2;
      if (var1 == null) {
         var2 = this.defaultEntityUiStyle;
      } else {
         var2 = (EntityUiStyle)this.entityUiStyleMap.get(var1);
         if (var2 == null) {
            DebugLog.General.warn("Cannot find entity ui info: " + var1 + ", attempting default...");
            var2 = this.defaultEntityUiStyle;
         }
      }

      return var2;
   }

   public ComponentUiStyle getComponentUiStyle(String var1, ComponentType var2) {
      EntityUiStyle var3 = this.getEntityUiStyle(var1);
      return var3.getComponentUiStyle(var2);
   }

   public Color color(String var1) {
      return this.colorInternal(var1, true, true);
   }

   protected Color colorInternal(String var1, boolean var2, boolean var3) {
      if (var1 == null) {
         return null;
      } else if (this.colorMap.containsKey(var1)) {
         return (Color)this.colorMap.get(var1);
      } else if (var2) {
         return Colors.GetColorByName(var1);
      } else {
         return !var3 ? Colors.White : null;
      }
   }

   public XuiLuaStyle getDefault(String var1) {
      return this.get(var1, (String)null);
   }

   public XuiLuaStyle get(String var1, String var2) {
      StyleInfo var3 = (StyleInfo)this.styles.get(var1);
      if (var3 != null) {
         return var2 == null ? var3.defaultStyle : var3.getStyle(var2);
      } else {
         return null;
      }
   }

   protected void Load() throws Exception {
      if (!this.hasLoaded) {
         this.hasLoaded = true;

         int var1;
         String var2;
         for(var1 = 0; var1 < this.script.getImports().size(); ++var1) {
            var2 = (String)this.script.getImports().get(var1);
            XuiSkin var3 = XuiManager.GetSkin(var2);
            XuiSkinScript var4 = ScriptManager.instance.getXuiSkinScript(var2);
            if (var3 == null) {
               throw new Exception("Import skin '" + var2 + "' not found for skin: " + this.name);
            }

            var3.Load();
            this.imports.add(var3);
            this.LoadColors(var4);
         }

         this.LoadColors(this.script);

         XuiSkinScript var5;
         for(var1 = 0; var1 < this.script.getImports().size(); ++var1) {
            var2 = (String)this.script.getImports().get(var1);
            var5 = ScriptManager.instance.getXuiSkinScript(var2);
            this.LoadDefaultEntityUiInfo(var5);
            this.LoadAllDefaultStyles(var5);
         }

         this.LoadDefaultEntityUiInfo(this.script);
         this.LoadAllDefaultStyles(this.script);

         for(var1 = 0; var1 < this.script.getImports().size(); ++var1) {
            var2 = (String)this.script.getImports().get(var1);
            var5 = ScriptManager.instance.getXuiSkinScript(var2);
            this.LoadEntityUiInfo(var5);
            this.LoadAllStyles(var5);
         }

         this.LoadEntityUiInfo(this.script);
         this.LoadAllStyles(this.script);
      }
   }

   private void LoadDefaultEntityUiInfo(XuiSkinScript var1) throws Exception {
      this.defaultEntityUiStyle.Load(var1.getDefaultEntityUiScript());
   }

   private void LoadEntityUiInfo(XuiSkinScript var1) throws Exception {
      Map.Entry var3;
      EntityUiStyle var4;
      for(Iterator var2 = var1.getEntityUiScriptMap().entrySet().iterator(); var2.hasNext(); var4.Load((XuiSkinScript.EntityUiScript)var3.getValue())) {
         var3 = (Map.Entry)var2.next();
         var4 = (EntityUiStyle)this.entityUiStyleMap.get(var3.getKey());
         if (var4 == null) {
            var4 = this.defaultEntityUiStyle.copy();
            this.entityUiStyleMap.put((String)var3.getKey(), var4);
         }
      }

   }

   private void LoadColors(XuiSkinScript var1) throws Exception {
      Iterator var2 = var1.getColorsScript().getColorMap().entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         this.colorMap.put((String)var3.getKey(), (Color)var3.getValue());
      }

   }

   private void LoadAllDefaultStyles(XuiSkinScript var1) throws Exception {
      Iterator var2 = var1.getStyleInfoMap().entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         this.LoadDefaultStyle((String)var3.getKey(), (XuiSkinScript.StyleInfoScript)var3.getValue());
      }

   }

   private void LoadDefaultStyle(String var1, XuiSkinScript.StyleInfoScript var2) throws Exception {
      StyleInfo var4 = (StyleInfo)this.styles.get(var1);
      if (var4 == null) {
         var4 = new StyleInfo();
         this.styles.put(var1, var4);
      }

      HashMap var3 = var2.getDefaultStyleBlock();
      if (var4.defaultStyle == null) {
         var4.defaultStyle = new XuiLuaStyle(var1, "default");
         var4.defaultStyle.xuiSkin = this;
      }

      Iterator var5 = var3.entrySet().iterator();

      while(var5.hasNext()) {
         Map.Entry var6 = (Map.Entry)var5.next();
         if (!var4.defaultStyle.loadVar((String)var6.getKey(), (String)var6.getValue())) {
            DebugLogStream var10000 = DebugLog.General;
            String var10001 = (String)var6.getKey();
            var10000.warn("Cannot load key = " + var10001 + ", val = " + (String)var6.getValue() + " for class = " + var1);

            assert false;
         }
      }

   }

   private void LoadAllStyles(XuiSkinScript var1) throws Exception {
      Iterator var2 = var1.getStyleInfoMap().entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         this.LoadStyle((String)var3.getKey(), (XuiSkinScript.StyleInfoScript)var3.getValue());
      }

   }

   private void LoadStyle(String var1, XuiSkinScript.StyleInfoScript var2) throws Exception {
      StyleInfo var4 = (StyleInfo)this.styles.get(var1);
      if (var4 == null) {
         var4 = new StyleInfo();
         this.styles.put(var1, var4);
      }

      Iterator var6 = var2.getStyleBlocks().entrySet().iterator();

      while(var6.hasNext()) {
         Map.Entry var7 = (Map.Entry)var6.next();
         String var5 = (String)var7.getKey();
         Map var3 = (Map)var7.getValue();
         XuiLuaStyle var8 = var4.getStyle((String)var7.getKey());
         if (var8 == null) {
            var8 = new XuiLuaStyle(var1, var5);
            var8.xuiSkin = this;
            if (var4.defaultStyle != null) {
               var8.copyVarsFrom(var4.defaultStyle);
            }

            var4.styles.put((String)var7.getKey(), var8);
         }

         Iterator var9 = var3.entrySet().iterator();

         while(var9.hasNext()) {
            Map.Entry var10 = (Map.Entry)var9.next();
            if (!var8.loadVar((String)var10.getKey(), (String)var10.getValue())) {
               DebugLogStream var10000 = DebugLog.General;
               String var10001 = (String)var10.getKey();
               var10000.warn("Cannot load key = " + var10001 + ", val = " + (String)var10.getValue() + " for class = " + var1);

               assert false;
            }
         }
      }

   }

   public void debugPrint() {
      DebugLog.log("================ SKIN ================");
      DebugLog.log("SkinName: " + this.name);
      DebugLog.log("imports {");
      Iterator var1 = this.imports.iterator();

      while(var1.hasNext()) {
         XuiSkin var2 = (XuiSkin)var1.next();
         DebugLog.log("   skin = " + var2.name);
      }

      DebugLog.log("}");
      DebugLog.log("================ ENTITY ================");
      this.printEntityStyle("Default Entity Style", this.defaultEntityUiStyle);
      var1 = this.entityUiStyleMap.entrySet().iterator();

      Map.Entry var3;
      while(var1.hasNext()) {
         var3 = (Map.Entry)var1.next();
         this.printEntityStyle("style=" + (String)var3.getKey(), (EntityUiStyle)var3.getValue());
      }

      DebugLog.log("================ STYLES ================");
      var1 = this.styles.entrySet().iterator();

      while(var1.hasNext()) {
         var3 = (Map.Entry)var1.next();
         this.printStyle((String)var3.getKey(), (StyleInfo)var3.getValue());
      }

      DebugLog.log("================ END ================");
   }

   private void printEntityStyle(String var1, EntityUiStyle var2) {
      DebugLog.log("[" + var1 + "]");
      DebugLog.log("WindowClass: " + var2.luaWindowClass);
      DebugLog.log("XuiStyle: " + var2.xuiStyle);
      DebugLog.log("DisplayName: " + var2.displayName);
      DebugLog.log("Description: " + var2.description);
      DebugLog.log("BuildDescription: " + var2.buildDescription);
      DebugLog.log("Icon: " + var2.icon);
      DebugLog.log("luaCanOpenWindow: " + var2.luaCanOpenWindow);
      DebugLog.log("luaOpenWindow: " + var2.luaOpenWindow);
      DebugLog.log("");
      DebugLog.log("-> <components> ");
      Iterator var3 = var2.componentUiStyleMap.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         this.printComponentStyle("  ", (ComponentType)var4.getKey(), (ComponentUiStyle)var4.getValue());
      }

      DebugLog.log("");
   }

   private void printComponentStyle(String var1, ComponentType var2, ComponentUiStyle var3) {
      DebugLog.log(var1 + "[" + var2 + "]");
      DebugLog.log(var1 + "luaPanelClass: " + var3.luaPanelClass);
      DebugLog.log(var1 + "xuiStyle: " + var3.xuiStyle);
      DebugLog.log(var1 + "displayName: " + var3.displayName);
      DebugLog.log(var1 + "icon: " + var3.icon);
      DebugLog.log(var1 + "listOrderZ: " + var3.listOrderZ);
      DebugLog.log(var1 + "enabled: " + var3.enabled);
      DebugLog.log("");
   }

   private void printStyle(String var1, StyleInfo var2) {
      DebugLog.log("[" + var1 + "]");
      if (var2.defaultStyle != null) {
         DebugLog.log("defaultStyle: " + var2.defaultStyle.getXuiStyleName());
         var2.defaultStyle.debugPrint("");
      } else {
         DebugLog.log("defaultStyle: null");
      }

      Iterator var3 = var2.styles.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         this.printStyle("  ", (String)var4.getKey(), (XuiLuaStyle)var4.getValue());
      }

      DebugLog.log("");
   }

   private void printStyle(String var1, String var2, XuiLuaStyle var3) {
      DebugLog.log(var1 + "[" + var2 + "]");
      if (var3 != null) {
         DebugLog.log(var1 + "style: " + var3.getXuiStyleName());
         var3.debugPrint(var1);
      } else {
         DebugLog.log(var1 + "style: null");
      }

      DebugLog.log("");
   }

   public static class EntityUiStyle {
      private String luaWindowClass;
      private String xuiStyle;
      private String luaCanOpenWindow;
      private String luaOpenWindow;
      private final Map<ComponentType, ComponentUiStyle> componentUiStyleMap = new HashMap();
      private String displayName;
      private String description;
      private String buildDescription;
      private Texture icon;

      public EntityUiStyle() {
      }

      public String getLuaWindowClass() {
         return this.luaWindowClass;
      }

      public String getXuiStyle() {
         return this.xuiStyle;
      }

      public Object getLuaCanOpenWindow() {
         return this.luaCanOpenWindow != null ? LuaManager.getFunctionObject(this.luaCanOpenWindow) : null;
      }

      public Object getLuaOpenWindow() {
         return this.luaOpenWindow != null ? LuaManager.getFunctionObject(this.luaOpenWindow) : null;
      }

      public String getDisplayName() {
         return Translator.getText(this.displayName);
      }

      public String getDescription() {
         return Translator.getText(this.description);
      }

      public String getBuildDescription() {
         return Translator.getText(this.buildDescription);
      }

      public Texture getIcon() {
         return this.icon;
      }

      public ComponentUiStyle getComponentUiStyle(ComponentType var1) {
         return (ComponentUiStyle)this.componentUiStyleMap.get(var1);
      }

      public boolean isComponentEnabled(ComponentType var1) {
         ComponentUiStyle var2 = (ComponentUiStyle)this.componentUiStyleMap.get(var1);
         return var2 != null && var2.isEnabled();
      }

      private EntityUiStyle copy() {
         EntityUiStyle var1 = new EntityUiStyle();
         var1.luaWindowClass = this.luaWindowClass;
         var1.xuiStyle = this.xuiStyle;
         var1.luaCanOpenWindow = this.luaCanOpenWindow;
         var1.luaOpenWindow = this.luaOpenWindow;
         var1.displayName = this.displayName;
         var1.description = this.description;
         var1.buildDescription = this.buildDescription;
         var1.icon = this.icon;
         Iterator var2 = this.componentUiStyleMap.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            var1.componentUiStyleMap.put((ComponentType)var3.getKey(), ((ComponentUiStyle)var3.getValue()).copy());
         }

         return var1;
      }

      private void Load(XuiSkinScript.EntityUiScript var1) {
         if (var1.getLuaWindowClass() != null) {
            this.luaWindowClass = var1.getLuaWindowClass();
         }

         if (var1.getXuiStyle() != null) {
            this.xuiStyle = var1.getXuiStyle();
         }

         if (var1.getLuaCanOpenWindow() != null) {
            this.luaCanOpenWindow = var1.getLuaCanOpenWindow();
         }

         if (var1.getLuaOpenWindow() != null) {
            this.luaOpenWindow = var1.getLuaOpenWindow();
         }

         if (var1.getDisplayName() != null) {
            this.displayName = var1.getDisplayName();
         }

         if (var1.getDescription() != null) {
            this.description = var1.getDescription();
         }

         if (var1.getBuildDescription() != null) {
            this.buildDescription = var1.getBuildDescription();
         }

         if (var1.getIconPath() != null) {
            Texture var2 = Texture.trygetTexture(var1.getIconPath());
            if (var2 != null) {
               this.icon = var2;
            } else {
               DebugLogStream var10000 = DebugLog.General;
               String var10001 = var1.getIconPath();
               var10000.warn("Could not find icon: " + var10001 + ", script = " + var1.getDisplayName());

               assert false;

               if (this.icon == null) {
                  this.icon = Texture.getSharedTexture("media/inventory/Question_On.png");
               }
            }
         }

         if (var1.isClearComponents()) {
            this.componentUiStyleMap.clear();
         }

         this.LoadComponentInfo(var1);
      }

      private void LoadComponentInfo(XuiSkinScript.EntityUiScript var1) {
         Iterator var2 = var1.getComponentUiScriptMap().entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry var3 = (Map.Entry)var2.next();
            ComponentUiStyle var4 = (ComponentUiStyle)this.componentUiStyleMap.get(var3.getKey());
            if (var4 == null) {
               var4 = new ComponentUiStyle();
               this.componentUiStyleMap.put((ComponentType)var3.getKey(), var4);
            }

            if (((XuiSkinScript.ComponentUiScript)var3.getValue()).getLuaPanelClass() != null) {
               var4.luaPanelClass = ((XuiSkinScript.ComponentUiScript)var3.getValue()).getLuaPanelClass();
            }

            if (((XuiSkinScript.ComponentUiScript)var3.getValue()).getXuiStyle() != null) {
               var4.xuiStyle = ((XuiSkinScript.ComponentUiScript)var3.getValue()).getXuiStyle();
            }

            if (((XuiSkinScript.ComponentUiScript)var3.getValue()).getDisplayName() != null) {
               var4.displayName = ((XuiSkinScript.ComponentUiScript)var3.getValue()).getDisplayName();
            }

            if (((XuiSkinScript.ComponentUiScript)var3.getValue()).getIconPath() != null) {
               Texture var5 = Texture.trygetTexture(((XuiSkinScript.ComponentUiScript)var3.getValue()).getIconPath());
               if (var5 != null) {
                  var4.icon = var5;
               } else {
                  DebugLog.General.warn("Could not find icon: " + ((XuiSkinScript.ComponentUiScript)var3.getValue()).getIconPath());

                  assert false;

                  if (var4.icon == null) {
                     var4.icon = Texture.getSharedTexture("media/inventory/Question_On.png");
                  }
               }
            }

            if (((XuiSkinScript.ComponentUiScript)var3.getValue()).isListOrderZ()) {
               var4.listOrderZ = ((XuiSkinScript.ComponentUiScript)var3.getValue()).getListOrderZ();
            }

            if (((XuiSkinScript.ComponentUiScript)var3.getValue()).isEnabledSet()) {
               var4.enabled = ((XuiSkinScript.ComponentUiScript)var3.getValue()).isEnabled();
            }
         }

      }
   }

   public static class ComponentUiStyle {
      private String luaPanelClass;
      private String xuiStyle;
      private String displayName;
      private Texture icon;
      private int listOrderZ = 0;
      private boolean enabled = true;

      public ComponentUiStyle() {
      }

      public String getLuaPanelClass() {
         return this.luaPanelClass;
      }

      public String getXuiStyle() {
         return this.xuiStyle;
      }

      public boolean isEnabled() {
         return this.enabled;
      }

      public Texture getIcon() {
         return this.icon;
      }

      public int getListOrderZ() {
         return this.listOrderZ;
      }

      public String getDisplayName() {
         return Translator.getText(this.displayName);
      }

      private ComponentUiStyle copy() {
         ComponentUiStyle var1 = new ComponentUiStyle();
         var1.luaPanelClass = this.luaPanelClass;
         var1.xuiStyle = this.xuiStyle;
         var1.displayName = this.displayName;
         var1.icon = this.icon;
         var1.listOrderZ = this.listOrderZ;
         var1.enabled = this.enabled;
         return var1;
      }
   }

   private static class StyleInfo {
      private XuiLuaStyle defaultStyle;
      private final Map<String, XuiLuaStyle> styles = new HashMap();

      private StyleInfo() {
      }

      public XuiLuaStyle getDefaultStyle() {
         return this.defaultStyle;
      }

      public XuiLuaStyle getStyle(String var1) {
         return (XuiLuaStyle)this.styles.get(var1);
      }
   }
}
