package zombie.iso.worldgen.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Alias(String name, List<String> tiles) {
   public Alias(String name, List<String> tiles) {
      this.name = name;
      this.tiles = tiles;
   }

   public static Alias load(BufferedReader var0, String[] var1) throws IOException {
      String var2 = "";
      ArrayList var3 = new ArrayList();

      while(true) {
         String var4 = var0.readLine();
         if (var4 == null || var4.equals("}")) {
            return new Alias(var2, var3);
         }

         var4 = var4.strip();
         if (!var4.isEmpty() && !var4.equals("{")) {
            String[] var5 = var4.split("\\h+");
            switch (var5[0]) {
               case "name":
                  var2 = String.join(" ", (CharSequence[])Arrays.copyOfRange(var5, 2, var5.length));
                  break;
               case "tiles":
                  if (var5[2].equals("[")) {
                     while(true) {
                        String var8 = var0.readLine();
                        if (var8 == null || var8.strip().equals("]")) {
                           break;
                        }

                        var3.add(var8.strip());
                     }
                  } else {
                     var3.add(var5[2]);
                  }
            }
         }
      }
   }

   public String name() {
      return this.name;
   }

   public List<String> tiles() {
      return this.tiles;
   }
}
