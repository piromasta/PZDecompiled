package zombie;

import java.io.File;
import java.util.function.Predicate;
import zombie.debug.DebugLog;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;

public final class PredicatedFileWatcher {
   private final String m_path;
   private final Predicate<String> m_predicate;
   private final IPredicatedFileWatcherCallback m_callback;

   public PredicatedFileWatcher(Predicate<String> var1, IPredicatedFileWatcherCallback var2) {
      this((String)null, (Predicate)var1, (IPredicatedFileWatcherCallback)var2);
   }

   public PredicatedFileWatcher(String var1, IPredicatedFileWatcherCallback var2) {
      this(var1, (Predicate)null, (IPredicatedFileWatcherCallback)var2);
   }

   public <T> PredicatedFileWatcher(String var1, Class<T> var2, IPredicatedDataPacketFileWatcherCallback<T> var3) {
      this(var1, (Predicate)null, (IPredicatedFileWatcherCallback)(new GenericPredicatedFileWatcherCallback(var2, var3)));
   }

   public PredicatedFileWatcher(String var1, Predicate<String> var2, IPredicatedFileWatcherCallback var3) {
      this.m_path = this.processPath(var1);
      this.m_predicate = var2 != null ? var2 : this::pathsEqual;
      this.m_callback = var3;
   }

   public String getPath() {
      return this.m_path;
   }

   private String processPath(String var1) {
      return var1 != null ? ZomboidFileSystem.processFilePath(var1, File.separatorChar) : null;
   }

   private boolean pathsEqual(String var1) {
      return var1.equals(this.m_path);
   }

   public void onModified(String var1) {
      if (this.m_predicate.test(var1)) {
         this.m_callback.call(var1);
      }

   }

   public interface IPredicatedFileWatcherCallback {
      void call(String var1);
   }

   public static class GenericPredicatedFileWatcherCallback<T> implements IPredicatedFileWatcherCallback {
      private final Class<T> m_class;
      private final IPredicatedDataPacketFileWatcherCallback<T> m_callback;

      public GenericPredicatedFileWatcherCallback(Class<T> var1, IPredicatedDataPacketFileWatcherCallback<T> var2) {
         this.m_class = var1;
         this.m_callback = var2;
      }

      public void call(String var1) {
         Object var2;
         try {
            var2 = PZXmlUtil.parse(this.m_class, var1);
         } catch (PZXmlParserException var4) {
            DebugLog.General.error("Exception thrown. " + var4);
            return;
         }

         this.m_callback.call(var2);
      }
   }

   public interface IPredicatedDataPacketFileWatcherCallback<T> {
      void call(T var1);
   }
}
