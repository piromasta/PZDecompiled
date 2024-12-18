package zombie.scripting.objects;

import zombie.scripting.ScriptType;

public final class VehicleTemplate extends BaseScriptObject {
   public String name;
   public String body;
   public VehicleScript script;

   public VehicleTemplate(ScriptModule var1, String var2, String var3) {
      super(ScriptType.VehicleTemplate);
      this.setModule(var1);
      this.name = var2;
      this.body = var3;
   }

   public VehicleScript getScript() {
      if (this.script == null) {
         this.script = new VehicleScript();
         this.script.setModule(this.getModule());

         try {
            this.script.Load(this.name, this.body);
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }

      return this.script;
   }
}
