package zombie.iso.worldgen.veins;

import java.util.List;
import zombie.iso.worldgen.biomes.TileGroup;

public class OreVeinConfig {
   private List<TileGroup> tiles;
   private int armsAmountMin;
   private int armsAmountMax;
   private int armsDistMin;
   private int armsDistMax;
   private int armsDeltaAngle;
   private float armsProb;
   private float centerProb;
   private float probability;
   private int centerRadius;

   public OreVeinConfig(List<TileGroup> var1, int var2, float var3, int var4, int var5, int var6, int var7, int var8, float var9, float var10) {
      this.tiles = var1;
      this.centerRadius = var2;
      this.centerProb = var3;
      this.armsAmountMin = var4;
      this.armsAmountMax = var5;
      this.armsDistMin = var6;
      this.armsDistMax = var7;
      this.armsDeltaAngle = var8;
      this.armsProb = var9;
      this.probability = var10;
   }

   public List<TileGroup> getTiles() {
      return this.tiles;
   }

   public float getCenterProb() {
      return this.centerProb;
   }

   public float getArmsProb() {
      return this.armsProb;
   }

   public int getCenterRadius() {
      return this.centerRadius;
   }

   public int getArmsAmountMin() {
      return this.armsAmountMin;
   }

   public int getArmsAmountMax() {
      return this.armsAmountMax;
   }

   public int getArmsDistMin() {
      return this.armsDistMin;
   }

   public int getArmsDistMax() {
      return this.armsDistMax;
   }

   public int getArmsDeltaAngle() {
      return this.armsDeltaAngle;
   }

   public float getProbability() {
      return this.probability;
   }
}
