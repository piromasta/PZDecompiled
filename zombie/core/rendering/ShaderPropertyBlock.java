package zombie.core.rendering;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.shader.Shader;

public class ShaderPropertyBlock {
   private final HashMap<String, ShaderParameter> parameters = new HashMap();
   private Shader shader;
   private ByteBuffer data;

   public ShaderPropertyBlock() {
   }

   public void SetShader(Shader var1) {
      if (var1 != this.shader) {
         Map.Entry var3;
         for(Iterator var2 = this.parameters.entrySet().iterator(); var2.hasNext(); ((ShaderParameter)var3.getValue()).offset = -1) {
            var3 = (Map.Entry)var2.next();
         }

         if (var1 != null && var1.isInstanced()) {
            if (this.shader != null) {
               MemoryUtil.memRealloc(this.data, var1.instancedData.GetBufferData().GetSize());
            } else {
               this.data = MemoryUtil.memAlloc(var1.instancedData.GetBufferData().GetSize());
            }

            ShaderBufferData var5 = var1.instancedData.GetBufferData();
            Iterator var6 = var5.parameters.entrySet().iterator();

            while(var6.hasNext()) {
               Map.Entry var4 = (Map.Entry)var6.next();
               this.parameters.put((String)var4.getKey(), new ShaderParameter((ShaderParameter)var4.getValue()));
            }
         } else if (this.data != null) {
            MemoryUtil.memFree(this.data);
            this.data = null;
         }

         this.shader = var1;
      }
   }

