package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import zombie.core.Core;
import zombie.core.VBO.IGLBufferObject;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.util.list.PZArrayUtil;

public final class VertexBufferObject {
   public static IGLBufferObject funcs;
   int[] elements;
   Vbo _handle;
   private final VertexFormat m_vertexFormat;
   private BeginMode _beginMode;
   public boolean bStatic = false;

   public VertexBufferObject() {
      this.bStatic = false;
      this.m_vertexFormat = new VertexFormat(4);
      this.m_vertexFormat.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.m_vertexFormat.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
      this.m_vertexFormat.setElement(2, VertexBufferObject.VertexType.ColorArray, 4);
      this.m_vertexFormat.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
      this.m_vertexFormat.calculate();
      this._beginMode = VertexBufferObject.BeginMode.Triangles;
   }

   /** @deprecated */
   @Deprecated
   public VertexBufferObject(VertexPositionNormalTangentTexture[] var1, int[] var2) {
      this.elements = var2;
      this.bStatic = true;
      RenderThread.invokeOnRenderContext(this, var1, var2, (var1x, var2x, var3) -> {
         var1x._handle = this.LoadVBO(var2x, var3);
      });
      this.m_vertexFormat = new VertexFormat(4);
      this.m_vertexFormat.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.m_vertexFormat.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
      this.m_vertexFormat.setElement(2, VertexBufferObject.VertexType.TangentArray, 12);
      this.m_vertexFormat.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
      this.m_vertexFormat.calculate();
      this._beginMode = VertexBufferObject.BeginMode.Triangles;
   }

   /** @deprecated */
   @Deprecated
   public VertexBufferObject(VertexPositionNormalTangentTextureSkin[] var1, int[] var2, boolean var3) {
      this.elements = var2;
      if (var3) {
         int[] var4 = new int[var2.length];
         int var5 = 0;

         for(int var6 = var2.length - 1 - 2; var6 >= 0; var6 -= 3) {
            var4[var5] = var2[var6];
            var4[var5 + 1] = var2[var6 + 1];
            var4[var5 + 2] = var2[var6 + 2];
            var5 += 3;
         }

         var2 = var4;
      }

      this.bStatic = false;
      this._handle = this.LoadVBO(var1, var2);
      this.m_vertexFormat = new VertexFormat(6);
      this.m_vertexFormat.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.m_vertexFormat.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
      this.m_vertexFormat.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
      this.m_vertexFormat.setElement(4, VertexBufferObject.VertexType.BlendWeightArray, 16);
      this.m_vertexFormat.setElement(5, VertexBufferObject.VertexType.BlendIndexArray, 16);
      this.m_vertexFormat.calculate();
      this._beginMode = VertexBufferObject.BeginMode.Triangles;
   }

   public VertexBufferObject(VertexArray var1, int[] var2) {
      this.m_vertexFormat = var1.m_format;
      this.elements = var2;
      this.bStatic = true;
      RenderThread.invokeOnRenderContext(this, var1, var2, (var1x, var2x, var3) -> {
         var1x._handle = this.LoadVBO(var2x, var3);
      });
      this._beginMode = VertexBufferObject.BeginMode.Triangles;
   }

   public VertexBufferObject(VertexArray var1, int[] var2, boolean var3) {
      this.m_vertexFormat = var1.m_format;
      if (var3) {
         int[] var4 = new int[var2.length];
         int var5 = 0;

         for(int var6 = var2.length - 1 - 2; var6 >= 0; var6 -= 3) {
            var4[var5] = var2[var6];
            var4[var5 + 1] = var2[var6 + 1];
            var4[var5 + 2] = var2[var6 + 2];
            var5 += 3;
         }

         var2 = var4;
      }

      this.elements = var2;
      this.bStatic = false;
      this._handle = this.LoadVBO(var1, var2);
      this._beginMode = VertexBufferObject.BeginMode.Triangles;
   }

