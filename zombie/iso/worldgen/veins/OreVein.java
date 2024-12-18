package zombie.iso.worldgen.veins;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import zombie.iso.IsoCell;
import zombie.iso.Vector2;
import zombie.iso.worldgen.biomes.TileGroup;

public class OreVein {
   private final int centerCellX;
   private final int centerCellY;
   private final int depth;
   private final int centerSquareX;
   private final int centerSquareY;
   private final int armsAmount;
   private final float[] armsOrientation;
   private final float[] armsDistance;
   private final Vector2 startPoint;
   private final Vector2[] endPoints;
   private final List<TileGroup> tileGroups;
   private final float armsProb;
   private final float centerProb;
   private final int centerRadius;

   public OreVein(int var1, int var2, OreVeinConfig var3, Random var4) {
      this.centerCellX = var1;
      this.centerCellY = var2;
      this.depth = 0;
      this.centerSquareX = var4.nextInt(IsoCell.CellSizeInSquares);
      this.centerSquareY = var4.nextInt(IsoCell.CellSizeInSquares);
      this.startPoint = new Vector2((float)(this.centerCellX * IsoCell.CellSizeInSquares + this.centerSquareX), (float)(this.centerCellY * IsoCell.CellSizeInSquares + this.centerSquareY));
      this.armsAmount = var4.nextInt(var3.getArmsAmountMax() - var3.getArmsAmountMin() + 1) + var3.getArmsAmountMin();
      this.armsOrientation = new float[this.armsAmount];
      this.armsDistance = new float[this.armsAmount];
      this.endPoints = new Vector2[this.armsAmount];
      float var5 = var4.nextFloat() * 360.0F;
      float var6 = 360.0F / (float)this.armsAmount;

      for(int var7 = 0; var7 < this.armsAmount; ++var7) {
         float var8 = (var4.nextFloat() * 2.0F - 1.0F) * (float)var3.getArmsDeltaAngle();
         this.armsOrientation[var7] = var6 * (float)var7 + var5 + var8;
         this.armsDistance[var7] = var4.nextFloat() * (float)var3.getArmsDistMax() + (float)var3.getArmsDistMin();
         this.endPoints[var7] = new Vector2(this.startPoint.x + (float)(Math.sin(Math.toRadians((double)this.armsOrientation[var7])) * (double)this.armsDistance[var7]), this.startPoint.y + (float)(-Math.cos(Math.toRadians((double)this.armsOrientation[var7])) * (double)this.armsDistance[var7]));
      }

      this.tileGroups = var3.getTiles();
      this.armsProb = var3.getArmsProb();
      this.centerProb = var3.getCenterProb();
      this.centerRadius = var3.getCenterRadius();
   }

   public boolean isValid(int var1, int var2, Random var3) {
      Vector2 var4 = new Vector2(this.startPoint.x - (float)var1, this.startPoint.y - (float)var2);
      float var5 = var4.getLengthSquared();
      if (var5 < (float)(this.centerRadius * this.centerRadius) && var3.nextFloat() < this.centerProb) {
         return true;
      } else {
         for(int var6 = 0; var6 < this.armsAmount; ++var6) {
            if (!(var5 > this.armsDistance[var6] * this.armsDistance[var6])) {
               Vector2 var7 = new Vector2(this.endPoints[var6].x - this.startPoint.x, this.endPoints[var6].y - this.startPoint.y);
               Vector2 var8 = new Vector2(this.endPoints[var6].x - (float)var1, this.endPoints[var6].y - (float)var2);
               var7.normalize();
               var8.normalize();
               if (Math.abs(var7.x - var8.x) < 0.001F && Math.abs(var7.y - var8.y) < 0.001F && var3.nextFloat() < this.armsProb) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public List<TileGroup> getSingleFeatures() {
      return this.tileGroups;
   }

   public String toString() {
      Vector2 var10000 = this.startPoint;
      return "OreVein{startPoint=" + var10000 + ", amountArms=" + this.armsAmount + ", orientationArms=" + Arrays.toString(this.armsOrientation) + ", endPoints=" + Arrays.toString(this.endPoints) + ", distanceArms=" + Arrays.toString(this.armsDistance) + ", singleFeatures=" + this.tileGroups + "}";
   }
}