   public void StoreProperties() {
      Iterator var1 = this.parameters.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         ((ShaderParameter)var2.getValue()).WriteToBuffer(this.data, 0);
      }

   }

   public void CopyToInstanced(InstancedBuffer var1) {
      var1.data.put(this.data);
   }

   private ShaderParameter SetOrReplace(String var1, CreateParameter var2, SetParameter var3) {
      ShaderParameter var4 = (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
      if (var4 != null) {
         var3.Run(var4);
      } else {
         var4 = var2.Run(var1);
         this.parameters.put(var1, var4);
      }

      return var4;
   }

   public ShaderParameter GetParameter(String var1) {
      return (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
   }

   public void CopyParameters(ShaderPropertyBlock var1) {
      Map.Entry var3;
      ShaderParameter var4;
      for(Iterator var2 = var1.parameters.entrySet().iterator(); var2.hasNext(); var4.Copy((ShaderParameter)var3.getValue(), true, false)) {
         var3 = (Map.Entry)var2.next();
         var4 = this.GetParameter((String)var3.getKey());
         if (var4 == null) {
            var4 = new ShaderParameter((String)var3.getKey(), false);
            this.parameters.put(var4.name, var4);
         }
      }

   }

   public void SetInt(String var1, int var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetInt(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetFloat(String var1, float var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetFloat(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector2(String var1, Vector2f var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetVector2(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector2(String var1, float var2, float var3) {
      ShaderParameter var4 = (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
      if (var4 != null) {
         var4.SetVector2(var2, var3);
      } else {
         Vector2f var5 = new Vector2f(var2, var3);
         var4 = new ShaderParameter(var1, var5);
         this.parameters.put(var1, var4);
      }

   }

   public void SetVector3(String var1, Vector3f var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetVector3(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector3(String var1, float var2, float var3, float var4) {
      ShaderParameter var5 = (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
      if (var5 != null) {
         var5.SetVector3(var2, var3, var4);
      } else {
         Vector3f var6 = new Vector3f(var2, var3, var4);
         var5 = new ShaderParameter(var1, var6);
         this.parameters.put(var1, var5);
      }

   }

   public void SetVector4(String var1, Vector4f var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetVector4(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector4(String var1, float var2, float var3, float var4, float var5) {
      ShaderParameter var6 = (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
      if (var6 != null) {
         var6.SetVector4(var2, var3, var4, var5);
      } else {
         Vector4f var7 = new Vector4f(var2, var3, var4, var5);
         var6 = new ShaderParameter(var1, var7);
         this.parameters.put(var1, var6);
      }

   }

   public void SetMatrix3(String var1, Matrix3f var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetMatrix3(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public Matrix4f SetMatrix4(String var1, Matrix4f var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, new Matrix4f(var2));
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetMatrix4(var2);
      };
      this.SetOrReplace(var1, var3, var4);
      return var2;
   }

   public Matrix4f SetMatrix4(String var1, org.joml.Matrix4f var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, PZMath.convertMatrix(var2, new Matrix4f()));
      };
      SetParameter var4 = (var1x) -> {
         if (var1x.GetType() != ShaderParameter.ParameterTypes.Matrix4) {
            var1x.SetMatrix4(PZMath.convertMatrix(var2, new Matrix4f()));
         } else {
            PZMath.convertMatrix(var2, var1x.GetMatrix4());
         }

      };
      ShaderParameter var5 = this.SetOrReplace(var1, var3, var4);
      return var5.GetMatrix4();
   }

   public Matrix4f SetMatrix4(String var1, FloatBuffer var2) {
      CreateParameter var3 = (var1x) -> {
         Matrix4f var2x = new Matrix4f();
         var2x.load(var2);
         return new ShaderParameter(var1x, var2x);
      };
      SetParameter var4 = (var1x) -> {
         Matrix4f var2x;
         if (var1x.GetType() != ShaderParameter.ParameterTypes.Matrix4) {
            var2x = new Matrix4f();
         } else {
            var2x = var1x.GetMatrix4();
         }

         var2x.load(var2);
         var1x.SetMatrix4(var2x);
      };
      ShaderParameter var5 = this.SetOrReplace(var1, var3, var4);
      return var5.GetMatrix4();
   }

   public void SetFloatArray(String var1, float[] var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetFloatArray(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector2Array(String var1, Vector2f[] var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetVector2Array(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector3Array(String var1, Vector3f[] var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetVector3Array(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetVector4Array(String var1, Vector4f[] var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetVector4Array(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetMatrix3Array(String var1, Matrix3f[] var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetMatrix3Array(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetMatrix4Array(String var1, Matrix4f[] var2) {
      CreateParameter var3 = (var1x) -> {
         return new ShaderParameter(var1x, var2);
      };
      SetParameter var4 = (var1x) -> {
         var1x.SetMatrix4Array(var2);
      };
      this.SetOrReplace(var1, var3, var4);
   }

   public void SetMatrix4Array(String var1, FloatBuffer var2) {
      int var3 = var2.limit() / 16;
      CreateParameter var4 = (var3x) -> {
         Matrix4f[] var4 = (Matrix4f[])this.CreateAndFillArray(Matrix4f[].class, Matrix4f.class, var3);

         for(int var5 = 0; var5 < var3; ++var5) {
            var4[var5].load(var2);
         }

         return new ShaderParameter(var3x, var4);
      };
      SetParameter var5 = (var3x) -> {
         Matrix4f[] var4;
         if (var3x.GetType() != ShaderParameter.ParameterTypes.Matrix4Array) {
            var4 = (Matrix4f[])this.CreateAndFillArray(Matrix4f[].class, Matrix4f.class, var3);
         } else {
            var4 = var3x.GetMatrix4Array();
            if (var4.length != var3) {
               var4 = (Matrix4f[])this.CreateAndFillArray(Matrix4f[].class, Matrix4f.class, var3);
            }
         }

         for(int var5 = 0; var5 < var3; ++var5) {
            var4[var5].load(var2);
         }

         var3x.SetMatrix4Array(var4);
      };
      this.SetOrReplace(var1, var4, var5);
      var2.flip();
   }

   private <T> void SetArrayElement(String var1, int var2, T var3, Class<T[]> var4) {
      ShaderParameter var5 = (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
      if (var5 != null) {
         Object[] var6 = (Object[])var4.cast(var5.GetValue());
         var6[var2] = var3;
      }

   }

   private <T> void SetArrayElement(String var1, int var2, Class<T[]> var3, Consumer<T> var4) {
      ShaderParameter var5 = (ShaderParameter)this.parameters.getOrDefault(var1, (Object)null);
      if (var5 != null) {
         Object[] var6 = (Object[])var3.cast(var5.GetValue());
         var4.accept(var6[var2]);
      }

   }

   public void SetFloatArrayElement(String var1, int var2, float var3) {
      this.SetArrayElement(var1, var2, (Object)var3, (Class)Float[].class);
   }

   public void SetVector2ArrayElement(String var1, int var2, Vector2f var3) {
      this.SetArrayElement(var1, var2, (Object)var3, (Class)Vector2f[].class);
   }

   public void SetVector2ArrayElement(String var1, int var2, float var3, float var4) {
      this.SetArrayElement(var1, var2, Vector2f[].class, (var2x) -> {
         var2x.set(var3, var4);
      });
   }

   public void SetVector3ArrayElement(String var1, int var2, Vector3f var3) {
      this.SetArrayElement(var1, var2, (Object)var3, (Class)Vector3f[].class);
   }

   public void SetVector3ArrayElement(String var1, int var2, float var3, float var4, float var5) {
      this.SetArrayElement(var1, var2, Vector3f[].class, (var3x) -> {
         var3x.set(var3, var4, var5);
      });
   }

   public void SetVector4ArrayElement(String var1, int var2, Vector4f var3) {
      this.SetArrayElement(var1, var2, (Object)var3, (Class)Vector4f[].class);
   }

   public void SetVector4ArrayElement(String var1, int var2, float var3, float var4, float var5, float var6) {
      this.SetArrayElement(var1, var2, Vector4f[].class, (var4x) -> {
         var4x.set(var3, var4, var5, var6);
      });
   }

   public void SetMatrix3ArrayElement(String var1, int var2, Matrix3f var3) {
      this.SetArrayElement(var1, var2, (Object)var3, (Class)Matrix3f[].class);
   }

   public void SetMatrix4ArrayElement(String var1, int var2, Matrix4f var3) {
      this.SetArrayElement(var1, var2, (Object)var3, (Class)Matrix4f[].class);
   }

   private <T> T[] CreateAndFillArray(Class<T[]> var1, Class<T> var2, int var3) {
      Object[] var4 = (Object[])var1.cast(Array.newInstance(var2, var3));

      try {
         Constructor var5 = var2.getDeclaredConstructor();

         for(int var6 = 0; var6 < var3; ++var6) {
            var4[var6] = var2.cast(var5.newInstance());
         }
      } catch (Exception var7) {
      }

      return var4;
   }

   private interface SetParameter {
      void Run(ShaderParameter var1);
   }

   private interface CreateParameter {
      ShaderParameter Run(String var1);
   }
}
