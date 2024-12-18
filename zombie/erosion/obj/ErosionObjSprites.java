package zombie.erosion.obj;

import java.util.ArrayList;

public final class ErosionObjSprites {
   public static final int SECTION_BASE = 0;
   public static final int SECTION_SNOW = 1;
   public static final int SECTION_FLOWER = 2;
   public static final int SECTION_CHILD = 3;
   public static final int NUM_SECTIONS = 4;
   public String name;
   public int stages;
   public boolean hasSnow;
   public boolean hasFlower;
   public boolean hasChildSprite;
   public boolean noSeasonBase;
   public int cycleTime = 1;
   private Stage[] sprites;

   public ErosionObjSprites(int var1, String var2, boolean var3, boolean var4, boolean var5) {
      this.name = var2;
      this.stages = var1;
      this.hasSnow = var3;
      this.hasFlower = var4;
      this.hasChildSprite = var5;
      this.sprites = new Stage[var1];

      for(int var6 = 0; var6 < var1; ++var6) {
         this.sprites[var6] = new Stage();
         this.sprites[var6].sections[0] = new Section();
         if (this.hasSnow) {
            this.sprites[var6].sections[1] = new Section();
         }

         if (this.hasFlower) {
            this.sprites[var6].sections[2] = new Section();
         }

         if (this.hasChildSprite) {
            this.sprites[var6].sections[3] = new Section();
         }
      }

   }

   private String getSprite(int var1, int var2, int var3) {
      return this.sprites[var1] != null && this.sprites[var1].sections[var2] != null && this.sprites[var1].sections[var2].seasons[var3] != null ? this.sprites[var1].sections[var2].seasons[var3].getNext() : null;
   }

   public String getBase(int var1, int var2) {
      return this.getSprite(var1, 0, var2);
   }

   public String getFlower(int var1) {
      return this.hasFlower ? this.getSprite(var1, 2, 0) : null;
   }

   public String getChildSprite(int var1, int var2) {
      return this.hasChildSprite ? this.getSprite(var1, 3, var2) : null;
   }

   private void setSprite(int var1, int var2, String var3, int var4) {
      if (this.sprites[var1] != null && this.sprites[var1].sections[var2] != null) {
         this.sprites[var1].sections[var2].seasons[var4] = new Sprites(var3);
      }

   }

   private void setSprite(int var1, int var2, ArrayList<String> var3, int var4) {
      assert !var3.isEmpty();

      if (this.sprites[var1] != null && this.sprites[var1].sections[var2] != null) {
         this.sprites[var1].sections[var2].seasons[var4] = new Sprites(var3);
      }

   }

   public void setBase(int var1, String var2, int var3) {
      this.setSprite(var1, 0, (String)var2, var3);
   }

   public void setBase(int var1, ArrayList<String> var2, int var3) {
      this.setSprite(var1, 0, (ArrayList)var2, var3);
   }

   public void setFlower(int var1, String var2) {
      this.setSprite(var1, 2, (String)var2, 0);
   }

   public void setFlower(int var1, ArrayList<String> var2) {
      this.setSprite(var1, 2, (ArrayList)var2, 0);
   }

   public void setChildSprite(int var1, String var2, int var3) {
      this.setSprite(var1, 3, (String)var2, var3);
   }

   public void setChildSprite(int var1, ArrayList<String> var2, int var3) {
      this.setSprite(var1, 3, (ArrayList)var2, var3);
   }

   private static class Stage {
      public Section[] sections = new Section[4];

      private Stage() {
      }
   }

   private static class Section {
      public Sprites[] seasons = new Sprites[6];

      private Section() {
      }
   }

   private static final class Sprites {
      public final ArrayList<String> sprites = new ArrayList();
      private int index = -1;

      public Sprites(String var1) {
         this.sprites.add(var1);
      }

      public Sprites(ArrayList<String> var1) {
         this.sprites.addAll(var1);
      }

      public String getNext() {
         if (++this.index >= this.sprites.size()) {
            this.index = 0;
         }

         return (String)this.sprites.get(this.index);
      }
   }
}
