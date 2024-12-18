package zombie.scripting.objects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Color;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public class XuiColorsScript extends BaseScriptObject {
   private String name;
   private final Map<String, Color> colorMap = new HashMap();

   public XuiColorsScript() {
      super(ScriptType.XuiColor);
   }

   public String getName() {
      return this.name;
   }

   public Map<String, Color> getColorMap() {
      return this.colorMap;
   }

   public void Load(String var1, String var2) throws Exception {
      this.name = var1;
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      this.LoadColorsBlock(var3);
   }

   protected void LoadColorsBlock(ScriptParser.Block var1) throws Exception {
      Iterator var2 = var1.values.iterator();

      while(true) {
         String var4;
         String var5;
         do {
            do {
               if (!var2.hasNext()) {
                  return;
               }

               ScriptParser.Value var3 = (ScriptParser.Value)var2.next();
               var4 = var3.getKey().trim();
               var5 = var3.getValue().trim();
            } while(var4.isEmpty());
         } while(var5.isEmpty());

         Color var6 = new Color();
         String[] var7 = var5.split(":");
         int var8;
         if (var7.length > 1 && var7[0].trim().equalsIgnoreCase("rgb")) {
            for(var8 = 1; var8 < var7.length; ++var8) {
               switch (var8) {
                  case 1:
                     var6.r = Float.parseFloat(var7[var8].trim()) / 255.0F;
                     break;
                  case 2:
                     var6.g = Float.parseFloat(var7[var8].trim()) / 255.0F;
                     break;
                  case 3:
                     var6.b = Float.parseFloat(var7[var8].trim()) / 255.0F;
                     break;
                  case 4:
                     var6.a = Float.parseFloat(var7[var8].trim()) / 255.0F;
               }
            }
         } else {
            for(var8 = 0; var8 < var7.length; ++var8) {
               switch (var8) {
                  case 0:
                     var6.r = Float.parseFloat(var7[var8].trim());
                     break;
                  case 1:
                     var6.g = Float.parseFloat(var7[var8].trim());
                     break;
                  case 2:
                     var6.b = Float.parseFloat(var7[var8].trim());
                     break;
                  case 3:
                     var6.a = Float.parseFloat(var7[var8].trim());
               }
            }
         }

         this.colorMap.put(var4, var6);
      }
   }
}
