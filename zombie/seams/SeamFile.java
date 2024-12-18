package zombie.seams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;

public final class SeamFile {
   final ArrayList<Tileset> tilesets = new ArrayList();
   public static final int VERSION1 = 1;
   public static final int VERSION_LATEST = 1;
   int m_version = 1;

   public SeamFile() {
   }

   void read(String var1) {
      File var2 = new File(var1);

      try {
         FileInputStream var3 = new FileInputStream(var2);

         try {
            InputStreamReader var4 = new InputStreamReader(var3);

            try {
               BufferedReader var5 = new BufferedReader(var4);

               try {
                  CharBuffer var6 = CharBuffer.allocate((int)var2.length());
                  var5.read(var6);
                  var6.flip();
                  this.parseFile(var6.toString());
               } catch (Throwable var11) {
                  try {
                     var5.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               var5.close();
            } catch (Throwable var12) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }

               throw var12;
            }

            var4.close();
         } catch (Throwable var13) {
            try {
               var3.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }

            throw var13;
         }

         var3.close();
      } catch (FileNotFoundException var14) {
      } catch (Exception var15) {
         ExceptionLogger.logException(var15);
      }

   }

   void parseFile(String var1) throws IOException {
      var1 = ScriptParser.stripComments(var1);
      ScriptParser.Block var2 = ScriptParser.parse(var1);
      var2 = (ScriptParser.Block)var2.children.get(0);
      ScriptParser.Value var3 = var2.getValue("VERSION");
      if (var3 == null) {
         throw new IOException("missing VERSION in seams.txt");
      } else {
         this.m_version = PZMath.tryParseInt(var3.getValue().trim(), -1);
         if (this.m_version >= 1 && this.m_version <= 1) {
            Iterator var4 = var2.children.iterator();

            while(var4.hasNext()) {
               ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
               if ("tileset".equals(var5.type)) {
                  Tileset var6 = this.parseTileset(var5);
                  if (var6 != null) {
                     this.tilesets.add(var6);
                  }
               }
            }

         } else {
            throw new IOException(String.format("unknown seams.txt VERSION \"%s\"", var3.getValue().trim()));
         }
      }
   }

   Tileset parseTileset(ScriptParser.Block var1) {
      Tileset var2 = new Tileset();
      ScriptParser.Value var3 = var1.getValue("name");
      var2.name = var3.getValue().trim();
      Iterator var4 = var1.children.iterator();

      while(true) {
         Tile var6;
         do {
            ScriptParser.Block var5;
            do {
               if (!var4.hasNext()) {
                  return var2;
               }

               var5 = (ScriptParser.Block)var4.next();
            } while(!"tile".equals(var5.type));

            var6 = this.parseTile(var2, var5);
         } while(var6 == null);

         int var7 = var2.tiles.size();

         for(int var8 = 0; var8 < var2.tiles.size(); ++var8) {
            Tile var9 = (Tile)var2.tiles.get(var8);
            if (var9.col + var9.row * 8 > var6.col + var6.row * 8) {
               var7 = var8;
               break;
            }
         }

         var2.tiles.add(var7, var6);
      }
   }

   Tile parseTile(Tileset var1, ScriptParser.Block var2) {
      Tile var3 = new Tile();
      ScriptParser.Value var4 = var2.getValue("xy");
      String[] var5 = var4.getValue().trim().split("x");
      var3.col = Integer.parseInt(var5[0]);
      var3.row = Integer.parseInt(var5[1]);
      var3.tileName = String.format("%s_%d", var1.name, var3.col + var3.row * 8);
      Iterator var6 = var2.children.iterator();

      while(var6.hasNext()) {
         ScriptParser.Block var7 = (ScriptParser.Block)var6.next();
         if ("east".equals(var7.type)) {
            var3.joinE = this.parseTileNameList(var7);
         } else if ("south".equals(var7.type)) {
            var3.joinS = this.parseTileNameList(var7);
         } else if ("belowEast".equals(var7.type)) {
            var3.joinBelowE = this.parseTileNameList(var7);
         } else if ("belowSouth".equals(var7.type)) {
            var3.joinBelowS = this.parseTileNameList(var7);
         } else if ("properties".equals(var7.type)) {
            var3.m_properties = this.parseTileProperties(var7);
         }
      }

      if (var3.isNull()) {
         return null;
      } else {
         return var3;
      }
   }

