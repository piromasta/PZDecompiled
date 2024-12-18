package zombie.scripting.ui;

import java.util.Iterator;
import zombie.scripting.ScriptParser;

public class XuiReference extends XuiScript {
   public final XuiScript.XuiString layout = (XuiScript.XuiString)this.addVar(new XuiScript.XuiString(this, "layout"));
   public final XuiScript.XuiBoolean dynamic;
   private XuiScript referenceScript;

   public XuiReference(String var1, boolean var2) {
      super(var1, var2, "Reference", XuiScriptType.Reference);
      this.layout.setIgnoreStyling(true);
      this.layout.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.dynamic = (XuiScript.XuiBoolean)this.addVar(new XuiScript.XuiBoolean(this, "dynamic"));
      this.dynamic.setIgnoreStyling(true);
      this.dynamic.setAutoApplyMode(XuiAutoApply.Forbidden);
   }

   public XuiScript getReferenceLayout() {
      return this.referenceScript;
   }

   public XuiScript.XuiString getLayout() {
      return this.layout;
   }

   public XuiScript.XuiBoolean getDynamic() {
      return this.dynamic;
   }

   public void Load(ScriptParser.Block var1) {
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         if (!var4.isEmpty() && !var5.isEmpty()) {
            if (this.layout.acceptsKey(var4)) {
               this.layout.fromString(var5);
            } else if (this.dynamic.acceptsKey(var4)) {
               this.dynamic.fromString(var5);
            }
         }
      }

      if (this.layout.value() != null && !(Boolean)this.dynamic.value()) {
         XuiScript var6 = XuiManager.GetLayout((String)this.layout.value());
         if (var6 != null) {
            this.referenceScript = var6;
         }
      }

   }

   public void setStyle(XuiScript var1) {
      this.warnWithInfo("Cannot set style on style.");
   }

   public void setDefaultStyle(XuiScript var1) {
      this.warnWithInfo("Cannot set style on style.");
   }

   public void addChild(XuiScript var1) {
      this.warnWithInfo("Cannot add children to style.");
   }
}
