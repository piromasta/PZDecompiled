package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.scripting.ScriptParser;

public final class AnimationsMesh extends BaseScriptObject {
   public String name = null;
   public String meshFile = null;
   public final ArrayList<String> animationDirectories = new ArrayList();
   public ModelMesh modelMesh = null;

   public AnimationsMesh() {
   }

   public void Load(String var1, String var2) {
      this.name = var1;
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      Iterator var4 = var3.values.iterator();

      while(var4.hasNext()) {
         ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
         String var6 = var5.getKey().trim();
         String var7 = var5.getValue().trim();
         if ("meshFile".equalsIgnoreCase(var6)) {
            this.meshFile = var7;
         } else if ("animationDirectory".equalsIgnoreCase(var6)) {
            this.animationDirectories.add(var7);
         }
      }

   }

   public void reset() {
      this.meshFile = null;
      this.animationDirectories.clear();
      this.modelMesh = null;
   }
}
