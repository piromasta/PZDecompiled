package zombie.fileSystem;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.core.textures.TexturePackPage;

public final class TexturePackDevice implements IFileDevice {
   static final int VERSION1 = 1;
   static final int VERSION_LATEST = 1;
   String m_name;
   String m_filename;
   int m_version = -1;
   final ArrayList<Page> m_pages = new ArrayList();
   final HashMap<String, Page> m_pagemap = new HashMap();
   final HashMap<String, SubTexture> m_submap = new HashMap();
   int m_textureFlags;

   private static long skipInput(InputStream var0, long var1) throws IOException {
      long var3 = 0L;

      while(var3 < var1) {
         long var5 = var0.skip(var1 - var3);
         if (var5 > 0L) {
            var3 += var5;
         }
      }

      return var3;
   }

   public TexturePackDevice(String var1, int var2) {
      this.m_name = var1;
      this.m_filename = ZomboidFileSystem.instance.getString("media/texturepacks/" + var1 + ".pack");
      this.m_textureFlags = var2;
   }

   public IFile createFile(IFile var1) {
      return null;
   }

   public void destroyFile(IFile var1) {
   }

   public InputStream createStream(String var1, InputStream var2) throws IOException {
      this.initMetaData();
      return new TexturePackInputStream(var1, this);
   }

   public void destroyStream(InputStream var1) {
      // $FF: Couldn't be decompiled
   }

   public String name() {
      return this.m_name;
   }

   public void getSubTextureInfo(FileSystem.TexturePackTextures var1) throws IOException {
      this.initMetaData();
      Iterator var2 = this.m_submap.values().iterator();

      while(var2.hasNext()) {
         SubTexture var3 = (SubTexture)var2.next();
         FileSystem.SubTexture var4 = new FileSystem.SubTexture(this.name(), var3.m_page.m_name, var3.m_info);
         var1.put(var3.m_info.name, var4);
      }

   }

   private void initMetaData() throws IOException {
      if (this.m_pages.isEmpty()) {
         FileInputStream var1 = new FileInputStream(this.m_filename);

         try {
            BufferedInputStream var2 = new BufferedInputStream(var1);

            try {
               PositionInputStream var3 = new PositionInputStream(var2);

               try {
                  var3.mark(4);
                  int var4 = var3.read();
                  int var5 = var3.read();
                  int var6 = var3.read();
                  int var7 = var3.read();
                  if (var4 == 80 && var5 == 90 && var6 == 80 && var7 == 75) {
                     this.m_version = TexturePackPage.readInt((InputStream)var3);
                     if (this.m_version < 1 || this.m_version > 1) {
                        throw new IOException("invalid .pack file version " + this.m_version);
                     }
                  } else {
                     var3.reset();
                     this.m_version = 0;
                  }

                  int var8 = TexturePackPage.readInt((InputStream)var3);

                  for(int var9 = 0; var9 < var8; ++var9) {
                     Page var10 = this.readPage(var3);
                     this.m_pages.add(var10);
                     this.m_pagemap.put(var10.m_name, var10);
                     Iterator var11 = var10.m_sub.iterator();

                     while(var11.hasNext()) {
                        TexturePackPage.SubTextureInfo var12 = (TexturePackPage.SubTextureInfo)var11.next();
                        this.m_submap.put(var12.name, new SubTexture(var10, var12));
                     }
                  }
               } catch (Throwable var16) {
                  try {
                     var3.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }

                  throw var16;
               }

               var3.close();
            } catch (Throwable var17) {
               try {
                  var2.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }

               throw var17;
            }

            var2.close();
         } catch (Throwable var18) {
            try {
               var1.close();
            } catch (Throwable var13) {
               var18.addSuppressed(var13);
            }

            throw var18;
         }

         var1.close();
      }
   }

