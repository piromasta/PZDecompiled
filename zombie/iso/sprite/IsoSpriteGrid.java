package zombie.iso.sprite;

public final class IsoSpriteGrid {
   private final IsoSprite[] sprites;
   private final int width;
   private final int height;
   private final int levels;

   public IsoSpriteGrid(int var1, int var2, int var3) {
      this.sprites = new IsoSprite[var1 * var2 * var3];
      this.width = var1;
      this.height = var2;
      this.levels = var3;
   }

   public IsoSpriteGrid(int var1, int var2) {
      this(var1, var2, 1);
   }

   public IsoSprite getAnchorSprite() {
      for(int var1 = 0; var1 < this.getWidth() * this.getHeight(); ++var1) {
         IsoSprite var2 = this.sprites[var1];
         if (var2 != null) {
            return var2;
         }
      }

      return null;
   }

   public IsoSprite getSprite(int var1, int var2, int var3) {
      return this.isValidXYZ(var1, var2, var3) ? this.sprites[this.getSpriteIndex(var1, var2, var3)] : null;
   }

   public IsoSprite getSprite(int var1, int var2) {
      return this.getSprite(var1, var2, 0);
   }

   public void setSprite(int var1, int var2, int var3, IsoSprite var4) {
      if (this.isValidXYZ(var1, var2, var3)) {
         this.sprites[this.getSpriteIndex(var1, var2, var3)] = var4;
      }

   }

   public void setSprite(int var1, int var2, IsoSprite var3) {
      this.setSprite(var1, var2, 0, var3);
   }

   public int getSpriteIndex(IsoSprite var1) {
      for(int var2 = 0; var2 < this.sprites.length; ++var2) {
         IsoSprite var3 = this.sprites[var2];
         if (var3 != null && var3 == var1) {
            return var2;
         }
      }

      return -1;
   }

   public int getSpriteGridPosX(IsoSprite var1) {
      int var2 = this.getSpriteIndex(var1);
      if (var2 >= 0) {
         var2 %= this.getWidth() * this.getHeight();
         return var2 % this.getWidth();
      } else {
         return -1;
      }
   }

   public int getSpriteGridPosY(IsoSprite var1) {
      int var2 = this.getSpriteIndex(var1);
      if (var2 >= 0) {
         var2 %= this.getWidth() * this.getHeight();
         return var2 / this.getWidth();
      } else {
         return -1;
      }
   }

   public int getSpriteGridPosZ(IsoSprite var1) {
      int var2 = this.getSpriteIndex(var1);
      return var2 >= 0 ? var2 / (this.getWidth() * this.getHeight()) : -1;
   }

   public IsoSprite getSpriteFromIndex(int var1) {
      return var1 >= 0 && var1 < this.getSpriteCount() ? this.sprites[var1] : null;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getLevels() {
      return this.levels;
   }

   public boolean validate() {
      if (this.getLevels() == 0) {
         return false;
      } else {
         for(int var1 = 0; var1 < this.getLevels(); ++var1) {
            int var2 = var1 * this.getWidth() * this.getHeight();
            boolean var3 = true;

            for(int var4 = 0; var4 < this.getWidth() * this.getHeight(); ++var4) {
               if (this.sprites[var2 + var4] != null) {
                  var3 = false;
                  break;
               }
            }

            if (var3) {
               return false;
            }
         }

         return true;
      }
   }

   public int getSpriteCount() {
      return this.sprites.length;
   }

   public IsoSprite[] getSprites() {
      return this.sprites;
   }

   public boolean isValidXYZ(int var1, int var2, int var3) {
      return var1 >= 0 && var1 < this.getWidth() && var2 >= 0 && var2 < this.getHeight() && var3 >= 0 && var3 < this.getLevels();
   }

   public int getSpriteIndex(int var1, int var2, int var3) {
      return this.isValidXYZ(var1, var2, var3) ? var1 + var2 * this.getWidth() + var3 * this.getWidth() * this.getHeight() : -1;
   }
}
