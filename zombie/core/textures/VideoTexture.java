package zombie.core.textures;

import org.lwjgl.opengl.GL11;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;

public class VideoTexture extends Texture {
   protected boolean useAsync = true;
   protected String videoFilename;
   protected int binkId = -1;

   public VideoTexture(String var1, int var2, int var3) {
      super(var2, var3, 0);
      this.videoFilename = var1;
      this.xStart = 0.0F;
      this.yStart = 0.0F;
      this.xEnd = 1.0F;
      this.yEnd = 1.0F;
   }

   public VideoTexture(String var1, int var2, int var3, boolean var4) {
      super(var2, var3, 0);
      this.videoFilename = var1;
      this.xStart = 0.0F;
      this.yStart = 0.0F;
      this.xEnd = 1.0F;
      this.yEnd = 1.0F;
      this.useAsync = var4;
   }

   public boolean LoadVideoFile() {
      if (this.binkId > -1) {
         DebugLog.log("VideoTexture warning - trying to load a video file which has already been loaded.");
      } else {
         this.binkId = this.openVideo(this.videoFilename);
         if (this.binkId > -1 && this.useAsync) {
            this.processFrameAsync(this.binkId);
         }
      }

      int var10000 = this.binkId;
      DebugLog.log("binkId: " + var10000);
      return this.binkId >= 0;
   }

   public void Close() {
      if (this.binkId > -1) {
         this.closeVideo(this.binkId);
         this.binkId = -1;
      }

   }

   protected void RenderFrameAsync() {
      if (this.isReadyForNewFrame(this.binkId)) {
         if (this.processFrameAsyncWait(this.binkId, 1000)) {
            while(this.shouldSkipFrame(this.binkId)) {
               this.nextFrame(this.binkId);
               this.processFrameAsync(this.binkId);
               this.processFrameAsyncWait(this.binkId, -1);
            }

            if (this.isEndOfVideo(this.binkId)) {
            }

            this.nextFrame(this.binkId);
            this.processFrameAsync(this.binkId);
         }

         long var1 = this.getCurrentFrameData(this.binkId);
         RenderThread.queueInvokeOnRenderContext(() -> {
            GL11.glBindTexture(3553, Texture.lastTextureID = this.getID());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10242, 10496);
            GL11.glTexParameteri(3553, 10243, 10496);
            GL11.glTexImage2D(3553, 0, 6408, this.getWidth(), this.getHeight(), 0, 6408, 5121, var1);
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
         });
      }

   }

   public void RenderFrame() {
      if (this.binkId >= 0) {
         if (this.useAsync) {
            this.RenderFrameAsync();
         } else {
            if (this.isReadyForNewFrame(this.binkId)) {
               this.processFrame(this.binkId);
               this.nextFrame(this.binkId);

               while(this.shouldSkipFrame(this.binkId)) {
                  this.processFrame(this.binkId);
                  this.nextFrame(this.binkId);
               }

               long var1 = this.getCurrentFrameData(this.binkId);
               RenderThread.queueInvokeOnRenderContext(() -> {
                  GL11.glBindTexture(3553, Texture.lastTextureID = this.getID());
                  GL11.glTexParameteri(3553, 10241, 9728);
                  GL11.glTexParameteri(3553, 10240, 9728);
                  GL11.glTexParameteri(3553, 10242, 10496);
                  GL11.glTexParameteri(3553, 10243, 10496);
                  GL11.glTexImage2D(3553, 0, 6408, this.getWidth(), this.getHeight(), 0, 6408, 5121, var1);
               });
            }

            if (this.isEndOfVideo(this.binkId)) {
            }
         }

      }
   }

   public boolean isValid() {
      return this.binkId >= 0;
   }

   private native int openVideo(String var1);

   private native boolean isReadyForNewFrame(int var1);

   private native void processFrame(int var1);

   private native void processFrameAsync(int var1);

   private native boolean processFrameAsyncWait(int var1, int var2);

   private native void nextFrame(int var1);

   private native boolean shouldSkipFrame(int var1);

   private native boolean isEndOfVideo(int var1);

   private native void closeVideo(int var1);

   private native long getCurrentFrameData(int var1);

   private native int getFrameDataSize(int var1);

   static {
      if (System.getProperty("os.name").contains("OS X")) {
         System.loadLibrary("bink64");
      } else if (System.getProperty("os.name").startsWith("Win")) {
         if (System.getProperty("sun.arch.data.model").equals("64")) {
            System.loadLibrary("bink2w64");
            System.loadLibrary("bink64");
         } else {
            System.loadLibrary("bink2w32");
            System.loadLibrary("bink32");
         }
      } else if (System.getProperty("sun.arch.data.model").equals("64")) {
         System.loadLibrary("Bink2x64");
         System.loadLibrary("bink64");
      } else {
         System.loadLibrary("Bink2");
         System.loadLibrary("bink32");
      }

   }
}
