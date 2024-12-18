package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;

public class XuiManager {
   private static final String DEFAULT_SKIN_NAME = "default";
   public static EnumSet<ScriptType> XuiScriptTypes;
   private static final Map<String, XuiLayoutScript> layoutScriptsMap;
   private static final Map<String, XuiLayoutScript> stylesScriptsMap;
   private static final Map<String, XuiLayoutScript> defaultStylesScriptsMap;
   private static final ArrayList<XuiScript> combinedList;
   private static final ArrayList<XuiScript> xuiLayoutsList;
   private static final ArrayList<XuiScript> xuiStylesList;
   private static final ArrayList<XuiScript> xuiDefaultStylesList;
   private static final Map<String, XuiScript> xuiLayouts;
   private static final Map<String, XuiScript> xuiStyles;
   private static final Map<String, XuiScript> xuiDefaultStyles;
   private static final Map<String, XuiSkin> xuiSkins;
   private static XuiSkin xuiDefaultSkin;
   private static boolean parseOnce;
   private static boolean hasParsedOnce;

   public XuiManager() {
   }

   public static String getDefaultSkinName() {
      return "default";
   }

   public static ArrayList<XuiScript> GetCombinedScripts() {
      return combinedList;
   }

   public static ArrayList<XuiScript> GetAllLayouts() {
      return xuiLayoutsList;
   }

   public static ArrayList<XuiScript> GetAllStyles() {
      return xuiStylesList;
   }

   public static ArrayList<XuiScript> GetAllDefaultStyles() {
      return xuiDefaultStylesList;
   }

   public static XuiLayoutScript GetLayoutScript(String var0) {
      return var0 == null ? null : (XuiLayoutScript)layoutScriptsMap.get(var0);
   }

   public static XuiLayoutScript GetStyleScript(String var0) {
      return var0 == null ? null : (XuiLayoutScript)stylesScriptsMap.get(var0);
   }

   public static XuiLayoutScript GetDefaultStyleScript(String var0) {
      return var0 == null ? null : (XuiLayoutScript)defaultStylesScriptsMap.get(var0);
   }

   public static XuiScript GetLayout(String var0) {
      return var0 == null ? null : (XuiScript)xuiLayouts.get(var0);
   }

   public static XuiScript GetStyle(String var0) {
      return var0 == null ? null : (XuiScript)xuiStyles.get(var0);
   }

   public static XuiScript GetDefaultStyle(String var0) {
      return var0 == null ? null : (XuiScript)xuiDefaultStyles.get(var0);
   }

   public static XuiSkin GetDefaultSkin() {
      return xuiDefaultSkin;
   }

   public static XuiSkin GetSkin(String var0) {
      XuiSkin var1 = (XuiSkin)xuiSkins.get(var0);
      if (var1 == null) {
         if (var0 != null) {
            DebugLog.General.warn("Skin not found: " + var0);
         }

         var1 = xuiDefaultSkin;
      }

      return var1;
   }

   private static void reset() {
      layoutScriptsMap.clear();
      stylesScriptsMap.clear();
      defaultStylesScriptsMap.clear();
      combinedList.clear();
      xuiLayoutsList.clear();
      xuiStylesList.clear();
      xuiDefaultStylesList.clear();
      xuiLayouts.clear();
      xuiStyles.clear();
      xuiDefaultStyles.clear();
      Iterator var0 = xuiSkins.values().iterator();

      while(var0.hasNext()) {
         XuiSkin var1 = (XuiSkin)var0.next();
         var1.setInvalidated(true);
      }

      xuiSkins.clear();
      xuiDefaultSkin = null;
      XuiLuaStyle.Reset();
   }

   public static void setParseOnce(boolean var0) {
      parseOnce = var0;
      hasParsedOnce = false;
   }

