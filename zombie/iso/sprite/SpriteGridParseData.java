package zombie.iso.sprite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.popman.ObjectPool;

public final class SpriteGridParseData {
   final ObjectPool<Level> levelPool = new ObjectPool(Level::new);
   public final ArrayList<Level> levels = new ArrayList();
   public int width;
   public int height;

   public SpriteGridParseData() {
   }

   public Level getOrCreateLevel(int var1) {
      for(int var2 = this.levels.size(); var2 <= var1; ++var2) {
         Level var3 = (Level)this.levelPool.alloc();
         var3.width = 0;
         var3.height = 0;
         var3.z = var2;
         var3.xyToSprite.clear();
         this.levels.add(var3);
      }

      return (Level)this.levels.get(var1);
   }

   public boolean isValid() {
      if (this.width > 0 && this.height > 0) {
         if (this.levels.isEmpty()) {
            return false;
         } else {
            Iterator var1 = this.levels.iterator();

            Level var2;
            do {
               if (!var1.hasNext()) {
                  return true;
               }

               var2 = (Level)var1.next();
            } while(var2.isValid());

            return false;
         }
      } else {
         return false;
      }
   }

   public void clear() {
      this.levelPool.releaseAll(this.levels);
      this.levels.clear();
      this.width = 0;
      this.height = 0;
   }

   public static final class Level {
      public int width;
      public int height;
      public int z;
      public HashMap<String, IsoSprite> xyToSprite = new HashMap();

      public Level() {
      }

      boolean isValid() {
         if (this.width > 0 && this.height > 0) {
            return !this.xyToSprite.isEmpty();
         } else {
            return false;
         }
      }
   }
}
