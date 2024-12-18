package zombie.core.opengl;

import java.nio.ByteBuffer;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.VBO.IGLBufferObject;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public final class VBOLines {
   private static VBOLines instance;
   private final int VERTEX_SIZE = 12;
   private final int COLOR_SIZE = 16;
   private final int ELEMENT_SIZE = 28;
   private final int COLOR_OFFSET = 12;
   private final int NUM_LINES = 128;
   private final int NUM_ELEMENTS = 256;
   private final int INDEX_SIZE = 2;
   private VBOLinesShader m_shader = null;
   private GLVertexBufferObject m_vbo;
   private GLVertexBufferObject m_ibo;
   private ByteBuffer m_elements;
   private ByteBuffer m_indices;
   private float m_lineWidth = 1.0F;
   private float m_dx = 0.0F;
   private float m_dy = 0.0F;
   private float m_dz = 0.0F;
   private int m_mode = 1;
   private boolean m_depth_test = false;

   public VBOLines() {
   }

   public static VBOLines getInstance() {
      if (instance == null) {
         instance = new VBOLines();
      }

      return instance;
   }

   private void create() {
      this.m_elements = BufferUtils.createByteBuffer(7168);
      this.m_indices = BufferUtils.createByteBuffer(512);
      IGLBufferObject var1 = GLVertexBufferObject.funcs;
      this.m_vbo = new GLVertexBufferObject(7168L, var1.GL_ARRAY_BUFFER(), var1.GL_STREAM_DRAW());
      this.m_vbo.create();
      this.m_ibo = new GLVertexBufferObject(512L, var1.GL_ELEMENT_ARRAY_BUFFER(), var1.GL_STREAM_DRAW());
      this.m_ibo.create();
   }

   public void setOffset(float var1, float var2, float var3) {
      this.m_dx = var1;
      this.m_dy = var2;
      this.m_dz = var3;
   }

   public void addElement(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      if (this.isFull()) {
         this.flush();
      }

      if (this.m_elements == null) {
         this.create();
      }

      this.m_elements.putFloat(this.m_dx + var1);
      this.m_elements.putFloat(this.m_dy + var2);
      this.m_elements.putFloat(this.m_dz + var3);
      this.m_elements.putFloat(var4);
      this.m_elements.putFloat(var5);
      this.m_elements.putFloat(var6);
      this.m_elements.putFloat(var7);
      short var8 = (short)(this.m_indices.position() / 2);
      this.m_indices.putShort(var8);
   }

   public void addLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
      this.reserve(2);
      this.addElement(var1, var2, var3, var7, var8, var9, var10);
      this.addElement(var4, var5, var6, var7, var8, var9, var10);
   }

   public void addLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14) {
      this.reserve(2);
      this.addElement(var1, var2, var3, var7, var8, var9, var10);
      this.addElement(var4, var5, var6, var11, var12, var13, var14);
   }

   public void addTriangle(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13) {
      this.reserve(3);
      this.addElement(var1, var2, var3, var10, var11, var12, var13);
      this.addElement(var4, var5, var6, var10, var11, var12, var13);
      this.addElement(var7, var8, var9, var10, var11, var12, var13);
   }

   public void addQuad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      this.reserve(6);
      this.addTriangle(var1, var2, var5, var3, var2, var5, var1, var4, var5, var6, var7, var8, var9);
      this.addTriangle(var3, var2, var5, var3, var4, var5, var1, var4, var5, var6, var7, var8, var9);
   }

   public void addAABB(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      float var10 = var4 / 2.0F;
      float var11 = var5 / 2.0F;
      float var12 = var6 / 2.0F;
      this.setOffset(var1, var2, var3);
      this.setLineWidth(1.0F);
      float var13 = 1.0F;
      this.addLine(var10, var11, var12, -var10, var11, var12, var7, var8, var9, var13);
      this.addLine(var10, var11, var12, var10, -var11, var12, var7, var8, var9, var13);
      this.addLine(var10, var11, var12, var10, var11, -var12, var7, var8, var9, var13);
      this.addLine(-var10, var11, var12, -var10, -var11, var12, var7, var8, var9, var13);
      this.addLine(-var10, var11, var12, -var10, var11, -var12, var7, var8, var9, var13);
      this.addLine(var10, var11, -var12, var10, -var11, -var12, var7, var8, var9, var13);
      this.addLine(var10, var11, -var12, -var10, var11, -var12, var7, var8, var9, var13);
      this.addLine(-var10, var11, -var12, -var10, -var11, -var12, var7, var8, var9, var13);
      this.addLine(var10, -var11, -var12, -var10, -var11, -var12, var7, var8, var9, var13);
      this.addLine(var10, -var11, var12, var10, -var11, -var12, var7, var8, var9, var13);
      this.addLine(-var10, -var11, var12, -var10, -var11, -var12, var7, var8, var9, var13);
      this.addLine(var10, -var11, var12, -var10, -var11, var12, var7, var8, var9, var13);
      this.setOffset(0.0F, 0.0F, 0.0F);
   }

   public void addAABB(float var1, float var2, float var3, Vector3f var4, Vector3f var5, float var6, float var7, float var8) {
      this.addAABB(var1, var2, var3, var4.x, var4.y, var4.z, var5.x, var5.y, var5.z, var6, var7, var8);
   }

   public void addAABB(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12) {
      this.setOffset(var1, var2, var3);
      this.setLineWidth(1.0F);
      float var13 = 1.0F;
      this.addLine(var7, var8, var9, var4, var8, var9, var10, var11, var12, var13);
      this.addLine(var7, var8, var9, var7, var5, var9, var10, var11, var12, var13);
      this.addLine(var7, var8, var9, var7, var8, var6, var10, var11, var12, var13);
      this.addLine(var4, var8, var9, var4, var5, var9, var10, var11, var12, var13);
      this.addLine(var4, var8, var9, var4, var8, var6, var10, var11, var12, var13);
      this.addLine(var7, var8, var6, var7, var5, var6, var10, var11, var12, var13);
      this.addLine(var7, var8, var6, var4, var8, var6, var10, var11, var12, var13);
      this.addLine(var4, var8, var6, var4, var5, var6, var10, var11, var12, var13);
      this.addLine(var7, var5, var6, var4, var5, var6, var10, var11, var12, var13);
      this.addLine(var7, var5, var9, var7, var5, var6, var10, var11, var12, var13);
      this.addLine(var4, var5, var9, var4, var5, var6, var10, var11, var12, var13);
      this.addLine(var7, var5, var9, var4, var5, var9, var10, var11, var12, var13);
      this.setOffset(0.0F, 0.0F, 0.0F);
   }

   boolean isFull() {
      if (this.m_elements == null) {
         return false;
      } else if (this.m_mode == 4 && this.m_elements.position() % 84 == 0 && this.m_elements.position() + 84 > 7168) {
         return true;
      } else {
         return this.m_elements.position() == 7168;
      }
   }

   public void reserve(int var1) {
      if (!this.hasRoomFor(var1)) {
         this.flush();
      }

   }

   boolean hasRoomFor(int var1) {
      return this.m_elements == null || this.m_elements.position() / 28 + var1 <= 256;
   }

   public void flush() {
      if (this.m_elements != null && this.m_elements.position() != 0) {
         this.m_elements.flip();
         this.m_indices.flip();
         GL13.glActiveTexture(33984);
         GL11.glDisableClientState(32888);
         this.m_vbo.bind();
         this.m_vbo.bufferData(this.m_elements);
         this.m_ibo.bind();
         this.m_ibo.bufferData(this.m_indices);
         if (this.m_shader == null) {
            this.m_shader = new VBOLinesShader("vbo_lines");
         }

         this.m_shader.Start();
         VertexBufferObject.setModelViewProjection(this.m_shader.getProgram());
         GL20.glEnableVertexAttribArray(0);
         GL20.glEnableVertexAttribArray(1);
         GL20.glEnableVertexAttribArray(2);
         GL20.glDisableVertexAttribArray(3);
         GL20.glDisableVertexAttribArray(4);
         GL11.glDisableClientState(32884);
         GL11.glDisableClientState(32886);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 28, 0L);
         GL20.glVertexAttribPointer(1, 4, 5126, true, 28, 12L);

         for(int var1 = 7; var1 >= 0; --var1) {
            GL13.glActiveTexture('蓀' + var1);
            GL11.glDisable(3553);
         }

         if (this.m_depth_test) {
            GL11.glEnable(2929);
         } else {
            GL11.glDisable(2929);
         }

         GL11.glEnable(2848);
         GL11.glLineWidth(Math.min(this.m_lineWidth, 1.0F));
         byte var5 = 0;
         int var2 = this.m_elements.limit() / 28;
         byte var3 = 0;
         int var4 = this.m_indices.limit() / 2;
         GL12.glDrawRangeElements(this.m_mode, var5, var5 + var2, var4 - var3, 5123, (long)(var3 * 2));
         this.m_vbo.bindNone();
         this.m_ibo.bindNone();
         this.m_elements.clear();
         this.m_indices.clear();
         this.m_shader.End();
         GL11.glEnable(2929);
         GL11.glEnable(3553);
         GL11.glDisable(2848);
         GL20.glEnableVertexAttribArray(0);
         GL20.glEnableVertexAttribArray(1);
         GL20.glEnableVertexAttribArray(2);
         GL20.glEnableVertexAttribArray(3);
         GL20.glEnableVertexAttribArray(4);
         GL13.glActiveTexture(33984);
         SpriteRenderer.ringBuffer.restoreVBOs = true;
      }
   }

   public void setLineWidth(float var1) {
      if (!PZMath.equal(this.m_lineWidth, var1, 0.01F)) {
         this.flush();
         this.m_lineWidth = var1;
      }

   }

   public void setMode(int var1) {
      assert var1 == 1 || var1 == 4;

      if (var1 != this.m_mode) {
         this.flush();
         this.m_mode = var1;
      }

   }

   public void setDepthTest(boolean var1) {
      if (var1 != this.m_depth_test) {
         this.flush();
         this.m_depth_test = var1;
      }

   }
}