   public static void ParseScripts() throws Exception {
      if (!parseOnce || !hasParsedOnce) {
         hasParsedOnce = true;
         reset();
         ArrayList var0 = ScriptManager.instance.getAllXuiConfigScripts();
         XuiLuaStyle.ReadConfigs(var0);
         ArrayList var1 = ScriptManager.instance.getAllXuiColors();
         ArrayList var2 = ScriptManager.instance.getAllXuiStyles();
         ArrayList var3 = ScriptManager.instance.getAllXuiDefaultStyles();
         ArrayList var4 = ScriptManager.instance.getAllXuiLayouts();
         ArrayList var5 = ScriptManager.instance.getAllXuiSkinScripts();
         Iterator var6 = var1.iterator();

         label124:
         while(var6.hasNext()) {
            XuiColorsScript var7 = (XuiColorsScript)var6.next();
            Iterator var8 = var7.getColorMap().entrySet().iterator();

            while(true) {
               while(true) {
                  if (!var8.hasNext()) {
                     continue label124;
                  }

                  Map.Entry var9 = (Map.Entry)var8.next();
                  if (Colors.GetColorInfo((String)var9.getKey()) != null) {
                     Colors.ColNfo var10 = Colors.GetColorInfo((String)var9.getKey());
                     if (var10.getColorSet() == Colors.ColorSet.Game) {
                        var10.getColor().set((Color)var9.getValue());
                        continue;
                     }
                  }

                  if (Colors.GetColorByName((String)var9.getKey()) == null) {
                     Colors.AddGameColor((String)var9.getKey(), (Color)var9.getValue());
                  } else {
                     DebugLog.General.error("Color '" + (String)var9.getKey() + "' is already defined in Colors.java");
                  }
               }
            }
         }

         XuiLayoutScript var11;
         for(var6 = var3.iterator(); var6.hasNext(); registerLayout(var11)) {
            var11 = (XuiLayoutScript)var6.next();
            if (var11.getName() != null) {
               defaultStylesScriptsMap.put(var11.getName(), var11);
            }
         }

         for(var6 = var2.iterator(); var6.hasNext(); registerLayout(var11)) {
            var11 = (XuiLayoutScript)var6.next();
            if (var11.getName() != null) {
               stylesScriptsMap.put(var11.getName(), var11);
            }
         }

         for(var6 = var4.iterator(); var6.hasNext(); registerLayout(var11)) {
            var11 = (XuiLayoutScript)var6.next();
            if (var11.getName() != null) {
               layoutScriptsMap.put(var11.getName(), var11);
            }
         }

         var6 = var3.iterator();

         while(var6.hasNext()) {
            var11 = (XuiLayoutScript)var6.next();
            parseLayout(var11);
         }

         var6 = var2.iterator();

         while(var6.hasNext()) {
            var11 = (XuiLayoutScript)var6.next();
            parseLayout(var11);
         }

         var6 = var4.iterator();

         while(var6.hasNext()) {
            var11 = (XuiLayoutScript)var6.next();
            parseLayout(var11);
         }

         combinedList.addAll(xuiLayoutsList);
         combinedList.addAll(xuiStylesList);
         combinedList.addAll(xuiDefaultStylesList);
         var6 = var5.iterator();

         while(var6.hasNext()) {
            XuiSkinScript var13 = (XuiSkinScript)var6.next();
            String var12 = var13.getScriptObjectName();
            if (!var13.getModule().getName().equals("Base")) {
               DebugLog.General.warn("XuiSkin '" + var13.getScriptObjectFullType() + "' ignored, skin needs to be module Base.");
            } else {
               XuiSkin var14 = new XuiSkin(var12, var13);
               xuiSkins.put(var12, var14);
            }
         }

         var6 = xuiSkins.values().iterator();

         while(var6.hasNext()) {
            XuiSkin var15 = (XuiSkin)var6.next();
            var15.Load();
         }

         xuiDefaultSkin = GetSkin("default");
      }
   }

   private static void registerLayout(XuiLayoutScript var0) {
      try {
         var0.preParse();
         XuiScript var1 = var0.getXuiScript();
         if (var1 != null) {
            if (var1.getScriptType() == XuiScriptType.Layout) {
               xuiLayoutsList.add(var1);
               xuiLayouts.put(var0.getName(), var1);
            } else if (var1.getScriptType() == XuiScriptType.Style) {
               xuiStylesList.add(var1);
               xuiStyles.put(var0.getName(), var1);
            } else if (var1.getScriptType() == XuiScriptType.DefaultStyle) {
               xuiDefaultStylesList.add(var1);
               xuiDefaultStyles.put(var0.getName(), var1);
            }
         } else {
            DebugLog.General.error("No XuiScript in XuiConfig: " + var0.getName());
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   private static void parseLayout(XuiLayoutScript var0) {
      try {
         var0.parseScript();
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   static {
      XuiScriptTypes = EnumSet.of(ScriptType.XuiConfig, ScriptType.XuiLayout, ScriptType.XuiStyle, ScriptType.XuiDefaultStyle, ScriptType.XuiColor, ScriptType.XuiSkin);
      layoutScriptsMap = new HashMap();
      stylesScriptsMap = new HashMap();
      defaultStylesScriptsMap = new HashMap();
      combinedList = new ArrayList();
      xuiLayoutsList = new ArrayList();
      xuiStylesList = new ArrayList();
      xuiDefaultStylesList = new ArrayList();
      xuiLayouts = new HashMap();
      xuiStyles = new HashMap();
      xuiDefaultStyles = new HashMap();
      xuiSkins = new HashMap();
      parseOnce = false;
      hasParsedOnce = false;
   }
}
