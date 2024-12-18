package zombie.core;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.ShaderPrograms;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.debug.DebugOptions;

public class ShaderHelper {
   private static int CurrentlyBound = -1;

   public ShaderHelper() {
   }

   public static void glUseProgramObjectARB(int var0) {
      if (var0 == 0) {
         if (CurrentlyBound == SceneShaderStore.DefaultShaderID) {
            return;
         }

         GL20.glUseProgram(SceneShaderStore.DefaultShaderID);
         DefaultShader.isActive = true;
         CurrentlyBound = SceneShaderStore.DefaultShaderID;
      } else {
         if (CurrentlyBound == var0) {
            if (DebugOptions.instance.Checks.BoundShader.getValue() && CurrentlyBound != GL11.glGetInteger(35725)) {
               boolean var1 = true;
            }

            return;
         }

         GL20.glUseProgram(var0);
         DefaultShader.isActive = SceneShaderStore.DefaultShaderID == var0;
         CurrentlyBound = var0;
      }

   }

   public static void forgetCurrentlyBound() {
      CurrentlyBound = -1;
      DefaultShader.isActive = false;
   }

   public static void setModelViewProjection() {
      if (CurrentlyBound > 0) {
         if (DebugOptions.instance.Checks.BoundShader.getValue() && CurrentlyBound != GL11.glGetInteger(35725)) {
            CurrentlyBound = GL11.glGetInteger(35725);
         }

         ShaderProgram var0 = ShaderPrograms.getInstance().getProgramByID(CurrentlyBound);
         if (var0 != null && var0.isCompiled()) {
            VertexBufferObject.setModelViewProjection(var0);
         }
      }
   }
}
