package zombie.network;

import java.io.IOException;
import zombie.core.logger.ExceptionLogger;

public final class DesktopBrowser {
   private static final String[] browsers = new String[]{"google-chrome", "firefox", "mozilla", "epiphany", "konqueror", "netscape", "opera", "links", "lynx", "chromium", "brave-browser"};

   public DesktopBrowser() {
   }

   public static boolean openURL(String var0) {
      try {
         if (System.getProperty("os.name").contains("OS X")) {
            Runtime.getRuntime().exec(new String[]{"open", var0});
            return true;
         }

         if (System.getProperty("os.name").startsWith("Win")) {
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", var0});
            return true;
         }

         String[] var1 = browsers;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            String var4 = var1[var3];
            Process var5 = Runtime.getRuntime().exec(new String[]{"which", var4});
            if (var5.getInputStream().read() != -1) {
               Runtime.getRuntime().exec(new String[]{var4, var0});
               return true;
            }
         }
      } catch (IOException var6) {
         ExceptionLogger.logException(var6);
      }

      return false;
   }
}
