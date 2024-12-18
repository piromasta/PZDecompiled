package zombie.scripting.entity;

import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

@DebugClassFields
public class GameEntityTemplate extends BaseScriptObject {
   public String name;
   public String body;
   public GameEntityScript script;

   public GameEntityTemplate(ScriptModule var1, String var2, String var3) {
      super(ScriptType.EntityTemplate);
      this.setModule(var1);
      this.name = var2;
      this.body = var3;
   }

   public void Load(String var1, String var2) {
   }

   public GameEntityScript getScript() throws Exception {
      if (this.script == null) {
         this.script = new GameEntityScript();
         this.script.setModule(this.getModule());
         this.script.Load(this.name, this.body);
      }

      return this.script;
   }
}
