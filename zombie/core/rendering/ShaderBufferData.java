package zombie.core.rendering;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.skinnedmodel.shader.Shader;

public class ShaderBufferData {
   protected List<ShaderParameter> parameterList = new ArrayList();
   protected List<ShaderParameter> uniformParameterList = new ArrayList();
   protected List<ShaderParameter> instancedParameterList = new ArrayList();
   public HashMap<String, ShaderParameter> parameters = new HashMap();
   private int size = 0;
   private int currentInstance = 0;

   public ShaderBufferData(Shader var1) {
      int var2 = var1.getShaderProgram().getShaderID();
      int var3 = var1.InstancedDataAttrib;
      int var7;
      if (var3 >= 0) {
         int[] var4 = ShaderBufferData.BufferUtility.GetBlockMembers(var2, var3);

         for(int var5 = 0; var5 < var4.length; ++var5) {
            String var6 = ShaderBufferData.BufferUtility.GetMemberName(var2, var4[var5]);
            var7 = ShaderBufferData.BufferUtility.GetMemberProperty(var2, var4[var5], 37626);
            int var8 = ShaderBufferData.BufferUtility.GetMemberProperty(var2, var4[var5], 37627);
            int var9 = ShaderBufferData.BufferUtility.GetMemberProperty(var2, var4[var5], 37628);
            int var10 = ShaderBufferData.BufferUtility.GetMemberProperty(var2, var4[var5], 37627);
            ShaderParameter var11 = CreateShaderParameter(var8, var7, var6);

            assert var11 != null;

            var11.offset = var9;
            var11.length = var10;
            this.AddBufferMember(var11);
         }

         this.size = ShaderBufferData.BufferUtility.GetBlockProperty(var2, var3, 37635);
         this.size = (int)Math.ceil((double)this.size / 128.0);
      }

      Uniform[] var12 = ShaderBufferData.BufferUtility.GetUniforms(var2);
      Uniform[] var13 = var12;
      int var14 = var12.length;

      for(var7 = 0; var7 < var14; ++var7) {
         Uniform var15 = var13[var7];
         ShaderParameter var16 = CreateShaderParameter(var15.size, var15.type, var15.name);

         assert var16 != null;

         var16.offset = var15.location;
         var16.PullUniform(var2);
         var16.UpdateDefault();
         this.AddUniform(var16);
      }

      this.instancedParameterList.sort(Comparator.comparingInt((var0) -> {
         return var0.offset;
      }));
   }

   private static ShaderParameter CreateShaderParameter(int var0, int var1, String var2) {
      if (var0 == 1) {
         switch (var1) {
            case 5124:
               return new ShaderParameter(var2, 0, false);
            case 5126:
               return new ShaderParameter(var2, 0.0F);
            case 35664:
               return new ShaderParameter(var2, new Vector2f());
            case 35665:
               return new ShaderParameter(var2, new Vector3f());
            case 35666:
               return new ShaderParameter(var2, new Vector4f());
            case 35670:
               return new ShaderParameter(var2, false);
            case 35675:
               return new ShaderParameter(var2, new Matrix3f());
            case 35676:
               return new ShaderParameter(var2, new Matrix4f());
            case 35677:
            case 35678:
            case 35679:
            case 35680:
            case 36288:
            case 36289:
            case 36297:
            case 36298:
            case 36299:
            case 36302:
            case 36303:
            case 36305:
            case 36306:
            case 36307:
            case 36308:
            case 36310:
            case 36311:
            case 36878:
            case 36879:
               return new ShaderParameter(var2, 0, true);
         }
      } else {
         switch (var1) {
            case 5124:
               return new ShaderParameter(var2, new int[var0], false);
            case 5126:
               return new ShaderParameter(var2, new float[var0]);
            case 35664:
               return new ShaderParameter(var2, new Vector2f[var0]);
            case 35665:
               return new ShaderParameter(var2, new Vector3f[var0]);
            case 35666:
               return new ShaderParameter(var2, new Vector4f[var0]);
            case 35675:
               return new ShaderParameter(var2, new Matrix3f[var0]);
            case 35676:
               return new ShaderParameter(var2, new Matrix4f[var0]);
            case 35677:
            case 35678:
            case 35679:
            case 35680:
            case 36305:
            case 36306:
            case 36307:
            case 36308:
               return new ShaderParameter(var2, 0, true);
         }
      }

      return null;
   }

   private void Reset(List<ShaderParameter> var1) {
      Iterator var2 = this.parameterList.iterator();

      while(var2.hasNext()) {
         ShaderParameter var3 = (ShaderParameter)var2.next();
         var3.ResetValue();
      }

   }

   public void ResetParameters() {
      this.Reset(this.parameterList);
   }

   public void ResetUniforms() {
      this.Reset(this.uniformParameterList);
   }

