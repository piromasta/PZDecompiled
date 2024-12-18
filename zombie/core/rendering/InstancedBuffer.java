package zombie.core.rendering;

import org.lwjgl.opengl.GL15;
import org.lwjglx.BufferUtils;
import org.lwjglx.opengl.Util;
import zombie.core.Core;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.debug.DebugOptions;

public class InstancedBuffer extends ShaderBuffer {
   private final ShaderBufferData bufferData;

   public InstancedBuffer(Shader var1, int var2) {
      this.bufferData = new ShaderBufferData(var1);
      int var3 = this.bufferData.GetSize() * var2;
      if (var3 > 0) {
         this.bufferID = GL15.glGenBuffers();
         this.data = BufferUtils.createByteBuffer(var3);
         GL15.glBindBuffer(37074, this.bufferID);
         GL15.glBufferData(37074, this.data, 35048);
         Util.checkGLError();
      } else {
         this.bufferID = -1;
         this.data = null;
      }

   }

   public ShaderBufferData GetBufferData() {
      return this.bufferData;
   }

   public void PushProperties(ShaderPropertyBlock var1) {
      this.bufferData.ResetParameters();
      this.bufferData.CopyParameters(var1);
      this.bufferData.PushParameters(this);
   }

   public void PushInstanced(ShaderPropertyBlock var1) {
      if (Core.bDebug && DebugOptions.instance.InstancingBufferCopy.getValue()) {
         var1.StoreProperties();
         this.bufferData.PushInstanced(this, var1);
      } else {
         this.bufferData.ResetInstanced();
         this.bufferData.CopyInstanced(var1);
         this.bufferData.PushInstanced(this);
      }

   }

   public void PushUniforms(ShaderPropertyBlock var1) {
      this.bufferData.ResetUniforms();
      this.bufferData.CopyUniforms(var1);
      this.bufferData.PushUniforms();
   }

   protected void PreUpdate() {
      this.data.position(this.bufferData.GetCurrentInstance() * this.bufferData.GetSize());
   }

   public void OnReset() {
      this.bufferData.Reset();
   }
}
