package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.objects.XuiConfigScript;
import zombie.ui.UIFont;

public class XuiLuaStyle {
   public static EnumSet<XuiVarType> AllowedVarTypes;
   private static final Map<String, XuiVar<?, ?>> varRegisteryMap;
   private final String xuiLuaClass;
   private final String xuiStyleName;
   protected XuiSkin xuiSkin;
   protected HashMap<String, XuiVar<?, ?>> varsMap = new HashMap();
   protected ArrayList<XuiVar<?, ?>> vars = new ArrayList();

   private static void addStaticVar(XuiVar<?, ?> var0) {
      if (var0 == null) {
         throw new RuntimeException("Var is null");
      } else if (var0.getLuaTableKey() == null) {
         throw new RuntimeException("Var key is null");
      } else if (varRegisteryMap.containsKey(var0.getLuaTableKey())) {
         throw new RuntimeException("Key already exists: " + var0.getLuaTableKey());
      } else {
         varRegisteryMap.put(var0.getLuaTableKey(), var0);
      }
   }

   private static XuiVar<?, ?> getStaticVar(String var0) {
      return (XuiVar)varRegisteryMap.get(var0);
   }

   public static void ReadConfigs(ArrayList<XuiConfigScript> var0) throws Exception {
      HashMap var1 = new HashMap();
      Iterator var2 = AllowedVarTypes.iterator();

      while(var2.hasNext()) {
         XuiVarType var3 = (XuiVarType)var2.next();
         var1.put(var3, new HashSet());
      }

      var2 = var0.iterator();

      while(var2.hasNext()) {
         XuiConfigScript var4 = (XuiConfigScript)var2.next();
         parseConfig(var1, var4);
      }

   }

