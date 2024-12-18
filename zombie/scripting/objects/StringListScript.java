package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@DebugClassFields
public class StringListScript extends BaseScriptObject {
   private final ArrayList<String> values = new ArrayList();

   protected StringListScript() {
      super(ScriptType.StringList);
   }

   public ArrayList<String> getValues() {
      return this.values;
   }

   public boolean getObsolete() {
      return false;
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
         String var6 = var5.string;
         if (!StringUtils.isNullOrWhitespace(var6)) {
            var6 = var6.trim();
            if (this.values.contains(var6)) {
               DebugLog.General.warn("Stringlist <" + var1 + "> double string entry: " + var6);
            } else {
               this.values.add(var6);
            }
         }
      }

   }

   public void reset() {
      this.values.clear();
   }

   public void PreReload() {
      this.values.clear();
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }
}
