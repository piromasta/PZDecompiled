package zombie.basements;

import zombie.iso.NewMapBinaryFile;

public final class BasementDefinition {
   public String name;
   public int width;
   public int height;
   public int stairx;
   public int stairy;
   public boolean north;
   public NewMapBinaryFile.Header header;

   public BasementDefinition() {
   }
}
