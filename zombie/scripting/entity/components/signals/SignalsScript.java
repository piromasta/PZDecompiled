package zombie.scripting.entity.components.signals;

import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

public class SignalsScript extends ComponentScript {
   private SignalsScript() {
      super(ComponentType.Signals);
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
   }
}
