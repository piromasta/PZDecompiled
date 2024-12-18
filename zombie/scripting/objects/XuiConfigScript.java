package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.ui.XuiLuaStyle;
import zombie.scripting.ui.XuiVarType;
import zombie.util.StringUtils;

public class XuiConfigScript extends BaseScriptObject {
   private final Map<XuiVarType, ArrayList<String>> varConfigs = new HashMap();

   public XuiConfigScript() {
      super(ScriptType.XuiConfig);
      Iterator var1 = XuiLuaStyle.AllowedVarTypes.iterator();

      while(var1.hasNext()) {
         XuiVarType var2 = (XuiVarType)var1.next();
         this.varConfigs.put(var2, new ArrayList());
      }

   }

   public Map<XuiVarType, ArrayList<String>> getVarConfigs() {
      return this.varConfigs;
   }

   public void reset() {
      Iterator var1 = this.varConfigs.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         ((ArrayList)var2.getValue()).clear();
      }

   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.children.iterator();

      while(var4.hasNext()) {
         ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
         XuiVarType var6 = XuiVarType.valueOf(var5.type);
         if (!XuiLuaStyle.AllowedVarTypes.contains(var6)) {
            throw new Exception("VarType not allowed: " + var6);
         }

         this.LoadVarTypeBlock(var6, var5);
      }

   }

   private void LoadVarTypeBlock(XuiVarType var1, ScriptParser.Block var2) throws Exception {
      Iterator var3 = var2.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         if (!StringUtils.isNullOrWhitespace(var4.string)) {
            String var5 = var4.string.trim();
            if (this.otherTypesContainsKey(var5, var1)) {
               throw new Exception("Key '" + var5 + "' duplicate from another value block. this var type = " + var1);
            }

            ArrayList var6 = (ArrayList)this.varConfigs.get(var1);
            if (!var6.contains(var5)) {
               var6.add(var5);
            }
         }
      }

   }

   private boolean otherTypesContainsKey(String var1, XuiVarType var2) {
      Iterator var3 = this.varConfigs.entrySet().iterator();

      Map.Entry var4;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         var4 = (Map.Entry)var3.next();
      } while(var4.getKey() == var2 || !((ArrayList)var4.getValue()).contains(var1));

      DebugLog.General.warn("Duplicate key '" + var1 + "' in var type: " + var4.getKey());
      return true;
   }

   public void PreReload() {
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }
}
