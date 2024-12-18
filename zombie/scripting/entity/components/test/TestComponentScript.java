package zombie.scripting.entity.components.test;

import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

public class TestComponentScript extends ComponentScript {
   private TestComponentScript() {
      super(ComponentType.TestComponent);
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
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
         if (!var4.isEmpty() && !var5.isEmpty() && !var4.equalsIgnoreCase("someKey") && var4.equalsIgnoreCase("someOtherKey")) {
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