   private Page readPage(PositionInputStream var1) throws IOException {
      Page var2 = new Page();
      String var3 = TexturePackPage.ReadString(var1);
      int var4 = TexturePackPage.readInt((InputStream)var1);
      boolean var5 = TexturePackPage.readInt((InputStream)var1) != 0;
      var2.m_name = var3;
      var2.m_has_alpha = var5;

      int var6;
      for(var6 = 0; var6 < var4; ++var6) {
         String var7 = TexturePackPage.ReadString(var1);
         int var8 = TexturePackPage.readInt((InputStream)var1);
         int var9 = TexturePackPage.readInt((InputStream)var1);
         int var10 = TexturePackPage.readInt((InputStream)var1);
         int var11 = TexturePackPage.readInt((InputStream)var1);
         int var12 = TexturePackPage.readInt((InputStream)var1);
         int var13 = TexturePackPage.readInt((InputStream)var1);
         int var14 = TexturePackPage.readInt((InputStream)var1);
         int var15 = TexturePackPage.readInt((InputStream)var1);
         var2.m_sub.add(new TexturePackPage.SubTextureInfo(var8, var9, var10, var11, var12, var13, var14, var15, var7));
      }

      var2.m_png_start = var1.getPosition();
      if (this.m_version == 0) {
         boolean var16 = false;

         do {
            var6 = TexturePackPage.readIntByte(var1);
         } while(var6 != -559038737);
      } else {
         var6 = TexturePackPage.readInt((InputStream)var1);
         skipInput(var1, (long)var6);
      }

      return var2;
   }

   public boolean isAlpha(String var1) {
      Page var2 = (Page)this.m_pagemap.get(var1);
      return var2.m_has_alpha;
   }

   public int getTextureFlags() {
      return this.m_textureFlags;
   }

   static class TexturePackInputStream extends FileInputStream {
      TexturePackDevice m_device;

      TexturePackInputStream(String var1, TexturePackDevice var2) throws IOException {
         super(var2.m_filename);
         this.m_device = var2;
         Page var3 = (Page)this.m_device.m_pagemap.get(var1);
         if (var3 == null) {
            throw new FileNotFoundException();
         } else {
            TexturePackDevice.skipInput(this, var3.m_png_start);
            if (var2.m_version >= 1) {
               int var4 = TexturePackPage.readInt((InputStream)this);
            }

         }
      }
   }

   static final class SubTexture {
      final Page m_page;
      final TexturePackPage.SubTextureInfo m_info;

      SubTexture(Page var1, TexturePackPage.SubTextureInfo var2) {
         this.m_page = var1;
         this.m_info = var2;
      }
   }

   static final class Page {
      String m_name;
      boolean m_has_alpha = false;
      long m_png_start = -1L;
      final ArrayList<TexturePackPage.SubTextureInfo> m_sub = new ArrayList();

      Page() {
      }
   }

   public final class PositionInputStream extends FilterInputStream {
      private long pos = 0L;
      private long mark = 0L;

      public PositionInputStream(InputStream var2) {
         super(var2);
      }

      public synchronized long getPosition() {
         return this.pos;
      }

      public synchronized int read() throws IOException {
         int var1 = super.read();
         if (var1 >= 0) {
            ++this.pos;
         }

         return var1;
      }

      public synchronized int read(byte[] var1, int var2, int var3) throws IOException {
         int var4 = super.read(var1, var2, var3);
         if (var4 > 0) {
            this.pos += (long)var4;
         }

         return var4;
      }

      public synchronized long skip(long var1) throws IOException {
         long var3 = super.skip(var1);
         if (var3 > 0L) {
            this.pos += var3;
         }

         return var3;
      }

      public synchronized void mark(int var1) {
         super.mark(var1);
         this.mark = this.pos;
      }

      public synchronized void reset() throws IOException {
         if (!this.markSupported()) {
            throw new IOException("Mark not supported.");
         } else {
            super.reset();
            this.pos = this.mark;
         }
      }
   }
}
