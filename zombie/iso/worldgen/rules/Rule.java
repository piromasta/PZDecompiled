package zombie.iso.worldgen.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Rule(String label, int bitmap, int[] color, List<String> tiles, String layer, int[] condition) {
   public Rule(String label, int bitmap, int[] color, List<String> tiles, String layer, int[] condition) {
      this.label = label;
      this.bitmap = bitmap;
      this.color = color;
      this.tiles = tiles;
      this.layer = layer;
      this.condition = condition;
   }

   public static Rule load(BufferedReader var0, String[] var1) throws IOException {
      String var2 = "";
      int var3 = 0;
      int[] var4 = new int[]{-1, -1, -1};
      ArrayList var5 = new ArrayList();
      String var6 = "";
      int[] var7 = new int[]{-1, -1, -1};

      label67:
      while(true) {
         String var8 = var0.readLine();
         if (var8 == null || var8.equals("}")) {
            return new Rule(var2, var3, var4, var5, var6, var7);
         }

         var8 = var8.strip();
         if (!var8.isEmpty() && !var8.equals("{")) {
            String[] var9 = var8.split("\\h+");
            switch (var9[0]) {
               case "label":
                  var2 = String.join(" ", (CharSequence[])Arrays.copyOfRange(var9, 2, var9.length));
                  break;
               case "bitmap":
                  var3 = Integer.parseInt(var9[2]);
                  break;
               case "color":
                  var4 = new int[]{Integer.parseInt(var9[2]), Integer.parseInt(var9[3]), Integer.parseInt(var9[4])};
                  break;
               case "tiles":
                  if (var9[2].equals("[")) {
                     while(true) {
                        String var12 = var0.readLine();
                        if (var12 == null || var12.strip().equals("]")) {
                           continue label67;
                        }

                        var5.add(var12.strip());
                     }
                  }

                  var5.add(var9[2]);
                  break;
               case "layer":
                  var6 = var9[2];
                  break;
               case "condition":
                  var7 = new int[]{Integer.parseInt(var9[2]), Integer.parseInt(var9[3]), Integer.parseInt(var9[4])};
            }
         }
      }
   }

   public String label() {
      return this.label;
   }

   public int bitmap() {
      return this.bitmap;
   }

   public int[] color() {
      return this.color;
   }

   public List<String> tiles() {
      return this.tiles;
   }

   public String layer() {
      return this.layer;
   }

   public int[] condition() {
      return this.condition;
   }
}
