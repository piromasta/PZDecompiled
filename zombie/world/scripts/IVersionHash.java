package zombie.world.scripts;

import zombie.world.WorldDictionaryException;

public interface IVersionHash {
   boolean isEmpty();

   String getString();

   void add(String var1);

   void add(IVersionHash var1);

   long getHash() throws WorldDictionaryException;
}
