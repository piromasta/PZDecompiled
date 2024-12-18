package zombie.radio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.Language;
import zombie.core.Languages;
import zombie.core.Translator;
import zombie.util.StringUtils;

public final class RadioTranslationData {
   private String filePath;
   private String guid;
   private String language;
   private Language languageEnum;
   private int version = -1;
   private final ArrayList<String> translators = new ArrayList();
   private final Map<String, String> translations = new HashMap();

   public RadioTranslationData(String var1) {
      this.filePath = var1;
   }

   public String getFilePath() {
      return this.filePath;
   }

   public String getGuid() {
      return this.guid;
   }

   public String getLanguage() {
      return this.language;
   }

   public Language getLanguageEnum() {
      return this.languageEnum;
   }

   public int getVersion() {
      return this.version;
   }

   public int getTranslationCount() {
      return this.translations.size();
   }

   public ArrayList<String> getTranslators() {
      return this.translators;
   }

   public boolean validate() {
      return this.guid != null && this.language != null && this.version >= 0;
   }

   public boolean loadTranslations() {
      boolean var1 = false;
      if (Translator.getLanguage() != this.languageEnum) {
         System.out.println("Radio translations trying to load language that is not the current language...");
         return false;
      } else {
         try {
            File var2 = new File(this.filePath);
            if (var2.exists() && !var2.isDirectory()) {
               BufferedReader var3 = new BufferedReader(new InputStreamReader(new FileInputStream(this.filePath), Charset.forName(this.languageEnum.charset())));
               String var4 = null;
               boolean var5 = false;
               ArrayList var6 = new ArrayList();

               while(true) {
                  while((var4 = var3.readLine()) != null) {
                     var4 = var4.trim();
                     if (var4.equals("[Translations]")) {
                        var5 = true;
                     } else if (var5) {
                        String var9;
                        if (!var4.equals("[Collection]")) {
                           if (var4.equals("[/Translations]")) {
                              var1 = true;
                              return var1;
                           }

                           String[] var12 = var4.split("=", 2);
                           if (var12.length == 2) {
                              String var14 = var12[0].trim();
                              var9 = var12[1].trim();
                              this.translations.put(var14, var9);
                           }
                        } else {
                           String var7 = null;

                           while((var4 = var3.readLine()) != null) {
                              var4 = var4.trim();
                              if (var4.equals("[/Collection]")) {
                                 break;
                              }

                              String[] var8 = var4.split("=", 2);
                              if (var8.length == 2) {
                                 var9 = var8[0].trim();
                                 String var10 = var8[1].trim();
                                 if (var9.equals("text")) {
                                    var7 = var10;
                                 } else if (var9.equals("member")) {
                                    var6.add(var10);
                                 }
                              }
                           }

                           if (var7 != null && var6.size() > 0) {
                              Iterator var13 = var6.iterator();

                              while(var13.hasNext()) {
                                 var9 = (String)var13.next();
                                 this.translations.put(var9, var7);
                              }
                           }

                           var6.clear();
                        }
                     }
                  }

                  return var1;
               }
            }
         } catch (Exception var11) {
            var11.printStackTrace();
            var1 = false;
         }

         return var1;
      }
   }

   public String getTranslation(String var1) {
      return this.translations.containsKey(var1) ? (String)this.translations.get(var1) : null;
   }

   public static RadioTranslationData ReadFile(String var0) {
      RadioTranslationData var1 = new RadioTranslationData(var0);
      File var2 = new File(var0);
      if (var2.exists() && !var2.isDirectory()) {
         Language var3 = parseLanguageFromFilename(var2.getName());
         String var4 = var3 == null ? null : var3.charset();

         try {
            FileInputStream var5 = new FileInputStream(var0);

            try {
               InputStreamReader var6 = new InputStreamReader(var5, var4);

               try {
                  BufferedReader var7 = new BufferedReader(var6);

                  try {
                     String var8 = null;

                     while((var8 = var7.readLine()) != null) {
                        String[] var9 = var8.split("=");
                        if (var9.length > 1) {
                           String var10 = var9[0].trim();
                           String var11 = "";

                           for(int var12 = 1; var12 < var9.length; ++var12) {
                              var11 = var11 + var9[var12];
                           }

                           var11 = var11.trim();
                           if (var10.equals("guid")) {
                              var1.guid = var11;
                           } else if (var10.equals("language")) {
                              var1.language = var11;
                           } else if (var10.equals("version")) {
                              var1.version = Integer.parseInt(var11);
                           } else if (var10.equals("translator")) {
                              String[] var27 = var11.split(",");
                              if (var27.length > 0) {
                                 String[] var13 = var27;
                                 int var14 = var27.length;

                                 for(int var15 = 0; var15 < var14; ++var15) {
                                    String var16 = var13[var15];
                                    var1.translators.add(var16);
                                 }
                              }
                           }
                        }

                        var8 = var8.trim();
                        if (var8.equals("[/Info]")) {
                           break;
                        }
                     }
                  } catch (Throwable var20) {
                     try {
                        var7.close();
                     } catch (Throwable var19) {
                        var20.addSuppressed(var19);
                     }

                     throw var20;
                  }

                  var7.close();
               } catch (Throwable var21) {
                  try {
                     var6.close();
                  } catch (Throwable var18) {
                     var21.addSuppressed(var18);
                  }

                  throw var21;
               }

               var6.close();
            } catch (Throwable var22) {
               try {
                  var5.close();
               } catch (Throwable var17) {
                  var22.addSuppressed(var17);
               }

               throw var22;
            }

            var5.close();
         } catch (Exception var23) {
            var23.printStackTrace();
         }
      }

      boolean var24 = false;
      if (var1.language != null) {
         Iterator var25 = Translator.getAvailableLanguage().iterator();

         while(var25.hasNext()) {
            Language var26 = (Language)var25.next();
            if (var26.toString().equals(var1.language)) {
               var1.languageEnum = var26;
               var24 = true;
               break;
            }
         }
      }

      if (!var24 && var1.language != null) {
         System.out.println("Language " + var1.language + " not found");
         return null;
      } else {
         return var1.guid != null && var1.language != null && var1.version >= 0 ? var1 : null;
      }
   }

   private static Language parseLanguageFromFilename(String var0) {
      if (!StringUtils.startsWithIgnoreCase(var0, "RadioData_")) {
         return null;
      } else {
         String var1 = var0.replaceFirst("RadioData_", "").replace(".txt", "");
         return Languages.instance.getByName(var1);
      }
   }
}
