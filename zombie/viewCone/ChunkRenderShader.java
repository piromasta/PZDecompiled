package zombie.viewCone;

import org.lwjgl.opengl.GL13;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public class ChunkRenderShader extends Shader {
   public ChunkRenderShader(String var1) {
      super(var1);
   }

   public void startMainThread(TextureDraw var1, int var2) {
   }

   public void startRenderThread(TextureDraw var1) {
      this.getProgram().setValue("DEPTH", var1.tex1, 1);
      GL13.glActiveTexture(33984);
      SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      Texture.lastTextureID = 0;
      SpriteRenderer.ringBuffer.shaderChangedTexture1();
      this.getProgram().setValue("chunkDepth", var1.chunkDepth);
   }

   public void onCompileSuccess(ShaderProgram var1) {
      this.Start();
      var1.setSamplerUnit("DIFFUSE", 0);
      var1.setSamplerUnit("DEPTH", 1);
      this.End();
   }
}
