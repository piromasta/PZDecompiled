package zombie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.entity.util.BitSet;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.list.PZUnmodifiableList;

public class TaggedObjectManager<T extends TaggedObject> {
   private static final String defaultTag = "untagged".toLowerCase();
   private final HashMap<String, Integer> tagStringToIndexMap = new HashMap();
   private final HashMap<Integer, String> tagIndexToStringMap = new HashMap();
   private final HashMap<String, List<T>> tagToObjectListMap = new HashMap();
   private final List<String> registeredTags = new ArrayList();
   private final List<String> registeredTagsView;
   private final HashMap<String, TagGroup<T>> tagGroupMap;
   private final List<TagGroup<T>> tagGroups;
   private final HashMap<String, String> tagsStringAliasMap;
   private final List<T> _emptyTagObjects;
   private final List<String> _tempStringList;
   private final BackingListProvider<T> backingListProvider;
   private boolean verbose;
   private boolean warnNonPreprocessedNewTag;

   public TaggedObjectManager(BackingListProvider<T> var1) {
      this.registeredTagsView = PZUnmodifiableList.wrap(this.registeredTags);
      this.tagGroupMap = new HashMap();
      this.tagGroups = new ArrayList();
      this.tagsStringAliasMap = new HashMap();
      this._emptyTagObjects = new ArrayList();
      this._tempStringList = new ArrayList();
      this.verbose = false;
      this.warnNonPreprocessedNewTag = true;
      this.backingListProvider = (BackingListProvider)Objects.requireNonNull(var1);
      this.registerTag(defaultTag, false);
   }

   public void setVerbose(boolean var1) {
      this.verbose = var1;
   }

   public boolean isVerbose() {
      return this.verbose;
   }

   public void setWarnNonPreprocessedNewTag(boolean var1) {
      this.warnNonPreprocessedNewTag = var1;
   }

   public boolean isWarnNonPreprocessedNewTag() {
      return this.warnNonPreprocessedNewTag;
   }

   public void clear() {
      this.tagStringToIndexMap.clear();
      this.tagIndexToStringMap.clear();
      this.registeredTags.clear();
      this.tagGroupMap.clear();
      this.tagGroups.clear();
      this.tagsStringAliasMap.clear();
   }

   public void setDirty() {
      for(int var1 = 0; var1 < this.tagGroups.size(); ++var1) {
         ((TagGroup)this.tagGroups.get(var1)).dirty = true;
      }

   }

   public List<String> getRegisteredTags() {
      return this.registeredTagsView;
   }

