package zombie.iso;

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
import java.util.Iterator;
import java.util.Locale;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class SpriteModelsFile {
   final ArrayList<Tileset> tilesets = new ArrayList();
   public static final int VERSION1 = 1;
   public static final int VERSION_LATEST = 1;
   int m_version = 1;

   public SpriteModelsFile() {
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
         throw new IOException("missing VERSION in spriteModels.txt");
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
            throw new IOException(String.format("unknown spriteModels.txt VERSION \"%s\"", var3.getValue().trim()));
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
            if (var9.getIndex() > var6.getIndex()) {
               var7 = var8;
               break;
            }
         }

         var2.tiles.add(var7, var6);
      }
   }

   Tile parseTile(Tileset var1, ScriptParser.Block var2) {
      ScriptParser.Value var3 = var2.getValue("runtime");
      if (var3 != null) {
         return this.parseRuntimeTile(var1, var2);
      } else {
         Tile var4 = new Tile();
         ScriptParser.Value var5 = var2.getValue("xy");
         String[] var6 = var5.getValue().trim().split(" ");
         var4.col = Integer.parseInt(var6[0]);
         var4.row = Integer.parseInt(var6[1]);
         var4.spriteModel.modelScriptName = var2.getValue("modelScript").getValue().trim();
         if ((var5 = var2.getValue("texture")) != null) {
            var4.spriteModel.textureName = StringUtils.discardNullOrWhitespace(var5.getValue().trim());
         }

         this.parseVector3f(var2.getValue("translate").getValue().trim(), var4.spriteModel.translate);
         this.parseVector3f(var2.getValue("rotate").getValue().trim(), var4.spriteModel.rotate);
         var4.spriteModel.scale = PZMath.tryParseFloat(var2.getValue("scale").getValue().trim(), 1.0F);
         String var7;
         if ((var5 = var2.getValue("animation")) != null) {
            var7 = var5.getValue().trim();
            var4.spriteModel.animationName = StringUtils.discardNullOrWhitespace(var7);
         }

         if ((var5 = var2.getValue("animationTime")) != null) {
            var7 = var5.getValue();
            var4.spriteModel.animationTime = PZMath.tryParseFloat(var7, -1.0F);
         }

         Iterator var9 = var2.children.iterator();

         while(var9.hasNext()) {
            ScriptParser.Block var8 = (ScriptParser.Block)var9.next();
            if ("xxx".equals(var8.type)) {
            }
         }

         return var4;
      }
   }

   Tile parseRuntimeTile(Tileset var1, ScriptParser.Block var2) {
      Tile var3 = new Tile();
      ScriptParser.Value var4 = var2.getValue("xy");
      String[] var5 = var4.getValue().trim().split(" ");
      var3.col = Integer.parseInt(var5[0]);
      var3.row = Integer.parseInt(var5[1]);
      var4 = var2.getValue("runtime");
      var3.spriteModel.parseRuntimeString(var1.name, var3.col, var3.row, var4.getValue());
      var3.spriteModel.setModule(ScriptManager.instance.getModule("Base"));
      var3.spriteModel.InitLoadPP(var3.spriteModel.getModelScriptName().substring("Base.".length()));
      return var3;
   }

   void parseVector3f(String var1, Vector3f var2) {
      String[] var3 = var1.split(" ");
      var2.setComponent(0, PZMath.tryParseFloat(var3[0], 0.0F));
      var2.setComponent(1, PZMath.tryParseFloat(var3[1], 0.0F));
      var2.setComponent(2, PZMath.tryParseFloat(var3[2], 0.0F));
   }

   void write(String var1) {
      ScriptParser.Block var2 = new ScriptParser.Block();
      var2.type = "spriteModel";
      var2.setValue("VERSION", String.valueOf(1));
      StringBuilder var3 = new StringBuilder();
      Iterator var4 = this.tilesets.iterator();

      while(var4.hasNext()) {
         Tileset var5 = (Tileset)var4.next();
         ScriptParser.Block var6 = new ScriptParser.Block();
         var6.type = "tileset";
         var6.setValue("name", var5.name);
         Iterator var7 = var5.tiles.iterator();

         while(var7.hasNext()) {
            Tile var8 = (Tile)var7.next();
            ScriptParser.Block var9 = new ScriptParser.Block();
            var9.type = "tile";
            var9.setValue("xy", String.format("%d %d", var8.col, var8.row));
            if (var8.spriteModel.runtimeString != null) {
               var9.setValue("runtime", var8.spriteModel.runtimeString);
            } else {
               SpriteModel var10 = var8.spriteModel;
               var9.setValue("modelScript", var10.modelScriptName);
               if (var10.textureName != null) {
                  var9.setValue("texture", var10.textureName);
               }

               var9.setValue("translate", String.format(Locale.US, "%.4f %.4f %.4f", var10.translate.x, var10.translate.y, var10.translate.z));
               var9.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var10.rotate.x, var10.rotate.y, var10.rotate.z));
               var9.setValue("scale", String.format(Locale.US, "%.4f", var10.scale));
               if (var10.animationName != null) {
                  var9.setValue("animation", var10.animationName);
               }

               if (var10.animationTime >= 0.0F) {
                  var9.setValue("animationTime", String.format(Locale.US, "%.4f", var10.animationTime));
               }
            }

            var6.elements.add(var9);
            var6.children.add(var9);
         }

         var2.elements.add(var6);
         var2.children.add(var6);
      }

      var3.setLength(0);
      String var11 = System.lineSeparator();
      var2.prettyPrint(0, var3, var11);
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

   public static final class Tileset {
      String name;
      final ArrayList<Tile> tiles = new ArrayList();

      public Tileset() {
      }

      public String getName() {
         return this.name;
      }

      public Tile getTile(int var1, int var2) {
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
               if (var6.getIndex() > var3.getIndex()) {
                  var4 = var5;
                  break;
               }
            }

            this.tiles.add(var4, var3);
            return var3;
         }
      }

      void initSprites() {
         for(int var1 = 0; var1 < this.tiles.size(); ++var1) {
            Tile var2 = (Tile)this.tiles.get(var1);
            if (var2 != null) {
               String var3 = String.format("%s_%d", this.name, var2.getIndex());
               IsoSprite var4 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var3);
               if (var4 != null && var4.spriteModel == null) {
                  var4.spriteModel = var2.spriteModel;
               }
            }
         }

      }
   }

   public static final class Tile {
      int col;
      int row;
      public final SpriteModel spriteModel = new SpriteModel();

      public Tile() {
      }

      int getIndex() {
         return this.col + this.row * 8;
      }
   }
}