   /** @deprecated */
   @Deprecated
   private Vbo LoadVBO(VertexPositionNormalTangentTextureSkin[] var1, int[] var2) {
      Vbo var3 = new Vbo();
      boolean var4 = false;
      byte var5 = 76;
      var3.FaceDataOnly = false;
      ByteBuffer var6 = BufferUtils.createByteBuffer(var1.length * var5);
      ByteBuffer var7 = BufferUtils.createByteBuffer(var2.length * 4);

      int var8;
      for(var8 = 0; var8 < var1.length; ++var8) {
         var1[var8].put(var6);
      }

      for(var8 = 0; var8 < var2.length; ++var8) {
         var7.putInt(var2[var8]);
      }

      var6.flip();
      var7.flip();
      var3.VboID = funcs.glGenBuffers();
      funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), var3.VboID);
      funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), var6, funcs.GL_STATIC_DRAW());
      funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var3.b);
      int var9 = var3.b.get();
      if (var1.length * var5 != var9) {
         throw new RuntimeException("Vertex data not uploaded correctly");
      } else {
         var3.EboID = funcs.glGenBuffers();
         funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), var3.EboID);
         funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), var7, funcs.GL_STATIC_DRAW());
         var3.b.clear();
         funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var3.b);
         var9 = var3.b.get();
         if (var2.length * 4 != var9) {
            throw new RuntimeException("Element data not uploaded correctly");
         } else {
            var3.NumElements = var2.length;
            var3.VertexStride = var5;
            return var3;
         }
      }
   }

   public Vbo LoadSoftwareVBO(ByteBuffer var1, Vbo var2, int[] var3) {
      Vbo var4 = var2;
      boolean var5 = false;
      ByteBuffer var6 = null;
      if (var2 == null) {
         var5 = true;
         var4 = new Vbo();
         var4.VboID = funcs.glGenBuffers();
         ByteBuffer var7 = BufferUtils.createByteBuffer(var3.length * 4);

         for(int var8 = 0; var8 < var3.length; ++var8) {
            var7.putInt(var3[var8]);
         }

         var7.flip();
         var6 = var7;
         var4.VertexStride = 36;
         var4.NumElements = var3.length;
      } else {
         var2.b.clear();
      }

      var4.FaceDataOnly = false;
      funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), var4.VboID);
      funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), var1, funcs.GL_STATIC_DRAW());
      funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var4.b);
      if (var6 != null) {
         var4.EboID = funcs.glGenBuffers();
         funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), var4.EboID);
         funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), var6, funcs.GL_STATIC_DRAW());
      }

      return var4;
   }

   /** @deprecated */
   @Deprecated
   private Vbo LoadVBO(VertexPositionNormalTangentTexture[] var1, int[] var2) {
      Vbo var3 = new Vbo();
      boolean var4 = false;
      byte var5 = 44;
      var3.FaceDataOnly = false;
      ByteBuffer var6 = BufferUtils.createByteBuffer(var1.length * var5);
      ByteBuffer var7 = BufferUtils.createByteBuffer(var2.length * 4);

      int var8;
      for(var8 = 0; var8 < var1.length; ++var8) {
         var1[var8].put(var6);
      }

      for(var8 = 0; var8 < var2.length; ++var8) {
         var7.putInt(var2[var8]);
      }

      var6.flip();
      var7.flip();
      var3.VboID = funcs.glGenBuffers();
      funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), var3.VboID);
      funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), var6, funcs.GL_STATIC_DRAW());
      funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var3.b);
      int var9 = var3.b.get();
      if (var1.length * var5 != var9) {
         throw new RuntimeException("Vertex data not uploaded correctly");
      } else {
         var3.EboID = funcs.glGenBuffers();
         funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), var3.EboID);
         funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), var7, funcs.GL_STATIC_DRAW());
         var3.b.clear();
         funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var3.b);
         var9 = var3.b.get();
         if (var2.length * 4 != var9) {
            throw new RuntimeException("Element data not uploaded correctly");
         } else {
            var3.NumElements = var2.length;
            var3.VertexStride = var5;
            return var3;
         }
      }
   }

   private Vbo LoadVBO(VertexArray var1, int[] var2) {
      Vbo var3 = new Vbo();
      var3.FaceDataOnly = false;
      ByteBuffer var4 = MemoryUtil.memAlloc(var2.length * 4);

      int var5;
      for(var5 = 0; var5 < var2.length; ++var5) {
         var4.putInt(var2[var5]);
      }

      var1.m_buffer.position(0);
      var1.m_buffer.limit(var1.m_numVertices * var1.m_format.m_stride);
      var4.flip();
      var3.VboID = funcs.glGenBuffers();
      funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), var3.VboID);
      funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), var1.m_buffer, funcs.GL_STATIC_DRAW());
      funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var3.b);
      var5 = var3.b.get();
      if (var1.m_numVertices * var1.m_format.m_stride != var5) {
         throw new RuntimeException("Vertex data not uploaded correctly");
      } else {
         var3.EboID = funcs.glGenBuffers();
         funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), var3.EboID);
         funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), var4, funcs.GL_STATIC_DRAW());
         MemoryUtil.memFree(var4);
         var3.b.clear();
         funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), var3.b);
         var5 = var3.b.get();
         if (var2.length * 4 != var5) {
            throw new RuntimeException("Element data not uploaded correctly");
         } else {
            var3.NumElements = var2.length;
            var3.VertexStride = var1.m_format.m_stride;
            return var3;
         }
      }
   }

   public void clear() {
      if (this._handle != null) {
         if (this._handle.VboID > 0) {
            funcs.glDeleteBuffers(this._handle.VboID);
            this._handle.VboID = -1;
         }

         if (this._handle.EboID > 0) {
            funcs.glDeleteBuffers(this._handle.EboID);
            this._handle.EboID = -1;
         }

         this._handle = null;
      }
   }

   public int BeginInstancedDraw(Shader var1) {
      if (CanDraw(this._handle)) {
         boolean var2 = BeginDraw(this._handle, this.m_vertexFormat, var1, 4);
         return var2 ? 1 : 0;
      } else {
         return -1;
      }
   }

   public void FinishInstancedDraw(Shader var1, boolean var2) {
      this.FinishDraw(var1, var2);
   }

   public boolean BeginDraw(Shader var1) {
      return BeginDraw(this._handle, this.m_vertexFormat, var1, 4);
   }

   public void Draw(Shader var1) {
      Draw(this._handle, this.m_vertexFormat, var1, 4);
   }

   public void DrawInstanced(Shader var1, int var2) {
      DrawInstanced(this._handle, this.m_vertexFormat, var1, 4, var2);
   }

   public void DrawStrip(Shader var1) {
      Draw(this._handle, this.m_vertexFormat, var1, 5);
   }

   private static boolean CanDraw(Vbo var0) {
      return var0 != null && !DebugOptions.instance.DebugDraw_SkipVBODraw.getValue();
   }

   private static boolean BeginDraw(Vbo var0, VertexFormat var1, Shader var2, int var3) {
      int var4 = 33984;
      boolean var5 = false;
      if (!var0.FaceDataOnly) {
         setModelViewProjection(var2);
         funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), var0.VboID);

         for(int var6 = 0; var6 < var1.m_elements.length; ++var6) {
            VertexElement var7 = var1.m_elements[var6];
            switch (var7.m_type) {
               case VertexArray:
                  GL20.glVertexAttribPointer(var6, 3, 5126, false, var0.VertexStride, (long)var7.m_byteOffset);
                  GL20.glEnableVertexAttribArray(var6);
                  break;
               case NormalArray:
                  GL20.glVertexAttribPointer(var6, 3, 5126, true, var0.VertexStride, (long)var7.m_byteOffset);
                  GL20.glEnableVertexAttribArray(var6);
                  break;
               case ColorArray:
                  GL20.glVertexAttribPointer(var6, 3, 5121, true, var0.VertexStride, (long)var7.m_byteOffset);
                  GL20.glEnableVertexAttribArray(var6);
                  break;
               case TextureCoordArray:
                  GL20.glActiveTexture(var4);
                  GL20.glVertexAttribPointer(var6, 2, 5126, false, var0.VertexStride, (long)var7.m_byteOffset);
                  GL20.glEnableVertexAttribArray(var6);
                  ++var4;
               case TangentArray:
               default:
                  break;
               case BlendWeightArray:
                  GL20.glVertexAttribPointer(var6, 4, 5126, false, var0.VertexStride, (long)var7.m_byteOffset);
                  GL20.glEnableVertexAttribArray(var6);
                  var5 = true;
                  break;
               case BlendIndexArray:
                  GL20.glVertexAttribPointer(var6, 4, 5126, false, var0.VertexStride, (long)var7.m_byteOffset);
                  GL20.glEnableVertexAttribArray(var6);
            }
         }
      }

      funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), var0.EboID);
      return var5;
   }

   public void FinishDraw(Shader var1, boolean var2) {
      FinishDraw(this.m_vertexFormat, var1, var2);
   }

   public static void FinishDraw(VertexFormat var0, Shader var1, boolean var2) {
      if (var2 && var1 != null) {
         int var3 = PZArrayUtil.indexOf((Object[])var0.m_elements, (var0x) -> {
            return var0x.m_type == VertexBufferObject.VertexType.BlendWeightArray;
         });
         int var4 = PZArrayUtil.indexOf((Object[])var0.m_elements, (var0x) -> {
            return var0x.m_type == VertexBufferObject.VertexType.BlendIndexArray;
         });
         GL20.glDisableVertexAttribArray(var3);
         GL20.glDisableVertexAttribArray(var4);
      }

   }

   private static void Draw(Vbo var0, VertexFormat var1, Shader var2, int var3) {
      if (CanDraw(var0)) {
         boolean var4 = BeginDraw(var0, var1, var2, var3);
         GL20.glDrawElements(var3, var0.NumElements, 5125, 0L);
         FinishDraw(var1, var2, var4);
      }

   }

   private static void DrawInstanced(Vbo var0, VertexFormat var1, Shader var2, int var3, int var4) {
      if (CanDraw(var0)) {
         boolean var5 = BeginDraw(var0, var1, var2, var3);
         GL31.glDrawElementsInstanced(var3, var0.NumElements, 5125, 0L, var4);
         FinishDraw(var1, var2, var5);
      }

   }

   public void PushDrawCall() {
      GL20.glDrawElements(4, this._handle.NumElements, 5125, 0L);
   }

   public static void getModelViewProjection(Matrix4f var0) {
      Core var1 = Core.getInstance();
      if (!var1.projectionMatrixStack.isEmpty() && !var1.modelViewMatrixStack.isEmpty()) {
         Matrix4f var2 = Core.getInstance().projectionMatrixStack.peek();
         Matrix4f var3 = Core.getInstance().modelViewMatrixStack.peek();
         var2.mul(var3, var0);
      } else {
         DebugLog.Shader.warn("Matrix stack is empty");
         var0.identity();
      }
   }

   public static float getDepthValueAt(float var0, float var1, float var2) {
      Matrix4f var3 = VertexBufferObject.L_getModelViewProjection.MVPjoml;
      getModelViewProjection(var3);
      Vector3f var4 = VertexBufferObject.L_getModelViewProjection.vector3f.set(var0, var1, var2);
      var3.transformPosition(var4);
      return var4.z;
   }

   public static void setModelViewProjection(Shader var0) {
      if (var0 != null) {
         setModelViewProjection(var0.getShaderProgram());
      }
   }

   public static void setModelViewProjection(ShaderProgram var0) {
      if (var0 != null && var0.isCompiled()) {
         ShaderProgram.Uniform var1 = var0.getUniform("ModelViewProjection", 35676);
         if (var1 != null) {
            Matrix4f var2 = VertexBufferObject.L_setModelViewProjection.PRJ;
            Matrix4f var3 = VertexBufferObject.L_setModelViewProjection.MV;
            if (Core.getInstance().modelViewMatrixStack.isEmpty()) {
               var3.identity();
               var2.identity();
            } else {
               var3.set(Core.getInstance().modelViewMatrixStack.peek());
               var2.set(Core.getInstance().projectionMatrixStack.peek());
            }

            if (!var3.equals(var0.ModelView) || !var2.equals(var0.Projection)) {
               var0.ModelView.set(var3);
               var0.Projection.set(var2);
               var2.mul(var3);
               var0.setValue("ModelViewProjection", var2);
            }
         }
      }
   }

   public static final class VertexFormat {
      final VertexElement[] m_elements;
      int m_stride;

      public VertexFormat(int var1) {
         this.m_elements = (VertexElement[])PZArrayUtil.newInstance(VertexElement.class, var1, VertexElement::new);
      }

      public void setElement(int var1, VertexType var2, int var3) {
         this.m_elements[var1].m_type = var2;
         this.m_elements[var1].m_byteSize = var3;
      }

      public int getNumElements() {
         return this.m_elements.length;
      }

      public VertexElement getElement(int var1) {
         return this.m_elements[var1];
      }

      public int indexOf(VertexType var1) {
         for(int var2 = 0; var2 < this.m_elements.length; ++var2) {
            VertexElement var3 = this.m_elements[var2];
            if (var3.m_type == var1) {
               return var2;
            }
         }

         return -1;
      }

      public void calculate() {
         this.m_stride = 0;

         for(int var1 = 0; var1 < this.m_elements.length; ++var1) {
            this.m_elements[var1].m_byteOffset = this.m_stride;
            this.m_stride += this.m_elements[var1].m_byteSize;
         }

      }

      public int getStride() {
         return this.m_stride;
      }
   }

   public static enum VertexType {
      VertexArray,
      NormalArray,
      ColorArray,
      IndexArray,
      TextureCoordArray,
      TangentArray,
      BlendWeightArray,
      BlendIndexArray,
      Depth;

      private VertexType() {
      }
   }

   public static enum BeginMode {
      Triangles;

      private BeginMode() {
      }
   }

   public static final class Vbo {
      public final IntBuffer b = BufferUtils.createIntBuffer(4);
      public int VboID;
      public int EboID;
      public int NumElements;
      public int VertexStride;
      public boolean FaceDataOnly;

      public Vbo() {
      }
   }

   public static final class VertexArray {
      public final VertexFormat m_format;
      public final int m_numVertices;
      public final ByteBuffer m_buffer;

      public VertexArray(VertexFormat var1, int var2) {
         this.m_format = var1;
         this.m_numVertices = var2;
         this.m_buffer = BufferUtils.createByteBuffer(this.m_numVertices * this.m_format.m_stride);
      }

      public void setElement(int var1, int var2, float var3, float var4) {
         int var5 = var1 * this.m_format.m_stride + this.m_format.m_elements[var2].m_byteOffset;
         this.m_buffer.putFloat(var5, var3);
         var5 += 4;
         this.m_buffer.putFloat(var5, var4);
      }

      public void setElement(int var1, int var2, float var3, float var4, float var5) {
         int var6 = var1 * this.m_format.m_stride + this.m_format.m_elements[var2].m_byteOffset;
         this.m_buffer.putFloat(var6, var3);
         var6 += 4;
         this.m_buffer.putFloat(var6, var4);
         var6 += 4;
         this.m_buffer.putFloat(var6, var5);
      }

      public void setElement(int var1, int var2, float var3, float var4, float var5, float var6) {
         int var7 = var1 * this.m_format.m_stride + this.m_format.m_elements[var2].m_byteOffset;
         this.m_buffer.putFloat(var7, var3);
         var7 += 4;
         this.m_buffer.putFloat(var7, var4);
         var7 += 4;
         this.m_buffer.putFloat(var7, var5);
         var7 += 4;
         this.m_buffer.putFloat(var7, var6);
      }

      public float getElementFloat(int var1, int var2, int var3) {
         int var4 = var1 * this.m_format.m_stride + this.m_format.m_elements[var2].m_byteOffset + var3 * 4;
         return this.m_buffer.getFloat(var4);
      }
   }

   public static final class VertexElement {
      public VertexType m_type;
      public int m_byteSize;
      public int m_byteOffset;

      public VertexElement() {
      }
   }

   private static final class L_getModelViewProjection {
      static final Matrix4f MVPjoml = new Matrix4f();
      static final Vector3f vector3f = new Vector3f();

      private L_getModelViewProjection() {
      }
   }

   private static final class L_setModelViewProjection {
      static final Matrix4f MV = new Matrix4f();
      static final Matrix4f PRJ = new Matrix4f();

      private L_setModelViewProjection() {
      }
   }
}
