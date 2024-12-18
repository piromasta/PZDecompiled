package zombie.scripting.objects;

import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.ui.XuiScript;
import zombie.scripting.ui.XuiScriptType;

public class XuiLayoutScript extends BaseScriptObject {
   private XuiScript xuiScript;
   private String name;
   private String totalFile;
   private boolean hasParsed = false;
   private final XuiScriptType scriptType;
   private ScriptParser.Block block;

   public XuiLayoutScript(ScriptType var1, XuiScriptType var2) {
      super(var1);
      this.scriptType = var2;
   }

   public String getName() {
      return this.name;
   }

   public XuiScriptType getScriptType() {
      return this.scriptType;
   }

   public void Load(String var1, String var2) throws Exception {
      this.hasParsed = false;
      this.totalFile = var2;
      this.name = var1;
      super.LoadCommonBlock(var2);
   }

   public void preParse() {
      this.block = ScriptParser.parse(this.totalFile);
      this.block = (ScriptParser.Block)this.block.children.get(0);
      String var1 = XuiScript.ReadLuaClassValue(this.block);
      this.xuiScript = XuiScript.CreateScriptForClass(this.name, var1, true, this.scriptType);
   }

   public void parseScript() {
      if (!this.hasParsed) {
         this.hasParsed = true;
         this.xuiScript.Load(this.block);
         this.block = null;
      }
   }

   public XuiScript getXuiScript() {
      return this.xuiScript;
   }
}
