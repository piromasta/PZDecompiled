package zombie.core.rendering;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.Color;

public class ShaderBuffer {
   protected int bufferID;
   protected int binding;
   protected ByteBuffer data;

   protected ShaderBuffer() {
   }

   public ShaderBuffer(int var1) {
      this.bufferID = GL45.glGenBuffers();
      this.data = MemoryUtil.memAlloc(var1);
      GL45.glBindBuffer(37074, this.bufferID);
      GL45.glNamedBufferStorage(this.bufferID, this.data, 256);
   }

   public int GetBufferID() {
      return this.bufferID;
   }

   public void Release() {
      GL45.glDeleteBuffers(this.bufferID);
      this.data.clear();
   }

   public void SetBinding(int var1) {
      if (var1 >= 0 && this.bufferID >= 0) {
         this.binding = var1;
         GL30.glBindBufferBase(37074, var1, this.bufferID);
      }

   }

   public int GetBinding() {
      return this.binding;
   }

   public void UpdateData() {
      if (this.bufferID >= 0 && this.data != null) {
         this.PreUpdate();
         this.data.flip();
         this.UpdateBufferData();
         this.data.limit(this.data.capacity());
         this.OnReset();
      }

   }

   protected void PreUpdate() {
   }

   protected void OnReset() {
   }

   protected void UpdateBufferData() {
      GL45.glBindBuffer(37074, this.bufferID);
      GL45.glBufferSubData(37074, 0L, this.data);
      GL30.glBindBufferBase(37074, this.binding, this.bufferID);
   }

   public void Advance(int var1) {
      this.data.position(this.data.position() + var1);
   }

   public void SetPosition(int var1) {
      this.data.position(var1);
   }

   public static void PushBool(ByteBuffer var0, boolean var1) {
      var0.put((byte)(var1 ? 1 : 0));
   }

   public static void PushInt(ByteBuffer var0, int var1) {
      var0.putInt(var1);
   }

   public static void PushFloat(ByteBuffer var0, float var1) {
      var0.putFloat(var1);
   }

   public static void PushFloat2(ByteBuffer var0, float var1, float var2) {
      var0.putFloat(var1);
      var0.putFloat(var2);
   }

   public static void PushFloat3(ByteBuffer var0, float var1, float var2, float var3) {
      var0.putFloat(var1);
      var0.putFloat(var2);
      var0.putFloat(var3);
   }

   public static void PushFloat4(ByteBuffer var0, float var1, float var2, float var3, float var4) {
      var0.putFloat(var1);
      var0.putFloat(var2);
      var0.putFloat(var3);
      var0.putFloat(var4);
   }

   public static void PushVector2(ByteBuffer var0, Vector2f var1) {
      var0.putFloat(var1.x);
      var0.putFloat(var1.y);
   }

   public static void PushVector3(ByteBuffer var0, Vector3f var1) {
      var0.putFloat(var1.x);
      var0.putFloat(var1.y);
      var0.putFloat(var1.z);
   }

   public static void PushVector4(ByteBuffer var0, Vector4f var1) {
      var0.putFloat(var1.x);
      var0.putFloat(var1.y);
      var0.putFloat(var1.z);
      var0.putFloat(var1.w);
   }

   public static void PushColor(ByteBuffer var0, Color var1) {
      var0.putFloat(var1.r);
      var0.putFloat(var1.g);
      var0.putFloat(var1.b);
      var0.putFloat(var1.a);
   }

   public static void PushMatrix3(ByteBuffer var0, Matrix3f var1) {
      var0.putFloat(var1.m00);
      var0.putFloat(var1.m10);
      var0.putFloat(var1.m20);
      var0.putFloat(var1.m01);
      var0.putFloat(var1.m11);
      var0.putFloat(var1.m21);
      var0.putFloat(var1.m02);
      var0.putFloat(var1.m12);
      var0.putFloat(var1.m22);
   }

   public static void PushMatrix4(ByteBuffer var0, Matrix4f var1) {
      var0.putFloat(var1.m00);
      var0.putFloat(var1.m10);
      var0.putFloat(var1.m20);
      var0.putFloat(var1.m30);
      var0.putFloat(var1.m01);
      var0.putFloat(var1.m11);
      var0.putFloat(var1.m21);
      var0.putFloat(var1.m31);
      var0.putFloat(var1.m02);
      var0.putFloat(var1.m12);
      var0.putFloat(var1.m22);
      var0.putFloat(var1.m32);
      var0.putFloat(var1.m03);
      var0.putFloat(var1.m13);
      var0.putFloat(var1.m23);
      var0.putFloat(var1.m33);
   }

   public static void PushIntArray(ByteBuffer var0, int[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         var0.putInt(var2);
      }

   }

   public static void PushFloatArray(ByteBuffer var0, float[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         var0.putFloat(var1[var2]);
      }

   }

   public static void PushVector2Array(ByteBuffer var0, Vector2f[] var1) {
      Vector2f[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Vector2f var5 = var2[var4];
         PushVector2(var0, var5);
      }

   }

   public static void PushVector3Array(ByteBuffer var0, Vector3f[] var1) {
      Vector3f[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Vector3f var5 = var2[var4];
         PushVector3(var0, var5);
         PushFloat(var0, 0.0F);
      }

   }

   public static void PushVector4Array(ByteBuffer var0, Vector4f[] var1) {
      Vector4f[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Vector4f var5 = var2[var4];
         PushVector4(var0, var5);
      }

   }

   public static void PushMatrix3Array(ByteBuffer var0, Matrix3f[] var1) {
      Matrix3f[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Matrix3f var5 = var2[var4];
         PushMatrix3(var0, var5);
      }

   }

   public static void PushMatrix4Array(ByteBuffer var0, Matrix4f[] var1) {
      Matrix4f[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Matrix4f var5 = var2[var4];
         PushMatrix4(var0, var5);
      }

   }

   public static void PushTextureArray(ByteBuffer var0, int[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         var0.putInt(var2);
      }

   }

   public static void PushColorArray(ByteBuffer var0, Color[] var1) {
      Color[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Color var5 = var2[var4];
         PushColor(var0, var5);
      }

   }
}
