package zombie.scripting.entity.components.attributes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.attributes.AttributeType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

@DebugClassFields
public class AttributesScript extends ComponentScript {
   private final HashMap<String, String> kvPairs = new HashMap();
   private AttributeContainer container = null;
   private boolean hasCreatedContainer = false;

   private AttributesScript() {
      super(ComponentType.Attributes);
   }

   public void PreReload() {
      this.kvPairs.clear();
      this.container = null;
      this.hasCreatedContainer = false;
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      this.createTemplateContainer();
   }

   private void createTemplateContainer() {
      if (!this.hasCreatedContainer) {
         if (this.kvPairs.size() > 0) {
            try {
               this.container = (AttributeContainer)ComponentType.Attributes.CreateComponent();
               Iterator var1 = this.kvPairs.entrySet().iterator();

               label32:
               while(true) {
                  Map.Entry var2;
                  AttributeType var3;
                  do {
                     if (!var1.hasNext()) {
                        break label32;
                     }

                     var2 = (Map.Entry)var1.next();
                     var3 = Attribute.TypeFromName((String)var2.getKey());
                  } while(var3 != null && this.container.putFromScript(var3, (String)var2.getValue()));

                  if (Core.bDebug) {
                     throw new RuntimeException("Attribute '" + var3 + "' could not be added.");
                  }

                  DebugLog.General.error("WARNING: Item - > Attribute '" + var3 + "' could not be added.");
               }
            } catch (Exception var4) {
               var4.printStackTrace();
               this.container = null;
            }
         }

         this.hasCreatedContainer = true;
      }
   }

   public AttributeContainer getTemplateContainer() {
      this.createTemplateContainer();
      return this.container;
   }

   protected void copyFrom(ComponentScript var1) {
      AttributesScript var2 = (AttributesScript)var1;
      Iterator var3 = var2.kvPairs.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         this.kvPairs.put((String)var4.getKey(), (String)var4.getValue());
      }

   }

   protected void load(ScriptParser.Block var1) throws Exception {
      super.load(var1);
      Iterator var2 = var1.values.iterator();

      while(var2.hasNext()) {
         ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
         String var4 = var3.getKey().trim();
         String var5 = var3.getValue().trim();
         this.parseKeyValue(var4, var5);
      }

   }

   protected boolean parseKeyValue(String var1, String var2) {
      AttributeType var3 = Attribute.TypeFromName(var1);
      if (var3 != null) {
         this.kvPairs.put(var1, var2);
         return true;
      } else {
         DebugLog.General.error("Unknown attribute, key = " + var1 + ", value = " + var2);
         return false;
      }
   }
}
