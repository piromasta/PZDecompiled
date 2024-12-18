package zombie.util.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class RegExFilenameFilter implements FilenameFilter {
   private final Pattern pattern;

   public RegExFilenameFilter(String var1) {
      this.pattern = Pattern.compile(var1);
   }

   public RegExFilenameFilter(Pattern var1) {
      this.pattern = var1;
   }

   public boolean accept(File var1, String var2) {
      return this.pattern.matcher(var2).matches();
   }
}