   public void ResetInstanced() {
      this.Reset(this.instancedParameterList);
   }

   protected void AddBufferMember(ShaderParameter var1) {
      this.instancedParameterList.add(var1);
      this.parameterList.add(var1);
      this.parameters.put(var1.name, var1);
   }

   protected void AddUniform(ShaderParameter var1) {
      this.uniformParameterList.add(var1);
      this.parameterList.add(var1);
      this.parameters.put(var1.name, var1);
   }

   private void Copy(ShaderPropertyBlock var1, List<ShaderParameter> var2) {
      Iterator var3 = var2.iterator();

      while(var3.hasNext()) {
         ShaderParameter var4 = (ShaderParameter)var3.next();
         ShaderParameter var5 = var1.GetParameter(var4.name);
         if (var5 != null) {
            var4.Copy(var5, false, true);
         }
      }

   }

   public void CopyParameters(ShaderPropertyBlock var1) {
      this.Copy(var1, this.parameterList);
   }

   public void CopyUniforms(ShaderPropertyBlock var1) {
      this.Copy(var1, this.uniformParameterList);
   }

   public void CopyInstanced(ShaderPropertyBlock var1) {
      this.Copy(var1, this.instancedParameterList);
   }

   public int GetSize() {
      return this.size;
   }

   public int GetCurrentInstance() {
      return this.currentInstance;
   }

   public void PushParameters(InstancedBuffer var1) {
      this.PushUniforms();
      this.PushInstanced(var1);
   }

   public void PushUniforms() {
      Iterator var1 = this.uniformParameterList.iterator();

      while(var1.hasNext()) {
         ShaderParameter var2 = (ShaderParameter)var1.next();
         var2.PushUniform();
      }

   }

   public void PushInstanced(InstancedBuffer var1) {
      if (var1.GetBufferID() >= 0) {
         int var2 = this.currentInstance * this.size;
         Iterator var3 = this.instancedParameterList.iterator();

         while(var3.hasNext()) {
            ShaderParameter var4 = (ShaderParameter)var3.next();
            var4.PushInstanced(var1, var2);
         }

         ++this.currentInstance;
      }

   }

   public void PushInstanced(InstancedBuffer var1, ShaderPropertyBlock var2) {
      if (var1.GetBufferID() >= 0) {
         int var3 = this.currentInstance * this.size;
         var1.data.position(var3);
         var2.CopyToInstanced(var1);
         ++this.currentInstance;
      }

   }

   public void Reset() {
      this.currentInstance = 0;
   }

   private static class BufferUtility {
      private static final int[] PropertyID = new int[]{1};
      private static final int[] Count = new int[]{1};
      private static final int[] Results = new int[1];

      private BufferUtility() {
      }

      public static Uniform[] GetUniforms(int var0) {
         int var1 = GL43.glGetProgrami(var0, 35718);
         IntBuffer var2 = MemoryUtil.memAllocInt(1);
         IntBuffer var3 = MemoryUtil.memAllocInt(1);
         Uniform[] var4 = new Uniform[var1];
         int var5 = GL43.glGetProgrami(var0, 35719);

         for(int var6 = 0; var6 < var1; ++var6) {
            Uniform var7 = new Uniform();
            var7.name = GL43.glGetActiveUniform(var0, var6, var5, var2, var3);
            var7.size = var2.get(0);
            var7.type = var3.get(0);
            var7.location = GL43.glGetUniformLocation(var0, var7.name);
            var4[var6] = var7;
         }

         MemoryUtil.memFree(var2);
         MemoryUtil.memFree(var3);
         return var4;
      }

      public static int GetBlockProperty(int var0, int var1, int var2) {
         PropertyID[0] = var2;
         Count[0] = 1;
         GL43.glGetProgramResourceiv(var0, 37606, var1, PropertyID, Count, Results);
         return Results[0];
      }

      public static int[] GetBlockMembers(int var0, int var1) {
         int var2 = GetBlockProperty(var0, var1, 37636);
         int[] var3 = new int[var2];
         PropertyID[0] = 37637;
         Count[0] = var2;
         GL43.glGetProgramResourceiv(var0, 37606, var1, PropertyID, Count, var3);
         return var3;
      }

      public static String GetMemberName(int var0, int var1) {
         int var2 = GetMemberProperty(var0, var1, 37625);
         String var3 = GL43.glGetProgramResourceName(var0, 37605, var1, var2);
         return var3;
      }

      public static int GetMemberProperty(int var0, int var1, int var2) {
         PropertyID[0] = var2;
         Count[0] = 1;
         GL43.glGetProgramResourceiv(var0, 37605, var1, PropertyID, Count, Results);
         return Results[0];
      }
   }

   private static class Uniform {
      public String name;
      public int size;
      public int type;
      public int location;

      private Uniform() {
      }
   }
}
