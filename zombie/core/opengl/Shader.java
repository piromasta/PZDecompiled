package zombie.core.opengl;

import java.util.HashMap;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import zombie.core.ShaderHelper;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public class Shader implements IShaderProgramListener {
   public static HashMap<Integer, Shader> ShaderMap = new HashMap();
   private String name;
   private int m_shaderMapID = 0;
   private final Object m_shaderProgramLock = new String("ShaderProgram Thread Lock");
   private volatile boolean m_shaderProgramInitialized = false;
   private ShaderProgramSpecific m_shaderProgramSpecific = null;

   public Shader(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public boolean GetRequiresSkinning() {
      return this.getRequiresSkinning();
   }

   private void Init() {
      this.initShaderProgram();
   }

   public void Activate() {
      GL20.glUseProgram(this.getID());
   }

   public void SetupInstancedData() {
   }

   public void SetupBones(ModelMesh var1) {
      if (this.getRequiresSkinning()) {
         this.m_shaderProgramSpecific.m_skinning.SetupBones(var1);
      }

   }

   public void setTexture(Texture var1) {
      this.initShaderProgram();
      this.m_shaderProgramSpecific.m_tex = var1;
   }

   public int getID() {
      return this.getShaderProgram().getShaderID();
   }

   public void Start() {
      ShaderHelper.glUseProgramObjectARB(this.getShaderProgram().getShaderID());
   }

   public void End() {
      ShaderHelper.glUseProgramObjectARB(0);
   }

   public void destroy() {
      this.getShaderProgram().destroy();
      ShaderMap.remove(this.m_shaderMapID);
      this.m_shaderMapID = 0;
   }

   public void startMainThread(TextureDraw var1, int var2) {
      boolean var3 = false;
   }

   public void startRenderThread(TextureDraw var1) {
      boolean var2 = false;
   }

   public void postRender(TextureDraw var1) {
      boolean var2 = false;
   }

   public boolean isCompiled() {
      return this.getShaderProgram().isCompiled();
   }

   public void callback(ShaderProgram var1) {
      ShaderMap.remove(this.m_shaderMapID);
      this.m_shaderMapID = var1.getShaderID();
      ShaderMap.put(this.m_shaderMapID, this);
      this.onCompileSuccess(var1);
   }

   protected void onCompileSuccess(ShaderProgram var1) {
      this.m_shaderProgramSpecific.m_requiresSkinning = this.m_shaderProgramSpecific.m_skinning.Init();
   }

   public ShaderProgram getProgram() {
      return this.getShaderProgram();
   }

   public ShaderProgram getShaderProgram() {
      this.initShaderProgram();
      return this.m_shaderProgramSpecific.m_shaderProgram;
   }

   protected void initShaderProgram() {
      if (!this.m_shaderProgramInitialized) {
         synchronized(this.m_shaderProgramLock) {
            if (!this.m_shaderProgramInitialized) {
               this.m_shaderProgramSpecific = new ShaderProgramSpecific();
               RenderThread.invokeOnRenderContext(this, (var1) -> {
                  var1.m_shaderProgramSpecific.m_shaderProgram = ShaderProgram.createShaderProgram(this.getName(), false, false, false);
                  var1.m_shaderProgramSpecific.m_shaderProgram.addCompileListener(this);
                  var1.m_shaderProgramInitialized = true;
                  var1.m_shaderProgramSpecific.m_shaderProgram.compile();
               });
            }
         }
      }

   }

   public int getWidth() {
      this.initShaderProgram();
      return this.m_shaderProgramSpecific.m_width;
   }

   public int getHeight() {
      this.initShaderProgram();
      return this.m_shaderProgramSpecific.m_height;
   }

   public void setWidth(int var1) {
      this.initShaderProgram();
      this.m_shaderProgramSpecific.m_width = var1;
   }

   public void setHeight(int var1) {
      this.initShaderProgram();
      this.m_shaderProgramSpecific.m_height = var1;
   }

   public boolean getRequiresSkinning() {
      this.initShaderProgram();
      return this.m_shaderProgramSpecific.m_requiresSkinning;
   }

   private class ShaderProgramSpecific {
      private ShaderProgram m_shaderProgram = null;
      private Texture m_tex;
      private int m_width;
      private int m_height;
      private Skinning m_skinning = Shader.this.new Skinning();
      private boolean m_requiresSkinning = false;

      private ShaderProgramSpecific() {
      }
   }

   private class Skinning {
      public IDs IDs = new IDs();

      private Skinning() {
      }

      public boolean Init() {
         int var1 = Shader.this.getID();
         this.IDs.boneCount = GL20.glGetUniformLocation(var1, "boneCount");
         this.IDs.boneMatrices = GL20.glGetUniformLocation(var1, "boneMatrices");
         this.IDs.boneWeights = GL20.glGetAttribLocation(var1, "boneWeights");
         this.IDs.boneIDs = GL20.glGetAttribLocation(var1, "boneIDs");
         return this.IDs.boneCount >= 0 && this.IDs.boneWeights >= 0 && this.IDs.boneMatrices >= 0 && this.IDs.boneIDs >= 0;
      }

      public void SetupBones(ModelMesh var1) {
         int var2 = var1.skinningData.numBones();
         GL20.glUniform1i(this.IDs.boneCount, var2);
         SkinningData.Buffers var3 = var1.skinningData.buffers;
         GL20.glUniform4fv(this.IDs.boneMatrices, var3.boneMatrices);
         GL20.glEnableVertexAttribArray(this.IDs.boneWeights);
         GL20.glVertexAttribPointer(this.IDs.boneWeights, 4, 5126, false, 0, var3.boneWeights);
         GL20.glEnableVertexAttribArray(this.IDs.boneIDs);
         GL20.glVertexAttribPointer(this.IDs.boneIDs, 4, 5123, false, 0, var3.boneIDs);
         GL20.glEnableVertexAttribArray(this.IDs.boneMatrices);
         GL20.glVertexAttribPointer(this.IDs.boneMatrices, 16, 35676, false, 0, var3.boneMatrices);
         GL43.glVertexAttribDivisor(this.IDs.boneMatrices, 0);
      }

      public class IDs {
         public int boneCount = -1;
         public int boneWeights = -1;
         public int boneMatrices = -1;
         public int boneIDs = -1;

         public IDs() {
         }
      }
   }
}
