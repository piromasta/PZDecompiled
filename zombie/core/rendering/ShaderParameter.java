package zombie.core.rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.function.Function;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.debug.DebugLog;

public class ShaderParameter {
   private static final FloatBuffer VectorBuffer = MemoryUtil.memAllocFloat(4);
   private static final FloatBuffer MatrixBuffer = MemoryUtil.memAllocFloat(16);
   public final String name;
   private Object value;
   private Object defaultValue;
   private ParameterTypes type;
   public int offset;
   public int length;

   public ShaderParameter(ShaderParameter var1) {
      this.name = var1.name;
      this.defaultValue = var1.defaultValue;
      this.type = var1.type;
      this.offset = var1.offset;
      this.length = var1.length;
      switch (var1.type) {
         case Vector2:
            this.value = new Vector2f(var1.GetVector2());
            break;
         case Vector3:
            this.value = new Vector3f(var1.GetVector3());
            break;
         case Vector4:
            this.value = new Vector4f(var1.GetVector4());
            break;
         case Matrix4:
            this.value = new Matrix4f(var1.GetMatrix4());
            break;
         case Vector2Array:
            this.value = this.CopyArray(Vector2f.class, var1.GetVector2Array(), Vector2f::new);
            break;
         case Vector3Array:
            this.value = this.CopyArray(Vector3f.class, var1.GetVector3Array(), Vector3f::new);
            break;
         case Vector4Array:
            this.value = this.CopyArray(Vector4f.class, var1.GetVector4Array(), Vector4f::new);
            break;
         case Matrix4Array:
            this.value = this.CopyArray(Matrix4f.class, var1.GetMatrix4Array(), Matrix4f::new);
            break;
         default:
            this.value = var1.value;
      }

   }

   private ShaderParameter(String var1, Object var2, ParameterTypes var3) {
      if (var1.startsWith("instancedStructs[0].")) {
         var1 = var1.substring("instancedStructs[0].".length());
      }

      if (var1.endsWith("[0]")) {
         var1 = var1.substring(0, var1.length() - "[0]".length());
      }

      this.name = var1;
      this.value = var2;
      this.defaultValue = var2;
      this.type = var3;
      this.offset = -1;
      this.length = 1;
   }

