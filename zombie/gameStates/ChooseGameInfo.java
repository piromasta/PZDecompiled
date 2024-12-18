package zombie.gameStates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.GameVersion;
import zombie.core.IndieFileLoader;
import zombie.core.Language;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ChooseGameInfo {
   private static final HashMap<String, Map> Maps = new HashMap();
   private static final HashMap<String, Mod> Mods = new HashMap();
   private static final HashSet<String> MissingMods = new HashSet();
   private static final ArrayList<String> tempStrings = new ArrayList();

   private ChooseGameInfo() {
   }

   public static void Reset() {
      Maps.clear();
      Mods.clear();
      MissingMods.clear();
   }

   private static void readTitleDotTxt(Map var0, String var1, Language var2) throws IOException {
      String var10000 = var2.toString();
      String var3 = "media/lua/shared/Translate/" + var10000 + "/" + var1 + "/title.txt";
      File var4 = new File(ZomboidFileSystem.instance.getString(var3));

      try {
         FileInputStream var5 = new FileInputStream(var4);

         try {
            InputStreamReader var6 = new InputStreamReader(var5, Charset.forName(var2.charset()));

            try {
               BufferedReader var7 = new BufferedReader(var6);

               try {
                  String var8 = var7.readLine();
                  var8 = StringUtils.stripBOM(var8);
                  if (!StringUtils.isNullOrWhitespace(var8)) {
                     var0.title = var8.trim();
                  }
               } catch (Throwable var13) {
                  try {
                     var7.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               var7.close();
            } catch (Throwable var14) {
               try {
                  var6.close();
               } catch (Throwable var11) {
                  var14.addSuppressed(var11);
               }

               throw var14;
            }

            var6.close();
         } catch (Throwable var15) {
            try {
               var5.close();
            } catch (Throwable var10) {
               var15.addSuppressed(var10);
            }

            throw var15;
         }

         var5.close();
      } catch (FileNotFoundException var16) {
      }

   }

   private static void readDescriptionDotTxt(Map var0, String var1, Language var2) throws IOException {
      String var10000 = var2.toString();
      String var3 = "media/lua/shared/Translate/" + var10000 + "/" + var1 + "/description.txt";
      File var4 = new File(ZomboidFileSystem.instance.getString(var3));

      try {
         FileInputStream var5 = new FileInputStream(var4);

         try {
            InputStreamReader var6 = new InputStreamReader(var5, Charset.forName(var2.charset()));

            try {
               BufferedReader var7 = new BufferedReader(var6);

               try {
                  var0.desc = "";

                  String var8;
                  for(boolean var9 = true; (var8 = var7.readLine()) != null; var0.desc = var0.desc + var8) {
                     if (var9) {
                        var8 = StringUtils.stripBOM(var8);
                        var9 = false;
                     }
                  }
               } catch (Throwable var13) {
                  try {
                     var7.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               var7.close();
            } catch (Throwable var14) {
               try {
                  var6.close();
               } catch (Throwable var11) {
                  var14.addSuppressed(var11);
               }

               throw var14;
            }

            var6.close();
         } catch (Throwable var15) {
            try {
               var5.close();
            } catch (Throwable var10) {
               var15.addSuppressed(var10);
            }

            throw var15;
         }

         var5.close();
      } catch (FileNotFoundException var16) {
      }

   }

   public static Map getMapDetails(String var0) {
      if (Maps.containsKey(var0)) {
         return (Map)Maps.get(var0);
      } else {
         File var1 = new File(ZomboidFileSystem.instance.getString("media/maps/" + var0 + "/map.info"));
         if (!var1.exists()) {
            return null;
         } else {
            Map var2 = new Map();
            var2.dir = (new File(var1.getParent())).getAbsolutePath();
            var2.title = var0;
            var2.lotsDir = new ArrayList();

            try {
               FileReader var3 = new FileReader(var1.getAbsolutePath());
               BufferedReader var4 = new BufferedReader(var3);
               String var5 = null;

               label65:
               while(true) {
                  try {
                     if ((var5 = var4.readLine()) != null) {
                        var5 = var5.trim();
                        if (var5.startsWith("title=")) {
                           var2.title = var5.replace("title=", "");
                        } else if (var5.startsWith("lots=")) {
                           var2.lotsDir.add(var5.replace("lots=", "").trim());
                        } else if (var5.startsWith("description=")) {
                           if (var2.desc == null) {
                              var2.desc = "";
                           }

                           String var10001 = var2.desc;
                           var2.desc = var10001 + var5.replace("description=", "");
                        } else if (var5.startsWith("fixed2x=")) {
                           var2.bFixed2x = Boolean.parseBoolean(var5.replace("fixed2x=", "").trim());
                        } else if (var5.startsWith("zoomX=")) {
                           var2.zoomX = Float.parseFloat(var5.replace("zoomX=", "").trim());
                        } else if (var5.startsWith("zoomY=")) {
                           var2.zoomY = Float.parseFloat(var5.replace("zoomY=", "").trim());
                        } else if (var5.startsWith("zoomS=")) {
                           var2.zoomS = Float.parseFloat(var5.replace("zoomS=", "").trim());
                        } else if (var5.startsWith("demoVideo=")) {
                           var2.demoVideo = var5.replace("demoVideo=", "");
                        }
                        continue;
                     }
                  } catch (IOException var10) {
                     Logger.getLogger(ChooseGameInfo.class.getName()).log(Level.SEVERE, (String)null, var10);
                  }

                  var4.close();
                  var2.thumb = Texture.getSharedTexture(var2.dir + "/thumb.png");
                  String var6 = ZomboidFileSystem.instance.getString(var2.dir + "/spawnSelectImagePyramid.zip");
                  if ((new File(var6)).exists()) {
                     var2.spawnSelectImagePyramid = var6;
                  } else {
                     var2.worldmap = Texture.getSharedTexture(var2.dir + "/worldmap.png");
                  }

                  ArrayList var7 = new ArrayList();
                  Translator.addLanguageToList(Translator.getLanguage(), var7);
                  Translator.addLanguageToList(Translator.getDefaultLanguage(), var7);
                  int var8 = var7.size() - 1;

                  while(true) {
                     if (var8 < 0) {
                        break label65;
                     }

                     Language var9 = (Language)var7.get(var8);
                     readTitleDotTxt(var2, var0, var9);
                     readDescriptionDotTxt(var2, var0, var9);
                     --var8;
                  }
               }
            } catch (Exception var11) {
               ExceptionLogger.logException(var11);
               return null;
            }

            Maps.put(var0, var2);
            return var2;
         }
      }
   }

   public static Mod getModDetails(String var0) {
      if (MissingMods.contains(var0)) {
         return null;
      } else if (Mods.containsKey(var0)) {
         return (Mod)Mods.get(var0);
      } else {
         String var1 = ZomboidFileSystem.instance.getModDir(var0);
         if (var1 == null) {
            ArrayList var2 = tempStrings;
            ZomboidFileSystem.instance.getAllModFolders(var2);

            for(int var3 = 0; var3 < var2.size(); ++var3) {
               Mod var4 = readModInfo((String)var2.get(var3));
               Mods.putIfAbsent(var4.getId(), var4);
               ZomboidFileSystem.instance.setModIdToDir(var4.getId(), (String)var2.get(var3));
               if (var4.getId().equals(var0)) {
                  return var4;
               }
            }
         }

         Mod var5 = readModInfo(var1);
         if (var5 == null) {
            MissingMods.add(var0);
         }

         return var5;
      }
   }

   public static Mod getAvailableModDetails(String var0) {
      Mod var1 = getModDetails(var0);
      return var1 != null && var1.isAvailable() ? var1 : null;
   }

   public static Mod readModInfo(String var0) {
      Mod var1 = readModInfoAux(var0);
      if (var1 != null) {
         Mod var2 = (Mod)Mods.get(var1.getId());
         if (var2 == null) {
            Mods.put(var1.getId(), var1);
         } else if (var2 != var1) {
            ZomboidFileSystem.instance.getAllModFolders(tempStrings);
            int var3 = tempStrings.indexOf(var1.getDir());
            int var4 = tempStrings.indexOf(var2.getDir());
            if (var3 < var4) {
               Mods.put(var1.getId(), var1);
            }
         }
      }

      return var1;
   }

   private static Mod readModInfoAux(String var0) {
      if (var0 != null) {
         Mod var1 = ZomboidFileSystem.instance.getModInfoForDir(var0);
         if (var1.bRead) {
            return var1.bValid ? var1 : null;
         }

         var1.bRead = true;
         String var2 = var1.getVersionDir();
         String var3 = var1.getCommonDir();
         String var4 = var2 + File.separator + "mod.info";
         File var5 = new File(var4);
         if (!var5.exists()) {
            var4 = var3 + File.separator + "mod.info";
            var5 = new File(var4);
            if (!var5.exists()) {
               DebugLog.Mod.warn("can't find \"" + var4 + "\"");
               return null;
            }
         }

         var1.setId(var5.getParentFile().getName());

         try {
            InputStreamReader var6 = IndieFileLoader.getStreamReader(var4);

            Mod var28;
            label321: {
               Object var11;
               label322: {
                  label323: {
                     Object var14;
                     label324: {
                        String var13;
                        label325: {
                           String var10;
                           label326: {
                              try {
                                 BufferedReader var7 = new BufferedReader(var6);

                                 label297: {
                                    label296: {
                                       label295: {
                                          label294: {
                                             label293: {
                                                label292: {
                                                   try {
                                                      String var8;
                                                      while((var8 = var7.readLine()) != null) {
                                                         if (var8.contains("name=")) {
                                                            var1.name = var8.replace("name=", "");
                                                         } else {
                                                            String var9;
                                                            if (var8.contains("poster=")) {
                                                               var9 = var8.replace("poster=", "");
                                                               if (!StringUtils.isNullOrWhitespace(var9)) {
                                                                  var1.posters.add(var2 + File.separator + var9);
                                                               }
                                                            } else if (var8.contains("description=")) {
                                                               String var10001 = var1.desc;
                                                               var1.desc = var10001 + var8.replace("description=", "");
                                                            } else if (var8.contains("require=")) {
                                                               var1.setRequire(new ArrayList(Arrays.asList(var8.replace("require=", "").split(","))));
                                                            } else if (var8.contains("incompatible=")) {
                                                               var1.setIncompatible(new ArrayList(Arrays.asList(var8.replace("incompatible=", "").split(","))));
                                                            } else if (var8.contains("loadModAfter=")) {
                                                               var1.setLoadAfter(new ArrayList(Arrays.asList(var8.replace("loadModAfter=", "").split(","))));
                                                            } else if (var8.contains("loadModBefore=")) {
                                                               var1.setLoadBefore(new ArrayList(Arrays.asList(var8.replace("loadModBefore=", "").split(","))));
                                                            } else if (var8.contains("id=")) {
                                                               var1.setId(var8.replace("id=", ""));
                                                            } else if (var8.contains("author=")) {
                                                               var1.setAuthor(var8.replace("author=", ""));
                                                            } else if (var8.contains("modversion=")) {
                                                               var1.setModVersion(var8.replace("modversion=", ""));
                                                            } else if (var8.contains("icon=")) {
                                                               var1.setIcon(var2 + File.separator + var8.replace("icon=", ""));
                                                            } else if (var8.contains("category=")) {
                                                               var1.setCategory(var8.replace("category=", ""));
                                                            } else if (var8.contains("url=")) {
                                                               var1.setUrl(var8.replace("url=", ""));
                                                            } else {
                                                               int var26;
                                                               int var30;
                                                               if (var8.contains("pack=")) {
                                                                  var9 = var8.replace("pack=", "").trim();
                                                                  if (var9.isEmpty()) {
                                                                     DebugLog.Mod.error("pack= line requires a file name");
                                                                     var10 = null;
                                                                     break label297;
                                                                  }

                                                                  int var25 = TextureID.bUseCompressionOption ? 4 : 0;
                                                                  var25 |= 64;
                                                                  var26 = var9.indexOf("type=");
                                                                  byte var31;
                                                                  if (var26 != -1) {
                                                                     switch (var9.substring(var26 + "type=".length())) {
                                                                        case "ui":
                                                                           var25 = 2;
                                                                           break;
                                                                        default:
                                                                           DebugLog.Mod.error("unknown pack type=" + var27);
                                                                     }

                                                                     var30 = var9.indexOf(32);
                                                                     var9 = var9.substring(0, var30).trim();
                                                                  }

                                                                  var27 = var9;
                                                                  var13 = "";
                                                                  if (var9.endsWith(".floor")) {
                                                                     var27 = var9.substring(0, var9.lastIndexOf(46));
                                                                     var13 = ".floor";
                                                                     var25 &= -5;
                                                                  }

                                                                  var31 = 1;
                                                                  if (Core.SafeModeForced) {
                                                                     var31 = 1;
                                                                  }

                                                                  if (var31 == 2) {
                                                                     File var15 = new File(var2 + File.separator + "media" + File.separator + "texturepacks" + File.separator + var27 + "2x" + var13 + ".pack");
                                                                     if (var15.isFile()) {
                                                                        DebugLog.Mod.printf("2x version of %s.pack found.\n", var9);
                                                                        var9 = var27 + "2x" + var13;
                                                                     } else {
                                                                        var15 = new File(var2 + File.separator + "media" + File.separator + "texturepacks" + File.separator + var9 + "2x.pack");
                                                                        if (var15.isFile()) {
                                                                           DebugLog.Mod.printf("2x version of %s.pack found.\n", var9);
                                                                           var9 = var9 + "2x";
                                                                        } else {
                                                                           var15 = new File(var3 + File.separator + "media" + File.separator + "texturepacks" + File.separator + var27 + "2x" + var13 + ".pack");
                                                                           if (var15.isFile()) {
                                                                              DebugLog.Mod.printf("2x version of %s.pack found.\n", var9);
                                                                              var9 = var27 + "2x" + var13;
                                                                           } else {
                                                                              var15 = new File(var3 + File.separator + "media" + File.separator + "texturepacks" + File.separator + var9 + "2x.pack");
                                                                              if (var15.isFile()) {
                                                                                 DebugLog.Mod.printf("2x version of %s.pack found.\n", var9);
                                                                                 var9 = var9 + "2x";
                                                                              } else {
                                                                                 DebugLog.Mod.printf("2x version of %s.pack not found.\n", var9);
                                                                              }
                                                                           }
                                                                        }
                                                                     }
                                                                  }

                                                                  var1.addPack(var9, var25);
                                                               } else if (var8.contains("tiledef=")) {
                                                                  String[] var24 = var8.replace("tiledef=", "").trim().split("\\s+");
                                                                  if (var24.length != 2) {
                                                                     DebugLog.Mod.error("tiledef= line requires file name and file number");
                                                                     var10 = null;
                                                                     break label296;
                                                                  }

                                                                  var10 = var24[0];

                                                                  try {
                                                                     var26 = Integer.parseInt(var24[1]);
                                                                  } catch (NumberFormatException var20) {
                                                                     DebugLog.Mod.error("tiledef= line requires file name and file number");
                                                                     var13 = null;
                                                                     break label295;
                                                                  }

                                                                  byte var12 = 100;
                                                                  boolean var29 = true;
                                                                  var30 = 16382;
                                                                  if (var26 < var12 || var26 > var30) {
                                                                     DebugLog.Mod.error("tiledef=%s %d file number must be from %d to %d", var10, var26, Integer.valueOf(var12), var30);
                                                                     var14 = null;
                                                                     break label294;
                                                                  }

                                                                  var1.addTileDef(var10, var26);
                                                               } else if (var8.startsWith("versionMax=")) {
                                                                  var9 = var8.replace("versionMax=", "").trim();
                                                                  if (!var9.isEmpty()) {
                                                                     try {
                                                                        var1.versionMax = GameVersion.parse(var9);
                                                                     } catch (Exception var18) {
                                                                        DebugLog.Mod.error("invalid versionMax: " + var18.getMessage());
                                                                        var11 = null;
                                                                        break label293;
                                                                     }
                                                                  }
                                                               } else if (var8.startsWith("versionMin=")) {
                                                                  var9 = var8.replace("versionMin=", "").trim();
                                                                  if (!var9.isEmpty()) {
                                                                     try {
                                                                        var1.versionMin = GameVersion.parse(var9);
                                                                     } catch (Exception var19) {
                                                                        DebugLog.Mod.error("invalid versionMin: " + var19.getMessage());
                                                                        var11 = null;
                                                                        break label292;
                                                                     }
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }

                                                      if (var1.getUrl() == null) {
                                                         var1.setUrl("");
                                                      }

                                                      var1.bValid = true;
                                                      var28 = var1;
                                                   } catch (Throwable var21) {
                                                      try {
                                                         var7.close();
                                                      } catch (Throwable var17) {
                                                         var21.addSuppressed(var17);
                                                      }

                                                      throw var21;
                                                   }

                                                   var7.close();
                                                   break label321;
                                                }

                                                var7.close();
                                                break label322;
                                             }

                                             var7.close();
                                             break label323;
                                          }

                                          var7.close();
                                          break label324;
                                       }

                                       var7.close();
                                       break label325;
                                    }

                                    var7.close();
                                    break label326;
                                 }

                                 var7.close();
                              } catch (Throwable var22) {
                                 if (var6 != null) {
                                    try {
                                       var6.close();
                                    } catch (Throwable var16) {
                                       var22.addSuppressed(var16);
                                    }
                                 }

                                 throw var22;
                              }

                              if (var6 != null) {
                                 var6.close();
                              }

                              return var10;
                           }

                           if (var6 != null) {
                              var6.close();
                           }

                           return var10;
                        }

                        if (var6 != null) {
                           var6.close();
                        }

                        return var13;
                     }

                     if (var6 != null) {
                        var6.close();
                     }

                     return (Mod)var14;
                  }

                  if (var6 != null) {
                     var6.close();
                  }

                  return (Mod)var11;
               }

               if (var6 != null) {
                  var6.close();
               }

               return (Mod)var11;
            }

            if (var6 != null) {
               var6.close();
            }

            return var28;
         } catch (Exception var23) {
            ExceptionLogger.logException(var23);
         }
      }

      return null;
   }

   public static final class Map {
      private String dir;
      private Texture thumb;
      private Texture worldmap;
      private String spawnSelectImagePyramid;
      private String title;
      private ArrayList<String> lotsDir;
      private float zoomX;
      private float zoomY;
      private float zoomS;
      private String demoVideo;
      private String desc;
      private boolean bFixed2x;

      public Map() {
      }

      public String getDirectory() {
         return this.dir;
      }

      public void setDirectory(String var1) {
         this.dir = var1;
      }

      public Texture getThumbnail() {
         return this.thumb;
      }

      public void setThumbnail(Texture var1) {
         this.thumb = var1;
      }

      public Texture getWorldmap() {
         return this.worldmap;
      }

      public void setWorldmap(Texture var1) {
         this.worldmap = var1;
      }

      public String getSpawnSelectImagePyramid() {
         return this.spawnSelectImagePyramid;
      }

      public String getTitle() {
         return this.title;
      }

      public void setTitle(String var1) {
         this.title = var1;
      }

      public ArrayList<String> getLotDirectories() {
         return this.lotsDir;
      }

      public float getZoomX() {
         return this.zoomX;
      }

      public void setZoomX(float var1) {
         this.zoomX = var1;
      }

      public float getZoomY() {
         return this.zoomY;
      }

      public void setZoomY(float var1) {
         this.zoomY = var1;
      }

      public float getZoomS() {
         return this.zoomS;
      }

      public void setZoomS(float var1) {
         this.zoomS = var1;
      }

      public String getDemoVideo() {
         return this.demoVideo;
      }

      public void setDemoVideo(String var1) {
         this.demoVideo = var1;
      }

      public String getDescription() {
         return this.desc;
      }

      public void setDescription(String var1) {
         this.desc = var1;
      }

      public boolean isFixed2x() {
         return this.bFixed2x;
      }

      public void setFixed2x(boolean var1) {
         this.bFixed2x = var1;
      }
   }

   public static final class Mod {
      public String dir;
      public String commonDir;
      public String versionDir;
      public final ZomboidFileSystem.PZModFolder baseFile = new ZomboidFileSystem.PZModFolder();
      public final ZomboidFileSystem.PZModFolder mediaFile = new ZomboidFileSystem.PZModFolder();
      public final ZomboidFileSystem.PZModFolder actionGroupsFile = new ZomboidFileSystem.PZModFolder();
      public final ZomboidFileSystem.PZModFolder animSetsFile = new ZomboidFileSystem.PZModFolder();
      public final ZomboidFileSystem.PZModFolder animsXFile = new ZomboidFileSystem.PZModFolder();
      private final ArrayList<String> posters = new ArrayList();
      public Texture tex;
      private ArrayList<String> require;
      private ArrayList<String> incompatible;
      private ArrayList<String> loadAfter;
      private ArrayList<String> loadBefore;
      private String name = "Unnamed Mod";
      private String desc = "";
      private String id = "";
      private String url;
      private String author;
      private String modVersion;
      private String icon;
      private String category;
      private String workshopID = "";
      private boolean bAvailableDone = false;
      private boolean available = true;
      private GameVersion versionMin;
      private GameVersion versionMax;
      private final ArrayList<PackFile> packs = new ArrayList();
      private final ArrayList<TileDef> tileDefs = new ArrayList();
      private boolean bRead = false;
      private boolean bValid = false;

      public Mod(String var1) {
         this.dir = var1;
         File var2 = new File(var1, "common");
         File var3 = ZomboidFileSystem.instance.getModVersionFile(var1);
         this.commonDir = var2.getAbsolutePath();
         this.versionDir = var3 == null ? "" : var3.getAbsolutePath();
         this.baseFile.setWithCatch(var2, var3);
         this.mediaFile.setWithCatch(new File(this.baseFile.common.canonicalFile, "media"), new File(this.baseFile.version.canonicalFile, "media"));
         this.actionGroupsFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "actiongroups"), new File(this.mediaFile.version.canonicalFile, "actiongroups"));
         this.animSetsFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "AnimSets"), new File(this.mediaFile.version.canonicalFile, "AnimSets"));
         this.animsXFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "anims_X"), new File(this.mediaFile.version.canonicalFile, "anims_X"));
         File var4 = this.baseFile.common.canonicalFile.getParentFile();
         if (var4 != null) {
            var4 = var4.getParentFile();
            if (var4 != null) {
               this.workshopID = SteamWorkshop.instance.getIDFromItemInstallFolder(var4.getAbsolutePath());
               if (this.workshopID == null) {
                  this.workshopID = "";
               }
            }
         }

      }

      public Texture getTexture() {
         if (this.tex == null) {
            String var1 = this.posters.isEmpty() ? null : (String)this.posters.get(0);
            if (!StringUtils.isNullOrWhitespace(var1)) {
               this.tex = Texture.getSharedTexture(var1);
            }

            if (this.tex == null || this.tex.isFailure()) {
               if (Core.bDebug && this.tex == null) {
                  String var10001 = var1 == null ? this.id : var1;
                  DebugLog.Mod.println("failed to load poster " + var10001);
               }

               this.tex = Texture.getWhite();
            }
         }

         return this.tex;
      }

      public void setTexture(Texture var1) {
         this.tex = var1;
      }

      public int getPosterCount() {
         return this.posters.size();
      }

      public String getPoster(int var1) {
         return var1 >= 0 && var1 < this.posters.size() ? (String)this.posters.get(var1) : null;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String var1) {
         this.name = var1;
      }

      public String getDir() {
         return this.dir;
      }

      public String getCommonDir() {
         return this.commonDir;
      }

      public String getVersionDir() {
         return this.versionDir;
      }

      public String getDescription() {
         return this.desc;
      }

      public ArrayList<String> getRequire() {
         return this.require;
      }

      public void setRequire(ArrayList<String> var1) {
         this.require = var1;
      }

      public ArrayList<String> getIncompatible() {
         return this.incompatible;
      }

      public void setIncompatible(ArrayList<String> var1) {
         this.incompatible = var1;
      }

      public ArrayList<String> getLoadAfter() {
         return this.loadAfter;
      }

      public void setLoadAfter(ArrayList<String> var1) {
         this.loadAfter = var1;
      }

      public ArrayList<String> getLoadBefore() {
         return this.loadBefore;
      }

      public void setLoadBefore(ArrayList<String> var1) {
         this.loadBefore = var1;
      }

      public String getId() {
         return this.workshopID + "\\" + this.id;
      }

      public void setId(String var1) {
         this.id = var1;
      }

      public boolean isAvailable() {
         if (this.bAvailableDone) {
            return this.available;
         } else {
            this.bAvailableDone = true;
            if (!this.isAvailableSelf()) {
               this.available = false;
               return false;
            } else {
               ChooseGameInfo.tempStrings.clear();
               ChooseGameInfo.tempStrings.add(this.getId());
               if (!this.isAvailableRequired(ChooseGameInfo.tempStrings)) {
                  this.available = false;
                  return false;
               } else {
                  this.available = true;
                  return true;
               }
            }
         }
      }

      public boolean isAvailableSelf() {
         GameVersion var1 = Core.getInstance().getGameVersion();
         if (this.versionMin != null && this.versionMin.isGreaterThan(var1)) {
            return false;
         } else {
            return this.versionMax == null || !this.versionMax.isLessThan(var1);
         }
      }

      private boolean isAvailableRequired(ArrayList<String> var1) {
         if (this.require != null && !this.require.isEmpty()) {
            for(int var2 = 0; var2 < this.require.size(); ++var2) {
               String var3 = ((String)this.require.get(var2)).trim();
               if (!var1.contains(var3)) {
                  var1.add(var3);
                  Mod var4 = ChooseGameInfo.getModDetails(var3);
                  if (var4 == null) {
                     return false;
                  }

                  if (!var4.isAvailableSelf()) {
                     return false;
                  }

                  if (!var4.isAvailableRequired(var1)) {
                     return false;
                  }
               }
            }

            return true;
         } else {
            return true;
         }
      }

      /** @deprecated */
      @Deprecated
      public void setAvailable(boolean var1) {
      }

      public String getUrl() {
         return this.url == null ? "" : this.url;
      }

      public void setUrl(String var1) {
         if (var1.startsWith("http://theindiestone.com") || var1.startsWith("http://www.theindiestone.com") || var1.startsWith("http://pz-mods.net") || var1.startsWith("http://www.pz-mods.net") || var1.startsWith("https://discord.gg")) {
            this.url = var1;
         }

      }

      public String getAuthor() {
         return this.author == null ? "" : this.author;
      }

      public void setAuthor(String var1) {
         this.author = var1;
      }

      public String getModVersion() {
         return this.modVersion == null ? "" : this.modVersion;
      }

      public void setModVersion(String var1) {
         this.modVersion = var1;
      }

      public String getIcon() {
         return this.icon == null ? "" : this.icon;
      }

      public void setIcon(String var1) {
         this.icon = var1;
      }

      public String getCategory() {
         return this.category == null ? "" : this.category;
      }

      public void setCategory(String var1) {
         this.category = var1;
      }

      public GameVersion getVersionMin() {
         return this.versionMin;
      }

      public GameVersion getVersionMax() {
         return this.versionMax;
      }

      public void addPack(String var1, int var2) {
         this.packs.add(new PackFile(var1, var2));
      }

      public void addTileDef(String var1, int var2) {
         this.tileDefs.add(new TileDef(var1, var2));
      }

      public ArrayList<PackFile> getPacks() {
         return this.packs;
      }

      public ArrayList<TileDef> getTileDefs() {
         return this.tileDefs;
      }

      public String getWorkshopID() {
         return this.workshopID;
      }
   }

   public static final class TileDef {
      public String name;
      public int fileNumber;

      public TileDef(String var1, int var2) {
         this.name = var1;
         this.fileNumber = var2;
      }
   }

   public static final class PackFile {
      public final String name;
      public final int flags;

      public PackFile(String var1, int var2) {
         this.name = var1;
         this.flags = var2;
      }
   }

   public static final class SpawnOrigin {
      public int x;
      public int y;
      public int w;
      public int h;

      public SpawnOrigin(int var1, int var2, int var3, int var4) {
         this.x = var1;
         this.y = var2;
         this.w = var3;
         this.h = var4;
      }
   }
}