   private static void parseConfig(Map<XuiVarType, HashSet<String>> var0, XuiConfigScript var1) throws Exception {
      Map var2 = var1.getVarConfigs();
      Iterator var3 = var2.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         XuiVarType var5 = (XuiVarType)var4.getKey();
         if (!AllowedVarTypes.contains(var5)) {
            throw new Exception("Var type not allowed: " + var5);
         }

         Iterator var6 = ((ArrayList)var4.getValue()).iterator();

         while(var6.hasNext()) {
            String var7 = (String)var6.next();
            if (otherTypesContainsKey(var0, var7, var5)) {
               throw new Exception("Duplicate key '" + var7 + "' in var type: " + var4.getKey() + ", and type: " + var5);
            }

            ((HashSet)var0.get(var5)).add(var7);
            if (!varRegisteryMap.containsKey(var7)) {
               switch (var5) {
                  case String:
                     addStaticVar(new XuiString((XuiLuaStyle)null, var7));
                     break;
                  case StringList:
                     addStaticVar(new XuiStringList((XuiLuaStyle)null, var7));
                     break;
                  case TranslateString:
                     addStaticVar(new XuiTranslateString((XuiLuaStyle)null, var7));
                     break;
                  case Double:
                     addStaticVar(new XuiDouble((XuiLuaStyle)null, var7));
                     break;
                  case Boolean:
                     addStaticVar(new XuiBoolean((XuiLuaStyle)null, var7));
                     break;
                  case FontType:
                     addStaticVar(new XuiFontType((XuiLuaStyle)null, var7));
                     break;
                  case Color:
                     addStaticVar(new XuiColor((XuiLuaStyle)null, var7));
                     break;
                  case Texture:
                     addStaticVar(new XuiTexture((XuiLuaStyle)null, var7));
                     break;
                  default:
                     throw new Exception("No handler for: " + var5);
               }
            }
         }
      }

   }

   private static boolean otherTypesContainsKey(Map<XuiVarType, HashSet<String>> var0, String var1, XuiVarType var2) throws Exception {
      Iterator var3 = var0.entrySet().iterator();

      Map.Entry var4;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         var4 = (Map.Entry)var3.next();
      } while(var4.getKey() == var2 || !((HashSet)var4.getValue()).contains(var1));

      return true;
   }

   public static void Reset() {
      varRegisteryMap.clear();
   }

   protected XuiLuaStyle(String var1, String var2) {
      this.xuiLuaClass = var1;
      this.xuiStyleName = var2;
   }

   public String getXuiLuaClass() {
      return this.xuiLuaClass;
   }

   public String getXuiStyleName() {
      return this.xuiStyleName;
   }

   public XuiVar<?, ?> getVar(String var1) {
      return (XuiVar)this.varsMap.get(var1);
   }

   private void addVar(String var1, XuiVar<?, ?> var2) {
      if (var1 == null) {
         throw new RuntimeException("Key is null");
      } else if (var2 == null) {
         throw new RuntimeException("Var is null");
      } else if (var2.getLuaTableKey() == null) {
         throw new RuntimeException("Var key is null");
      } else if (!this.varsMap.containsKey(var2.getLuaTableKey()) && !this.vars.contains(var2)) {
         this.varsMap.put(var1, var2);
         this.vars.add(var2);
      } else {
         throw new RuntimeException("Var already added: " + var2.getLuaTableKey());
      }
   }

   public ArrayList<XuiVar<?, ?>> getVars() {
      return this.vars;
   }

   public boolean loadVar(String var1, String var2) throws Exception {
      XuiVar var3 = (XuiVar)this.varsMap.get(var1);
      if (var3 == null) {
         XuiVar var4 = (XuiVar)varRegisteryMap.get(var1);
         if (var4 == null || !var4.acceptsKey(var1)) {
            this.logInfo();
            throw new Exception("Variable '" + var1 + "' is not registered or key typo. [registered=" + var4 + "]");
         }

         var3 = var4.copy(this);
         this.addVar(var1, var3);
      }

      if (var2 != null && var3.acceptsKey(var1)) {
         return var3.load(var1, var2);
      } else if (var2 == null && var3.acceptsKey(var1)) {
         var3.setValue((Object)null);
         return true;
      } else {
         return false;
      }
   }

   public void copyVarsFrom(XuiLuaStyle var1) {
      this.vars.clear();
      this.varsMap.clear();

      for(int var2 = 0; var2 < var1.vars.size(); ++var2) {
         XuiVar var3 = (XuiVar)var1.vars.get(var2);
         XuiVar var4 = var3.copy(this);
         this.addVar(var4.getLuaTableKey(), var4);
      }

   }

   public String toString() {
      String var1 = super.toString();
      return "XuiLuaStyle [class=" + this.xuiLuaClass + ", styleName=" + this.xuiStyleName + ",  u=" + var1 + "]";
   }

   protected void logWithInfo(String var1) {
      DebugLog.General.debugln(var1);
      this.logInfo();
   }

   protected void warnWithInfo(String var1) {
      DebugLog.General.debugln(var1);
      this.logInfo();
   }

   protected void errorWithInfo(String var1) {
      DebugLog.General.error(var1);
      this.logInfo();
   }

   private void logInfo() {
      DebugLog.log(this.toString());
   }

   protected void debugPrint(String var1) {
      Iterator var2 = this.vars.iterator();

      while(var2.hasNext()) {
         XuiVar var3 = (XuiVar)var2.next();
         DebugLog.log(var1 + "-> " + var3.getLuaTableKey() + " = " + var3.getValueString());
      }

   }

   static {
      AllowedVarTypes = EnumSet.of(XuiVarType.String, XuiVarType.StringList, XuiVarType.TranslateString, XuiVarType.Double, XuiVarType.Boolean, XuiVarType.FontType, XuiVarType.Color, XuiVarType.Texture);
      varRegisteryMap = new HashMap();
   }

   public abstract static class XuiVar<T, C extends XuiVar<?, ?>> {
      private int uiOrder;
      protected final XuiVarType type;
      protected final XuiLuaStyle parent;
      protected boolean valueSet;
      protected XuiAutoApply autoApply;
      protected T defaultValue;
      protected T value;
      protected final String luaTableKey;

      protected XuiVar(XuiVarType var1, XuiLuaStyle var2, String var3) {
         this(var1, var2, var3, (Object)null);
      }

      protected XuiVar(XuiVarType var1, XuiLuaStyle var2, String var3, T var4) {
         this.uiOrder = 1000;
         this.valueSet = false;
         this.autoApply = XuiAutoApply.IfSet;
         this.type = (XuiVarType)Objects.requireNonNull(var1);
         this.parent = var2;
         this.luaTableKey = (String)Objects.requireNonNull(var3);
         this.defaultValue = var4;
      }

      protected abstract XuiVar<T, C> copy(XuiLuaStyle var1);

      protected XuiVar<T, C> copyValuesTo(XuiVar<T, C> var1) {
         var1.uiOrder = this.uiOrder;
         var1.valueSet = this.valueSet;
         var1.autoApply = this.autoApply;
         var1.defaultValue = this.defaultValue;
         var1.value = this.value;
         return var1;
      }

      public XuiVarType getType() {
         return this.type;
      }

      public int setUiOrder(int var1) {
         this.uiOrder = var1;
         return this.uiOrder;
      }

      public int getUiOrder() {
         return this.uiOrder;
      }

      protected void setDefaultValue(T var1) {
         this.defaultValue = var1;
      }

      protected T getDefaultValue() {
         return this.defaultValue;
      }

      public void setValue(T var1) {
         this.value = var1;
         this.valueSet = true;
      }

      public void setAutoApplyMode(XuiAutoApply var1) {
         this.autoApply = var1;
      }

      public XuiAutoApply getAutoApplyMode() {
         return this.autoApply;
      }

      public String getLuaTableKey() {
         return this.luaTableKey;
      }

      protected String getScriptKey() {
         return this.luaTableKey;
      }

      public boolean isValueSet() {
         return this.valueSet;
      }

      public T value() {
         return this.valueSet ? this.value : this.defaultValue;
      }

      public String getValueString() {
         return this.value() != null ? this.value().toString() : "null";
      }

      protected boolean acceptsKey(String var1) {
         return this.luaTableKey.equals(var1);
      }

      protected abstract void fromString(String var1);

      protected boolean load(String var1, String var2) {
         if (this.acceptsKey(var1)) {
            this.fromString(var2);
            return true;
         } else {
            return false;
         }
      }
   }

   public static class XuiString extends XuiVar<String, XuiString> {
      protected XuiString(XuiLuaStyle var1, String var2) {
         super(XuiVarType.String, var1, var2);
      }

      protected XuiString(XuiLuaStyle var1, String var2, String var3) {
         super(XuiVarType.String, var1, var2, var3);
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }

      protected XuiString copy(XuiLuaStyle var1) {
         XuiString var2 = new XuiString(var1, this.luaTableKey, (String)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }

   public static class XuiStringList extends XuiVar<ArrayList<String>, XuiStringList> {
      protected XuiStringList(XuiLuaStyle var1, String var2) {
         super(XuiVarType.StringList, var1, var2, new ArrayList());
      }

      protected XuiStringList(XuiLuaStyle var1, String var2, ArrayList<String> var3) {
         super(XuiVarType.StringList, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            String[] var2 = var1.split(":");
            ArrayList var3 = new ArrayList(var2.length);

            for(int var4 = 0; var4 < var2.length; ++var4) {
               var3.add(var2[var4].trim());
            }

            this.setValue(var3);
         } catch (Exception var5) {
            this.parent.logInfo();
            var5.printStackTrace();
         }

      }

      protected XuiStringList copy(XuiLuaStyle var1) {
         XuiStringList var2 = new XuiStringList(var1, this.luaTableKey, (ArrayList)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }

   public static class XuiTranslateString extends XuiVar<String, XuiTranslateString> {
      protected XuiTranslateString(XuiLuaStyle var1, String var2) {
         super(XuiVarType.TranslateString, var1, var2);
      }

      protected XuiTranslateString(XuiLuaStyle var1, String var2, String var3) {
         super(XuiVarType.TranslateString, var1, var2, var3);
      }

      public String value() {
         return super.value() == null ? null : Translator.getText((String)super.value());
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }

      public String getValueString() {
         return super.value() != null ? (String)super.value() : "null";
      }

      protected XuiTranslateString copy(XuiLuaStyle var1) {
         XuiTranslateString var2 = new XuiTranslateString(var1, this.luaTableKey, (String)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }

   public static class XuiDouble extends XuiVar<Double, XuiDouble> {
      protected XuiDouble(XuiLuaStyle var1, String var2) {
         super(XuiVarType.Double, var1, var2, 0.0);
      }

      protected XuiDouble(XuiLuaStyle var1, String var2, double var3) {
         super(XuiVarType.Double, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(Double.parseDouble(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }

      protected XuiDouble copy(XuiLuaStyle var1) {
         XuiDouble var2 = new XuiDouble(var1, this.luaTableKey, (Double)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }

   public static class XuiBoolean extends XuiVar<Boolean, XuiBoolean> {
      protected XuiBoolean(XuiLuaStyle var1, String var2) {
         super(XuiVarType.Boolean, var1, var2, false);
      }

      protected XuiBoolean(XuiLuaStyle var1, String var2, boolean var3) {
         super(XuiVarType.Boolean, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            this.setValue(Boolean.parseBoolean(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }

      protected XuiBoolean copy(XuiLuaStyle var1) {
         XuiBoolean var2 = new XuiBoolean(var1, this.luaTableKey, (Boolean)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }

   public static class XuiFontType extends XuiVar<UIFont, XuiFontType> {
      protected XuiFontType(XuiLuaStyle var1, String var2) {
         super(XuiVarType.FontType, var1, var2, UIFont.Small);
      }

      protected XuiFontType(XuiLuaStyle var1, String var2, UIFont var3) {
         super(XuiVarType.FontType, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            if (var1.startsWith("UIFont.")) {
               var1 = var1.substring(var1.indexOf(".") + 1);
            }

            this.setValue(UIFont.valueOf(var1));
         } catch (Exception var3) {
            this.parent.logInfo();
            var3.printStackTrace();
         }

      }

      protected XuiFontType copy(XuiLuaStyle var1) {
         XuiFontType var2 = new XuiFontType(var1, this.luaTableKey, (UIFont)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }

   public static class XuiColor extends XuiVar<Color, XuiColor> {
      protected XuiColor(XuiLuaStyle var1, String var2) {
         super(XuiVarType.Color, var1, var2);
      }

      protected XuiColor(XuiLuaStyle var1, String var2, Color var3) {
         super(XuiVarType.Color, var1, var2, var3);
      }

      protected void fromString(String var1) {
         try {
            Color var2 = null;
            if (this.parent.xuiSkin != null) {
               var2 = this.parent.xuiSkin.color(var1);
            }

            if (var2 == null) {
               var2 = Colors.GetColorByName(var1);
            }

            if (var2 == null && var1.contains(":")) {
               var2 = new Color();
               String[] var3 = var1.split(":");
               if (var3.length < 3) {
                  this.parent.errorWithInfo("Warning color has <3 values. color: " + var1);
               }

               int var4;
               if (var3.length > 1 && var3[0].trim().equalsIgnoreCase("rgb")) {
                  for(var4 = 1; var4 < var3.length; ++var4) {
                     switch (var4) {
                        case 1:
                           var2.r = Float.parseFloat(var3[var4].trim()) / 255.0F;
                           break;
                        case 2:
                           var2.g = Float.parseFloat(var3[var4].trim()) / 255.0F;
                           break;
                        case 3:
                           var2.b = Float.parseFloat(var3[var4].trim()) / 255.0F;
                           break;
                        case 4:
                           var2.a = Float.parseFloat(var3[var4].trim()) / 255.0F;
                     }
                  }
               } else {
                  for(var4 = 0; var4 < var3.length; ++var4) {
                     switch (var4) {
                        case 0:
                           var2.r = Float.parseFloat(var3[var4].trim());
                           break;
                        case 1:
                           var2.g = Float.parseFloat(var3[var4].trim());
                           break;
                        case 2:
                           var2.b = Float.parseFloat(var3[var4].trim());
                           break;
                        case 3:
                           var2.a = Float.parseFloat(var3[var4].trim());
                     }
                  }
               }
            }

            if (var2 == null) {
               throw new Exception("Could not read color: " + var1);
            }

            this.setValue(var2);
         } catch (Exception var5) {
            if (Core.bDebug) {
               this.parent.logInfo();
               var5.printStackTrace();
            } else {
               DebugLog.General.warn("Could not read color: " + var1);
            }
         }

      }

      public float getR() {
         return this.value() != null ? ((Color)this.value()).r : 1.0F;
      }

      public float getG() {
         return this.value() != null ? ((Color)this.value()).g : 1.0F;
      }

      public float getB() {
         return this.value() != null ? ((Color)this.value()).b : 1.0F;
      }

      public float getA() {
         return this.value() != null ? ((Color)this.value()).a : 1.0F;
      }

      public String getValueString() {
         float var10000 = this.getR();
         return "" + var10000 + ", " + this.getG() + ", " + this.getB() + ", " + this.getA();
      }

      protected XuiColor copy(XuiLuaStyle var1) {
         XuiColor var2 = new XuiColor(var1, this.luaTableKey, (Color)this.defaultValue);
         this.copyValuesTo(var2);
         if (this.value != null) {
            var2.value = new Color((Color)this.value);
         }

         return var2;
      }
   }

   public static class XuiTexture extends XuiVar<String, XuiTexture> {
      protected XuiTexture(XuiLuaStyle var1, String var2) {
         super(XuiVarType.Texture, var1, var2);
      }

      protected XuiTexture(XuiLuaStyle var1, String var2, String var3) {
         super(XuiVarType.Texture, var1, var2, var3);
      }

      public Texture getTexture() {
         if (this.value() != null) {
            Texture var1 = Texture.getSharedTexture((String)this.value());
            if (var1 != null) {
               return var1;
            }

            if (Core.bDebug) {
               DebugLog.General.warn("Could not find texture for: " + (String)this.value());
            }
         }

         return null;
      }

      protected void fromString(String var1) {
         this.setValue(var1);
      }

      protected XuiTexture copy(XuiLuaStyle var1) {
         XuiTexture var2 = new XuiTexture(var1, this.luaTableKey, (String)this.defaultValue);
         this.copyValuesTo(var2);
         return var2;
      }
   }
}
