package zombie.network;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.codec.binary.Hex;
import zombie.GameWindow;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.packets.service.ChecksumPacket;

public final class NetChecksum {
   public static final Checksummer checksummer = new Checksummer();
   public static final Comparer comparer = new Comparer();

   public NetChecksum() {
   }

   public static final class Checksummer {
      private MessageDigest md;
      private final byte[] fileBytes = new byte[1024];
      private final byte[] convertBytes = new byte[1024];
      private boolean convertLineEndings;

      public Checksummer() {
      }

      public void reset(boolean var1) throws NoSuchAlgorithmException {
         if (this.md == null) {
            this.md = MessageDigest.getInstance("MD5");
         }

         this.convertLineEndings = var1;
         this.md.reset();
      }

      public void addFile(String var1, String var2) throws NoSuchAlgorithmException {
         if (this.md == null) {
            this.md = MessageDigest.getInstance("MD5");
         }

         try {
            FileInputStream var3 = new FileInputStream(var2);

            try {
               NetChecksum.GroupOfFiles.addFile(var1, var2);

               while(true) {
                  int var4;
                  while((var4 = var3.read(this.fileBytes)) != -1) {
                     if (this.convertLineEndings) {
                        boolean var5 = false;
                        int var6 = 0;

                        for(int var7 = 0; var7 < var4 - 1; ++var7) {
                           if (this.fileBytes[var7] == 13 && this.fileBytes[var7 + 1] == 10) {
                              this.convertBytes[var6++] = 10;
                              var5 = true;
                           } else {
                              var5 = false;
                              this.convertBytes[var6++] = this.fileBytes[var7];
                           }
                        }

                        if (!var5) {
                           this.convertBytes[var6++] = this.fileBytes[var4 - 1];
                        }

                        this.md.update(this.convertBytes, 0, var6);
                        NetChecksum.GroupOfFiles.updateFile(this.convertBytes, var6);
                     } else {
                        this.md.update(this.fileBytes, 0, var4);
                        NetChecksum.GroupOfFiles.updateFile(this.fileBytes, var4);
                     }
                  }

                  NetChecksum.GroupOfFiles.endFile();
                  break;
               }
            } catch (Throwable var9) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var3.close();
         } catch (Exception var10) {
            DebugLog.General.printException(var10, "absPath:" + var2, LogSeverity.Error);
         }

      }

      public String checksumToString() {
         byte[] var1 = this.md.digest();
         StringBuilder var2 = new StringBuilder();

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var2.append(Integer.toString((var1[var3] & 255) + 256, 16).substring(1));
         }

         return var2.toString();
      }

      public String toString() {
         StringBuilder var1 = new StringBuilder();
         Iterator var2 = NetChecksum.GroupOfFiles.groups.iterator();

         while(var2.hasNext()) {
            GroupOfFiles var3 = (GroupOfFiles)var2.next();
            String var4 = var3.toString();
            var1.append("\n").append(var4);
            if (GameClient.bClient) {
               ChecksumPacket.sendError(GameClient.connection, var4);
            }
         }

         return var1.toString();
      }
   }

   public static final class Comparer {
      public static final short NUM_GROUPS_TO_SEND = 10;
      public State state;
      public short currentIndex;
      public String error;

      public Comparer() {
         this.state = NetChecksum.Comparer.State.Init;
      }

      public void beginCompare() {
         this.error = null;
         ChecksumPacket.sendTotalChecksum();
      }

      private void gc() {
         NetChecksum.GroupOfFiles.gc();
      }

      public void update() {
         switch (this.state) {
            case Init:
            case SentTotalChecksum:
            case SentGroupChecksum:
            case SentFileChecksums:
            default:
               break;
            case Success:
               this.gc();
               GameClient.checksumValid = true;
               break;
            case Failed:
               this.gc();
               GameClient.connection.forceDisconnect("checksum-" + this.error);
               GameWindow.bServerDisconnected = true;
               GameWindow.kickReason = this.error;
         }

      }

      public static enum State {
         Init,
         SentTotalChecksum,
         SentGroupChecksum,
         SentFileChecksums,
         Success,
         Failed;

         private State() {
         }
      }
   }

   public static final class GroupOfFiles {
      public static final int MAX_FILES = 20;
      static MessageDigest mdTotal;
      static MessageDigest mdCurrentFile;
      public static final ArrayList<GroupOfFiles> groups = new ArrayList();
      static GroupOfFiles currentGroup;
      public byte[] totalChecksum;
      public short fileCount;
      public final String[] relPaths = new String[20];
      final String[] absPaths = new String[20];
      public final byte[][] checksums = new byte[20][];

      private GroupOfFiles() throws NoSuchAlgorithmException {
         if (mdTotal == null) {
            mdTotal = MessageDigest.getInstance("MD5");
            mdCurrentFile = MessageDigest.getInstance("MD5");
         }

         mdTotal.reset();
         groups.add(this);
      }

      public String toString() {
         StringBuilder var1 = (new StringBuilder()).append(this.fileCount).append(" files, ").append(this.absPaths.length).append("/").append(this.relPaths.length).append("/").append(this.checksums.length).append(" \"").append(Hex.encodeHexString(this.totalChecksum)).append("\"");

         for(int var2 = 0; var2 < 20; ++var2) {
            var1.append("\n");
            if (var2 < this.relPaths.length) {
               var1.append(" \"").append(this.relPaths[var2]).append("\"");
            }

            if (var2 < this.checksums.length) {
               if (this.checksums[var2] == null) {
                  var1.append(" \"\"");
               } else {
                  var1.append(" \"").append(Hex.encodeHexString(this.checksums[var2])).append("\"");
               }
            }

            if (var2 < this.absPaths.length) {
               var1.append(" \"").append(this.absPaths[var2]).append("\"");
            }
         }

         return var1.toString();
      }

      private void gc_() {
         Arrays.fill(this.relPaths, (Object)null);
         Arrays.fill(this.absPaths, (Object)null);
         Arrays.fill(this.checksums, (Object)null);
      }

      public static void initChecksum() {
         groups.clear();
         currentGroup = null;
      }

      public static void finishChecksum() {
         if (currentGroup != null) {
            currentGroup.totalChecksum = mdTotal.digest();
            currentGroup = null;
         }

      }

      private static void addFile(String var0, String var1) throws NoSuchAlgorithmException {
         if (currentGroup == null) {
            currentGroup = new GroupOfFiles();
         }

         currentGroup.relPaths[currentGroup.fileCount] = var0;
         currentGroup.absPaths[currentGroup.fileCount] = var1;
         mdCurrentFile.reset();
      }

      private static void updateFile(byte[] var0, int var1) {
         mdCurrentFile.update(var0, 0, var1);
         mdTotal.update(var0, 0, var1);
      }

      private static void endFile() {
         currentGroup.checksums[currentGroup.fileCount] = mdCurrentFile.digest();
         ++currentGroup.fileCount;
         if (currentGroup.fileCount >= 20) {
            currentGroup.totalChecksum = mdTotal.digest();
            currentGroup = null;
         }

      }

      public static void gc() {
         Iterator var0 = groups.iterator();

         while(var0.hasNext()) {
            GroupOfFiles var1 = (GroupOfFiles)var0.next();
            var1.gc_();
         }

         groups.clear();
      }
   }
}
