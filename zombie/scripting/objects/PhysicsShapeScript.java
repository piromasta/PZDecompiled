package zombie.scripting.objects;

import java.util.Iterator;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public class PhysicsShapeScript extends BaseScriptObject {
   public String meshName;
   public final Vector3f translate = new Vector3f();
   public final Vector3f rotate = new Vector3f();
   public float scale = 1.0F;
   public String postProcess = null;
   public boolean bAllMeshes = false;

   protected PhysicsShapeScript() {
      super(ScriptType.PhysicsShape);
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.children.iterator();

      while(var4.hasNext()) {
         ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
         if ("xxx".equals(var5.type)) {
         }
      }

      boolean var9 = false;
      Iterator var10 = var3.values.iterator();

      while(var10.hasNext()) {
         ScriptParser.Value var6 = (ScriptParser.Value)var10.next();
         String var7 = var6.getKey().trim();
         String var8 = var6.getValue().trim();
         if ("mesh".equalsIgnoreCase(var7)) {
            this.meshName = var8;
         } else if ("translate".equalsIgnoreCase(var7)) {
            this.parseVector3f(var8, this.translate);
         } else if ("rotate".equalsIgnoreCase(var7)) {
            this.parseVector3f(var8, this.rotate);
         } else if ("scale".equalsIgnoreCase(var7)) {
            this.scale = Float.parseFloat(var8);
         } else if ("allMeshes".equalsIgnoreCase(var7)) {
            this.bAllMeshes = Boolean.parseBoolean(var8);
         } else if ("postProcess".equalsIgnoreCase(var7)) {
            this.postProcess = var8;
         } else if ("undoCoreScale".equalsIgnoreCase(var7)) {
            var9 = Boolean.parseBoolean(var8);
         }
      }

      if (var9) {
         this.scale *= 0.6666667F;
      }

   }

   private void parseVector3f(String var1, Vector3f var2) {
      String[] var3 = var1.split(" ");
      var2.setComponent(0, PZMath.tryParseFloat(var3[0], 0.0F));
      var2.setComponent(1, PZMath.tryParseFloat(var3[1], 0.0F));
      var2.setComponent(2, PZMath.tryParseFloat(var3[2], 0.0F));
   }
}
