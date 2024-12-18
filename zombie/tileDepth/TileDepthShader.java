package zombie.tileDepth;

import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.TextureDraw;

public class TileDepthShader extends Shader {
   private int DIFFUSE;
   private int DEPTH;

   public TileDepthShader(String var1) {
      super(var1);
   }

   public void startRenderThread(TextureDraw var1) {
      GL20.glUniform1i(this.DIFFUSE, 0);
      GL20.glUniform1i(this.DEPTH, 1);
      VertexBufferObject.setModelViewProjection(this.getProgram());
   }

   protected void onCompileSuccess(ShaderProgram var1) {
      int var2 = this.getID();
      this.DIFFUSE = GL20.glGetUniformLocation(var2, "DIFFUSE");
      this.DEPTH = GL20.glGetUniformLocation(var2, "DEPTH");
   }
}
