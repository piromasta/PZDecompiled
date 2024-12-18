package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;

@XmlRootElement
public class VoiceStyles {
   @XmlElement(
      name = "style"
   )
   public final ArrayList<VoiceStyle> m_Styles = new ArrayList();
   @XmlTransient
   public static VoiceStyles instance;

   public static void init() {
      String var10000 = ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath();
      instance = Parse(var10000 + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar));
      if (instance != null) {
         instance.m_Styles.add(0, new VoiceStyle());
         Iterator var0 = ZomboidFileSystem.instance.getModIDs().iterator();

         while(true) {
            while(true) {
               String var1;
               ChooseGameInfo.Mod var2;
               do {
                  if (!var0.hasNext()) {
                     return;
                  }

                  var1 = (String)var0.next();
                  var2 = ChooseGameInfo.getAvailableModDetails(var1);
               } while(var2 == null);

               String var3 = var2.getVersionDir();
               VoiceStyles var4 = Parse(var3 + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar));
               Iterator var5;
               VoiceStyle var6;
               VoiceStyle var7;
               int var8;
               if (var4 != null) {
                  var5 = var4.m_Styles.iterator();

                  while(var5.hasNext()) {
                     var6 = (VoiceStyle)var5.next();
                     var7 = instance.FindStyle(var6.prefix);
                     if (var7 == null) {
                        instance.m_Styles.add(var6);
                     } else {
                        if (DebugLog.isEnabled(DebugType.Sound)) {
                           DebugLog.Sound.println("mod \"%s\" overrides voice \"%s\"", var1, var6.prefix);
                        }

                        var8 = instance.m_Styles.indexOf(var7);
                        instance.m_Styles.set(var8, var6);
                     }
                  }
               } else {
                  var3 = var2.getCommonDir();
                  var4 = Parse(var3 + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar));
                  if (var4 != null) {
                     var5 = var4.m_Styles.iterator();

                     while(var5.hasNext()) {
                        var6 = (VoiceStyle)var5.next();
                        var7 = instance.FindStyle(var6.prefix);
                        if (var7 == null) {
                           instance.m_Styles.add(var6);
                        } else {
                           if (DebugLog.isEnabled(DebugType.Sound)) {
                              DebugLog.Sound.println("mod \"%s\" overrides voice \"%s\"", var1, var6.prefix);
                           }

                           var8 = instance.m_Styles.indexOf(var7);
                           instance.m_Styles.set(var8, var6);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void Reset() {
      if (instance != null) {
         instance.m_Styles.clear();
         instance = null;
      }
   }

   public VoiceStyles() {
   }

   public static VoiceStyles Parse(String var0) {
      try {
         return parse(var0);
      } catch (FileNotFoundException var2) {
      } catch (IOException | JAXBException var3) {
         ExceptionLogger.logException(var3);
      }

      return null;
   }

   public static VoiceStyles parse(String var0) throws JAXBException, IOException {
      FileInputStream var1 = new FileInputStream(var0);

      VoiceStyles var4;
      try {
         JAXBContext var2 = JAXBContext.newInstance(new Class[]{VoiceStyles.class});
         Unmarshaller var3 = var2.createUnmarshaller();
         var4 = (VoiceStyles)var3.unmarshal(var1);
      } catch (Throwable var6) {
         try {
            var1.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      var1.close();
      return var4;
   }

   public VoiceStyle FindStyle(String var1) {
      for(int var2 = 0; var2 < this.m_Styles.size(); ++var2) {
         VoiceStyle var3 = (VoiceStyle)this.m_Styles.get(var2);
         if (var3.prefix.equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   public VoiceStyles getInstance() {
      return instance;
   }

   public ArrayList<VoiceStyle> getAllStyles() {
      return this.m_Styles;
   }
}
