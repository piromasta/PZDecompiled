package zombie.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import zombie.util.StringUtils;

public class RCONClient {
   private Socket socket;

   public RCONClient() {
   }

   public boolean disconnect() {
      try {
         this.socket.close();
         return true;
      } catch (IOException var2) {
         System.out.println("Disconnect failed: " + var2.getMessage());
         return false;
      }
   }

   public boolean connect(String var1, String var2) {
      try {
         this.socket = new Socket();
         this.socket.setSoTimeout(5000);
         InetSocketAddress var3 = new InetSocketAddress(var1, Integer.parseInt(var2));
         this.socket.connect(var3, 1000);
         return true;
      } catch (IOException var4) {
         System.out.println("Connect failed: " + var4.getMessage());
         return false;
      }
   }

   public boolean auth(String var1) {
      try {
         int var2 = (int)(65535L & System.currentTimeMillis());
         RCONMessage var3 = new RCONMessage(var2, 3, var1);
         var3.writeObject(this.socket.getOutputStream());
         RCONMessage var4 = new RCONMessage();
         var4.readObject(this.socket.getInputStream(), 14);
         if (var4.type == 0 && var4.id == var2) {
            RCONMessage var5 = new RCONMessage();
            var5.readObject(this.socket.getInputStream(), 14);
            if (var5.type == 2 && var4.id == var2) {
               return true;
            } else {
               System.out.println("Authentication failed: auth response");
               return false;
            }
         } else {
            System.out.println("Authentication failed: response value");
            return false;
         }
      } catch (IOException var6) {
         System.out.println("Authentication failed: timeout");
         return false;
      }
   }

   public String exec(String var1) {
      try {
         int var2 = (int)(65535L & System.currentTimeMillis());
         RCONMessage var3 = new RCONMessage(var2, 2, var1);
         var3.writeObject(this.socket.getOutputStream());
         RCONMessage var4 = new RCONMessage();
         var4.readObject(this.socket.getInputStream(), 0);
         return new String(var4.body);
      } catch (IOException var5) {
         System.out.println("Command execution failed");
         return null;
      }
   }

   public boolean send(String var1, String var2) {
      try {
         HttpClient var3 = HttpClient.newHttpClient();
         HttpRequest var4 = HttpRequest.newBuilder().setHeader("Content-type", "application/json").uri(URI.create(var1)).POST(BodyPublishers.ofString("{\"text\":\"" + var2 + "\"}")).build();
         HttpResponse var5 = var3.send(var4, BodyHandlers.ofString());
         if (var5 != null && var5.statusCode() != 200) {
            System.out.println((String)var5.body());
            return false;
         } else {
            return true;
         }
      } catch (Exception var6) {
         System.out.println("Result post failed");
         return false;
      }
   }

   private static void sleep(long var0) {
      try {
         Thread.sleep(var0);
      } catch (Exception var3) {
      }

   }

   public static void main(String[] var0) {
      String var1 = null;
      String var2 = null;
      String var3 = null;
      String var4 = null;
      String var5 = null;
      boolean var6 = false;

      for(int var7 = 0; var7 < var0.length; ++var7) {
         if (!StringUtils.isNullOrEmpty(var0[var7])) {
            if (var0[var7].equals("-ip")) {
               ++var7;
               var1 = var0[var7].trim();
            } else if (var0[var7].equals("-port")) {
               ++var7;
               var2 = var0[var7].trim();
            } else if (var0[var7].equals("-password")) {
               ++var7;
               var3 = var0[var7].trim();
            } else if (var0[var7].equals("-command")) {
               ++var7;
               var4 = var0[var7].trim();
            } else if (var0[var7].equals("-webhook")) {
               ++var7;
               var5 = var0[var7].trim();
            }
         }
      }

      if (!StringUtils.isNullOrEmpty(var1) && !StringUtils.isNullOrEmpty(var2) && !StringUtils.isNullOrEmpty(var3) && !StringUtils.isNullOrEmpty(var4)) {
         if (!StringUtils.isNullOrEmpty(var5)) {
            var6 = true;
         }

         RCONClient var10 = new RCONClient();

         do {
            if (var10.connect(var1, var2)) {
               if (var10.auth(var3)) {
                  if (var6 && !var10.send(var5, String.format("Connected to server %s:%s", var1, var2))) {
                     break;
                  }

                  String var8 = null;

                  do {
                     String var9 = var10.exec(var4);
                     if (StringUtils.isNullOrEmpty(var9)) {
                        break;
                     }

                     if (!var9.equals(var8)) {
                        if (var6) {
                           if (!var10.send(var5, var9)) {
                              break;
                           }

                           sleep(5000L);
                        } else {
                           System.out.println(var9);
                        }
                     }

                     var8 = var9;
                  } while(var6);

                  if (var6 && !var10.send(var5, "Connection to server lost")) {
                     break;
                  }
               }

               var10.disconnect();
            }

            if (var6) {
               sleep(60000L);
            }
         } while(var6);

      } else {
         System.out.println("Incorrect arguments");
      }
   }

   private static class RCONMessage {
      private static final byte[] input = new byte[4096];
      private static final ByteBuffer bbr;
      private static final byte[] output;
      private static final ByteBuffer bbw;
      static final int baseSize = 10;
      int size;
      int id;
      int type;
      byte[] body;

      RCONMessage() {
      }

      RCONMessage(int var1, int var2, String var3) throws UnsupportedEncodingException {
         this.id = var1;
         this.type = var2;
         this.body = var3.getBytes();
         this.size = 10 + var3.length();
      }

      private void writeObject(OutputStream var1) throws IOException {
         bbw.putInt(this.size);
         bbw.putInt(this.id);
         bbw.putInt(this.type);
         bbw.put(this.body);
         bbw.put((byte)0);
         bbw.put((byte)0);
         var1.write(output, 0, this.size + 4);
         bbw.clear();
      }

      private void readObject(InputStream var1, int var2) throws IOException {
         if (var2 == 0) {
            var1.read(input);
         } else {
            var1.read(input, 0, var2);
         }

         this.size = Math.min(bbr.getInt(), bbr.capacity() - 4);
         this.id = bbr.getInt();
         this.type = bbr.getInt();
         if (this.size > 10) {
            this.body = new byte[this.size - 10];
            bbr.get(this.body, 0, this.size - 10);
         }

         bbr.get();
         bbr.get();
         bbr.clear();
      }

      static {
         bbr = ByteBuffer.wrap(input);
         output = new byte[4096];
         bbw = ByteBuffer.wrap(output);
         bbr.order(ByteOrder.LITTLE_ENDIAN);
         bbw.order(ByteOrder.LITTLE_ENDIAN);
      }
   }
}
