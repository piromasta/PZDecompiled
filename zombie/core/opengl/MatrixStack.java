package zombie.core.opengl;

import java.util.ArrayDeque;
import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.popman.ObjectPool;

public final class MatrixStack {
   private final int m_mode;
   private final ArrayDeque<Matrix4f> m_matrices = new ArrayDeque();
   private final ObjectPool<Matrix4f> m_pool = new ObjectPool(Matrix4f::new);

   public MatrixStack(int var1) {
      this.m_mode = var1;
   }

   public Matrix4f alloc() {
      if (Core.bDebug && Thread.currentThread() != RenderThread.RenderThread) {
         boolean var1 = true;
      }

      return (Matrix4f)this.m_pool.alloc();
   }

   public void release(Matrix4f var1) {
      this.m_pool.release((Object)var1);
   }

   public void push(Matrix4f var1) {
      if (Core.bDebug && Thread.currentThread() != RenderThread.RenderThread) {
         boolean var2 = true;
      }

      this.m_matrices.push(var1);
   }

   public void pop() {
      if (Core.bDebug && Thread.currentThread() != RenderThread.RenderThread) {
         boolean var1 = true;
      }

      Matrix4f var2 = (Matrix4f)this.m_matrices.pop();
      this.m_pool.release((Object)var2);
   }

   public Matrix4f peek() {
      return (Matrix4f)this.m_matrices.peek();
   }

   public boolean isEmpty() {
      return this.m_matrices.isEmpty();
   }

   public void clear() {
      while(!this.isEmpty()) {
         this.pop();
      }

   }
}
