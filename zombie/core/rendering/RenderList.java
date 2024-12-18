package zombie.core.rendering;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import zombie.GameProfiler;
import zombie.core.ShaderHelper;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.ShaderProgram;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;

public class RenderList {
   private static final HashMap<Model, QueuedModelList> OpaqueLists = new HashMap();
   private static final HashMap<Model, QueuedModelList> TransparentLists = new HashMap();
   private static final ArrayList<Texture> UniqueTextures = new ArrayList();
   private static final ArrayList<ArrayTexture> ATPool = new ArrayList();
   private static final ArrayDeque<RenderData> RDPool = new ArrayDeque();
   private static final QueuedModelList ImmediateModel = new QueuedModelList((Model)null);

   public RenderList() {
   }

   public static void DrawQueued(ModelSlotRenderData var0, AnimatedModel.AnimatedModelInstanceRenderData var1) {
      Model var2 = var1.modelInstance.model;
      QueuedModelList var3 = (QueuedModelList)OpaqueLists.getOrDefault(var2, (Object)null);
      if (var3 == null) {
         OpaqueLists.put(var2, var3 = new QueuedModelList(var2));
      }

      var3.list.add(GetRenderData(var0, var1));
   }

   public static void DrawImmediate(ModelSlotRenderData var0, AnimatedModel.AnimatedModelInstanceRenderData var1) {
      int var2 = GL43.glGetInteger(35725);
      ImmediateModel.key = var1.modelInstance.model;
      ImmediateModel.list.add(GetRenderData(var0, var1));
      ImmediateModel.Draw();
      ImmediateModel.Reset();
      GL43.glBindBuffer(37074, 0);
      GL43.glUseProgram(var2);
   }

   private static RenderData GetRenderData(ModelSlotRenderData var0, AnimatedModel.AnimatedModelInstanceRenderData var1) {
      RenderData var2;
      if (RDPool.isEmpty()) {
         var2 = new RenderData(var0, var1);
      } else {
         var2 = (RenderData)RDPool.removeFirst();
         var2.slotData = var0;
         var2.instData = var1;
      }

      return var2;
   }

   private static int CompareOpaque(RenderData var0, RenderData var1) {
      return var0.mesh.hashCode() - var1.mesh.hashCode();
   }

   public static void SortOpaque() {
      Object var0 = null;
      ((ArrayList)var0).sort(RenderList::CompareOpaque);
   }

   private static int CompareTransparent(RenderData var0, RenderData var1) {
      return var0.mesh.hashCode() - var1.mesh.hashCode();
   }

   public static void SortTransparent() {
      Object var0 = null;
      ((ArrayList)var0).sort(RenderList::CompareTransparent);
   }

   private static <K, V extends BaseDrawList> void Render(HashMap<K, V> var0) {
      if (!var0.isEmpty()) {
         int var1 = GL43.glGetInteger(35725);
         var0.forEach(RenderList::RenderLists);
         GL43.glBindBuffer(37074, 0);
         ShaderHelper.glUseProgramObjectARB(var1);
      }
   }

   public static void RenderOpaque() {
      GL43.glPushClientAttrib(-1);
      GL43.glPushAttrib(1048575);
      GL43.glEnable(2884);
      GL43.glCullFace(1028);
      GL43.glEnable(2929);
      GL43.glDepthFunc(513);
      GL43.glDepthMask(true);
      GL43.glDepthRangef(0.0F, 1.0F);
      GL43.glEnable(3008);
      GL43.glAlphaFunc(516, 0.01F);
      GL43.glBlendFunc(770, 771);
      GameProfiler.getInstance().invokeAndMeasure("Render Opaque", OpaqueLists, RenderList::Render);
      GL43.glPopAttrib();
      GL43.glPopClientAttrib();
      Model.SwapInstancedBasic();
   }

   public static void RenderTransparent() {
      GL43.glPushClientAttrib(-1);
      GL43.glPushAttrib(1048575);
      GL43.glEnable(2884);
      GL43.glCullFace(1028);
      GL43.glEnable(2929);
      GL43.glDepthFunc(513);
      GL43.glDepthMask(false);
      GL43.glDepthRangef(0.0F, 1.0F);
      GL43.glEnable(3008);
      GL43.glAlphaFunc(516, 0.01F);
      GL43.glBlendFunc(770, 771);
      GameProfiler.getInstance().invokeAndMeasure("Render Transparent", TransparentLists, RenderList::Render);
      GL43.glPopAttrib();
      GL43.glPopClientAttrib();
   }

   public static void Reset() {
      OpaqueLists.forEach(RenderList::ResetLists);
      TransparentLists.forEach(RenderList::ResetLists);
      RemoveEmptyLists(OpaqueLists);
      RemoveEmptyLists(TransparentLists);
   }

   public static void ResetOpaque() {
      OpaqueLists.forEach(RenderList::ResetLists);
      RemoveEmptyLists(OpaqueLists);
   }

   public static void ResetTransparent() {
      TransparentLists.forEach(RenderList::ResetLists);
      RemoveEmptyLists(TransparentLists);
   }

