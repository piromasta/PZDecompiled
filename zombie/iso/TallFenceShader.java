package zombie.iso;

import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;

public final class TallFenceShader {
   public static final TallFenceShader instance = new TallFenceShader();
   private ShaderProgram shaderProgram;
   private int u_alpha;
   private int u_outlineColor;
   private int u_stepSize;

   public TallFenceShader() {
   }

   public void initShader() {
      this.shaderProgram = ShaderProgram.createShaderProgram("tallFence", false, false, true);
      if (this.shaderProgram.isCompiled()) {
         this.u_alpha = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "u_alpha");
         this.u_stepSize = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "u_stepSize");
         this.u_outlineColor = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "u_outlineColor");
         ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
         GL20.glUniform2f(this.u_stepSize, 0.001F, 0.001F);
         ShaderHelper.glUseProgramObjectARB(0);
      }

   }

   public void setAlpha(float var1) {
      SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.u_alpha, var1);
   }

   public void setOutlineColor(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.u_outlineColor, var1, var2, var3, var4);
   }

   public void setStepSize(float var1, int var2, int var3) {
      SpriteRenderer.instance.ShaderUpdate2f(this.shaderProgram.getShaderID(), this.u_stepSize, var1 / (float)var2, var1 / (float)var3);
   }

   public boolean StartShader() {
      if (this.shaderProgram == null) {
         RenderThread.invokeOnRenderContext(this::initShader);
      }

      if (this.shaderProgram.isCompiled()) {
         IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
         return true;
      } else {
         return false;
      }
   }
}
