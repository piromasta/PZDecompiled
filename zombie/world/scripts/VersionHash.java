package zombie.world.scripts;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;
import zombie.world.WorldDictionaryException;

public class VersionHash implements IVersionHash {
   private boolean dirty = false;
   private boolean hasHashed = false;
   private long hash = 0L;
   private String toHash = "";
   private boolean corrupted = false;

   public VersionHash() {
   }

   public String getString() {
      return this.toHash;
   }

   public void reset() {
      this.toHash = "";
      this.dirty = false;
      this.hash = 0L;
      this.corrupted = false;
      this.hasHashed = false;
   }

   public boolean isEmpty() {
      return StringUtils.isNullOrEmpty(this.toHash);
   }

   public boolean isCorrupted() {
      return this.corrupted;
   }

   public void add(String var1) {
      if (var1 != null) {
         this.toHash = this.toHash + var1;
         this.dirty = true;
      } else {
         this.corrupted = true;
         DebugLog.General.error("Trying to add a null String to hash.");
      }

   }

   public void add(IVersionHash var1) {
      if (var1 != null) {
         String var10001 = this.toHash;
         this.toHash = var10001 + var1.getString();
         this.dirty = true;
      } else {
         this.corrupted = true;
         DebugLog.General.error("Trying to add a null IVersionHash to hash.");
      }

   }

   public long getHash() throws WorldDictionaryException {
      if (this.corrupted) {
         throw new WorldDictionaryException("Corrupted hash");
      } else if (this.hasHashed) {
         if (this.dirty) {
            throw new WorldDictionaryException("ToHash is dirty");
         } else {
            return this.hash;
         }
      } else {
         this.dirty = false;
         HashCode var1 = Hashing.sha256().hashString(this.toHash, StandardCharsets.UTF_8);
         this.hash = var1.asLong();
         this.hasHashed = true;
         return this.hash;
      }
   }
}
