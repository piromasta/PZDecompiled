package zombie.core.opengl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.VBO.IGLBufferObject;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.TextureID;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

public final class VBORenderer {
   private static VBORenderer instance;
   public final VertexBufferObject.VertexFormat FORMAT_PositionColor = new VertexBufferObject.VertexFormat(2);
   public final VertexBufferObject.VertexFormat FORMAT_PositionColorUV = new VertexBufferObject.VertexFormat(3);
   public final VertexBufferObject.VertexFormat FORMAT_PositionColorUVDepth = new VertexBufferObject.VertexFormat(4);
   public final VertexBufferObject.VertexFormat FORMAT_PositionNormalColor = new VertexBufferObject.VertexFormat(3);
   public final VertexBufferObject.VertexFormat FORMAT_PositionNormalColorUV = new VertexBufferObject.VertexFormat(4);
   private final int BUFFER_SIZE = 4096;
   private int ELEMENT_SIZE;
   private int NUM_ELEMENTS;
   private final int INDEX_SIZE = 2;
   private int VERTEX_OFFSET;
   private int NORMAL_OFFSET;
   private int COLOR_OFFSET;
   private int UV1_OFFSET;
   private int UV2_OFFSET;
   private int DEPTH_OFFSET;
   private VBOLinesShader m_shader_PositionColor = null;
   private VBOLinesShader m_shader_PositionColorUV = null;
   private VBOLinesShader m_shader_PositionColorUVDepth = null;
   private VBOLinesShader m_shader_PositionNormalColor = null;
   private VBOLinesShader m_shader_PositionNormalColorUV = null;
   private VertexBufferObject.VertexFormat m_format;
   private VertexBufferObject.VertexFormat m_formatUsedByVertexAttribArray;
   private GLVertexBufferObject m_vbo;
   private GLVertexBufferObject m_ibo;
   private ByteBuffer m_elements;
   private ByteBuffer m_indices;
   private float m_dx = 0.0F;
   private float m_dy = 0.0F;
   private float m_dz = 0.0F;
   private ShaderProgram m_currentShaderProgram;
   private final VBORendererCommands m_commands = new VBORendererCommands(this);
   private final VBORendererCommands m_commandsReady = new VBORendererCommands(this);
   private Boolean forceDepthTest = null;
   private Float forceUserDepth = null;
   private final ObjectPool<Run> m_runPool = new ObjectPool(Run::new);
   private final ArrayList<Run> m_runs = new ArrayList();
   private Run m_runInProgress = null;
   private static final Vector3f tempVector3f_1 = new Vector3f();
   private static final Vector3f tempVector3f_2 = new Vector3f();

   public static VBORenderer getInstance() {
      if (instance == null) {
         instance = new VBORenderer();
      }

      return instance;
   }

   private Run currentRun() {
      return this.m_runInProgress;
   }

   public VBORenderer() {
      this.FORMAT_PositionColor.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.FORMAT_PositionColor.setElement(1, VertexBufferObject.VertexType.ColorArray, 16);
      this.FORMAT_PositionColor.calculate();
      this.FORMAT_PositionColorUV.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.FORMAT_PositionColorUV.setElement(1, VertexBufferObject.VertexType.ColorArray, 16);
      this.FORMAT_PositionColorUV.setElement(2, VertexBufferObject.VertexType.TextureCoordArray, 8);
      this.FORMAT_PositionColorUV.calculate();
      this.FORMAT_PositionColorUVDepth.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.FORMAT_PositionColorUVDepth.setElement(1, VertexBufferObject.VertexType.ColorArray, 16);
      this.FORMAT_PositionColorUVDepth.setElement(2, VertexBufferObject.VertexType.TextureCoordArray, 8);
      this.FORMAT_PositionColorUVDepth.setElement(3, VertexBufferObject.VertexType.Depth, 4);
      this.FORMAT_PositionColorUVDepth.calculate();
      this.FORMAT_PositionNormalColor.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.FORMAT_PositionNormalColor.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
      this.FORMAT_PositionNormalColor.setElement(2, VertexBufferObject.VertexType.ColorArray, 16);
      this.FORMAT_PositionNormalColor.calculate();
      this.FORMAT_PositionNormalColorUV.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      this.FORMAT_PositionNormalColorUV.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
      this.FORMAT_PositionNormalColorUV.setElement(2, VertexBufferObject.VertexType.ColorArray, 16);
      this.FORMAT_PositionNormalColorUV.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
      this.FORMAT_PositionNormalColorUV.calculate();
      this.setFormat(this.FORMAT_PositionColorUV);
   }

   private void setFormat(VertexBufferObject.VertexFormat var1) {
      if (var1 != this.m_format) {
         this.m_format = var1;
         this.ELEMENT_SIZE = this.m_format.getStride();
         this.NUM_ELEMENTS = 4096 / this.ELEMENT_SIZE;
         this.VERTEX_OFFSET = -1;
         this.NORMAL_OFFSET = -1;
         this.COLOR_OFFSET = -1;
         this.UV1_OFFSET = -1;
         this.UV2_OFFSET = -1;
         this.DEPTH_OFFSET = -1;

         for(int var2 = 0; var2 < this.m_format.getNumElements(); ++var2) {
            VertexBufferObject.VertexElement var3 = this.m_format.getElement(var2);
            switch (var3.m_type) {
               case VertexArray:
                  this.VERTEX_OFFSET = var3.m_byteOffset;
                  break;
               case NormalArray:
                  this.NORMAL_OFFSET = var3.m_byteOffset;
                  break;
               case ColorArray:
                  this.COLOR_OFFSET = var3.m_byteOffset;
                  break;
               case TextureCoordArray:
                  if (this.UV1_OFFSET == -1) {
                     this.UV1_OFFSET = var3.m_byteOffset;
                  } else {
                     this.UV2_OFFSET = var3.m_byteOffset;
                  }
                  break;
               case Depth:
                  this.DEPTH_OFFSET = var3.m_byteOffset;
            }
         }

      }
   }

