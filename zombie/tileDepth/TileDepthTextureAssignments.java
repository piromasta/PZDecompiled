package zombie.tileDepth;

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
import java.util.HashMap;
import java.util.Iterator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class TileDepthTextureAssignments {
   private final HashMap<String, String> m_assignments = new HashMap();
   private final String m_mediaAbsPath;
   public static final int VERSION1 = 1;
   public static final int VERSION_LATEST = 1;
   int m_version = 1;

   public TileDepthTextureAssignments(String var1) {
      this.m_mediaAbsPath = var1;
   }

   public void assignTileName(String var1, String var2) {
      if (!StringUtils.isNullOrEmpty(var1)) {
         if (StringUtils.isNullOrWhitespace(var2)) {
            this.m_assignments.remove(var1);
         } else if (!var1.equals(var2)) {
            this.m_assignments.put(var1, var2);
         }
      }
   }

   public void clearAssignedTileName(String var1) {
      this.m_assignments.remove(var1);
   }

   public String getAssignedTileName(String var1) {
      return (String)this.m_assignments.get(var1);
   }

   public void load() {
      this.m_assignments.clear();
      String var1 = this.m_mediaAbsPath + "/tileDepthTextureAssignments.txt";
      this.read(var1);
   }

   public void save() {
      String var1 = this.m_mediaAbsPath + "/tileDepthTextureAssignments.txt";
      this.write(var1);
   }

   private void read(String var1) {
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

   private void parseFile(String var1) throws IOException {
      ScriptParser.Block var2 = ScriptParser.parse(var1);
      var2 = (ScriptParser.Block)var2.children.get(0);
      ScriptParser.Value var3 = var2.getValue("VERSION");
      if (var3 == null) {
         throw new IOException("missing VERSION in tileGeometry.txt");
      } else {
         this.m_version = PZMath.tryParseInt(var3.getValue().trim(), -1);
         if (this.m_version >= 1 && this.m_version <= 1) {
            Iterator var4 = var2.children.iterator();

            while(var4.hasNext()) {
               ScriptParser.Block var5 = (ScriptParser.Block)var4.next();
               if ("xxx".equals(var5.type)) {
               }
            }

            var4 = var2.values.iterator();

            while(true) {
               TileGeometryFile.Tile var11;
               do {
                  String var7;
                  int var8;
                  do {
                     String var6;
                     do {
                        do {
                           if (!var4.hasNext()) {
                              return;
                           }

                           ScriptParser.Value var13 = (ScriptParser.Value)var4.next();
                           var6 = StringUtils.discardNullOrWhitespace(var13.getKey().trim());
                           var7 = StringUtils.discardNullOrWhitespace(var13.getValue().trim());
                        } while(var6 == null);
                     } while(var7 == null);

                     this.assignTileName(var6, var7);
                     var8 = var7.lastIndexOf(95);
                  } while(var8 == -1);

                  String var9 = var7.substring(0, var8);
                  int var10 = PZMath.tryParseInt(var7.substring(var8 + 1), -1);
                  var11 = TileGeometryManager.getInstance().getTile("game", var9, var10 % 8, var10 / 8);
               } while(var11 != null && !var11.m_geometry.isEmpty());

               boolean var12 = true;
            }
         } else {
            throw new IOException(String.format("unknown tileGeometry.txt VERSION \"%s\"", var3.getValue().trim()));
         }
      }
   }

   void write(String var1) {
      ScriptParser.Block var2 = new ScriptParser.Block();
      var2.type = "tileDepthTextureAssignments";
      var2.setValue("VERSION", String.valueOf(1));
      ArrayList var3 = new ArrayList(this.m_assignments.keySet());
      var3.sort(String::compareTo);
      Iterator var4 = var3.iterator();

      String var5;
      while(var4.hasNext()) {
         var5 = (String)var4.next();
         var2.setValue(var5, (String)this.m_assignments.get(var5));
      }

      StringBuilder var6 = new StringBuilder();
      var6.setLength(0);
      var5 = System.lineSeparator();
      var2.prettyPrint(0, var6, var5);
      this.write(var1, var6.toString());
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

   public void assignDepthTextureToSprite(String var1) {
      IsoSprite var2 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var1);
      if (var2 != null) {
         if (var2.depthTexture == null || !var2.depthTexture.getName().equals(var2.name)) {
            String var3 = this.getAssignedTileName(var1);
            if (var3 == null) {
               var2.depthTexture = null;
            } else {
               TileDepthTexture var4 = TileDepthTextureManager.getInstance().getTextureFromTileName(var3);
               if (var4 != null && !var4.isEmpty()) {
                  var2.depthTexture = var4;
               } else {
                  var2.depthTexture = null;
               }
            }
         }
      }
   }
}
