package zombie.seating;

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
import java.util.Locale;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class SeatingFile {
   final ArrayList<Tileset> tilesets = new ArrayList();
   public static final int VERSION1 = 1;
   public static final int VERSION2 = 2;
   public static final int VERSION3 = 3;
   public static final int VERSION_LATEST = 3;
   int m_version = 3;
   private static final int COORD_MULT = 10000;

   public SeatingFile() {
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
         throw new IOException("missing VERSION in seating.txt");
      } else {
         this.m_version = PZMath.tryParseInt(var3.getValue().trim(), -1);
         if (this.m_version >= 1 && this.m_version <= 3) {
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
            throw new IOException(String.format("unknown seating.txt VERSION \"%s\"", var3.getValue().trim()));
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
      String[] var5 = var4.getValue().trim().split(this.m_version < 3 ? "x" : "\\s+");
      var3.col = Integer.parseInt(var5[0]);
      var3.row = Integer.parseInt(var5[1]);
      if (this.m_version == 1) {
         --var3.col;
         --var3.row;
      }

      Iterator var6 = var2.children.iterator();

      while(var6.hasNext()) {
         ScriptParser.Block var7 = (ScriptParser.Block)var6.next();
         if ("position".equals(var7.type)) {
            Position var8 = this.parsePosition(var7);
            if (var8 != null) {
               var3.positions.add(var8);
            }
         } else if ("properties".equals(var7.type)) {
            this.parseProperties(var7, var3.properties);
            if (this.m_version < 3) {
               this.propertiesToPositions(var3);
            }
         }
      }

      return var3;
   }

   Position parsePosition(ScriptParser.Block var1) {
      Position var2 = new Position();
      ScriptParser.Value var3 = var1.getValue("id");
      if (var3 != null && !StringUtils.isNullOrWhitespace(var3.getValue())) {
         var2.id = var3.getValue().trim();
         this.parseVector3(var1, "translate", var2.translate);
         Iterator var4 = var1.children.iterator();

         while(var4.hasNext()) {
            ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
            if ("properties".equals(var5.type)) {
               this.parseProperties(var5, var2.properties);
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   void parseProperties(ScriptParser.Block var1, HashMap<String, String> var2) {
      if (!var1.values.isEmpty()) {
         Iterator var3 = var1.values.iterator();

         while(var3.hasNext()) {
            ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
            String var5 = var4.getKey().trim();
            String var6 = var4.getValue().trim();
            if (!var5.isEmpty()) {
               var2.put(var5, var6);
            }
         }

      }
   }

   float parseCoord(String var1) {
      return this.m_version < 3 ? PZMath.tryParseFloat(var1, 0.0F) : (float)PZMath.tryParseInt(var1, 0) / 10000.0F;
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
      String[] var3 = var1.trim().split(this.m_version < 3 ? "x" : "\\s+");
      var2.x = this.parseCoord(var3[0]);
      var2.y = this.parseCoord(var3[1]);
      return var2;
   }

   Vector3f parseVector3(String var1, Vector3f var2) {
      String[] var3 = var1.trim().split(this.m_version < 3 ? "x" : "\\s+");
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

   void propertiesToPositions(Tile var1) {
      if (var1.properties != null) {
         String var2 = (String)var1.properties.remove("translateX");
         String var3 = (String)var1.properties.remove("translateY");
         String var4 = (String)var1.properties.remove("translateZ");
         if (var2 != null && var3 != null && var4 != null) {
            float var5 = this.parseCoord(var2);
            float var6 = this.parseCoord(var3);
            float var7 = this.parseCoord(var4);
            Position var8 = new Position();
            var8.id = "default";
            var8.translate.set(var5, var6, var7);
            var1.positions.add(var8);
         }

      }
   }

   void write(String var1) {
      ScriptParser.Block var2 = new ScriptParser.Block();
      var2.type = "seating";
      var2.setValue("VERSION", String.valueOf(3));
      StringBuilder var3 = new StringBuilder();
      ArrayList var4 = new ArrayList();
      Iterator var5 = this.tilesets.iterator();

      label44:
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
                  continue label44;
               }

               var9 = (Tile)var8.next();
            } while(var9.isEmpty());

            ScriptParser.Block var10 = new ScriptParser.Block();
            var10.type = "tile";
            var10.setValue("xy", String.format("%d %d", var9.col, var9.row));
            Iterator var11 = var9.positions.iterator();

            while(var11.hasNext()) {
               Position var12 = (Position)var11.next();
               ScriptParser.Block var13 = new ScriptParser.Block();
               var13.type = "position";
               var13.setValue("id", var12.id);
               var13.setValue("translate", this.formatVector3(var12.translate));
               if (!var12.properties.isEmpty()) {
                  ScriptParser.Block var14 = this.propertiesToBlock(var12.properties, var4);
                  var13.elements.add(var14);
                  var13.children.add(var14);
               }

               var10.elements.add(var13);
               var10.children.add(var13);
            }

            if (var9.properties != null && !var9.properties.isEmpty()) {
               ScriptParser.Block var16 = this.propertiesToBlock(var9.properties, var4);
               var10.elements.add(var16);
               var10.children.add(var16);
            }

            var10.comment = String.format("/* %s_%d */", var6.name, var9.col + var9.row * 8);
            var7.elements.add(var10);
            var7.children.add(var10);
         }
      }

      var3.setLength(0);
      String var15 = System.lineSeparator();
      var2.prettyPrint(0, var3, var15);
      this.write(var1, var3.toString());
   }

   ScriptParser.Block propertiesToBlock(HashMap<String, String> var1, ArrayList<String> var2) {
      ScriptParser.Block var3 = new ScriptParser.Block();
      var3.type = "properties";
      var2.clear();
      var2.addAll(var1.keySet());
      var2.sort(Comparator.naturalOrder());

      for(int var4 = 0; var4 < var2.size(); ++var4) {
         String var5 = (String)var2.get(var4);
         var3.setValue(var5, (String)var1.get(var5));
      }

      return var3;
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

   int coordInt(float var1) {
      return (int)(var1 * 10000.0F);
   }

   String formatFloat(float var1) {
      return String.format(Locale.US, "%d", this.coordInt(var1));
   }

   String formatVector3(float var1, float var2, float var3) {
      return String.format(Locale.US, "%d %d %d", this.coordInt(var1), this.coordInt(var2), this.coordInt(var3));
   }

   String formatVector3(Vector3f var1) {
      return this.formatVector3(var1.x, var1.y, var1.z);
   }

   void Reset() {
      this.tilesets.clear();
   }

   static final class Tileset {
      String name;
      final ArrayList<Tile> tiles = new ArrayList();

      Tileset() {
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

      void merge(Tileset var1) {
         Iterator var2 = var1.tiles.iterator();

         while(true) {
            Tile var3;
            Tile var4;
            do {
               if (!var2.hasNext()) {
                  return;
               }

               var3 = (Tile)var2.next();
               var4 = this.getTile(var3.col, var3.row);
            } while(var4 != null && !var4.isEmpty());

            var4 = this.getOrCreateTile(var3.col, var3.row);
            var4.set(var3);
         }
      }
   }

   static final class Tile {
      int col;
      int row;
      final ArrayList<Position> positions = new ArrayList();
      final HashMap<String, String> properties = new HashMap();

      Tile() {
      }

      boolean isEmpty() {
         return this.properties.isEmpty() && this.positions.isEmpty();
      }

      Position getPositionByIndex(int var1) {
         return var1 >= 0 && var1 < this.positions.size() ? (Position)this.positions.get(var1) : null;
      }

      Tile set(Tile var1) {
         this.positions.clear();
         Iterator var2 = var1.positions.iterator();

         while(var2.hasNext()) {
            Position var3 = (Position)var2.next();
            this.positions.add((new Position()).set(var3));
         }

         this.properties.clear();
         this.properties.putAll(var1.properties);
         return this;
      }
   }

   static final class Position {
      String id;
      final Vector3f translate = new Vector3f();
      final HashMap<String, String> properties = new HashMap();

      Position() {
      }

      Position set(Position var1) {
         this.id = var1.id;
         this.translate.set(var1.translate);
         this.properties.clear();
         this.properties.putAll(var1.properties);
         return this;
      }

      String getProperty(String var1) {
         return (String)this.properties.get(var1);
      }

      void setProperty(String var1, String var2) {
         if (var2 == null) {
            this.properties.remove(var1);
         } else {
            this.properties.put(var1, var2);
         }

      }
   }
}