   ArrayList<String> parseTileNameList(ScriptParser.Block var1) {
      if (var1.values.isEmpty()) {
         return null;
      } else {
         ArrayList var2 = new ArrayList();
         Iterator var3 = var1.values.iterator();

         while(var3.hasNext()) {
            ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
            String var5 = var4.getKey().trim();
            if (!var5.isEmpty()) {
               var2.add(var5);
            }
         }

         return var2;
      }
   }

   HashMap<String, String> parseTileProperties(ScriptParser.Block var1) {
      if (var1.values.isEmpty()) {
         return null;
      } else {
         HashMap var2 = new HashMap();
         Iterator var3 = var1.values.iterator();

         while(var3.hasNext()) {
            ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
            String var5 = var4.getKey().trim();
            String var6 = var4.getValue().trim();
            if (!var5.isEmpty()) {
               var2.put(var5, var6);
            }
         }

         return var2;
      }
   }

   float parseCoord(String var1) {
      return PZMath.tryParseFloat(var1, 0.0F);
   }

   float parseCoord(ScriptParser.Block var1, String var2, float var3) {
      ScriptParser.Value var4 = var1.getValue(var2);
      if (var4 == null) {
         return var3;
      } else {
         String var5 = var4.getValue().trim();
         return this.parseCoord(var5);
      }
   }

   Vector2f parseVector2(String var1, Vector2f var2) {
      String[] var3 = var1.trim().split("x");
      var2.x = this.parseCoord(var3[0]);
      var2.y = this.parseCoord(var3[1]);
      return var2;
   }

   Vector3f parseVector3(String var1, Vector3f var2) {
      String[] var3 = var1.trim().split("x");
      var2.x = this.parseCoord(var3[0]);
      var2.y = this.parseCoord(var3[1]);
      var2.z = this.parseCoord(var3[2]);
      return var2;
   }

   boolean parseVector3(ScriptParser.Block var1, String var2, Vector3f var3) {
      ScriptParser.Value var4 = var1.getValue(var2);
      if (var4 == null) {
         return false;
      } else {
         String var5 = var4.getValue().trim();
         this.parseVector3(var5, var3);
         return true;
      }
   }

   void write(String var1) {
      ScriptParser.Block var2 = new ScriptParser.Block();
      var2.type = "seams";
      var2.setValue("VERSION", String.valueOf(1));
      StringBuilder var3 = new StringBuilder();
      ArrayList var4 = new ArrayList();
      Iterator var5 = this.tilesets.iterator();

      label43:
      while(var5.hasNext()) {
         Tileset var6 = (Tileset)var5.next();
         ScriptParser.Block var7 = new ScriptParser.Block();
         var7.type = "tileset";
         var7.setValue("name", var6.name);
         Iterator var8 = var6.tiles.iterator();

         while(true) {
            Tile var9;
            do {
               if (!var8.hasNext()) {
                  var2.elements.add(var7);
                  var2.children.add(var7);
                  continue label43;
               }

               var9 = (Tile)var8.next();
            } while(var9.isNull());

            ScriptParser.Block var10 = new ScriptParser.Block();
            var10.type = "tile";
            var10.setValue("xy", String.format("%dx%d", var9.col, var9.row));
            this.writeTileNames(var10, var9.joinE, "east");
            this.writeTileNames(var10, var9.joinS, "south");
            this.writeTileNames(var10, var9.joinBelowE, "belowEast");
            this.writeTileNames(var10, var9.joinBelowS, "belowSouth");
            if (var9.m_properties != null && !var9.m_properties.isEmpty()) {
               ScriptParser.Block var11 = new ScriptParser.Block();
               var11.type = "properties";
               var4.clear();
               var4.addAll(var9.m_properties.keySet());
               var4.sort(Comparator.naturalOrder());

               for(int var12 = 0; var12 < var4.size(); ++var12) {
                  String var13 = (String)var4.get(var12);
                  var11.setValue(var13, (String)var9.m_properties.get(var13));
               }

               var10.elements.add(var11);
               var10.children.add(var11);
            }

            var10.comment = String.format("/* %s_%d */", var6.name, var9.col + var9.row * 8);
            var7.elements.add(var10);
            var7.children.add(var10);
         }
      }

      var3.setLength(0);
      String var14 = System.lineSeparator();
      var2.prettyPrint(0, var3, var14);
      this.write(var1, var3.toString());
   }

