package zombie.pathfind;

import java.util.ArrayDeque;

final class ImmutableRectF {
   private float x;
   private float y;
   private float w;
   private float h;
   static final ArrayDeque<ImmutableRectF> pool = new ArrayDeque();

   ImmutableRectF() {
   }

   ImmutableRectF init(float var1, float var2, float var3, float var4) {
      this.x = var1;
      this.y = var2;
      this.w = var3;
      this.h = var4;
      return this;
   }

   float left() {
      return this.x;
   }

   float top() {
      return this.y;
   }

   float right() {
      return this.x + this.w;
   }

   float bottom() {
      return this.y + this.h;
   }

   float width() {
      return this.w;
   }

   float height() {
      return this.h;
   }

   boolean containsPoint(float var1, float var2) {
      return var1 >= this.left() && var1 < this.right() && var2 >= this.top() && var2 < this.bottom();
   }

   boolean intersects(ImmutableRectF var1) {
      return this.left() < var1.right() && this.right() > var1.left() && this.top() < var1.bottom() && this.bottom() > var1.top();
   }

   static ImmutableRectF alloc() {
      return pool.isEmpty() ? new ImmutableRectF() : (ImmutableRectF)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