   private void create() {
      byte var1 = 8;
      int var2 = 4096 / var1;
      this.m_elements = BufferUtils.createByteBuffer(4096);
      this.m_indices = BufferUtils.createByteBuffer(var2 * 2);
      IGLBufferObject var3 = GLVertexBufferObject.funcs;
      this.m_vbo = new GLVertexBufferObject(4096L, var3.GL_ARRAY_BUFFER(), var3.GL_STREAM_DRAW());
      this.m_vbo.create();
      this.m_ibo = new GLVertexBufferObject((long)(var2 * 2), var3.GL_ELEMENT_ARRAY_BUFFER(), var3.GL_STREAM_DRAW());
      this.m_ibo.create();
   }

   public void setOffset(float var1, float var2, float var3) {
      this.m_dx = var1;
      this.m_dy = var2;
      this.m_dz = var3;
   }

   public VBORenderer addElement() {
      if (this.isFull()) {
         this.flush();
      }

      if (this.m_elements == null) {
         this.create();
      }

      short var1 = (short)(this.m_elements.position() / this.ELEMENT_SIZE);
      this.m_indices.putShort(var1);
      this.m_elements.position(this.m_elements.position() + this.ELEMENT_SIZE);
      ++this.currentRun().vertexCount;
      return this;
   }

   public VBORenderer putByte(byte var1) {
      this.m_elements.put(var1);
      return this;
   }

   public VBORenderer putFloat(float var1) {
      this.m_elements.putFloat(var1);
      return this;
   }

   public VBORenderer putInt(int var1) {
      this.m_elements.putInt(var1);
      return this;
   }

   public VBORenderer putShort(short var1) {
      this.m_elements.putShort(var1);
      return this;
   }

   public void setFloats1(int var1, float var2) {
      if (var1 != -1) {
         int var3 = this.m_elements.position() - this.ELEMENT_SIZE + var1;
         this.m_elements.putFloat(var3, var2);
      }
   }

   public void setFloats2(int var1, float var2, float var3) {
      if (var1 != -1) {
         int var4 = this.m_elements.position() - this.ELEMENT_SIZE + var1;
         this.m_elements.putFloat(var4, var2);
         var4 += 4;
         this.m_elements.putFloat(var4, var3);
      }
   }

   public void setFloats3(int var1, float var2, float var3, float var4) {
      if (var1 != -1) {
         int var5 = this.m_elements.position() - this.ELEMENT_SIZE + var1;
         this.m_elements.putFloat(var5, var2);
         var5 += 4;
         this.m_elements.putFloat(var5, var3);
         var5 += 4;
         this.m_elements.putFloat(var5, var4);
      }
   }

   public void setFloats4(int var1, float var2, float var3, float var4, float var5) {
      if (var1 != -1) {
         int var6 = this.m_elements.position() - this.ELEMENT_SIZE + var1;
         this.m_elements.putFloat(var6, var2);
         var6 += 4;
         this.m_elements.putFloat(var6, var3);
         var6 += 4;
         this.m_elements.putFloat(var6, var4);
         var6 += 4;
         this.m_elements.putFloat(var6, var5);
      }
   }

   public void setVertex(float var1, float var2, float var3) {
      this.setFloats3(this.VERTEX_OFFSET, var1, var2, var3);
   }

   public void setNormal(float var1, float var2, float var3) {
      this.setFloats3(this.NORMAL_OFFSET, var1, var2, var3);
   }

   public void setColor(float var1, float var2, float var3, float var4) {
      this.setFloats4(this.COLOR_OFFSET, var1, var2, var3, var4);
   }

   public void setUV1(float var1, float var2) {
      this.setFloats2(this.UV1_OFFSET, var1, var2);
   }

   public void setUV2(float var1, float var2) {
      this.setFloats2(this.UV2_OFFSET, var1, var2);
   }

   public void setDepth(float var1) {
      this.setFloats1(this.DEPTH_OFFSET, var1);
   }

   public void addElement(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      this.addElement();
      this.setVertex(this.m_dx + var1, this.m_dy + var2, this.m_dz + var3);
      this.setColor(var6, var7, var8, var9);
      this.setUV1(var4, var5);
   }

   public void addElementDepth(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
      this.addElement(var1, var2, var3, var4, var5, var7, var8, var9, var10);
      this.setDepth(var6);
   }