   private static <K> void RenderLists(K var0, BaseDrawList var1) {
      var1.Draw();
   }

   private static <K, L extends BaseDrawList> void ResetLists(K var0, L var1) {
      var1.Reset();
   }

   private static <K, L extends BaseDrawList> void RemoveEmptyLists(HashMap<K, L> var0) {
      Set var1 = var0.entrySet();
      var1.removeIf((var0x) -> {
         return ((BaseDrawList)var0x.getValue()).IsEmpty();
      });
   }

   private static class QueuedModelList extends DrawList<Model, RenderData> {
      public QueuedModelList(Model var1) {
         super(var1);
         this.list = new ArrayList();
      }

      protected void DrawSingular() {
         GL43.glDisable(2884);
         Iterator var1 = this.list.iterator();

         while(var1.hasNext()) {
            RenderData var2 = (RenderData)var1.next();
            if (var2.instData instanceof ModelInstanceRenderData) {
               ((Model)this.key).Effect.startCharacter(var2.slotData, (ModelInstanceRenderData)var2.instData);
            }

            ((Model)this.key).Effect.instancedData.PushUniforms(var2.instData.properties);
            ((Model)this.key).Mesh.Draw(((Model)this.key).Effect);
         }

      }

      protected Texture GetTexture(int var1) {
         RenderData var2 = (RenderData)this.list.get(var1);
         Texture var3 = var2.instData.tex;
         if (var3 == null) {
            var3 = var2.instData.modelInstance.tex;
         }

         if (var3 == null && var2.instData.model != null) {
            var3 = var2.instData.model.tex;
         }

         return var3;
      }

      protected ShaderPropertyBlock GetProperties(int var1) {
         RenderData var2 = (RenderData)this.list.get(var1);
         if (((Model)this.key).Effect.isInstanced()) {
            var2.instData.properties.SetShader(((Model)this.key).Effect);
         }

         return var2.instData.properties;
      }

      protected Shader GetEffect() {
         if (((Model)this.key).Effect == null) {
            ((Model)this.key).Effect = ShaderManager.instance.getOrCreateShader("basicEffect", ((Model)this.key).bStatic, true);
         }

         return ((Model)this.key).Effect;
      }

      protected void DrawCount(int var1) {
         ((Model)this.key).Mesh.DrawInstanced(((Model)this.key).Effect, var1);
      }

      protected void OnComplete(int var1) {
         RenderData var2 = (RenderData)this.list.get(var1);
         RenderList.RDPool.add(var2);
      }

      protected void UpdateProperties(int var1) {
      }
   }

   private static class RenderData {
      public ModelMesh mesh;
      public ModelSlotRenderData slotData;
      public AnimatedModel.AnimatedModelInstanceRenderData instData;
      public float Z;

      public RenderData(ModelSlotRenderData var1, AnimatedModel.AnimatedModelInstanceRenderData var2) {
         this.slotData = var1;
         this.instData = var2;
      }
   }

   private abstract static class BaseDrawList {
      private BaseDrawList() {
      }

      protected abstract int Count();

      public abstract boolean IsEmpty();

      public abstract void Draw();

      protected abstract void DrawCount(int var1);

      protected abstract void DrawSingular();

      protected void DrawInstanced() {
         Shader var1 = this.GetEffect();
         ShaderPropertyBlock var2 = this.GetProperties(0);
         PZGLUtil.checkGLError(true);
         var1.instancedData.PushUniforms(var2);
         PZGLUtil.checkGLError(true);
         int var3 = this.Count();
         int var4 = 0;

         while(var4 < var3) {
            int var5 = Math.min(128, var3 - var4);
            int var6 = var4 + var5;
            RenderList.UniqueTextures.clear();
            GameProfiler.getInstance().invokeAndMeasure("Push Data", var4, var6, this::PushData);
            var4 = var6;
            ArrayTexture var7 = this.GetTextureArray();
            ShaderProgram var8 = var1.getShaderProgram();
            ShaderProgram.Uniform var9 = var8.getUniform("InstTexArr", 36289);
            if (var9 != null) {
               GL44.glActiveTexture('蓀' + var9.sampler);
            } else {
               GL44.glActiveTexture(33984);
            }

            GameProfiler.getInstance().invokeAndMeasure("Copy Textures", var7, BaseDrawList::CopyTextures);
            var7.Bind();
            if (var9 != null) {
               GL44.glUniform1i(var9.loc, var9.sampler);
            }

            var9 = var8.getUniform("TextureDimensions", 35664);
            if (var9 != null) {
               GL44.glUniform2f(var9.loc, (float)var7.width, (float)var7.height);
            }

            GameProfiler.getInstance().invokeAndMeasure("Update Instanced", this, BaseDrawList::UpdateData);
            GameProfiler.getInstance().invokeAndMeasure("Draw Instanced", this, var5, BaseDrawList::DrawCount);
         }

         var1.End();
      }

      protected abstract Texture GetTexture(int var1);

      protected abstract ShaderPropertyBlock GetProperties(int var1);

      protected abstract Shader GetEffect();

      public abstract void Reset();

