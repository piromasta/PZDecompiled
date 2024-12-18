package zombie.scripting.entity;

import zombie.core.Core;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@DebugClassFields
public abstract class ComponentScript extends BaseScriptObject {
   public final ComponentType type;

   protected ComponentScript(ComponentType var1) {
      super(ScriptType.EntityComponent);
      this.type = var1;
      this.InitLoadPP(var1.toString());
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public String getName() {
      if (!Core.bDebug || this.getParent() != null && this.getParent().getScriptObjectName() != null) {
         return this.getParent() != null ? this.getParent().getScriptObjectName() : "UnknownScriptName";
      } else {
         throw new RuntimeException("Parent is null or parent name is null.");
      }
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   protected abstract <T extends ComponentScript> void copyFrom(T var1);

   protected void load(ScriptParser.Block var1) throws Exception {
   }

   protected boolean parseKeyValue(String var1, String var2) {
      throw new RuntimeException("'parseKeyValue' not implemented for " + this.getClass());
   }
}