   public void getRegisteredTagGroups(ArrayList<String> var1) {
      Iterator var2 = this.tagGroupMap.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         var1.add((String)var3.getKey());
      }

   }

   public void registerObjectsFromBackingList() {
      this.registerObjectsFromBackingList(false);
   }

   public void registerObjectsFromBackingList(boolean var1) {
      if (this.verbose) {
         DebugLog.General.println("Registering objects from backing list...");
      }

      if (var1) {
         this.clear();
         this.registerTag(defaultTag, false);
      }

      List var2 = this.backingListProvider.getTaggedObjectList();

      for(int var4 = 0; var4 < var2.size(); ++var4) {
         TaggedObject var3 = (TaggedObject)var2.get(var4);
         this.registerObject(var3, false);
      }

   }

   public void registerObject(T var1, boolean var2) {
      if (this.verbose) {
         DebugLog.General.println("register tagged object: " + var1);
      }

      var1.getTagBits().clear();
      List var3 = var1.getTags();

      int var4;
      for(var4 = 0; var4 < var3.size(); ++var4) {
         this.registerTag((String)var3.get(var4), var2);
      }

      if (var3.size() == 0) {
         var4 = (Integer)this.tagStringToIndexMap.get(defaultTag);
         var1.getTagBits().set(var4);
         ((List)this.tagToObjectListMap.get(defaultTag)).add(var1);
      } else {
         for(int var5 = 0; var5 < var3.size(); ++var5) {
            String var6 = this.sanitizeTag((String)var3.get(var5));
            var4 = (Integer)this.tagStringToIndexMap.get(var6);
            var1.getTagBits().set(var4);
            ((List)this.tagToObjectListMap.get(var6)).add(var1);
         }
      }

      if (var2) {
         this.setDirty();
      }

   }

   private String sanitizeTag(String var1) {
      return var1 != null ? var1.trim().toLowerCase() : var1;
   }

   private int registerTag(String var1, boolean var2) {
      var1 = this.sanitizeTag(var1);
      if (!this.registeredTags.contains(var1)) {
         if (this.verbose) {
            DebugLog.General.println("register new tag: " + var1);
         }

         int var3 = this.registeredTags.size() + 1;
         this.registeredTags.add(var1);
         this.tagStringToIndexMap.put(var1, var3);
         this.tagIndexToStringMap.put(var3, var1);
         this.tagToObjectListMap.put(var1, new ArrayList());
         if (var2) {
            this.setDirty();
         }

         return var3;
      } else {
         return this.registeredTags.indexOf(var1) + 1;
      }
   }

   public List<T> getListForTag(String var1) {
      List var2 = (List)this.tagToObjectListMap.get(this.sanitizeTag(var1));
      return var2 != null ? var2 : this._emptyTagObjects;
   }

   public List<T> getListForTag(int var1) {
      String var2 = (String)this.tagIndexToStringMap.get(var1);
      return var2 != null ? this.getListForTag(var2) : this._emptyTagObjects;
   }

   public List<T> queryTaggedObjects(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         DebugLog.General.warn("manager-> returning empty list for: " + var1);
         return this._emptyTagObjects;
      } else {
         TagGroup var2 = (TagGroup)this.tagGroupMap.get(var1);
         if (var2 != null) {
            if (this.verbose) {
               DebugLog.General.println("manager-> returning cached list for: " + var1);
            }

            return var2.getUpdatedClientView();
         } else {
            String var3 = (String)this.tagsStringAliasMap.get(var1);
            if (var3 != null) {
               var2 = (TagGroup)this.tagGroupMap.get(var3);
               if (var2 != null) {
                  if (this.verbose) {
                     DebugLog.General.println("manager-> returning cached list for alias '" + var3 + "', cache: " + var1);
                  }

                  return var2.getUpdatedClientView();
               }
            }

            String var4 = this.formatQueryString(var1);
            var2 = (TagGroup)this.tagGroupMap.get(var4);
            if (var2 != null) {
               if (this.verbose) {
                  DebugLog.General.println("manager-> created new alias '" + var1 + "' for: " + var4);
               }

               this.tagsStringAliasMap.put(var1, var4);
               return var2.getUpdatedClientView();
            } else {
               String[] var5 = this.readWhitelist(var4);
               String[] var6 = this.readBlackList(var4);
               BitSet var7 = this.createTagBits(var5);
               BitSet var8 = this.createTagBits(var6);
               boolean var9 = var7.notEmpty();
               boolean var10 = var8.notEmpty();
               if (!var9 && !var10) {
                  DebugLog.General.warn("manager-> could not gather objects for key: " + var1);
                  return this._emptyTagObjects;
               } else {
                  var2 = new TagGroup(this, var7, var8);
                  this.populateTagGroupList(var2, false);
                  this.tagGroupMap.put(var4, var2);
                  this.tagGroups.add(var2);
                  if (this.verbose) {
                     DebugLog.General.println("manager-> created new set for: " + var4);
                  }

                  return var2.getUpdatedClientView();
               }
            }
         }
      }
   }

   private BitSet createTagBits(String[] var1) {
      return this.createTagBits(var1, true);
   }

   private BitSet createTagBits(String[] var1, boolean var2) {
      BitSet var3 = new BitSet();
      if (var1 != null) {
         for(int var5 = 0; var5 < var1.length; ++var5) {
            String var4 = var1[var5];
            Integer var6 = (Integer)this.tagStringToIndexMap.get(var4);
            if (var6 != null) {
               var3.set(var6);
            } else if (var2) {
               if (this.warnNonPreprocessedNewTag) {
                  DebugLog.General.warn("manager-> new tag discovered that was not preprocessed, tag: " + var4);
               }

               int var7 = this.registerTag(var4, true);
               var3.set(var7);
            }
         }
      }

      return var3;
   }

   private List<T> populateTagGroupList(TagGroup<T> var1, boolean var2) {
      return this.populateTaggedObjectList(var1.whitelist, var1.blacklist, var1.list, var2);
   }

   private List<T> populateTaggedObjectList(BitSet var1, BitSet var2, List<T> var3, boolean var4) {
      return this.populateTaggedObjectList(var1, var2, var3, (List)null, var4);
   }

   private List<T> populateTaggedObjectList(BitSet var1, BitSet var2, List<T> var3, List<T> var4, boolean var5) {
      if (var5 && var3.size() > 0) {
         var3.clear();
      }

      List var6;
      if (var4 != null) {
         var6 = var4;
      } else {
         var6 = this.backingListProvider.getTaggedObjectList();
      }

      boolean var7 = var1.notEmpty();
      boolean var8 = var2.notEmpty();

      for(int var10 = 0; var10 < var6.size(); ++var10) {
         TaggedObject var9 = (TaggedObject)var6.get(var10);
         if ((!var7 || var9.getTagBits().intersects(var1)) && (!var8 || !var9.getTagBits().intersects(var2))) {
            var3.add(var9);
         }
      }

      return var3;
   }

   public List<T> filterList(String var1, List<T> var2, List<T> var3, boolean var4) {
      if (var4) {
         var2.clear();
      }

      if (StringUtils.isNullOrWhitespace(var1)) {
         if (this.verbose) {
            DebugLog.General.warn("manager-> query string empty, returning input list for: " + var1);
         }

         return var2;
      } else {
         var1 = this.formatQueryString(var1);
         String[] var5 = this.readWhitelist(var1);
         String[] var6 = this.readBlackList(var1);
         BitSet var7 = this.createTagBits(var5, false);
         BitSet var8 = this.createTagBits(var6, false);
         boolean var9 = var7.notEmpty();
         boolean var10 = var8.notEmpty();
         if (!var9 && !var10) {
            if (this.verbose) {
               DebugLog.General.warn("manager-> could not gather objects for key: " + var1);
            }

            return var2;
         } else {
            List var11;
            if (var3 != null) {
               var11 = var3;
            } else {
               var11 = this.backingListProvider.getTaggedObjectList();
            }

            for(int var13 = 0; var13 < var11.size(); ++var13) {
               TaggedObject var12 = (TaggedObject)var11.get(var13);
               if ((!var9 || var12.getTagBits().intersects(var7)) && (!var10 || !var12.getTagBits().intersects(var8))) {
                  var2.add(var12);
               }
            }

            return var2;
         }
      }
   }

   public List<T> populateList(String var1, List<T> var2, List<T> var3, boolean var4) {
      if (var4) {
         var2.clear();
      }

      if (StringUtils.isNullOrWhitespace(var1)) {
         if (this.verbose) {
            DebugLog.General.warn("manager-> query string empty, returning input list for: " + var1);
         }

         return var2;
      } else {
         var1 = this.formatQueryString(var1);
         String[] var5 = this.readWhitelist(var1);
         String[] var6 = this.readBlackList(var1);
         BitSet var7 = this.createTagBits(var5);
         BitSet var8 = this.createTagBits(var6);
         boolean var9 = var7.notEmpty();
         boolean var10 = var8.notEmpty();
         if (!var9 && !var10) {
            DebugLog.General.warn("manager-> could not gather objects for key: " + var1);
            return var2;
         } else {
            return this.populateTaggedObjectList(var7, var8, var2, var3, var4);
         }
      }
   }

   public String formatAndRegisterQueryString(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("Key is null or whitespace.");
      } else if (this.tagGroupMap.containsKey(var1)) {
         return var1;
      } else {
         var1 = this.formatQueryString(var1);
         if (!this.tagGroupMap.containsKey(var1)) {
            this.queryTaggedObjects(var1);
         }

         return var1;
      }
   }

   public String formatQueryString(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         return var1;
      } else {
         String[] var2 = this.readWhitelist(var1);
         String[] var3 = this.readBlackList(var1);
         StringBuilder var4 = new StringBuilder();
         this._tempStringList.clear();
         int var5;
         if (var2 != null) {
            for(var5 = 0; var5 < var2.length; ++var5) {
               this._tempStringList.add(this.sanitizeTag(var2[var5]));
            }

            Collections.sort(this._tempStringList);

            for(var5 = 0; var5 < this._tempStringList.size(); ++var5) {
               if (var5 == 0) {
                  var4.append((String)this._tempStringList.get(var5));
               } else {
                  var4.append(";").append((String)this._tempStringList.get(var5));
               }
            }
         }

         this._tempStringList.clear();
         if (var3 != null) {
            var4.append("-");

            for(var5 = 0; var5 < var3.length; ++var5) {
               this._tempStringList.add(this.sanitizeTag(var3[var5]));
            }

            Collections.sort(this._tempStringList);

            for(var5 = 0; var5 < this._tempStringList.size(); ++var5) {
               if (var5 == 0) {
                  var4.append((String)this._tempStringList.get(var5));
               } else {
                  var4.append(";").append((String)this._tempStringList.get(var5));
               }
            }
         }

         return var4.toString();
      }
   }

   private String[] readWhitelist(String var1) {
      if (!var1.contains("-")) {
         return var1.split(";");
      } else if (var1.startsWith("-")) {
         return null;
      } else {
         String[] var2 = var1.split("-");
         return var2[0].split(";");
      }
   }

   private String[] readBlackList(String var1) {
      if (!var1.contains("-")) {
         return null;
      } else if (var1.startsWith("-")) {
         String var3 = var1.substring(1, var1.length());
         return var3.split(";");
      } else {
         String[] var2 = var1.split("-");
         return var2[1].split(";");
      }
   }

   public void debugPrint() {
      this.debugPrint((ArrayList)null);
   }

   public void debugPrint(ArrayList<String> var1) {
      this.debugLog("[TaggedObjectManager]", var1);
      this.debugLog("{", var1);
      this.debugLog("[registeredTags]", var1);
      this.debugLog("{", var1);

      for(int var2 = 0; var2 < this.registeredTags.size(); ++var2) {
         String var3 = (String)this.registeredTags.get(var2);
         this.debugLog("  " + var2 + " = " + var3, var1);
      }

      this.debugLog("}", var1);
      this.debugLog("", var1);
      this.debugLog("[tagStringToIndexMap]", var1);
      this.debugLog("{", var1);
      Iterator var6 = this.tagStringToIndexMap.entrySet().iterator();

      Map.Entry var7;
      while(var6.hasNext()) {
         var7 = (Map.Entry)var6.next();
         this.debugLog("  " + (String)var7.getKey() + " = " + var7.getValue(), var1);
      }

      this.debugLog("}", var1);
      this.debugLog("", var1);
      this.debugLog("[tagIndexToStringMap]", var1);
      this.debugLog("{", var1);
      var6 = this.tagIndexToStringMap.entrySet().iterator();

      while(var6.hasNext()) {
         var7 = (Map.Entry)var6.next();
         this.debugLog("  " + var7.getKey() + " = " + (String)var7.getValue(), var1);
      }

      this.debugLog("}", var1);
      this.debugLog("", var1);
      this.debugLog("[tagToObjectListMap]", var1);
      this.debugLog("{", var1);
      var6 = this.tagToObjectListMap.entrySet().iterator();

      Iterator var4;
      TaggedObject var5;
      while(var6.hasNext()) {
         var7 = (Map.Entry)var6.next();
         this.debugLog("  " + (String)var7.getKey(), var1);
         this.debugLog("  {", var1);
         var4 = ((List)var7.getValue()).iterator();

         while(var4.hasNext()) {
            var5 = (TaggedObject)var4.next();
            if (var5 instanceof BaseScriptObject) {
               this.debugLog("    " + ((BaseScriptObject)var5).getScriptObjectFullType(), var1);
            } else {
               this.debugLog("    " + var5, var1);
            }
         }

         this.debugLog("  }", var1);
      }

      this.debugLog("}", var1);
      this.debugLog("", var1);
      this.debugLog("[tagStringAliasMap]", var1);
      this.debugLog("{", var1);
      var6 = this.tagsStringAliasMap.entrySet().iterator();

      while(var6.hasNext()) {
         var7 = (Map.Entry)var6.next();
         this.debugLog("  " + (String)var7.getKey() + " = " + (String)var7.getValue(), var1);
      }

      this.debugLog("}", var1);
      this.debugLog("", var1);
      this.debugLog("[tagGroupMap]", var1);
      this.debugLog("{", var1);
      var6 = this.tagGroupMap.entrySet().iterator();

      while(var6.hasNext()) {
         var7 = (Map.Entry)var6.next();
         this.debugLog("  [" + (String)var7.getKey() + "]", var1);
         this.debugLog("  {", var1);
         this.debugLog("    whitelist = " + this.getBitSetString(((TagGroup)var7.getValue()).whitelist), var1);
         this.debugLog("    blacklist = " + this.getBitSetString(((TagGroup)var7.getValue()).blacklist), var1);
         this.debugLog("    [objects]", var1);
         this.debugLog("    {", var1);
         var4 = ((TagGroup)var7.getValue()).list.iterator();

         while(var4.hasNext()) {
            var5 = (TaggedObject)var4.next();
            if (var5 instanceof BaseScriptObject) {
               this.debugLog("      " + ((BaseScriptObject)var5).getScriptObjectFullType(), var1);
            } else {
               this.debugLog("      " + var5, var1);
            }
         }

         this.debugLog("    }", var1);
         this.debugLog("  }", var1);
      }

      this.debugLog("}", var1);
      this.debugLog("}", var1);
   }

   private String getBitSetString(BitSet var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append("{ ");
      boolean var3 = false;

      for(int var4 = 0; var4 < var1.length(); ++var4) {
         if (var1.get(var4)) {
            if (var3) {
               var2.append(", ");
            }

            var2.append((String)this.tagIndexToStringMap.get(var4));
            var2.append("(");
            var2.append(var4);
            var2.append(")");
            var3 = true;
         }
      }

      var2.append(" }");
      return var2.toString();
   }

   private void debugLog(String var1, ArrayList<String> var2) {
      if (var2 != null) {
         var2.add(var1);
      } else {
         DebugLog.log(var1);
      }

   }

   public interface BackingListProvider<T extends TaggedObject> {
      List<T> getTaggedObjectList();
   }

   private static class TagGroup<T extends TaggedObject> {
      private final TaggedObjectManager<T> manager;
      private final BitSet whitelist;
      private final BitSet blacklist;
      private final List<T> list = new ArrayList();
      private final List<T> clientView;
      private boolean dirty = false;

      private TagGroup(TaggedObjectManager<T> var1, BitSet var2, BitSet var3) {
         this.manager = var1;
         this.whitelist = var2;
         this.blacklist = var3;
         this.clientView = PZUnmodifiableList.wrap(this.list);
      }

      private List<T> getUpdatedClientView() {
         if (this.dirty) {
            this.manager.populateTaggedObjectList(this.whitelist, this.blacklist, this.list, true);
            this.dirty = false;
         }

         return this.clientView;
      }
   }

   public interface TaggedObject {
      List<String> getTags();

      BitSet getTagBits();
   }
}
