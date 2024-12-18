package zombie.core;

import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public class DepthShader extends Shader {
   public static boolean isActive;
   private static int DIFFUSE;
   private static int DEPTH;
   private int XOFF;
   private int YOFF;
   private int ZOFF;
   private int xOff;
   private int yOff;
   private int zOff;

   public DepthShader(String var1) {
      super(var1);
   }

   public void startMainThread(TextureDraw var1, int var2) {
   }

   public void startRenderThread(TextureDraw var1) {
      GL20.glUniform1i(DEPTH, 0);
      int var2 = Texture.lastTextureID;
      Texture.lastTextureID = 0;
      this.getProgram().setValue("DIFFUSE", var1.tex1, 1);
      Texture.lastTextureID = var2;
      GL13.glActiveTexture(33984);
      SpriteRenderer.ringBuffer.shaderChangedTexture1();
   }

   public void onCompileSuccess(ShaderProgram var1) {
      int var2 = this.getID();
      DEPTH = GL20.glGetUniformLocation(var2, "DEPTH");
      this.XOFF = GL20.glGetUniformLocation(var2, "XOFF");
      this.YOFF = GL20.glGetUniformLocation(var2, "YOFF");
      this.ZOFF = GL20.glGetUniformLocation(var2, "ZOFF");
   }
}
