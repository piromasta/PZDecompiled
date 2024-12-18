package zombie.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;

public final class ConfigFile {
   protected ArrayList<ConfigOption> options;
   protected int version;
   protected String versionString = "Version";
   protected boolean bWriteTooltips = true;

   public ConfigFile() {
   }

   private void fileError(String var1, int var2, String var3) {
      DebugLog.log(var1 + ":" + var2 + " " + var3);
   }

   public boolean read(String var1) {
      this.options = new ArrayList();
      this.version = 0;
      File var2 = new File(var1);
      if (!var2.exists()) {
         return false;
      } else {
         DebugLog.DetailedInfo.trace("reading " + var1);

         try {
            FileReader var3 = new FileReader(var2);

            try {
               BufferedReader var4 = new BufferedReader(var3);

               try {
                  int var5 = 0;

                  while(true) {
                     String var6 = var4.readLine();
                     if (var6 == null) {
                        break;
                     }

                     ++var5;
                     var6 = var6.trim();
                     if (!var6.isEmpty() && !var6.startsWith("#")) {
                        if (!var6.contains("=")) {
                           this.fileError(var1, var5, var6);
                        } else {
                           String[] var7 = var6.split("=");
                           if (this.versionString.equals(var7[0])) {
                              try {
                                 this.version = Integer.parseInt(var7[1]);
                              } catch (NumberFormatException var11) {
                                 this.fileError(var1, var5, "expected version number, got \"" + var7[1] + "\"");
                              }
                           } else {
                              StringConfigOption var8 = new StringConfigOption(var7[0], var7.length > 1 ? var7[1] : "", -1);
                              this.options.add(var8);
                           }
                        }
                     }
                  }
               } catch (Throwable var12) {
                  try {
                     var4.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }

                  throw var12;
               }

               var4.close();
            } catch (Throwable var13) {
               try {
                  var3.close();
               } catch (Throwable var9) {
                  var13.addSuppressed(var9);
               }

               throw var13;
            }

            var3.close();
            return true;
         } catch (Exception var14) {
            ExceptionLogger.logException(var14);
            return false;
         }
      }
   }

   public boolean write(String var1, int var2, ArrayList<? extends ConfigOption> var3) {
      File var4 = new File(var1);
      DebugLog.DetailedInfo.trace("writing " + var1);

      try {
         FileWriter var5 = new FileWriter(var4, false);

         try {
            String var10001;
            if (var2 != 0) {
               var10001 = this.versionString;
               var5.write(var10001 + "=" + var2 + System.lineSeparator());
            }

            String var6 = System.lineSeparator();
            if (this.bWriteTooltips) {
               var6 = var6 + System.lineSeparator();
            }

            label72:
            for(int var7 = 0; var7 < var3.size(); ++var7) {
               ConfigOption var8 = (ConfigOption)var3.get(var7);
               if (this.bWriteTooltips) {
                  String var9 = var8.getTooltip();
                  if (var9 != null) {
                     var9 = var9.replaceAll("\n", System.lineSeparator() + "# ");
                     var5.write("# " + var9 + System.lineSeparator());
                  }
               }

               if (var8 instanceof ArrayConfigOption var15) {
                  if (var15.isMultiLine()) {
                     int var10 = 0;

                     while(true) {
                        if (var10 >= var15.size()) {
                           continue label72;
                        }

                        ConfigOption var11 = ((ArrayConfigOption)var8).getElement(var10);
                        var10001 = var8.getName();
                        var5.write(var10001 + "=" + var11.getValueAsString());
                        if (var10 < var15.size() - 1 || var7 < var3.size() - 1) {
                           var5.write(var6);
                        }

                        ++var10;
                     }
                  }
               }

               var10001 = var8.getName();
               var5.write(var10001 + "=" + var8.getValueAsString() + (var7 < var3.size() - 1 ? var6 : ""));
            }
         } catch (Throwable var13) {
            try {
               var5.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         var5.close();
         return true;
      } catch (Exception var14) {
         ExceptionLogger.logException(var14);
         return false;
      }
   }

   public ArrayList<ConfigOption> getOptions() {
      return this.options;
   }

   public int getVersion() {
      return this.version;
   }

   public void setVersionString(String var1) {
      this.versionString = var1;
   }

   public void setWriteTooltips(boolean var1) {
      this.bWriteTooltips = var1;
   }
}
