package zombie.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ImportantArea {
   public int sx = 0;
   public int sy = 0;
   public long lastUpdate = 0L;

   public ImportantArea(int var1, int var2) {
      this.lastUpdate = System.currentTimeMillis();
      this.sx = var1;
      this.sy = var2;
   }

   public final void load(ByteBuffer var1, int var2) throws IOException {
      this.sx = var1.getInt();
      this.sy = var1.getInt();
      this.lastUpdate = System.currentTimeMillis();
   }

   public final void save(ByteBuffer var1) throws IOException {
      var1.putInt(this.sx);
      var1.putInt(this.sy);
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         ImportantArea var2 = (ImportantArea)var1;
         return this.sx == var2.sx && this.sy == var2.sy;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.sx, this.sy});
   }
}
