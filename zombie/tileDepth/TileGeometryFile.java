package zombie.tileDepth;

import gnu.trove.list.array.TFloatArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.BiConsumer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.worldMap.Rasterize;

public final class TileGeometryFile {
   private static Clipper s_clipper;
   final ArrayList<Tileset> tilesets = new ArrayList();
   private static final int COORD_MULT = 10000;
   public static final int VERSION1 = 1;
   public static final int VERSION2 = 2;
   public static final int VERSION_LATEST = 2;
   int m_version = 2;

   public TileGeometryFile() {
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
         throw new IOException("missing VERSION in tileGeometry.txt");
      } else {
         this.m_version = PZMath.tryParseInt(var3.getValue().trim(), -1);
         if (this.m_version >= 1 && this.m_version <= 2) {
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
            throw new IOException(String.format("unknown tileGeometry.txt VERSION \"%s\"", var3.getValue().trim()));
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
      Iterator var6 = var2.children.iterator();

      while(var6.hasNext()) {
         ScriptParser.Block var7 = (ScriptParser.Block)var6.next();
         if ("box".equals(var7.type)) {
            Box var8 = this.parseBox(var7);
            if (var8 != null) {
               var3.m_geometry.add(var8);
            }
         }

         if ("cylinder".equals(var7.type)) {
            Cylinder var9 = this.parseCylinder(var7);
            if (var9 != null) {
               var3.m_geometry.add(var9);
            }
         }

         if ("polygon".equals(var7.type)) {
            Polygon var10 = this.parsePolygon(var7);
            if (var10 != null) {
               var3.m_geometry.add(var10);
            }
         }

         if ("properties".equals(var7.type)) {
            var3.m_properties = this.parseTileProperties(var7);
         }
      }

      return var3;
   }

   Box parseBox(ScriptParser.Block var1) {
      Box var2 = new Box();
      if (!this.parseVector3(var1, "translate", var2.translate)) {
         return null;
      } else if (!this.parseVector3(var1, "rotate", var2.rotate)) {
         return null;
      } else if (!this.parseVector3(var1, "min", var2.min)) {
         return null;
      } else {
         return !this.parseVector3(var1, "max", var2.max) ? null : var2;
      }
   }

   Cylinder parseCylinder(ScriptParser.Block var1) {
      Cylinder var2 = new Cylinder();
      if (!this.parseVector3(var1, "translate", var2.translate)) {
         return null;
      } else if (!this.parseVector3(var1, "rotate", var2.rotate)) {
         return null;
      } else {
         var2.radius1 = this.parseCoord(var1, "radius1", -1.0F);
         if (var2.radius1 <= 0.0F) {
            return null;
         } else {
            var2.radius2 = this.parseCoord(var1, "radius2", -1.0F);
            if (var2.radius2 <= 0.0F) {
               return null;
            } else {
               var2.height = this.parseCoord(var1, "height", -1.0F);
               return var2.height < 0.0F ? null : var2;
            }
         }
      }
   }

   Polygon parsePolygon(ScriptParser.Block var1) {
      Polygon var2 = new Polygon();
      if (!this.parseVector3(var1, "translate", var2.translate)) {
         return null;
      } else {
         ScriptParser.Value var3 = var1.getValue("plane");
         var2.plane = TileGeometryFile.Plane.valueOf(var3.getValue().trim());
         if (var1.getValue("rotate") == null) {
            switch (var2.plane) {
               case XY:
                  var2.rotate.set(0.0F, 0.0F, 0.0F);
                  break;
               case XZ:
                  var2.rotate.set(90.0F, 0.0F, 0.0F);
                  break;
               case YZ:
                  var2.rotate.set(0.0F, 270.0F, 0.0F);
            }
         } else if (!this.parseVector3(var1, "rotate", var2.rotate)) {
         }

         var3 = var1.getValue("points");
         String[] var4 = var3.getValue().trim().split(" ");
         String[] var5 = var4;
         int var6 = var4.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            String var8 = var5[var7];
            String[] var9 = var8.split("x");
            float var10 = this.parseCoord(var9[0]);
            float var11 = this.parseCoord(var9[1]);
            var2.m_points.add(var10);
            var2.m_points.add(var11);
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
      return this.m_version == 1 ? PZMath.tryParseFloat(var1, 0.0F) : (float)PZMath.tryParseInt(var1, 0) / 10000.0F;
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

   int coordInt(float var1) {
      return (int)(var1 * 10000.0F);
   }

   void write(String var1) {
      ScriptParser.Block var2 = new ScriptParser.Block();
      var2.type = "tileGeometry";
      var2.setValue("VERSION", String.valueOf(2));
      StringBuilder var3 = new StringBuilder();
      ArrayList var4 = new ArrayList();
      Iterator var5 = this.tilesets.iterator();

      label61:
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
                  continue label61;
               }

               var9 = (Tile)var8.next();
            } while(var9.m_geometry.isEmpty() && (var9.m_properties == null || var9.m_properties.isEmpty()));

            ScriptParser.Block var10 = new ScriptParser.Block();
            var10.type = "tile";
            var10.setValue("xy", String.format("%dx%d", var9.col, var9.row));
            Iterator var11 = var9.m_geometry.iterator();

            while(var11.hasNext()) {
               Geometry var12 = (Geometry)var11.next();
               ScriptParser.Block var13 = var12.toBlock(var3);
               if (var13 != null) {
                  var10.elements.add(var13);
                  var10.children.add(var13);
               }
            }

            if (var9.m_properties != null && !var9.m_properties.isEmpty()) {
               ScriptParser.Block var15 = new ScriptParser.Block();
               var15.type = "properties";
               var4.clear();
               var4.addAll(var9.m_properties.keySet());
               var4.sort(Comparator.naturalOrder());

               for(int var16 = 0; var16 < var4.size(); ++var16) {
                  String var17 = (String)var4.get(var16);
                  var15.setValue(var17, (String)var9.m_properties.get(var17));
               }

               var10.elements.add(var15);
               var10.children.add(var15);
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

      void initSpriteProperties() {
         for(int var1 = 0; var1 < this.tiles.size(); ++var1) {
            Tile var2 = (Tile)this.tiles.get(var1);
            if (var2.m_properties != null && !var2.m_properties.isEmpty()) {
               int var3 = var2.col + var2.row * 8;
               IsoSprite var4 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(this.name + "_" + var3);
               if (var4 != null) {
                  String var5 = (String)var2.m_properties.get("ItemHeight");
                  if (var5 != null) {
                     var4.getProperties().Set("ItemHeight", var5, false);
                  }

                  var5 = (String)var2.m_properties.get("Surface");
                  if (var5 != null) {
                     var4.getProperties().Set("Surface", var5, false);
                  }

                  var5 = (String)var2.m_properties.get("OpaquePixelsOnly");
                  if (StringUtils.tryParseBoolean(var5)) {
                     var4.depthFlags |= 4;
                  }

                  var5 = (String)var2.m_properties.get("Translucent");
                  if (StringUtils.tryParseBoolean(var5)) {
                     var4.depthFlags |= 2;
                  }

                  var5 = (String)var2.m_properties.get("UseObjectDepthTexture");
                  if (StringUtils.tryParseBoolean(var5)) {
                     var4.depthFlags |= 1;
                  }
               }
            }
         }

      }
   }

   static final class Tile {
      int col;
      int row;
      final ArrayList<Geometry> m_geometry = new ArrayList();
      HashMap<String, String> m_properties;

      Tile() {
      }

      void setGeometry(ArrayList<Geometry> var1) {
         this.m_geometry.clear();
         this.m_geometry.addAll(var1);
      }
   }

   public static final class Box extends Geometry {
      public final Vector3f translate = new Vector3f();
      public final Vector3f rotate = new Vector3f();
      public final Vector3f min = new Vector3f();
      public final Vector3f max = new Vector3f();

      public Box() {
      }

      public Object clone() {
         Box var1 = new Box();
         var1.translate.set(this.translate);
         var1.rotate.set(this.rotate);
         var1.min.set(this.min);
         var1.max.set(this.max);
         return var1;
      }

      public boolean isBox() {
         return true;
      }

      public ScriptParser.Block toBlock(StringBuilder var1) {
         ScriptParser.Block var2 = new ScriptParser.Block();
         var2.type = "box";
         var2.setValue("translate", this.formatVector3(this.translate));
         var2.setValue("rotate", this.formatVector3(this.rotate));
         var2.setValue("min", this.formatVector3(this.min));
         var2.setValue("max", this.formatVector3(this.max));
         return var2;
      }

      public void offset(int var1, int var2) {
         this.translate.add((float)var1, 0.0F, (float)var2);
      }

      float getNormalizedDepthAt(float var1, float var2) {
         return TileGeometryUtils.getNormalizedDepthOnBoxAt(var1, var2, this.translate, this.rotate, this.min, this.max);
      }
   }

   public static final class Cylinder extends Geometry {
      public final Vector3f translate = new Vector3f();
      public final Vector3f rotate = new Vector3f();
      public float radius1;
      public float radius2;
      public float height;

      public Cylinder() {
      }

      public Object clone() {
         Cylinder var1 = new Cylinder();
         var1.translate.set(this.translate);
         var1.rotate.set(this.rotate);
         var1.radius1 = this.radius1;
         var1.radius2 = this.radius2;
         var1.height = this.height;
         return var1;
      }

      public boolean isCylinder() {
         return true;
      }

      public ScriptParser.Block toBlock(StringBuilder var1) {
         ScriptParser.Block var2 = new ScriptParser.Block();
         var2.type = "cylinder";
         var2.setValue("translate", this.formatVector3(this.translate));
         var2.setValue("rotate", this.formatVector3(this.rotate));
         var2.setValue("radius1", this.formatFloat(this.radius1));
         var2.setValue("radius2", this.formatFloat(this.radius2));
         var2.setValue("height", this.formatFloat(this.height));
         return var2;
      }

      public void offset(int var1, int var2) {
         this.translate.add((float)var1, 0.0F, (float)var2);
      }

      float getNormalizedDepthAt(float var1, float var2) {
         return TileGeometryUtils.getNormalizedDepthOnCylinderAt(var1, var2, this.translate, this.rotate, this.radius1, this.height);
      }
   }

   public static final class Polygon extends Geometry {
      public Plane plane;
      public final Vector3f translate = new Vector3f();
      public final Vector3f rotate = new Vector3f();
      public final TFloatArrayList m_points = new TFloatArrayList();
      public final TFloatArrayList m_triangles = new TFloatArrayList();

      public Polygon() {
      }

      public Object clone() {
         Polygon var1 = new Polygon();
         var1.plane = this.plane;
         var1.translate.set(this.translate);
         var1.rotate.set(this.rotate);
         var1.m_points.addAll(this.m_points);
         var1.m_triangles.addAll(this.m_triangles);
         return var1;
      }

      public ScriptParser.Block toBlock(StringBuilder var1) {
         ScriptParser.Block var2 = new ScriptParser.Block();
         var2.type = "polygon";
         var2.setValue("translate", this.formatVector3(this.translate));
         var2.setValue("rotate", this.formatVector3(this.rotate));
         var2.setValue("plane", this.plane.name());
         var1.setLength(0);

         for(int var3 = 0; var3 < this.m_points.size(); var3 += 2) {
            float var4 = this.m_points.get(var3);
            float var5 = this.m_points.get(var3 + 1);
            var1.append(String.format(Locale.US, "%dx%d ", this.coordInt(var4), this.coordInt(var5)));
         }

         var2.setValue("points", var1.toString());
         return var2;
      }

      public void offset(int var1, int var2) {
         this.translate.add((float)var1, 0.0F, (float)var2);
      }

      public boolean isPolygon() {
         return true;
      }

      boolean isClockwise() {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.m_points.size(); var2 += 2) {
            float var3 = this.m_points.get(var2);
            float var4 = this.m_points.get(var2 + 1);
            float var5 = this.m_points.get((var2 + 2) % this.m_points.size());
            float var6 = this.m_points.get((var2 + 3) % this.m_points.size());
            var1 += (var5 - var3) * (var6 + var4);
         }

         return (double)var1 > 0.0;
      }

      void triangulate() {
         this.m_triangles.clear();
         if (TileGeometryFile.s_clipper == null) {
            TileGeometryFile.s_clipper = new Clipper();
         }

         TileGeometryFile.s_clipper.clear();
         ByteBuffer var1 = ByteBuffer.allocateDirect(8 * (this.m_points.size() / 2) * 3);
         int var2;
         if (this.isClockwise()) {
            for(var2 = this.m_points.size() - 2; var2 >= 0; var2 -= 2) {
               var1.putFloat(this.m_points.getQuick(var2));
               var1.putFloat(this.m_points.getQuick(var2 + 1));
            }
         } else {
            for(var2 = 0; var2 < this.m_points.size(); var2 += 2) {
               var1.putFloat(this.m_points.getQuick(var2));
               var1.putFloat(this.m_points.getQuick(var2 + 1));
            }
         }

         TileGeometryFile.s_clipper.addPath(this.m_points.size() / 2, var1, false);
         var2 = TileGeometryFile.s_clipper.generatePolygons();
         if (var2 >= 1) {
            var1.clear();
            int var3 = TileGeometryFile.s_clipper.triangulate(0, var1);
            Matrix4f var4 = new Matrix4f();
            var4.translation(this.translate);
            var4.rotateXYZ(this.rotate.x * 0.017453292F, this.rotate.y * 0.017453292F, this.rotate.z * 0.017453292F);
            Vector3f var5 = new Vector3f();

            for(int var6 = 0; var6 < var3; ++var6) {
               float var7 = var1.getFloat();
               float var8 = var1.getFloat();
               float var9 = 0.0F;
               var5.set(var7, var8, var9);
               var4.transformPosition(var5);
               this.m_triangles.add(var5.x);
               this.m_triangles.add(var5.y);
               this.m_triangles.add(var5.z);
            }

         }
      }

      void triangulate2() {
         this.m_triangles.clear();
         if (TileGeometryFile.s_clipper == null) {
            TileGeometryFile.s_clipper = new Clipper();
         }

         TileGeometryFile.s_clipper.clear();
         ByteBuffer var1 = ByteBuffer.allocateDirect(8 * (this.m_points.size() / 2) * 3);
         int var2;
         if (this.isClockwise()) {
            for(var2 = this.m_points.size() - 2; var2 >= 0; var2 -= 2) {
               var1.putFloat(this.m_points.getQuick(var2));
               var1.putFloat(this.m_points.getQuick(var2 + 1));
            }
         } else {
            for(var2 = 0; var2 < this.m_points.size(); var2 += 2) {
               var1.putFloat(this.m_points.getQuick(var2));
               var1.putFloat(this.m_points.getQuick(var2 + 1));
            }
         }

         TileGeometryFile.s_clipper.addPath(this.m_points.size() / 2, var1, false);
         var2 = TileGeometryFile.s_clipper.generatePolygons();
         if (var2 >= 1) {
            var1.clear();
            int var3 = TileGeometryFile.s_clipper.triangulate(0, var1);
            new Vector3f();

            for(int var5 = 0; var5 < var3; ++var5) {
               float var6 = var1.getFloat();
               float var7 = var1.getFloat();
               this.m_triangles.add(var6);
               this.m_triangles.add(var7);
            }

         }
      }

      float getNormalizedDepthAt(float var1, float var2) {
         Vector3f var3 = BaseVehicle.allocVector3f().set(0.0F, 0.0F, 1.0F);
         Matrix4f var4 = BaseVehicle.allocMatrix4f().rotationXYZ(this.rotate.x * 0.017453292F, this.rotate.y * 0.017453292F, this.rotate.z * 0.017453292F);
         var4.transformDirection(var3);
         BaseVehicle.releaseMatrix4f(var4);
         float var5 = TileGeometryUtils.getNormalizedDepthOnPlaneAt(var1, var2, this.translate, var3);
         BaseVehicle.releaseVector3f(var3);
         return var5;
      }

      public void rasterize(BiConsumer<Integer, Integer> var1) {
         this.triangulate2();
         this.calcMatrices(TileGeometryFile.Polygon.L_rasterizeDepth.m_projection, TileGeometryFile.Polygon.L_rasterizeDepth.m_modelView);
         Vector2f var2 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_1;
         Vector2f var3 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_2;
         Vector2f var4 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_3;
         Vector2f var5 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_4;
         Vector2f var6 = TileGeometryFile.Polygon.L_rasterizeDepth.vector2f_5;
         this.calculateTextureTopLeft(0.0F, 0.0F, 0.0F, var6);
         float var7 = this.calculatePixelSize();

         for(int var8 = 0; var8 < this.m_triangles.size(); var8 += 6) {
            float var9 = this.m_triangles.get(var8);
            float var10 = this.m_triangles.get(var8 + 1);
            float var11 = this.m_triangles.get(var8 + 2);
            float var12 = this.m_triangles.get(var8 + 3);
            float var13 = this.m_triangles.get(var8 + 4);
            float var14 = this.m_triangles.get(var8 + 5);
            this.planeToUI(var5.set(var9, var10), var2);
            this.planeToUI(var5.set(var11, var12), var3);
            this.planeToUI(var5.set(var13, var14), var4);
            this.uiToTile(var6, var7, var2, var2);
            this.uiToTile(var6, var7, var3, var3);
            this.uiToTile(var6, var7, var4, var4);
            TileGeometryFile.Polygon.L_rasterizeDepth.rasterize.scanTriangle(var2.x, var2.y, var3.x, var3.y, var4.x, var4.y, -1000, 1000, var1);
         }

         this.m_triangles.clear();
      }

      float zoomMult() {
         byte var1 = 3;
         return (float)Math.exp((double)((float)var1 * 0.2F)) * 160.0F / Math.max(1.82F, 1.0F);
      }

      private void calcMatrices(Matrix4f var1, Matrix4f var2) {
         float var3 = (float)this.screenWidth();
         float var4 = 1366.0F / var3;
         float var5 = (float)this.screenHeight() * var4;
         var3 = 1366.0F;
         var3 /= this.zoomMult();
         var5 /= this.zoomMult();
         var1.setOrtho(-var3 / 2.0F, var3 / 2.0F, -var5 / 2.0F, var5 / 2.0F, -10.0F, 10.0F);
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = var6 / this.zoomMult() * var4;
         float var9 = var7 / this.zoomMult() * var4;
         var1.translate(-var8, var9, 0.0F);
         var2.identity();
         float var10 = 30.0F;
         float var11 = 315.0F;
         float var12 = 0.0F;
         var2.rotateXYZ(var10 * 0.017453292F, var11 * 0.017453292F, var12 * 0.017453292F);
      }

      float calculatePixelSize() {
         float var1 = this.sceneToUIX(0.0F, 0.0F, 0.0F);
         float var2 = this.sceneToUIY(0.0F, 0.0F, 0.0F);
         float var3 = this.sceneToUIX(1.0F, 0.0F, 0.0F);
         float var4 = this.sceneToUIY(1.0F, 0.0F, 0.0F);
         return (float)(Math.sqrt((double)((var3 - var1) * (var3 - var1) + (var4 - var2) * (var4 - var2))) / Math.sqrt(5120.0));
      }

      Vector2f calculateTextureTopLeft(float var1, float var2, float var3, Vector2f var4) {
         float var5 = this.sceneToUIX(var1, var2, var3);
         float var6 = this.sceneToUIY(var1, var2, var3);
         float var7 = this.calculatePixelSize();
         float var8 = var5 - 64.0F * var7;
         float var9 = var6 - 224.0F * var7;
         return var4.set(var8, var9);
      }

      Vector3f planeTo3D(Vector2f var1, Vector3f var2) {
         Matrix4f var3 = BaseVehicle.allocMatrix4f();
         var3.translation(this.translate);
         var3.rotateXYZ(this.rotate.x * 0.017453292F, this.rotate.y * 0.017453292F, this.rotate.z * 0.017453292F);
         var3.transformPosition(var1.x, var1.y, 0.0F, var2);
         BaseVehicle.releaseMatrix4f(var3);
         return var2;
      }

      Vector2f planeToUI(Vector2f var1, Vector2f var2) {
         Vector3f var3 = this.planeTo3D(var1, BaseVehicle.allocVector3f());
         var2.set(this.sceneToUIX(var3), this.sceneToUIY(var3));
         BaseVehicle.releaseVector3f(var3);
         return var2;
      }

      public float sceneToUIX(Vector3f var1) {
         return this.sceneToUIX(var1.x, var1.y, var1.z);
      }

      public float sceneToUIY(Vector3f var1) {
         return this.sceneToUIY(var1.x, var1.y, var1.z);
      }

      public float sceneToUIX(float var1, float var2, float var3) {
         Matrix4f var4 = TileGeometryFile.Polygon.L_rasterizeDepth.matrix4f_1;
         var4.set(TileGeometryFile.Polygon.L_rasterizeDepth.m_projection);
         var4.mul(TileGeometryFile.Polygon.L_rasterizeDepth.m_modelView);
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[0] = 0;
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[1] = 0;
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[2] = this.screenWidth();
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[3] = this.screenHeight();
         var4.project(var1, var2, var3, TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport, TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1);
         return TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1.x();
      }

      public float sceneToUIY(float var1, float var2, float var3) {
         Matrix4f var4 = TileGeometryFile.Polygon.L_rasterizeDepth.matrix4f_1;
         var4.set(TileGeometryFile.Polygon.L_rasterizeDepth.m_projection);
         var4.mul(TileGeometryFile.Polygon.L_rasterizeDepth.m_modelView);
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[0] = 0;
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[1] = 0;
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[2] = this.screenWidth();
         TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport[3] = this.screenHeight();
         var4.project(var1, var2, var3, TileGeometryFile.Polygon.L_rasterizeDepth.m_viewport, TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1);
         return (float)this.screenHeight() - TileGeometryFile.Polygon.L_rasterizeDepth.vector3f_1.y();
      }

      Vector2f uiToTile(Vector2f var1, float var2, Vector2f var3, Vector2f var4) {
         float var5 = (var3.x - var1.x) / var2;
         float var6 = (var3.y - var1.y) / var2;
         return var4.set(var5, var6);
      }

      int screenWidth() {
         return 1366;
      }

      int screenHeight() {
         return 768;
      }

      static final class L_rasterizeDepth {
         static final Rasterize rasterize = new Rasterize();
         static final Vector2f vector2f_1 = new Vector2f();
         static final Vector2f vector2f_2 = new Vector2f();
         static final Vector2f vector2f_3 = new Vector2f();
         static final Vector2f vector2f_4 = new Vector2f();
         static final Vector2f vector2f_5 = new Vector2f();
         static final Matrix4f m_projection = new Matrix4f();
         static final Matrix4f m_modelView = new Matrix4f();
         static final Matrix4f matrix4f_1 = new Matrix4f();
         static final int[] m_viewport = new int[4];
         static final Vector3f vector3f_1 = new Vector3f();

         L_rasterizeDepth() {
         }
      }
   }

   public static enum Plane {
      XY,
      XZ,
      YZ;

      private Plane() {
      }
   }

   public static class Geometry {
      public Geometry() {
      }

      public Object clone() {
         return null;
      }

      public boolean isBox() {
         return false;
      }

      public Box asBox() {
         return (Box)Type.tryCastTo(this, Box.class);
      }

      public boolean isCylinder() {
         return false;
      }

      public Cylinder asCylinder() {
         return (Cylinder)Type.tryCastTo(this, Cylinder.class);
      }

      public boolean isPolygon() {
         return false;
      }

      public Polygon asPolygon() {
         return (Polygon)Type.tryCastTo(this, Polygon.class);
      }

      public ScriptParser.Block toBlock(StringBuilder var1) {
         return null;
      }

      public void offset(int var1, int var2) {
      }

      int coordInt(float var1) {
         return (int)(var1 * 10000.0F);
      }

      String formatFloat(float var1) {
         return String.format(Locale.US, "%d", this.coordInt(var1));
      }

      String formatVector3(Vector3f var1) {
         return this.formatVector3(var1.x, var1.y, var1.z);
      }

      String formatVector3(float var1, float var2, float var3) {
         return String.format(Locale.US, "%dx%dx%d", this.coordInt(var1), this.coordInt(var2), this.coordInt(var3));
      }

      float getNormalizedDepthAt(float var1, float var2) {
         return -1.0F;
      }
   }
}
