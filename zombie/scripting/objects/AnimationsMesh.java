package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

public final class AnimationsMesh extends BaseScriptObject {
   public String meshFile = null;
   public boolean bKeepMeshAnimations = false;
   public String postProcess = null;
   public final ArrayList<String> animationDirectories = new ArrayList();
   public final ArrayList<String> animationPrefixes = new ArrayList();
   public ModelMesh modelMesh = null;

   public AnimationsMesh() {
      super(ScriptType.AnimationMesh);
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.values.iterator();

      while(var4.hasNext()) {
         ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
         String var6 = var5.getKey().trim();
         String var7 = var5.getValue().trim();
         if ("meshFile".equalsIgnoreCase(var6)) {
            this.meshFile = var7;
         } else if ("animationDirectory".equalsIgnoreCase(var6)) {
            this.animationDirectories.add(var7);
         } else if ("animationPrefix".equalsIgnoreCase(var6)) {
            this.animationPrefixes.add(var7);
         } else if ("keepMeshAnimations".equalsIgnoreCase(var6)) {
            this.bKeepMeshAnimations = StringUtils.tryParseBoolean(var7);
         } else if ("postProcess".equalsIgnoreCase(var6)) {
            this.postProcess = var7;
         }
      }

   }

   public void reset() {
      this.meshFile = null;
      this.animationDirectories.clear();
      this.animationPrefixes.clear();
      this.modelMesh = null;
   }
}