      protected final void UpdateData() {
         Shader var1 = this.GetEffect();
         var1.instancedData.UpdateData();
      }

      protected ArrayTexture GetTextureArray() {
         int var1 = 1;
         int var2 = 1;
         Iterator var3 = RenderList.UniqueTextures.iterator();

         while(var3.hasNext()) {
            Texture var4 = (Texture)var3.next();
            if (var4 != null) {
               var1 = Math.max(var1, var4.getWidth());
               var2 = Math.max(var2, var4.getHeight());
            }
         }

         var3 = RenderList.ATPool.iterator();

         ArrayTexture var6;
         do {
            if (!var3.hasNext()) {
               ArrayTexture var5 = new ArrayTexture(var1, var2, RenderList.UniqueTextures.size());
               RenderList.ATPool.add(var5);
               return var5;
            }

            var6 = (ArrayTexture)var3.next();
         } while(var6.width != var1 || var6.height < var2);

         if (var6.length < RenderList.UniqueTextures.size()) {
            var6.length = RenderList.UniqueTextures.size();
            var6.Recreate();
         }

         return var6;
      }

      protected void OnComplete(int var1) {
      }

      protected abstract void UpdateProperties(int var1);

      private void WriteToBuffer(ShaderPropertyBlock var1) {
         Shader var2 = this.GetEffect();
         var2.instancedData.PushInstanced(var1);
      }

      protected final void PushData(int var1, int var2) {
         while(var1 < var2) {
            Texture var3 = this.GetTexture(var1);
            ShaderPropertyBlock var4 = this.GetProperties(var1);
            if (var3 instanceof SmartTexture var5) {
               var5.getID();
               var3 = var5.result;
            }

            int var6 = RenderList.UniqueTextures.indexOf(var3);
            if (var6 == -1) {
               var6 = RenderList.UniqueTextures.size();
               RenderList.UniqueTextures.add(var3);
            }

            var4.SetInt("textureIndex", var6);
            if (var3 == null) {
               var4.SetVector2("textureSize", 0.0F, 0.0F);
            } else {
               var4.SetVector2("textureSize", (float)var3.getWidth(), (float)var3.getHeight());
            }

            GameProfiler.getInstance().invokeAndMeasure("Update Properties", var1, this::UpdateProperties);
            GameProfiler.getInstance().invokeAndMeasure("Write to Buffer", var4, this::WriteToBuffer);
            this.OnComplete(var1);
            ++var1;
         }

      }

      protected static void CopyTextures(ArrayTexture var0) {
         for(int var1 = 0; var1 < RenderList.UniqueTextures.size(); ++var1) {
            var0.Copy((Texture)RenderList.UniqueTextures.get(var1), var1);
         }

      }
   }

   private static class ArrayTexture {
      private static final float[] White = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
      public int texture = GL44.glGenTextures();
      public int width;
      public int height;
      public int length;

      public ArrayTexture(int var1, int var2, int var3) {
         this.width = var1;
         this.height = var2;
         this.length = var3;
         this.Init();
      }

      private void Init() {
         GL44.glBindTexture(35866, this.texture);
         GL44.glTexImage3D(35866, 0, 32856, this.width, this.height, this.length, 0, 6408, 5121, 0L);
         GL44.glTexParameteri(35866, 10240, 9729);
         GL44.glTexParameteri(35866, 10241, 9729);
         GL44.glTexParameteri(35866, 10242, 33071);
         GL44.glTexParameteri(35866, 10243, 33071);
         GL44.glTexParameteri(35866, 32882, 33071);
      }

      public void Recreate() {
         this.Init();
      }

      public void Copy(Texture var1, int var2) {
         if (var1 != null && !var1.isDestroyed() && var1.isValid() && var1.isReady() && var1.getID() != -1) {
            int var3 = var1.getID();
            int var4 = Math.max(0, var1.getX());
            int var5 = Math.max(0, var1.getY());
            GL44.glCopyImageSubData(var3, 3553, 0, var4, var5, 0, this.texture, 35866, 0, 0, 0, var2, var1.getWidth(), var1.getHeight(), 1);
         } else {
            GL44.glClearTexSubImage(this.texture, 0, 0, 0, var2, this.width, this.height, 1, 6408, 5126, White);
         }

      }

      public void Bind() {
         GL44.glBindTexture(35866, this.texture);
      }
   }

   private abstract static class DrawList<Key, Value> extends BaseDrawList {
      public Key key;
      public ArrayList<Value> list;

      public DrawList(Key var1) {
         this.key = var1;
         this.list = new ArrayList();
      }

      public String toString() {
         return String.format("Draw List: %1s - %2s", this.key, this.list.size());
      }

      protected final int Count() {
         return this.list.size();
      }

      public boolean IsEmpty() {
         return this.list.isEmpty();
      }

      public void Draw() {
         if (!this.list.isEmpty()) {
            Shader var1 = this.GetEffect();
            var1.Start();
            if (var1.isInstanced()) {
               this.DrawInstanced();
            } else {
               this.DrawSingular();
            }

            var1.End();
         }
      }

      public final void Reset() {
         this.list.clear();
      }
   }
}
