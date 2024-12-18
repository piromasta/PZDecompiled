package zombie.network;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class CustomizationManager {
   static CustomizationManager instance = new CustomizationManager();
   private ByteBuffer ServerImageIcon = null;
   private ByteBuffer ServerImageLoadingScreen = null;
   private ByteBuffer ServerImageLoginScreen = null;
   private final ArrayList<Texture> customBackgrounds = new ArrayList();
   static byte customBackgroundIndex = -1;

   public CustomizationManager() {
   }

   public static CustomizationManager getInstance() {
      return instance;
   }

   public void load() {
      if (GameServer.bServer) {
         this.ServerImageIcon = this.load(ServerOptions.instance.ServerImageIcon.getValue(), 64, 64);
         this.ServerImageLoginScreen = this.load(ServerOptions.instance.ServerImageLoginScreen.getValue(), 1280, 720);
         this.ServerImageLoadingScreen = this.load(ServerOptions.instance.ServerImageLoadingScreen.getValue(), 1280, 720);
      } else {
         this.customBackgrounds.clear();
         String var10000 = ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath();
         String var1 = var10000 + File.separator + "media" + File.separator + "ui" + File.separator + "Illustrations" + File.separator;

         try {
            Stream var2 = Files.walk((new File(var1)).toPath());

            try {
               var2.forEach((var1x) -> {
                  if (var1x.getFileName().toString().matches("\\S+_1920x1080_.jpg")) {
                     try {
                        this.customBackgrounds.add(new Texture(var1x.toAbsolutePath().toString()));
                     } catch (Exception var3) {
                        ExceptionLogger.logException(var3);
                     }
                  }

               });
            } catch (Throwable var6) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (IOException var7) {
            throw new RuntimeException(var7);
         }
      }

   }

   public ByteBuffer getServerImageIcon() {
      return this.ServerImageIcon;
   }

   public ByteBuffer getServerImageLoginScreen() {
      return this.ServerImageLoginScreen;
   }

   public ByteBuffer getServerImageLoadingScreen() {
      return this.ServerImageLoadingScreen;
   }

   public void pickRandomCustomBackground() {
      customBackgroundIndex = (byte)Rand.Next(this.customBackgrounds.size());
   }

   public byte getRandomCustomBackground() {
      if (customBackgroundIndex == -1) {
         this.pickRandomCustomBackground();
      }

      return customBackgroundIndex;
   }

   public Texture getClientCustomBackground(int var1) {
      int var2 = this.customBackgrounds.size();
      return (Texture)this.customBackgrounds.get(var1 % var2);
   }

   private ByteBuffer load(String var1, int var2, int var3) {
      ByteBuffer var4 = null;
      if (!var1.isEmpty()) {
         DebugLog.General.println("Loading " + var1 + " with size " + var2 + "x" + var3);

         try {
            BufferedImage var5 = ImageIO.read((new File(var1)).getAbsoluteFile());
            var4 = loadCompressAndResizeInstance(var5, var2, var3);
            DebugLog.General.println("Data size " + var4.limit());
         } catch (IOException var6) {
            DebugLog.General.printException(var6, "Error loading file " + var1, LogSeverity.Error);
         }
      }

      return var4;
   }

   public static ByteBuffer loadCompressAndResizeInstance(BufferedImage var0, int var1, int var2) {
      BufferedImage var3 = new BufferedImage(var1, var2, 1);
      Graphics2D var4 = var3.createGraphics();
      double var5 = getIconRatio(var0, var3);
      double var7 = (double)var0.getWidth() * var5;
      double var9 = (double)var0.getHeight() * var5;
      var4.drawImage(var0, (int)(((double)var3.getWidth() - var7) / 2.0), (int)(((double)var3.getHeight() - var9) / 2.0), (int)var7, (int)var9, (ImageObserver)null);
      var4.dispose();
      return compressToByteBuffer(var3);
   }

   public static ByteBuffer loadAndResizeInstance(BufferedImage var0, int var1, int var2) {
      BufferedImage var3 = new BufferedImage(var1, var2, 3);
      Graphics2D var4 = var3.createGraphics();
      double var5 = getIconRatio(var0, var3);
      double var7 = (double)var0.getWidth() * var5;
      double var9 = (double)var0.getHeight() * var5;
      var4.drawImage(var0, (int)(((double)var3.getWidth() - var7) / 2.0), (int)(((double)var3.getHeight() - var9) / 2.0), (int)var7, (int)var9, (ImageObserver)null);
      var4.dispose();
      return convertToByteBuffer(var3);
   }

   public static double getIconRatio(BufferedImage var0, BufferedImage var1) {
      double var2 = 1.0;
      if (var0.getWidth() > var1.getWidth()) {
         var2 = (double)var1.getWidth() / (double)var0.getWidth();
      } else {
         var2 = (double)(var1.getWidth() / var0.getWidth());
      }

      double var4;
      if (var0.getHeight() > var1.getHeight()) {
         var4 = (double)var1.getHeight() / (double)var0.getHeight();
         if (var4 < var2) {
            var2 = var4;
         }
      } else {
         var4 = (double)(var1.getHeight() / var0.getHeight());
         if (var4 < var2) {
            var2 = var4;
         }
      }

      return var2;
   }

   public static ByteBuffer convertToByteBuffer(BufferedImage var0) {
      byte[] var1 = new byte[var0.getWidth() * var0.getHeight() * 4];
      int var2 = 0;

      for(int var3 = 0; var3 < var0.getHeight(); ++var3) {
         for(int var4 = 0; var4 < var0.getWidth(); ++var4) {
            int var5 = var0.getRGB(var4, var3);
            var1[var2] = (byte)(var5 << 8 >> 24);
            var1[var2 + 1] = (byte)(var5 << 16 >> 24);
            var1[var2 + 2] = (byte)(var5 << 24 >> 24);
            var1[var2 + 3] = (byte)(var5 >> 24);
            var2 += 4;
         }
      }

      ByteBuffer var6 = ByteBuffer.allocateDirect(var1.length);
      var6.put(var1);
      var6.flip();
      return var6;
   }

   public static ByteBuffer compressToByteBuffer(BufferedImage var0) {
      try {
         ByteArrayOutputStream var1 = new ByteArrayOutputStream(10000000);
         ImageOutputStream var2 = ImageIO.createImageOutputStream(var1);
         ImageTypeSpecifier var3 = ImageTypeSpecifier.createFromRenderedImage(var0);
         ImageWriter var4 = (ImageWriter)ImageIO.getImageWriters(var3, "jpg").next();
         ImageWriteParam var5 = var4.getDefaultWriteParam();
         if (var5.canWriteCompressed()) {
            var5.setCompressionMode(2);
            var5.setCompressionQuality(0.8F);
         }

         var4.setOutput(var2);
         var4.write((IIOMetadata)null, new IIOImage(var0, (List)null, (IIOMetadata)null), var5);
         var4.dispose();
         if (var1.size() > 900000) {
            throw new RuntimeException("Compressed image too big");
         } else {
            byte[] var6 = var1.toByteArray();
            ByteBuffer var7 = ByteBuffer.allocate(var6.length);
            var7.put(var6);
            var7.flip();
            return var7;
         }
      } catch (IOException var8) {
         var8.printStackTrace();
         return null;
      }
   }
}
