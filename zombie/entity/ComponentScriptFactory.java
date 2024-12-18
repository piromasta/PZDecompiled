package zombie.entity;

import zombie.entity.util.Null;
import zombie.entity.util.ObjectMap;
import zombie.entity.util.reflect.ClassReflection;
import zombie.entity.util.reflect.Constructor;
import zombie.entity.util.reflect.ReflectionException;
import zombie.scripting.entity.ComponentScript;

public class ComponentScriptFactory {
   private final ObjectMap<Class<?>, ScriptConstructor> scriptConstructors = new ObjectMap();

   public ComponentScriptFactory() {
   }

   public <T extends ComponentScript> T create(Class<T> var1) {
      ScriptConstructor var2 = (ScriptConstructor)this.scriptConstructors.get(var1);
      if (var2 == null) {
         var2 = new ScriptConstructor(var1);
         this.scriptConstructors.put(var1, var2);
      }

      return var2.obtain();
   }

   private static class ScriptConstructor<T extends ComponentScript> {
      private final Constructor constructor;

      public ScriptConstructor(Class<T> var1) {
         this.constructor = this.findConstructor(var1);
         if (this.constructor == null) {
            throw new RuntimeException("Class cannot be created (missing no-arg constructor): " + var1.getName());
         }
      }

      @Null
      private Constructor findConstructor(Class<T> var1) {
         try {
            return ClassReflection.getConstructor(var1, (Class[])null);
         } catch (Exception var5) {
            try {
               Constructor var3 = ClassReflection.getDeclaredConstructor(var1, (Class[])null);
               var3.setAccessible(true);
               return var3;
            } catch (ReflectionException var4) {
               return null;
            }
         }
      }

      protected T obtain() {
         try {
            return (ComponentScript)this.constructor.newInstance((Object[])null);
         } catch (Exception var2) {
            throw new RuntimeException("Unable to create new instance: " + this.constructor.getDeclaringClass().getName(), var2);
         }
      }
   }
}