   public ShaderParameter(String var1, boolean var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Bool);
   }

   public ShaderParameter(String var1, int var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Int);
   }

   public ShaderParameter(String var1, int var2, boolean var3) {
      this(var1, var2, var3 ? ShaderParameter.ParameterTypes.Texture : ShaderParameter.ParameterTypes.Int);
   }

   public ShaderParameter(String var1, float var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Float);
   }

   public ShaderParameter(String var1, Vector2f var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Vector2);
   }

   public ShaderParameter(String var1, Vector3f var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Vector3);
   }

   public ShaderParameter(String var1, Vector4f var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Vector4);
   }

   public ShaderParameter(String var1, Matrix3f var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Matrix3);
   }

   public ShaderParameter(String var1, Matrix4f var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Matrix4);
   }

   public ShaderParameter(String var1, int[] var2, boolean var3) {
      this(var1, var2, var3 ? ShaderParameter.ParameterTypes.TextureArray : ShaderParameter.ParameterTypes.IntArray);
   }

   public ShaderParameter(String var1, float[] var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.FloatArray);
   }

   public ShaderParameter(String var1, Vector2f[] var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Vector2Array);

      for(int var3 = 0; var3 < var2.length; ++var3) {
         if (var2[var3] == null) {
            var2[var3] = new Vector2f();
         }
      }

   }

   public ShaderParameter(String var1, Vector3f[] var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Vector3Array);

      for(int var3 = 0; var3 < var2.length; ++var3) {
         if (var2[var3] == null) {
            var2[var3] = new Vector3f();
         }
      }

   }

   public ShaderParameter(String var1, Vector4f[] var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Vector4Array);

      for(int var3 = 0; var3 < var2.length; ++var3) {
         if (var2[var3] == null) {
            var2[var3] = new Vector4f();
         }
      }

   }

   public ShaderParameter(String var1, Matrix3f[] var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Matrix3Array);

      for(int var3 = 0; var3 < var2.length; ++var3) {
         if (var2[var3] == null) {
            var2[var3] = new Matrix3f();
         }
      }

   }

   public ShaderParameter(String var1, Matrix4f[] var2) {
      this(var1, var2, ShaderParameter.ParameterTypes.Matrix4Array);

      for(int var3 = 0; var3 < var2.length; ++var3) {
         if (var2[var3] == null) {
            var2[var3] = new Matrix4f();
         }
      }

   }

   public String toString() {
      return String.format("%s { %s: %3s }", this.getClass().getSimpleName(), this.name, this.value);
   }

   public ParameterTypes GetType() {
      return this.type;
   }

   private <T> T[] CopyArray(Class<T> var1, T[] var2, Function<T, T> var3) {
      Object[] var4 = Arrays.copyOf(var2, var2.length);

      for(int var5 = 0; var5 < var4.length; ++var5) {
         var4[var5] = var3.apply(var2[var5]);
      }

      return var4;
   }

   public void Copy(ShaderParameter var1, boolean var2, boolean var3) {
      if (this.type == var1.type || !var3) {
         if (var2 || this.type != var1.type) {
            this.defaultValue = var1.defaultValue;
         }

         this.type = var1.type;
         this.value = var1.value;
      }

   }

   public void ResetValue() {
      this.value = this.defaultValue;
   }

   public int GetSize() {
      int var10000;
      switch (this.type) {
         case Vector2:
            var10000 = 8;
            break;
         case Vector3:
            var10000 = 12;
            break;
         case Vector4:
            var10000 = 16;
            break;
         case Matrix4:
            var10000 = 64;
            break;
         case Vector2Array:
            var10000 = this.length * 8;
            break;
         case Vector3Array:
         case Vector4Array:
            var10000 = this.length * 16;
            break;
         case Matrix4Array:
            var10000 = this.length * 64;
            break;
         case Bool:
         case Int:
         case Float:
         case Texture:
            var10000 = 4;
            break;
         case Matrix3:
            var10000 = 36;
            break;
         case IntArray:
         case FloatArray:
         case TextureArray:
            var10000 = this.length * 4;
            break;
         case Matrix3Array:
            var10000 = this.length * 36;
            break;
         default:
            var10000 = 0;
      }

      return var10000;
   }

   public Object GetValue() {
      return this.value;
   }

   public Boolean GetBool() {
      return (Boolean)this.value;
   }

   public int GetInt() {
      return (Integer)this.value;
   }

   public float GetFloat() {
      return (Float)this.value;
   }

   public Vector2f GetVector2() {
      return (Vector2f)this.value;
   }

   public Vector3f GetVector3() {
      return (Vector3f)this.value;
   }

   public Vector4f GetVector4() {
      return (Vector4f)this.value;
   }

   public Matrix3f GetMatrix3() {
      return (Matrix3f)this.value;
   }

   public Matrix4f GetMatrix4() {
      return (Matrix4f)this.value;
   }

   public int GetTexture() {
      return (Integer)this.value;
   }

   public int[] GetIntArray() {
      return (int[])this.value;
   }

   public float[] GetFloatArray() {
      return (float[])this.value;
   }

   public Vector2f[] GetVector2Array() {
      return (Vector2f[])this.value;
   }

   public Vector3f[] GetVector3Array() {
      return (Vector3f[])this.value;
   }

   public Vector4f[] GetVector4Array() {
      return (Vector4f[])this.value;
   }

   public Matrix3f[] GetMatrix3Array() {
      return (Matrix3f[])this.value;
   }

   public Matrix4f[] GetMatrix4Array() {
      return (Matrix4f[])this.value;
   }

   public int[] GetTextureArray() {
      return (int[])this.value;
   }

   public FloatBuffer GetBuffer() {
      return (FloatBuffer)this.value;
   }

   private void SetValue(Object var1, ParameterTypes var2) {
      if (var2 != this.type) {
         String var3 = String.format("Changing parameter %s from %s to %s", this.name, this.type, var2);
         DebugLog.Shader.warn(var3);
         this.type = var2;
         this.defaultValue = var1;
      }

      this.value = var1;
   }

   public void SetBool(boolean var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Bool);
   }

   public void SetInt(int var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Int);
   }

   public void SetFloat(float var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Float);
   }

   public void SetVector2(Vector2f var1) {
      this.SetVector2(var1.x, var1.y);
   }

   public void SetVector2(float var1, float var2) {
      if (this.value != null && this.type == ShaderParameter.ParameterTypes.Vector2) {
         Vector2f var3 = (Vector2f)this.value;
         var3.set(var1, var2);
      } else {
         this.SetValue(new Vector2f(var1, var2), ShaderParameter.ParameterTypes.Vector2);
      }

   }

   public void SetVector3(Vector3f var1) {
      this.SetVector3(var1.x, var1.y, var1.z);
   }

   public void SetVector3(float var1, float var2, float var3) {
      if (this.value != null && this.type == ShaderParameter.ParameterTypes.Vector3) {
         Vector3f var4 = (Vector3f)this.value;
         var4.set(var1, var2, var3);
      } else {
         this.SetValue(new Vector3f(var1, var2, var3), ShaderParameter.ParameterTypes.Vector3);
      }

   }

   public void SetVector4(Vector4f var1) {
      this.SetVector4(var1.x, var1.y, var1.z, var1.w);
   }

   public void SetVector4(float var1, float var2, float var3, float var4) {
      if (this.value != null && this.type == ShaderParameter.ParameterTypes.Vector4) {
         Vector4f var5 = (Vector4f)this.value;
         var5.set(var1, var2, var3, var4);
      } else {
         this.SetValue(new Vector4f(var1, var2, var3, var4), ShaderParameter.ParameterTypes.Vector4);
      }

   }

   public void SetMatrix3(Matrix3f var1) {
      Matrix3f var2;
      if (this.value != null && this.type == ShaderParameter.ParameterTypes.Matrix3) {
         var2 = (Matrix3f)this.value;
      } else {
         this.SetValue(var2 = new Matrix3f(), ShaderParameter.ParameterTypes.Matrix3);
      }

      var2.load(var1);
   }

   public void SetMatrix4(Matrix4f var1) {
      Matrix4f var2;
      if (this.value != null && this.type == ShaderParameter.ParameterTypes.Matrix4) {
         var2 = (Matrix4f)this.value;
      } else {
         this.SetValue(var2 = new Matrix4f(), ShaderParameter.ParameterTypes.Matrix4);
      }

      var2.load(var1);
   }

   public void SetTexture(int var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Texture);
   }

   public void SetIntArray(int[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.IntArray);
   }

   public void SetFloatArray(float[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.FloatArray);
   }

   public void SetVector2Array(Vector2f[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Vector2Array);
   }

   public void SetVector3Array(Vector3f[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Vector3Array);
   }

   public void SetVector4Array(Vector4f[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Vector4Array);
   }

   public void SetMatrix3Array(Matrix3f[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Matrix3Array);
   }

   public void SetMatrix4Array(Matrix4f[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.Matrix4Array);
   }

   public void SetTextureArray(int[] var1) {
      this.SetValue(var1, ShaderParameter.ParameterTypes.TextureArray);
   }

   private static FloatBuffer StoreVectors(Vector[] var0, int var1) {
      FloatBuffer var2 = MemoryUtil.memAllocFloat(var1 * var0.length);

      for(int var3 = 0; var3 < var0.length; ++var3) {
         var0[var3].store(var2);
      }

      var2.flip();
      return var2;
   }

   private void LoadVectors(int var1, Vector[] var2, int var3) {
      FloatBuffer var4 = MemoryUtil.memAllocFloat(var3 * var2.length);
      GL43.glGetUniformfv(var1, this.offset, var4);
      var4.rewind();

      for(int var5 = 0; var5 < var2.length; ++var5) {
         var2[var5].load(var4);
      }

      MemoryUtil.memFree(var4);
   }

   private static FloatBuffer StoreMatrices(Matrix[] var0, int var1) {
      FloatBuffer var2 = MemoryUtil.memAllocFloat(var1 * var0.length);

      for(int var3 = 0; var3 < var0.length; ++var3) {
         var0[var3].store(var2);
      }

      var2.flip();
      return var2;
   }

   private void LoadMatrices(int var1, Matrix[] var2, int var3) {
      FloatBuffer var4 = MemoryUtil.memAllocFloat(var3 * var2.length);
      GL43.glGetUniformfv(var1, this.offset, var4);
      var4.rewind();

      for(int var5 = 0; var5 < var2.length; ++var5) {
         var2[var5].load(var4);
      }

      MemoryUtil.memFree(var4);
   }

   public void UpdateDefault() {
      this.defaultValue = this.value;
   }

   public void PushUniform() {
      FloatBuffer var1;
      switch (this.type) {
         case Vector2:
            Vector2f var4 = (Vector2f)this.value;
            GL43.glUniform2f(this.offset, var4.x, var4.y);
            break;
         case Vector3:
            Vector3f var3 = (Vector3f)this.value;
            GL43.glUniform3f(this.offset, var3.x, var3.y, var3.z);
            break;
         case Vector4:
            Vector4f var2 = (Vector4f)this.value;
            GL43.glUniform4f(this.offset, var2.x, var2.y, var2.z, var2.w);
            break;
         case Matrix4:
            ((Matrix4f)this.value).store(MatrixBuffer);
            GL43.glUniformMatrix4fv(this.offset, true, MatrixBuffer.flip());
            break;
         case Vector2Array:
            var1 = StoreVectors((Vector2f[])this.value, 2);
            GL43.glUniform2fv(this.offset, var1);
            MemoryUtil.memFree(var1);
            break;
         case Vector3Array:
            var1 = StoreVectors((Vector3f[])this.value, 3);
            GL43.glUniform3fv(this.offset, var1);
            MemoryUtil.memFree(var1);
            break;
         case Vector4Array:
            var1 = StoreVectors((Vector3f[])this.value, 4);
            GL43.glUniform4fv(this.offset, var1);
            MemoryUtil.memFree(var1);
            break;
         case Matrix4Array:
            var1 = StoreMatrices((Matrix4f[])this.value, 16);
            GL43.glUniformMatrix4fv(this.offset, true, var1);
            MemoryUtil.memFree(var1);
            break;
         case Bool:
            GL43.glUniform1i(this.offset, (Boolean)this.value ? 1 : 0);
            break;
         case Int:
         case Texture:
            GL43.glUniform1i(this.offset, (Integer)this.value);
            break;
         case Float:
            GL43.glUniform1f(this.offset, (Float)this.value);
            break;
         case Matrix3:
            ((Matrix3f)this.value).store(MatrixBuffer);
            GL43.glUniformMatrix3fv(this.offset, true, MatrixBuffer.flip());
            MatrixBuffer.limit(MatrixBuffer.capacity());
            break;
         case IntArray:
         case TextureArray:
            GL43.glUniform1iv(this.offset, (int[])this.value);
            break;
         case FloatArray:
            GL43.glUniform1fv(this.offset, (float[])this.value);
            break;
         case Matrix3Array:
            var1 = StoreMatrices((Matrix3f[])this.value, 9);
            GL43.glUniformMatrix3fv(this.offset, true, var1);
            MemoryUtil.memFree(var1);
      }

   }

   public void PullUniform(int var1) {
      switch (this.type) {
         case Vector2:
            GL43.glGetUniformfv(var1, this.offset, VectorBuffer);
            VectorBuffer.rewind();
            ((Vector2f)this.value).load(VectorBuffer);
            VectorBuffer.rewind();
            break;
         case Vector3:
            GL43.glGetUniformfv(var1, this.offset, VectorBuffer);
            VectorBuffer.rewind();
            ((Vector3f)this.value).load(VectorBuffer);
            VectorBuffer.rewind();
            break;
         case Vector4:
            GL43.glGetUniformfv(var1, this.offset, VectorBuffer);
            VectorBuffer.rewind();
            ((Vector4f)this.value).load(VectorBuffer);
            VectorBuffer.rewind();
            break;
         case Matrix4:
            GL43.glGetUniformfv(var1, this.offset, MatrixBuffer);
            MatrixBuffer.rewind();
            ((Matrix4f)this.value).load(MatrixBuffer);
            MatrixBuffer.rewind();
            break;
         case Vector2Array:
            this.LoadVectors(var1, (Vector2f[])this.value, 2);
            break;
         case Vector3Array:
            this.LoadVectors(var1, (Vector3f[])this.value, 3);
            break;
         case Vector4Array:
            this.LoadVectors(var1, (Vector4f[])this.value, 4);
            break;
         case Matrix4Array:
            this.LoadMatrices(var1, (Matrix4f[])this.value, 16);
            break;
         case Bool:
            this.value = GL43.glGetUniformi(var1, this.offset) != 0;
            break;
         case Int:
         case Texture:
            this.value = GL43.glGetUniformi(var1, this.offset);
            break;
         case Float:
            this.value = GL43.glGetUniformf(var1, this.offset);
            break;
         case Matrix3:
            GL43.glGetUniformfv(var1, this.offset, MatrixBuffer);
            MatrixBuffer.rewind();
            ((Matrix3f)this.value).load(MatrixBuffer);
            MatrixBuffer.rewind();
            break;
         case IntArray:
         case TextureArray:
            GL43.glGetUniformiv(var1, this.offset, (int[])this.value);
            break;
         case FloatArray:
            GL43.glGetUniformfv(var1, this.offset, (float[])this.value);
            break;
         case Matrix3Array:
            this.LoadMatrices(var1, (Matrix3f[])this.value, 9);
      }

   }

   public void PushInstanced(InstancedBuffer var1, int var2) {
      this.WriteToBuffer(var1.data, var2);
   }

   public void WriteToBuffer(ByteBuffer var1, int var2) {
      if (this.offset >= 0 && this.type != ShaderParameter.ParameterTypes.Texture && this.type != ShaderParameter.ParameterTypes.TextureArray) {
         var1.position(var2 + this.offset);
         switch (this.type) {
            case Vector2:
               ShaderBuffer.PushVector2(var1, (Vector2f)this.value);
               break;
            case Vector3:
               ShaderBuffer.PushVector3(var1, (Vector3f)this.value);
               break;
            case Vector4:
               ShaderBuffer.PushVector4(var1, (Vector4f)this.value);
               break;
            case Matrix4:
               ShaderBuffer.PushMatrix4(var1, (Matrix4f)this.value);
               break;
            case Vector2Array:
               ShaderBuffer.PushVector2Array(var1, (Vector2f[])this.value);
               break;
            case Vector3Array:
               ShaderBuffer.PushVector3Array(var1, (Vector3f[])this.value);
               break;
            case Vector4Array:
               ShaderBuffer.PushVector4Array(var1, (Vector4f[])this.value);
               break;
            case Matrix4Array:
               ShaderBuffer.PushMatrix4Array(var1, (Matrix4f[])this.value);
               break;
            case Bool:
               ShaderBuffer.PushBool(var1, (Boolean)this.value);
               break;
            case Int:
            case Texture:
               ShaderBuffer.PushInt(var1, (Integer)this.value);
               break;
            case Float:
               ShaderBuffer.PushFloat(var1, (Float)this.value);
               break;
            case Matrix3:
               ShaderBuffer.PushMatrix3(var1, (Matrix3f)this.value);
               break;
            case IntArray:
            case TextureArray:
               ShaderBuffer.PushIntArray(var1, (int[])this.value);
               break;
            case FloatArray:
               ShaderBuffer.PushFloatArray(var1, (float[])this.value);
         }

      }
   }

   public static enum ParameterTypes {
      Bool,
      Int,
      Float,
      Vector2,
      Vector3,
      Vector4,
      Matrix3,
      Matrix4,
      Texture,
      IntArray,
      FloatArray,
      Vector2Array,
      Vector3Array,
      Vector4Array,
      Matrix3Array,
      Matrix4Array,
      TextureArray;

      private ParameterTypes() {
      }
   }
}
