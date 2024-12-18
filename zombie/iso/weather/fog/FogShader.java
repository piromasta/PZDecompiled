package zombie.iso.weather.fog;

import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.IShaderProgramListener;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;

public class FogShader implements IShaderProgramListener {
   public static final FogShader instance = new FogShader();
   private ShaderProgram shaderProgram;
   private int noiseTexture;
   private int screenInfo;
   private int textureInfo;
   private int rectangleInfo;
   private int worldOffset;
   private int scalingInfo;
   private int colorInfo;
   private int paramInfo;
   private int cameraInfo;
   private int uTargetDepth;

   public FogShader() {
   }

   public void initShader() {
      this.shaderProgram = ShaderProgram.createShaderProgram("fog", false, false, false);
      this.shaderProgram.addCompileListener(this);
      this.shaderProgram.compile();
      if (this.shaderProgram.isCompiled()) {
         ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
         ShaderHelper.glUseProgramObjectARB(0);
      }

   }

   public ShaderProgram getProgram() {
      return this.shaderProgram;
   }

   public void setScreenInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.screenInfo, var1, var2, var3, var4);
   }

   public void setTextureInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.textureInfo, var1, var2, var3, var4);
   }

   public void setRectangleInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.rectangleInfo, var1, var2, var3, var4);
   }

   public void setScalingInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.scalingInfo, var1, var2, var3, var4);
   }

   public void setColorInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.colorInfo, var1, var2, var3, var4);
   }

   public void setWorldOffset(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.worldOffset, var1, var2, var3, var4);
   }

   public void setParamInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.paramInfo, var1, var2, var3, var4);
   }

   public void setCameraInfo(float var1, float var2, float var3, float var4) {
      SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.cameraInfo, var1, var2, var3, var4);
   }

   public void setTargetDepth(float var1) {
      SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.uTargetDepth, var1);
   }

   public void setScreenInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.screenInfo, var1, var2, var3, var4);
   }

   public void setTextureInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.textureInfo, var1, var2, var3, var4);
   }

   public void setRectangleInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.rectangleInfo, var1, var2, var3, var4);
   }

   public void setScalingInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.scalingInfo, var1, var2, var3, var4);
   }

   public void setColorInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.colorInfo, var1, var2, var3, var4);
   }

   public void setWorldOffset2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.worldOffset, var1, var2, var3, var4);
   }

   public void setParamInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.paramInfo, var1, var2, var3, var4);
   }

   public void setCameraInfo2(float var1, float var2, float var3, float var4) {
      VBORenderer.getInstance().cmdShader4f(this.cameraInfo, var1, var2, var3, var4);
   }

   public void setTargetDepth2(float var1) {
      VBORenderer.getInstance().cmdShader1f(this.uTargetDepth, var1);
   }

   public void setTextureInfo3(float var1, float var2, float var3, float var4) {
      GL20.glUniform4f(this.textureInfo, var1, var2, var3, var4);
   }

   public void setWorldOffset3(float var1, float var2, float var3, float var4) {
      GL20.glUniform4f(this.worldOffset, var1, var2, var3, var4);
   }

   public void setParamInfo3(float var1, float var2, float var3, float var4) {
      GL20.glUniform4f(this.paramInfo, var1, var2, var3, var4);
   }

   public void setColorInfo3(float var1, float var2, float var3, float var4) {
      GL20.glUniform4f(this.colorInfo, var1, var2, var3, var4);
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

   protected void reloadShader() {
      if (this.shaderProgram != null) {
         this.shaderProgram = null;
      }

   }

   public void callback(ShaderProgram var1) {
      this.noiseTexture = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "NoiseTexture");
      this.screenInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "screenInfo");
      this.textureInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "textureInfo");
      this.rectangleInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "rectangleInfo");
      this.scalingInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "scalingInfo");
      this.colorInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "colorInfo");
      this.worldOffset = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "worldOffset");
      this.paramInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "paramInfo");
      this.cameraInfo = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "cameraInfo");
      this.uTargetDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "targetDepth");
   }
}
