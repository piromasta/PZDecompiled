package zombie.core.skinnedmodel.shader;

import java.util.ArrayList;

public final class ShaderManager {
   public static final ShaderManager instance = new ShaderManager();
   private final ArrayList<Shader> shaders = new ArrayList();

   public ShaderManager() {
   }

   private Shader getShader(String var1, boolean var2, boolean var3) {
      for(int var4 = 0; var4 < this.shaders.size(); ++var4) {
         Shader var5 = (Shader)this.shaders.get(var4);
         if (var1.equals(var5.name) && var2 == var5.bStatic && var3 == var5.bInstanced) {
            return var5;
         }
      }

      return null;
   }

   public Shader getOrCreateShader(String var1, boolean var2, boolean var3) {
      Shader var4 = this.getShader(var1, var2, var3);
      if (var4 != null) {
         return var4;
      } else {
         for(int var5 = 0; var5 < this.shaders.size(); ++var5) {
            Shader var6 = (Shader)this.shaders.get(var5);
            if (var6.name.equalsIgnoreCase(var1) && !var6.name.equals(var1)) {
               throw new IllegalArgumentException("shader filenames are case-sensitive");
            }
         }

         var4 = new Shader(var1, var2, var3);
         this.shaders.add(var4);
         return var4;
      }
   }

   public Shader getShaderByID(int var1) {
      for(int var2 = 0; var2 < this.shaders.size(); ++var2) {
         Shader var3 = (Shader)this.shaders.get(var2);
         if (var3.getID() == var1) {
            return var3;
         }
      }

      return null;
   }
}
