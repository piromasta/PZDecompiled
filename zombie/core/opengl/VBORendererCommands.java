package zombie.core.opengl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.util.Type;

public final class VBORendererCommands {
   public static final short COMMAND_StartRun = 0;
   public static final short COMMAND_RenderRun = 1;
   public static final short COMMAND_PushAndLoadMatrix = 2;
   public static final short COMMAND_PushAndMultMatrix = 3;
   public static final short COMMAND_PopMatrix = 4;
   public static final short COMMAND_UseProgram = 5;
   public static final short COMMAND_Shader1f = 6;
   public static final short COMMAND_Shader2f = 7;
   public static final short COMMAND_Shader3f = 8;
   public static final short COMMAND_Shader4f = 9;
   private final VBORenderer m_vboRenderer;
   private int m_commandCount;
   private ByteBuffer m_commandBuffer;
   private final ArrayList<Object> m_objects = new ArrayList();
   private final Matrix4f tempMatrix4f = new Matrix4f();

   VBORendererCommands(VBORenderer var1) {
      this.m_vboRenderer = var1;
   }

   public void adopt(VBORendererCommands var1) {
      if (var1.m_commandCount != 0) {
         this.m_commandCount += var1.m_commandCount;
         this.reserve(var1.m_commandBuffer.position());
         var1.m_commandBuffer.flip();
         this.m_commandBuffer.put(var1.m_commandBuffer);
         this.m_objects.addAll(var1.m_objects);
         var1.clear();
      }
   }

   public void clear() {
      if (this.m_commandBuffer != null) {
         this.m_commandCount = 0;
         this.m_commandBuffer.clear();
         this.m_objects.clear();
      }
   }

   public int position() {
      return this.m_commandBuffer == null ? 0 : this.m_commandBuffer.position();
   }

   public void putFloat(float var1) {
      this.reserve(4);
      this.m_commandBuffer.putFloat(var1);
   }

   public void putInt(int var1) {
      this.reserve(4);
      this.m_commandBuffer.putInt(var1);
   }

   public void putMatrix4f(Matrix4f var1) {
      this.reserve(64);
      var1.get(this.m_commandBuffer);
      this.m_commandBuffer.position(this.m_commandBuffer.position() + 64);
   }

   public void putShort(short var1) {
      this.reserve(2);
      this.m_commandBuffer.putShort(var1);
   }

   public void putObject(Object var1) {
      this.m_objects.add(var1);
   }

   public float getFloat() {
      return this.m_commandBuffer.getFloat();
   }

   public int getInt() {
      return this.m_commandBuffer.getInt();
   }

   public float getShort() {
      return (float)this.m_commandBuffer.getShort();
   }

   public <C> C getObject(Class<C> var1) {
      Object var2 = this.m_objects.remove(0);
      return Type.tryCastTo(var2, var1);
   }

   private void reserve(int var1) {
      int var2;
      if (this.m_commandBuffer == null) {
         var2 = (int)PZMath.ceil((float)var1 / 512.0F);
         this.m_commandBuffer = ByteBuffer.allocateDirect(var2 * 512);
      } else if (this.m_commandBuffer.position() + var1 > this.m_commandBuffer.capacity()) {
         var2 = (int)PZMath.ceil((float)(this.m_commandBuffer.position() + var1) / 512.0F);
         ByteBuffer var3 = this.m_commandBuffer;
         this.m_commandBuffer = ByteBuffer.allocateDirect(var2 * 512);
         if (var3.position() > 0) {
            var3.flip();
            this.m_commandBuffer.put(var3);
         }

      }
   }

   public void invoke() {
      if (this.m_commandBuffer != null) {
         int var1 = this.m_commandBuffer.position();
         this.m_commandBuffer.position(0);

         try {
            for(int var2 = 0; var2 < this.m_commandCount; ++var2) {
               short var3 = this.m_commandBuffer.getShort();
               this.invokeCommand(var3);
            }
         } finally {
            this.m_commandBuffer.position(var1);
         }

      }
   }

   private void invokeCommand(short var1) {
      int var3;
      switch (var1) {
         case 0:
            this.m_vboRenderer.startNextRun();
            break;
         case 1:
            this.m_vboRenderer.renderNextRun();
            break;
         case 2:
            var3 = this.getInt();
            this.tempMatrix4f.set(this.m_commandBuffer);
            this.m_commandBuffer.position(this.m_commandBuffer.position() + 64);
            PZGLUtil.pushAndLoadMatrix(var3, this.tempMatrix4f);
            break;
         case 3:
            var3 = this.getInt();
            this.tempMatrix4f.set(this.m_commandBuffer);
            this.m_commandBuffer.position(this.m_commandBuffer.position() + 64);
            PZGLUtil.pushAndMultMatrix(var3, this.tempMatrix4f);
            break;
         case 4:
            PZGLUtil.popMatrix(this.getInt());
            break;
         case 5:
            ShaderProgram var2 = (ShaderProgram)this.getObject(ShaderProgram.class);
            var2.Start();
            VertexBufferObject.setModelViewProjection(var2);
            break;
         case 6:
            GL20.glUniform1f(this.getInt(), this.getFloat());
            break;
         case 7:
            GL20.glUniform2f(this.getInt(), this.getFloat(), this.getFloat());
         case 8:
         default:
            break;
         case 9:
            GL20.glUniform4f(this.getInt(), this.getFloat(), this.getFloat(), this.getFloat(), this.getFloat());
      }

   }

   public void cmdStartRun() {
      this.putShort((short)0);
      ++this.m_commandCount;
   }

   public void cmdRenderRun() {
      this.putShort((short)1);
      ++this.m_commandCount;
   }

   public void cmdPushAndLoadMatrix(int var1, Matrix4f var2) {
      this.putShort((short)2);
      this.putInt(var1);
      this.putMatrix4f(var2);
      ++this.m_commandCount;
   }

   public void cmdPushAndMultMatrix(int var1, Matrix4f var2) {
      this.putShort((short)3);
      this.putInt(var1);
      this.putMatrix4f(var2);
      ++this.m_commandCount;
   }

   public void cmdPopMatrix(int var1) {
      this.putShort((short)4);
      this.putInt(var1);
      ++this.m_commandCount;
   }

   public void cmdShader1f(int var1, float var2) {
      this.putShort((short)6);
      this.putInt(var1);
      this.putFloat(var2);
      ++this.m_commandCount;
   }

   public void cmdShader2f(int var1, float var2, float var3) {
      this.putShort((short)7);
      this.putInt(var1);
      this.putFloat(var2);
      this.putFloat(var3);
      ++this.m_commandCount;
   }

   public void cmdShader3f(int var1, float var2, float var3, float var4) {
      this.putShort((short)8);
      this.putInt(var1);
      this.putFloat(var2);
      this.putFloat(var3);
      this.putFloat(var4);
      ++this.m_commandCount;
   }

   public void cmdShader4f(int var1, float var2, float var3, float var4, float var5) {
      this.putShort((short)9);
      this.putInt(var1);
      this.putFloat(var2);
      this.putFloat(var3);
      this.putFloat(var4);
      this.putFloat(var5);
      ++this.m_commandCount;
   }

   public void cmdUseProgram(ShaderProgram var1) {
      this.putShort((short)5);
      this.putObject(var1);
      ++this.m_commandCount;
   }
}