   public void addElement(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      this.addElement();
      this.setVertex(this.m_dx + var1, this.m_dy + var2, this.m_dz + var3);
      this.setColor(var4, var5, var6, var7);
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

   public void addLineWithThickness(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11) {
      Vector3f var12 = tempVector3f_1.set(var4 - var1, var5 - var2, 0.0F).normalize();
      Vector3f var13 = var12.cross(0.0F, 0.0F, 1.0F, tempVector3f_2);
      var13.x *= var7;
      var13.y *= var7;
      float var14 = var1 - var13.x / 2.0F;
      float var15 = var1 + var13.x / 2.0F;
      float var16 = var4 - var13.x / 2.0F;
      float var17 = var4 + var13.x / 2.0F;
      float var18 = var2 - var13.y / 2.0F;
      float var19 = var2 + var13.y / 2.0F;
      float var20 = var5 - var13.y / 2.0F;
      float var21 = var5 + var13.y / 2.0F;
      float var22 = 0.0F;
      float var23 = 0.0F;
      this.addQuad(var14, var18, var22, var23, var15, var19, var22, var23, var17, var21, var22, var23, var16, var20, var22, var23, var3, var8, var9, var10, var11);
   }

   public void addTriangle(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19) {
      this.reserve(3);
      this.addElement(var1, var2, var3, var4, var5, var16, var17, var18, var19);
      this.addElement(var6, var7, var8, var9, var10, var16, var17, var18, var19);
      this.addElement(var11, var12, var13, var14, var15, var16, var17, var18, var19);
   }

   public void addTriangleDepth(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19, float var20, float var21, float var22) {
      if (this.currentRun().mode == 1) {
         this.reserve(6);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var19, var20, var21, var22);
         this.addElementDepth(var7, var8, var9, var10, var11, var12, var19, var20, var21, var22);
         this.addElementDepth(var7, var8, var9, var10, var11, var12, var19, var20, var21, var22);
         this.addElementDepth(var13, var14, var15, var16, var17, var18, var19, var20, var21, var22);
         this.addElementDepth(var13, var14, var15, var16, var17, var18, var19, var20, var21, var22);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var19, var20, var21, var22);
      } else {
         this.reserve(3);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var19, var20, var21, var22);
         this.addElementDepth(var7, var8, var9, var10, var11, var12, var19, var20, var21, var22);
         this.addElementDepth(var13, var14, var15, var16, var17, var18, var19, var20, var21, var22);
      }
   }

   public void addTriangleDepth(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19, float var20, float var21, float var22, float var23, float var24, float var25) {
      if (this.currentRun().mode == 1) {
         this.reserve(6);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var22, var23, var24, var25 * var7);
         this.addElementDepth(var8, var9, var10, var11, var12, var13, var22, var23, var24, var25 * var14);
         this.addElementDepth(var8, var9, var10, var11, var12, var13, var22, var23, var24, var25 * var14);
         this.addElementDepth(var15, var16, var17, var18, var19, var20, var22, var23, var24, var25 * var21);
         this.addElementDepth(var15, var16, var17, var18, var19, var20, var22, var23, var24, var25 * var21);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var22, var23, var24, var25 * var7);
      } else {
         this.reserve(3);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var22, var23, var24, var25 * var7);
         this.addElementDepth(var8, var9, var10, var11, var12, var13, var22, var23, var24, var25 * var14);
         this.addElementDepth(var15, var16, var17, var18, var19, var20, var22, var23, var24, var25 * var21);
      }
   }

   public void addTriangle(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13) {
      this.reserve(3);
      this.addElement(var1, var2, var3, var10, var11, var12, var13);
      this.addElement(var4, var5, var6, var10, var11, var12, var13);
      this.addElement(var7, var8, var9, var10, var11, var12, var13);
   }

   public void addQuad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      if (this.currentRun().mode == 1) {
         this.reserve(6);
         this.addLine(var1, var2, var5, var3, var2, var5, var6, var7, var8, var9);
         this.addLine(var3, var2, var5, var3, var4, var5, var6, var7, var8, var9);
         this.addLine(var3, var4, var5, var1, var4, var5, var6, var7, var8, var9);
         this.addLine(var1, var4, var5, var1, var2, var5, var6, var7, var8, var9);
      } else if (this.currentRun().mode == 4) {
         this.reserve(6);
         this.addTriangle(var1, var2, var5, var3, var2, var5, var1, var4, var5, var6, var7, var8, var9);
         this.addTriangle(var3, var2, var5, var3, var4, var5, var1, var4, var5, var6, var7, var8, var9);
      } else {
         this.reserve(4);
         this.addElement(var1, var2, var5, var6, var7, var8, var9);
         this.addElement(var3, var2, var5, var6, var7, var8, var9);
         this.addElement(var3, var4, var5, var6, var7, var8, var9);
         this.addElement(var1, var4, var5, var6, var7, var8, var9);
      }
   }

   public void addQuad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13) {
      this.addQuad(var1, var2, var3, var4, var5, var2, var7, var4, var5, var6, var7, var8, var1, var6, var3, var8, var9, var10, var11, var12, var13);
   }

   public void addQuad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19, float var20, float var21) {
      this.addQuad(var1, var2, var17, var3, var4, var5, var6, var17, var7, var8, var9, var10, var17, var11, var12, var13, var14, var17, var15, var16, var18, var19, var20, var21);
   }

   public void addQuad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19, float var20, float var21, float var22, float var23, float var24) {
      if (this.currentRun().mode == 4) {
         this.reserve(6);
         this.addTriangle(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var16, var17, var18, var19, var20, var21, var22, var23, var24);
         this.addTriangle(var6, var7, var8, var9, var10, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20, var21, var22, var23, var24);
      } else {
         this.reserve(4);
         this.addElement(var1, var2, var3, var4, var5, var21, var22, var23, var24);
         this.addElement(var6, var7, var8, var9, var10, var21, var22, var23, var24);
         this.addElement(var11, var12, var13, var14, var15, var21, var22, var23, var24);
         this.addElement(var16, var17, var18, var19, var20, var21, var22, var23, var24);
      }
   }

   public void addQuadDepth(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14, float var15, float var16, float var17, float var18, float var19, float var20, float var21, float var22, float var23, float var24, float var25, float var26, float var27, float var28) {
      if (this.currentRun().mode == 4) {
         this.reserve(6);
         this.addTriangleDepth(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var19, var20, var21, var22, var23, var24, var25, var26, var27, var28);
         this.addTriangleDepth(var7, var8, var9, var10, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20, var21, var22, var23, var24, var25, var26, var27, var28);
      } else {
         this.reserve(4);
         this.addElementDepth(var1, var2, var3, var4, var5, var6, var25, var26, var27, var28);
         this.addElementDepth(var7, var8, var9, var10, var11, var12, var25, var26, var27, var28);
         this.addElementDepth(var13, var14, var15, var16, var17, var18, var25, var26, var27, var28);
         this.addElementDepth(var19, var20, var21, var22, var23, var24, var25, var26, var27, var28);
      }
   }

   public void addAABB(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, boolean var14) {
      if (var14) {
         zombie.core.skinnedmodel.shader.Shader var15 = ShaderManager.instance.getOrCreateShader("debug_chunk_state_geometry", false, false);
         ShaderProgram var16 = var15.getShaderProgram();
         if (!var16.isCompiled()) {
            var16 = null;
         }

         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2884);
         GL11.glDepthFunc(515);
         GL11.glDepthMask(true);
         this.setDepthTestForAllRuns(Boolean.TRUE);
         this.addBox(var7 - var4, var8 - var5, var9 - var6, var10, var11, var12, var13, var16);
         this.flush();
         this.setDepthTestForAllRuns((Boolean)null);
         GL11.glDepthMask(false);
      } else {
         this.setOffset(var1, var2, var3);
         this.startRun(this.FORMAT_PositionColor);
         this.setMode(1);
         this.setLineWidth(1.0F);
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
         this.endRun();
         this.flush();
         this.setOffset(0.0F, 0.0F, 0.0F);
      }
   }

   public void addAABB(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      float var10 = var4 / 2.0F;
      float var11 = var5 / 2.0F;
      float var12 = var6 / 2.0F;
      this.addAABB(var1, var2, var3, -var10, -var11, -var12, var10, var11, var12, var7, var8, var9, 1.0F, false);
   }

   public void addAABB(float var1, float var2, float var3, Vector3f var4, Vector3f var5, float var6, float var7, float var8) {
      this.addAABB(var1, var2, var3, var4.x, var4.y, var4.z, var5.x, var5.y, var5.z, var6, var7, var8, 1.0F, false);
   }

   boolean isFull() {
      Run var1 = this.currentRun();
      if (this.m_elements == null) {
         return false;
      } else if (var1.mode == 4 && var1.vertexCount % 3 == 0 && this.m_elements.position() + 3 * this.ELEMENT_SIZE > this.ELEMENT_SIZE * this.NUM_ELEMENTS) {
         return true;
      } else if (var1.mode == 7 && var1.vertexCount % 4 == 0 && this.m_elements.position() + 4 * this.ELEMENT_SIZE > this.ELEMENT_SIZE * this.NUM_ELEMENTS) {
         return true;
      } else {
         return this.m_elements.position() >= this.ELEMENT_SIZE * this.NUM_ELEMENTS;
      }
   }

   public void reserve(int var1) {
      if (!this.hasRoomFor(var1)) {
         this.flush();
      }

   }

   boolean hasRoomFor(int var1) {
      return this.m_elements == null || this.m_elements.position() + this.ELEMENT_SIZE * var1 <= this.m_elements.limit();
   }

   private VBOLinesShader initShader(String var1, VBOLinesShader var2) {
      return var2 == null ? new VBOLinesShader(var1) : var2;
   }

   private VBOLinesShader getShaderForFormat() {
      if (this.m_format == this.FORMAT_PositionColor) {
         return this.m_shader_PositionColor = this.initShader("vboRenderer_PositionColor", this.m_shader_PositionColor);
      } else if (this.m_format == this.FORMAT_PositionColorUV) {
         return this.m_shader_PositionColorUV = this.initShader("vboRenderer_PositionColorUV", this.m_shader_PositionColorUV);
      } else if (this.m_format == this.FORMAT_PositionColorUVDepth) {
         return this.m_shader_PositionColorUVDepth = this.initShader("vboRenderer_PositionColorUVDepth", this.m_shader_PositionColorUVDepth);
      } else if (this.m_format == this.FORMAT_PositionNormalColor) {
         return this.m_shader_PositionNormalColor = this.initShader("vboRenderer_PositionNormalColor", this.m_shader_PositionNormalColor);
      } else {
         return this.m_format == this.FORMAT_PositionNormalColorUV ? (this.m_shader_PositionNormalColorUV = this.initShader("vboRenderer_PositionNormalColorUV", this.m_shader_PositionNormalColorUV)) : null;
      }
   }

   public void flush() {
      boolean var1;
      if (this.m_runInProgress == null) {
         this.m_commandsReady.adopt(this.m_commands);
      } else if (this.m_runInProgress.vertexCount > 0) {
         this.m_commands.cmdRenderRun();
         this.m_commandsReady.adopt(this.m_commands);
         this.m_commands.cmdStartRun();
      } else {
         var1 = true;
      }

      if (this.m_elements != null && this.m_elements.position() != 0) {
         this.m_elements.flip();
         this.m_indices.flip();
         GL13.glActiveTexture(33984);
         this.m_vbo.bind();
         this.m_vbo.bufferData(this.m_elements);
         this.m_ibo.bind();
         this.m_ibo.bufferData(this.m_indices);
         GL11.glDisableClientState(32884);
         GL11.glDisableClientState(32886);
         GL11.glDisableClientState(32888);
         GL11.glEnable(2848);
         this.m_currentShaderProgram = null;
         this.m_formatUsedByVertexAttribArray = null;
         this.m_commandsReady.invoke();
         var1 = true;
         int var2;
         if (this.m_runInProgress != null) {
            if (this.m_runInProgress.mode != 8) {
               this.m_runInProgress.startVertex = 0;
               this.m_runInProgress.startIndex = 0;
               this.m_runInProgress.vertexCount = 0;
            } else {
               var2 = this.m_runInProgress.startVertex + this.m_runInProgress.vertexCount - 2;

               for(int var3 = 0; var3 < this.ELEMENT_SIZE * 2; ++var3) {
                  this.m_elements.put(var3, this.m_elements.get(var2 * this.ELEMENT_SIZE + var3));
               }

               this.m_indices.putShort(0, (short)0);
               this.m_indices.putShort(2, (short)1);
               this.m_elements.limit(this.m_elements.capacity());
               this.m_indices.limit(this.m_indices.capacity());
               this.m_elements.position(this.ELEMENT_SIZE * 2);
               this.m_indices.position(4);
               this.m_runInProgress.startVertex = 0;
               this.m_runInProgress.startIndex = 0;
               this.m_runInProgress.vertexCount = 2;
               var1 = false;
            }

            this.setFormat(this.m_runInProgress.format);
         }

         this.m_vbo.bindNone();
         this.m_ibo.bindNone();
         if (var1) {
            this.m_elements.clear();
            this.m_indices.clear();
         }

         this.m_commandsReady.clear();
         this.m_runPool.releaseAll(this.m_runs);
         this.m_runs.clear();
         if (this.m_runInProgress == null && this.m_currentShaderProgram != null) {
            this.m_currentShaderProgram.End();
            this.m_currentShaderProgram = null;
         }

         GL11.glEnable(2929);
         GL11.glDisable(2848);

         for(var2 = 0; var2 < 5; ++var2) {
            GL20.glEnableVertexAttribArray(var2);
         }

         this.m_formatUsedByVertexAttribArray = null;
         GL13.glActiveTexture(33984);
         SpriteRenderer.ringBuffer.restoreVBOs = true;
         SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      } else {
         this.m_commandsReady.invoke();
         this.m_commandsReady.clear();
      }
   }

   private void useShaderProgram(ShaderProgram var1) {
      if (var1 == this.m_currentShaderProgram) {
         VertexBufferObject.setModelViewProjection(this.m_currentShaderProgram);
      } else {
         this.m_currentShaderProgram = var1;
         if (var1 == null) {
            ShaderHelper.glUseProgramObjectARB(0);
         } else {
            var1.Start();
            VertexBufferObject.setModelViewProjection(this.m_currentShaderProgram);
         }
      }
   }

   private void setVertexAttribArrays(VertexBufferObject.VertexFormat var1) {
      this.setFormat(var1);
      this.m_formatUsedByVertexAttribArray = var1;

      int var2;
      for(var2 = 0; var2 < var1.getNumElements(); ++var2) {
         VertexBufferObject.VertexElement var3 = var1.getElement(var2);
         GL20.glEnableVertexAttribArray(var2);
         boolean var4 = var3.m_type == VertexBufferObject.VertexType.ColorArray;
         GL20.glVertexAttribPointer(var2, var3.m_byteSize / 4, 5126, var4, this.ELEMENT_SIZE, (long)var3.m_byteOffset);
      }

      for(var2 = var1.getNumElements(); var2 < 5; ++var2) {
         GL20.glDisableVertexAttribArray(var2);
      }

   }

   private void startRun(Run var1) {
      if (var1.textureID == null) {
         GL11.glDisable(3553);
      } else {
         GL11.glEnable(3553);
         var1.textureID.bind();
         if (var1.minFilter != 0) {
            GL11.glTexParameteri(3553, 10241, var1.minFilter);
         }

         if (var1.magFilter != 0) {
            GL11.glTexParameteri(3553, 10240, var1.magFilter);
         }
      }

      if (this.forceDepthTest != Boolean.TRUE && !var1.depthTest) {
         GL11.glDisable(2929);
      } else {
         GL11.glEnable(2929);
      }

      this.useShaderProgram(var1.shaderProgram);
      if (var1.shaderProgram != null) {
         var1.shaderProgram.setValue("userDepth", this.forceUserDepth == null ? var1.userDepth : this.forceUserDepth);
      }

      GL11.glLineWidth(PZMath.min(var1.lineWidth, 1.0F));
      if (var1.format != this.m_formatUsedByVertexAttribArray) {
         this.setVertexAttribArrays(var1.format);
      }

   }

   private void renderRun(Run var1) {
      if (var1.vertexCount != 0) {
         GL12.glDrawRangeElements(var1.mode, var1.startVertex, var1.startVertex + var1.vertexCount, var1.vertexCount, 5123, (long)var1.startIndex * 2L);
      }
   }

   void startNextRun() {
      Run var1 = this.m_runs.isEmpty() ? this.m_runInProgress : (Run)this.m_runs.get(0);
      if (var1 != null) {
         if (var1.vertexCount != 0) {
            this.startRun(var1);
         }
      }
   }

   void renderNextRun() {
      if (this.m_runs.isEmpty()) {
         if (this.m_runInProgress != null) {
            this.renderRun(this.m_runInProgress);
         }
      } else {
         Run var1 = (Run)this.m_runs.remove(0);
         this.renderRun(var1);
         this.m_runPool.release((Object)var1);
      }
   }

   public void setDepthTest(boolean var1) {
      this.currentRun().depthTest = var1;
   }

   public void setDepthTestForAllRuns(Boolean var1) {
      this.forceDepthTest = var1;
   }

   public void setUserDepthForAllRuns(Float var1) {
      this.forceUserDepth = var1;
   }

   public void setUserDepth(float var1) {
      this.currentRun().userDepth = var1;
   }

   public void setLineWidth(float var1) {
      this.currentRun().lineWidth = var1;
   }

   public void setMode(int var1) {
      this.currentRun().mode = var1;
   }

   public void setShaderProgram(ShaderProgram var1) {
      this.currentRun().shaderProgram = var1;
   }

   public void setTextureID(TextureID var1) {
      this.currentRun().textureID = var1;
   }

   public void setMinMagFilters(int var1, int var2) {
      this.currentRun().minFilter = var1;
      this.currentRun().magFilter = var2;
   }

   private void checkVertexBufferAlignment(int var1) {
      if (this.m_elements != null) {
         if (this.m_elements.position() % var1 != 0) {
            int var2 = this.m_elements.position() + (var1 - this.m_elements.position() % var1);
            if (var2 >= this.m_elements.limit()) {
               this.flush();
            }

         }
      }
   }

   public void startRun(VertexBufferObject.VertexFormat var1) {
      if (this.m_elements != null) {
         this.checkVertexBufferAlignment(var1.getStride());
      }

      this.setFormat(var1);
      if (this.m_runInProgress != null) {
         this.m_runPool.release((Object)this.m_runInProgress);
         this.m_runInProgress = null;
         throw new RuntimeException("forgot to call endRun()");
      } else {
         Run var2 = ((Run)this.m_runPool.alloc()).init();
         var2.format = var1;
         if (this.m_elements != null) {
            if (this.m_elements.position() % this.ELEMENT_SIZE != 0) {
               int var3 = this.m_elements.position() + (this.ELEMENT_SIZE - this.m_elements.position() % this.ELEMENT_SIZE);
               this.m_elements.position(var3);
            }

            var2.startVertex = this.m_elements.position() / this.ELEMENT_SIZE;
            var2.startIndex = this.m_indices.position() / 2;
         }

         VBOLinesShader var4 = this.getShaderForFormat();
         var2.shaderProgram = var4 == null ? null : var4.getProgram();
         this.m_commands.cmdStartRun();
         this.m_runInProgress = var2;
      }
   }

   public void endRun() {
      if (this.m_runInProgress == null) {
         throw new RuntimeException("forgot to call startRun()");
      } else {
         this.m_runInProgress.bEnded = true;
         this.m_runs.add(this.m_runInProgress);
         this.m_commands.cmdRenderRun();
         this.m_commandsReady.adopt(this.m_commands);
         this.m_runInProgress = null;
      }
   }

   public void cmdPushAndLoadMatrix(int var1, Matrix4f var2) {
      this.m_commands.cmdPushAndLoadMatrix(var1, var2);
   }

   public void cmdPushAndMultMatrix(int var1, Matrix4f var2) {
      this.m_commands.cmdPushAndMultMatrix(var1, var2);
   }

   public void cmdPopMatrix(int var1) {
      this.m_commands.cmdPopMatrix(var1);
   }

   public void cmdShader1f(String var1, float var2) {
      ShaderProgram.Uniform var3 = this.currentRun().shaderProgram.getUniform(var1, 5126);
      if (var3 != null) {
         this.m_commands.cmdShader1f(var3.loc, var2);
      }
   }

   public void cmdShader2f(String var1, float var2, float var3) {
      ShaderProgram.Uniform var4 = this.currentRun().shaderProgram.getUniform(var1, 35664);
      if (var4 != null) {
         this.m_commands.cmdShader2f(var4.loc, var2, var3);
      }
   }

   public void cmdShader3f(String var1, float var2, float var3, float var4) {
      ShaderProgram.Uniform var5 = this.currentRun().shaderProgram.getUniform(var1, 35664);
      if (var5 != null) {
         this.m_commands.cmdShader3f(var5.loc, var2, var3, var4);
      }
   }

   public void cmdShader1f(int var1, float var2) {
      this.m_commands.cmdShader1f(var1, var2);
   }

   public void cmdShader4f(int var1, float var2, float var3, float var4, float var5) {
      this.m_commands.cmdShader4f(var1, var2, var3, var4, var5);
   }

   public void cmdUseProgram(ShaderProgram var1) {
      this.m_commands.cmdUseProgram(var1);
   }

   float cos(float var1) {
      return (float)Math.cos((double)var1);
   }

   float sin(float var1) {
      return (float)Math.sin((double)var1);
   }

   void normal3f(float var1, float var2, float var3) {
      float var4 = (float)Math.sqrt((double)(var1 * var1 + var2 * var2 + var3 * var3));
      if (var4 > 1.0E-5F) {
         var1 /= var4;
         var2 /= var4;
         var3 /= var4;
      }

      this.setNormal(var1, var2, var3);
   }

   public void addBox(float var1, float var2, float var3, float var4, float var5, float var6, float var7, ShaderProgram var8) {
      this.startRun(this.FORMAT_PositionNormalColor);
      if (var8 != null) {
         this.setShaderProgram(var8);
      }

      this.setMode(7);
      float var9 = var1 / 2.0F;
      float var10 = var2 / 2.0F;
      float var11 = var3 / 2.0F;
      float var12 = -var1 / 2.0F;
      float var13 = -var2 / 2.0F;
      float var14 = -var3 / 2.0F;
      this.reserve(4);
      this.addElement(var9, var10, var11, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, 1.0F);
      this.addElement(var12, var10, var11, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, 1.0F);
      this.addElement(var12, var13, var11, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, 1.0F);
      this.addElement(var9, var13, var11, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, 1.0F);
      this.reserve(4);
      this.addElement(var9, var10, var14, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, -1.0F);
      this.addElement(var12, var10, var14, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, -1.0F);
      this.addElement(var12, var13, var14, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, -1.0F);
      this.addElement(var9, var13, var14, var4, var5, var6, var7);
      this.normal3f(0.0F, 0.0F, -1.0F);
      this.reserve(4);
      this.addElement(var9, var10, var11, var4, var5, var6, var7);
      this.normal3f(1.0F, 0.0F, 0.0F);
      this.addElement(var9, var13, var11, var4, var5, var6, var7);
      this.normal3f(1.0F, 0.0F, 0.0F);
      this.addElement(var9, var13, var14, var4, var5, var6, var7);
      this.normal3f(1.0F, 0.0F, 0.0F);
      this.addElement(var9, var10, var14, var4, var5, var6, var7);
      this.normal3f(1.0F, 0.0F, 0.0F);
      this.reserve(4);
      this.addElement(var12, var10, var11, var4, var5, var6, var7);
      this.normal3f(-1.0F, 0.0F, 0.0F);
      this.addElement(var12, var13, var11, var4, var5, var6, var7);
      this.normal3f(-1.0F, 0.0F, 0.0F);
      this.addElement(var12, var13, var14, var4, var5, var6, var7);
      this.normal3f(-1.0F, 0.0F, 0.0F);
      this.addElement(var12, var10, var14, var4, var5, var6, var7);
      this.normal3f(-1.0F, 0.0F, 0.0F);
      boolean var15 = false;
      if (var15) {
         this.reserve(4);
         this.addElement(var9, var10, var11, var4, var5, var6, var7);
         this.normal3f(0.0F, 1.0F, 0.0F);
         this.addElement(var12, var10, var11, var4, var5, var6, var7);
         this.normal3f(0.0F, 1.0F, 0.0F);
         this.addElement(var12, var10, var14, var4, var5, var6, var7);
         this.normal3f(0.0F, 1.0F, 0.0F);
         this.addElement(var9, var10, var14, var4, var5, var6, var7);
         this.normal3f(0.0F, 1.0F, 0.0F);
      }

      this.endRun();
   }

   public void addCylinder_Fill(float var1, float var2, float var3, int var4, int var5, float var6, float var7, float var8, float var9) {
      this.addCylinder(100012, 100020, var1, var2, var3, var4, var5, var6, var7, var8, var9, (ShaderProgram)null);
   }

   public void addCylinder_Line(float var1, float var2, float var3, int var4, int var5, float var6, float var7, float var8, float var9) {
      this.addCylinder(100011, 100020, var1, var2, var3, var4, var5, var6, var7, var8, var9, (ShaderProgram)null);
   }

   public void addCylinder_Fill(float var1, float var2, float var3, int var4, int var5, float var6, float var7, float var8, float var9, ShaderProgram var10) {
      this.addCylinder(100012, 100020, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
   }

   public void addCylinder_Line(float var1, float var2, float var3, int var4, int var5, float var6, float var7, float var8, float var9, ShaderProgram var10) {
      this.addCylinder(100011, 100020, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
   }

   public void addCylinder(int var1, int var2, float var3, float var4, float var5, int var6, int var7, float var8, float var9, float var10, float var11, ShaderProgram var12) {
      float var21;
      if (var2 == 100021) {
         var21 = -1.0F;
      } else {
         var21 = 1.0F;
      }

      float var13 = 6.2831855F / (float)var6;
      float var15 = (var4 - var3) / (float)var7;
      float var16 = var5 / (float)var7;
      float var20 = (var3 - var4) / var5;
      float var14;
      float var17;
      float var18;
      float var19;
      int var22;
      int var23;
      if (var1 == 100010) {
         this.startRun(this.FORMAT_PositionColor);
         this.setMode(0);

         for(var22 = 0; var22 < var6; ++var22) {
            var19 = 0.0F;
            var14 = var3;

            for(var23 = 0; var23 <= var7; ++var23) {
               var17 = this.cos((float)var22 * var13);
               var18 = this.sin((float)var22 * var13);
               this.addElement();
               this.normal3f(var17 * var21, var18 * var21, var20 * var21);
               this.setVertex(var17 * var14, var18 * var14, var19);
               var19 += var16;
               var14 += var15;
            }
         }

         this.endRun();
      } else if (var1 != 100011 && var1 != 100013) {
         if (var1 == 100012) {
            float var24 = 1.0F / (float)var6;
            float var25 = 1.0F / (float)var7;
            float var26 = 0.0F;
            var19 = 0.0F;
            var14 = var3;

            for(var23 = 0; var23 < var7; ++var23) {
               float var27 = 0.0F;
               this.startRun(this.FORMAT_PositionNormalColor);
               if (var12 != null) {
                  this.setShaderProgram(var12);
               }

               this.setMode(8);

               for(var22 = 0; var22 <= var6; ++var22) {
                  if (var22 == var6) {
                     var17 = this.sin(0.0F);
                     var18 = this.cos(0.0F);
                  } else {
                     var17 = this.sin((float)var22 * var13);
                     var18 = this.cos((float)var22 * var13);
                  }

                  this.reserve(2);
                  if (var21 == 1.0F) {
                     this.addElement();
                     this.normal3f(var17 * var21, var18 * var21, var20 * var21);
                     this.setUV1(var27, var26);
                     this.setVertex(var17 * var14, var18 * var14, var19);
                     this.setColor(var8, var9, var10, var11);
                     this.addElement();
                     this.normal3f(var17 * var21, var18 * var21, var20 * var21);
                     this.setUV1(var27, var26 + var25);
                     this.setVertex(var17 * (var14 + var15), var18 * (var14 + var15), var19 + var16);
                     this.setColor(var8, var9, var10, var11);
                  } else {
                     this.addElement();
                     this.normal3f(var17 * var21, var18 * var21, var20 * var21);
                     this.setUV1(var27, var26);
                     this.setVertex(var17 * var14, var18 * var14, var19);
                     this.setColor(var8, var9, var10, var11);
                     this.addElement();
                     this.normal3f(var17 * var21, var18 * var21, var20 * var21);
                     this.setUV1(var27, var26 + var25);
                     this.setVertex(var17 * (var14 + var15), var18 * (var14 + var15), var19 + var16);
                     this.setColor(var8, var9, var10, var11);
                  }

                  var27 += var24;
               }

               this.endRun();
               var14 += var15;
               var26 += var25;
               var19 += var16;
            }
         }
      } else {
         if (var1 == 100011) {
            var19 = 0.0F;
            var14 = var3;

            for(var23 = 0; var23 <= var7; ++var23) {
               this.startRun(this.FORMAT_PositionNormalColor);
               this.setMode(2);

               for(var22 = 0; var22 < var6; ++var22) {
                  var17 = this.cos((float)var22 * var13);
                  var18 = this.sin((float)var22 * var13);
                  this.addElement();
                  this.normal3f(var17 * var21, var18 * var21, var20 * var21);
                  this.setVertex(var17 * var14, var18 * var14, var19);
                  this.setColor(var8, var9, var10, var11);
               }

               this.endRun();
               var19 += var16;
               var14 += var15;
            }
         } else if ((double)var3 != 0.0) {
            this.startRun(this.FORMAT_PositionNormalColor);
            this.setMode(2);

            for(var22 = 0; var22 < var6; ++var22) {
               var17 = this.cos((float)var22 * var13);
               var18 = this.sin((float)var22 * var13);
               this.addElement();
               this.normal3f(var17 * var21, var18 * var21, var20 * var21);
               this.setVertex(var17 * var3, var18 * var3, 0.0F);
               this.setColor(var8, var9, var10, var11);
            }

            this.endRun();
            this.startRun(this.FORMAT_PositionNormalColor);
            this.setMode(2);

            for(var22 = 0; var22 < var6; ++var22) {
               var17 = this.cos((float)var22 * var13);
               var18 = this.sin((float)var22 * var13);
               this.addElement();
               this.normal3f(var17 * var21, var18 * var21, var20 * var21);
               this.setVertex(var17 * var4, var18 * var4, var5);
               this.setColor(var8, var9, var10, var11);
            }

            this.endRun();
         }

         this.startRun(this.FORMAT_PositionNormalColor);
         this.setMode(1);

         for(var22 = 0; var22 < var6; ++var22) {
            var17 = this.cos((float)var22 * var13);
            var18 = this.sin((float)var22 * var13);
            this.addElement();
            this.setNormal(var17 * var21, var18 * var21, var20 * var21);
            this.setVertex(var17 * var3, var18 * var3, 0.0F);
            this.setColor(var8, var9, var10, var11);
            this.addElement();
            this.setNormal(var17 * var21, var18 * var21, var20 * var21);
            this.setVertex(var17 * var4, var18 * var4, var5);
            this.setColor(var8, var9, var10, var11);
         }

         this.endRun();
      }

   }

   public void addTorus(double var1, double var3, int var5, int var6, float var7, float var8, float var9, UI3DScene.Ray var10) {
      GL11.glFrontFace(2304);
      double var11 = 3.141592653589793;
      double var13 = 2.0 * var11;
      Vector3f var15 = BaseVehicle.allocVector3f();

      for(int var16 = 0; var16 < var5; ++var16) {
         this.startRun(this.FORMAT_PositionColor);
         this.setMode(8);
         this.setDepthTest(true);

         for(int var17 = 0; var17 <= var6; ++var17) {
            this.reserve(2);

            for(int var18 = 0; var18 <= 1; ++var18) {
               double var19 = (double)((var16 + var18) % var5) + 0.5;
               double var21 = (double)(var17 % (var6 + 1));
               double var23 = (var3 + var1 * Math.cos(var19 * var13 / (double)var5)) * Math.cos(var21 * var13 / (double)var6);
               double var25 = (var3 + var1 * Math.cos(var19 * var13 / (double)var5)) * Math.sin(var21 * var13 / (double)var6);
               double var27 = var1 * Math.sin(var19 * var13 / (double)var5);
               var15.set(var23, var25, var27).normalize();
               float var29 = var15.dot(var10.direction);
               this.addElement();
               if (var29 < 0.1F) {
                  this.setColor(var7, var8, var9, 1.0F);
               } else {
                  this.setColor(var7 / 2.0F, var8 / 2.0F, var9 / 2.0F, 0.25F);
               }

               this.setVertex(2.0F * (float)var23, 2.0F * (float)var25, 2.0F * (float)var27);
            }
         }

         this.endRun();
      }

      this.flush();
      BaseVehicle.releaseVector3f(var15);
      GL11.glFrontFace(2305);
   }

   private static final class Run {
      boolean bEnded;
      VertexBufferObject.VertexFormat format;
      int startVertex;
      int startIndex;
      int vertexCount;
      int mode = 1;
      float lineWidth = 1.0F;
      ShaderProgram shaderProgram;
      TextureID textureID;
      int minFilter = 0;
      int magFilter = 0;
      boolean depthTest = false;
      float userDepth = 0.0F;

      private Run() {
      }

      Run init() {
         this.bEnded = false;
         this.format = null;
         this.startVertex = 0;
         this.startIndex = 0;
         this.vertexCount = 0;
         this.mode = 1;
         this.lineWidth = 1.0F;
         this.shaderProgram = null;
         this.textureID = null;
         this.minFilter = 0;
         this.magFilter = 0;
         this.depthTest = false;
         this.userDepth = 0.0F;
         return this;
      }
   }
}
