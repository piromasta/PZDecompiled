package zombie.entity.util;

import java.util.Comparator;

public class SnapshotArray<T> extends Array<T> {
   private T[] snapshot;
   private T[] recycled;
   private int snapshots;

   public SnapshotArray() {
   }

   public SnapshotArray(Array var1) {
      super(var1);
   }

   public SnapshotArray(boolean var1, int var2, Class var3) {
      super(var1, var2, var3);
   }

   public SnapshotArray(boolean var1, int var2) {
      super(var1, var2);
   }

   public SnapshotArray(boolean var1, T[] var2, int var3, int var4) {
      super(var1, var2, var3, var4);
   }

   public SnapshotArray(Class var1) {
      super(var1);
   }

   public SnapshotArray(int var1) {
      super(var1);
   }

   public SnapshotArray(T[] var1) {
      super(var1);
   }

   public T[] begin() {
      this.modified();
      this.snapshot = this.items;
      ++this.snapshots;
      return this.items;
   }

   public void end() {
      this.snapshots = Math.max(0, this.snapshots - 1);
      if (this.snapshot != null) {
         if (this.snapshot != this.items && this.snapshots == 0) {
            this.recycled = this.snapshot;
            int var1 = 0;

            for(int var2 = this.recycled.length; var1 < var2; ++var1) {
               this.recycled[var1] = null;
            }
         }

         this.snapshot = null;
      }
   }

   private void modified() {
      if (this.snapshot != null && this.snapshot == this.items) {
         if (this.recycled != null && this.recycled.length >= this.size) {
            System.arraycopy(this.items, 0, this.recycled, 0, this.size);
            this.items = this.recycled;
            this.recycled = null;
         } else {
            this.resize(this.items.length);
         }

      }
   }

   public void set(int var1, T var2) {
      this.modified();
      super.set(var1, var2);
   }

   public void insert(int var1, T var2) {
      this.modified();
      super.insert(var1, var2);
   }

   public void insertRange(int var1, int var2) {
      this.modified();
      super.insertRange(var1, var2);
   }

   public void swap(int var1, int var2) {
      this.modified();
      super.swap(var1, var2);
   }

   public boolean removeValue(T var1, boolean var2) {
      this.modified();
      return super.removeValue(var1, var2);
   }

   public T removeIndex(int var1) {
      this.modified();
      return super.removeIndex(var1);
   }

   public void removeRange(int var1, int var2) {
      this.modified();
      super.removeRange(var1, var2);
   }

   public boolean removeAll(Array<? extends T> var1, boolean var2) {
      this.modified();
      return super.removeAll(var1, var2);
   }

   public T pop() {
      this.modified();
      return super.pop();
   }

   public void clear() {
      this.modified();
      super.clear();
   }

   public void sort() {
      this.modified();
      super.sort();
   }

   public void sort(Comparator<? super T> var1) {
      this.modified();
      super.sort(var1);
   }

   public void reverse() {
      this.modified();
      super.reverse();
   }

   public void shuffle() {
      this.modified();
      super.shuffle();
   }

   public void truncate(int var1) {
      this.modified();
      super.truncate(var1);
   }

   public T[] setSize(int var1) {
      this.modified();
      return super.setSize(var1);
   }

   public static <T> SnapshotArray<T> with(T... var0) {
      return new SnapshotArray(var0);
   }
}
