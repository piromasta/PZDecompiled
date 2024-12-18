package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;

public final class VehicleRect {
   VehicleCluster cluster;
   Vehicle vehicle;
   public int x;
   public int y;
   public int w;
   public int h;
   public int z;
   static final ArrayDeque<VehicleRect> pool = new ArrayDeque();

   public VehicleRect() {
   }

   VehicleRect init(Vehicle var1, int var2, int var3, int var4, int var5, int var6) {
      this.cluster = null;
      this.vehicle = var1;
      this.x = var2;
      this.y = var3;
      this.w = var4;
      this.h = var5;
      this.z = var6;
      return this;
   }

   VehicleRect init(int var1, int var2, int var3, int var4, int var5) {
      this.cluster = null;
      this.vehicle = null;
      this.x = var1;
      this.y = var2;
      this.w = var3;
      this.h = var4;
      this.z = var5;
      return this;
   }

   public int left() {
      return this.x;
   }

   public int top() {
      return this.y;
   }

   public int right() {
      return this.x + this.w;
   }

   public int bottom() {
      return this.y + this.h;
   }

   boolean containsPoint(float var1, float var2, float var3) {
      return var1 >= (float)this.left() && var1 < (float)this.right() && var2 >= (float)this.top() && var2 < (float)this.bottom() && PZMath.fastfloor(var3) == this.z;
   }

   boolean containsPoint(float var1, float var2, float var3, int var4) {
      int var5 = this.x - var4;
      int var6 = this.y - var4;
      int var7 = this.right() + var4;
      int var8 = this.bottom() + var4;
      return var1 >= (float)var5 && var1 < (float)var7 && var2 >= (float)var6 && var2 < (float)var8 && PZMath.fastfloor(var3) == this.z;
   }

   boolean intersects(VehicleRect var1) {
      return this.left() < var1.right() && this.right() > var1.left() && this.top() < var1.bottom() && this.bottom() > var1.top();
   }

   boolean intersects(int var1, int var2, int var3, int var4, int var5) {
      return this.left() - var5 < var3 && this.right() + var5 > var1 && this.top() - var5 < var4 && this.bottom() + var5 > var2;
   }

   boolean isAdjacent(VehicleRect var1) {
      --this.x;
      --this.y;
      this.w += 2;
      this.h += 2;
      boolean var2 = this.intersects(var1);
      ++this.x;
      ++this.y;
      this.w -= 2;
      this.h -= 2;
      return var2;
   }

   static VehicleRect alloc() {
      boolean var0;
      if (pool.isEmpty()) {
         var0 = false;
      } else {
         var0 = false;
      }

      return pool.isEmpty() ? new VehicleRect() : (VehicleRect)pool.pop();
   }

   public void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