   void writeTileNames(ScriptParser.Block var1, ArrayList<String> var2, String var3) {
      if (var2 != null && !var2.isEmpty()) {
         ScriptParser.Block var4 = new ScriptParser.Block();
         var4.type = var3;

         for(int var5 = 0; var5 < var2.size(); ++var5) {
            String var6 = (String)var2.get(var5);
            var4.setValue(var6, "");
         }

         var1.elements.add(var4);
         var1.children.add(var4);
      }
   }

   void write(String var1, String var2) {
      File var3 = new File(var1);

      try {
         FileWriter var4 = new FileWriter(var3);

         try {
            BufferedWriter var5 = new BufferedWriter(var4);

            try {
               var5.write(var2);
            } catch (Throwable var10) {
               try {
                  var5.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            var5.close();
         } catch (Throwable var11) {
            try {
               var4.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         var4.close();
      } catch (Throwable var12) {
         ExceptionLogger.logException(var12);
      }

   }

   void Reset() {
   }

   public static final class Tileset {
      String name;
      final ArrayList<Tile> tiles = new ArrayList();

      public Tileset() {
      }

      Tile getTile(int var1, int var2) {
         Iterator var3 = this.tiles.iterator();

         Tile var4;
         do {
            if (!var3.hasNext()) {
               return null;
            }

            var4 = (Tile)var3.next();
         } while(var4.col != var1 || var4.row != var2);

         return var4;
      }

      Tile getOrCreateTile(int var1, int var2) {
         Tile var3 = this.getTile(var1, var2);
         if (var3 != null) {
            return var3;
         } else {
            var3 = new Tile();
            var3.col = var1;
            var3.row = var2;
            int var4 = this.tiles.size();

            for(int var5 = 0; var5 < this.tiles.size(); ++var5) {
               Tile var6 = (Tile)this.tiles.get(var5);
               if (var6.col + var6.row * 8 > var1 + var2 * 8) {
                  var4 = var5;
                  break;
               }
            }

            this.tiles.add(var4, var3);
            return var3;
         }
      }
   }

   public static final class Tile {
      public String tileName;
      public int col;
      public int row;
      public ArrayList<String> joinE;
      public ArrayList<String> joinS;
      public ArrayList<String> joinBelowE;
      public ArrayList<String> joinBelowS;
      public HashMap<String, String> m_properties;

      public Tile() {
      }

      public boolean isMasterTile() {
         return this.joinE != null && !this.joinE.isEmpty() || this.joinS != null && !this.joinS.isEmpty() || this.joinBelowE != null && !this.joinBelowE.isEmpty() || this.joinBelowS != null && !this.joinBelowS.isEmpty();
      }

      public String getMasterTileName() {
         return this.m_properties == null ? null : (String)this.m_properties.get("master");
      }

      public boolean isNull() {
         if (this.joinE != null && !this.joinE.isEmpty()) {
            return false;
         } else if (this.joinS != null && !this.joinS.isEmpty()) {
            return false;
         } else if (this.joinBelowE != null && !this.joinBelowE.isEmpty()) {
            return false;
         } else if (this.joinBelowS != null && !this.joinBelowS.isEmpty()) {
            return false;
         } else {
            return this.m_properties == null || this.m_properties.isEmpty();
         }
      }
   }
}
