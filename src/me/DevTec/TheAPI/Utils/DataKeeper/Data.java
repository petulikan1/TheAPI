package me.DevTec.TheAPI.Utils.DataKeeper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import me.DevTec.TheAPI.SortedMap.SortedMap;
import me.DevTec.TheAPI.Utils.StringUtils;
import me.DevTec.TheAPI.Utils.File.Reader;
import me.DevTec.TheAPI.Utils.File.Writer;
import me.DevTec.TheAPI.Utils.Reflections.Ref;

public class Data {
	
	public static class Dat {
		private Object o;
		private List<String> c = Lists.newArrayList();
		public Dat(Object item, List<String> c) {
			o=item;
			this.c=c;
		}
		
		public Dat() {
		}
		
		public Dat(Object value) {
			o=value;
		}

		public List<String> getComments() {
			return c;
		}
		
		public Object get() {
			return o;
		}
		
		public void set(Object o) {
			this.o=o;
		}
	}
	
	public Data() {
		
	}
	
	private File f;
	public Data(File toRead) {
		f=toRead;
		load(this, f);
	}
	
	private HashMap<String, Dat> map = Maps.newHashMap();
	private static Gson g = new Gson();
	
	private static String getSpaces(String s) {
		String space = "";
		for(char c : s.toCharArray())
			if(c==' ')
				space+=" ";
			else break;
		return space;
	}
	
	//Whole Config or Byte[] in String
	@SuppressWarnings("unchecked")
	public static void load(Data d, File f) {
		try {
			ByteArrayInputStream bos = new ByteArrayInputStream(Base64.getDecoder().decode(Reader.read(f, false)));
			GZIPInputStream zos = new GZIPInputStream(bos);
			ObjectInputStream ous = new ObjectInputStream(zos);
			while(true)
				try {
					String key = ous.readUTF();
					d.set(key, ous.readObject());
					d.setComments(key, (List<String>)ous.readObject());
				}catch(Exception e) {
				break;
				}
			bos.close();
			zos.close();
			ous.close();
		}catch(Exception e) {
			String key = "";
			StringBuilder b = new StringBuilder();
			List<Object> items = Lists.newArrayList();
			List<String> comments = Lists.newArrayList();
			int last = 0, splitting=0;
			for(String split : Reader.read(f, true).split(System.lineSeparator())) {
				if(split.startsWith("# ")) {
					comments.add(split.replaceFirst(split.split("# ")[0]+"# ", ""));
					continue;
				}
				if(split.trim().isEmpty())
					continue;
				if(split.startsWith(getSpaces(split)+"- ")) {
					String item = removeQuetos(split.replaceFirst(split.split("- ")[0]+"- ", "").replaceFirst(" ", ""));
					try {
						String i = item.substring(2, item.length()-1).replaceFirst(Pattern.quote(item.split(",")[0])+",", "");
						String clas = i.split("\",")[0], json = i.replaceFirst(clas+"\",", "");
						items.add(g.fromJson(json, Ref.getClass(clas)));
					} catch (Exception e1) {
						items.add(item);
					}
					continue;
				}
				if(!items.isEmpty()) {
					d.set(key, items);
					d.setComments(key, comments);
					comments = Lists.newArrayList();
					items = Lists.newArrayList();
				}
				if(splitting!=0) {
					if(split.contains(":") && split.replaceFirst(split.split(":")[0], "").startsWith(" ")) {
					if(splitting==1) {
					d.set(key, b.toString());
					b=new StringBuilder();
					}else {
						d.set(key, items);
						items = Lists.newArrayList();
					}
					splitting=0;
					d.setComments(key, comments);
					comments = Lists.newArrayList();
					}else {
						String space = "  ";
						for(int i = 0; i < last; ++i)
							space+="  ";
						if(splitting==2) {
						try {
							String item = split.replaceFirst(space, "");
							String i = item.substring(2, item.length()-1).replaceFirst(Pattern.quote(item.split(",")[0])+",", "");
							String clas = i.split("\",")[0], json = i.replaceFirst(clas+"\",", "");
							items.add(g.fromJson(json, Ref.getClass(clas)));
						} catch (Exception e1) {
							items.add(split.replaceFirst(space, ""));
						}}else
						b.append(split.replaceFirst(space, ""));
						continue;
					}
				}
				if(split.split("  ").length<last) {
					if(!split.startsWith(" "))key="";
					else {
						for(int i = 0; i < last-split.split("  ").length+1; ++i)
							try {
							key=key.substring(0,key.length()-((key.split("\\.")[key.split("\\.").length-1]).length()+1));
							}catch(Exception er) {
								key=key.substring(0,key.length()-((key.split("\\.")[key.split("\\.").length-1]).length()));
								break;
							}
					}
				}else
				if(split.split("  ").length==last) {
					try {
					key=key.substring(0,key.length()-((key.split("\\.")[key.split("\\.").length-1]).length()+1));
					}catch(Exception er) {
						key=key.substring(0,key.length()-((key.split("\\.")[key.split("\\.").length-1]).length()));
					}
				}
				last = split.split("  ").length;
				
				key+=(key.equals("")?"":".")+split.split(":")[0].trim();
				if(split.replaceFirst(key+":", "").trim().isEmpty())continue;
				String item = removeQuetos(split.replaceFirst(split.split(":")[0]+":", "").replaceFirst(" ", ""));
				if(item.trim().equals("|")) {
					splitting=1;
				}else
					if(item.trim().equals("|-")) {
						splitting=2;
					}
				else{
				try {
					String i = item.substring(2, item.length()-1).replaceFirst(Pattern.quote(item.split(",")[0])+",", "");
					String clas = i.split("\",")[0], json = i.replaceFirst(clas+"\",", "");
					d.set(key, g.getAdapter(Ref.getClass(clas)).fromJson(json));
					d.setComments(key, comments);
				} catch (Exception e1) {
					d.set(key, item);
					d.setComments(key, comments);
				}
				comments = Lists.newArrayList();
			}}
			if(!items.isEmpty()) {
				d.set(key, items);
				d.setComments(key, comments);
			}
		}
	}
	
	public void setFile(File f) {
		this.f=f;
	}
	
	public File getFile() {
		return f;
	}
	
	public List<String> getComments(String key) {
		if(key.startsWith("#")||key.trim().isEmpty())return Lists.newArrayList();
		Dat d = map.getOrDefault(key, new Dat());
		return d.getComments();
	}
	
	public void addComment(String key, String comment) {
		if(key.startsWith("#")||key.trim().isEmpty())return;
		Dat d = map.getOrDefault(key, new Dat());
		if(comment!=null)
			d.getComments().add(comment);
		map.put(key, d);
	}
	
	public void addComments(String key, List<String> comments) {
		if(key.startsWith("#")||key.trim().isEmpty())return;
		Dat d = map.getOrDefault(key, new Dat());
		if(comments!=null)
			d.getComments().addAll(comments);
		map.put(key, d);
	}
	
	public void setComments(String key, List<String> comments) {
		if(key.startsWith("#")||key.trim().isEmpty())return;
		Dat d = map.getOrDefault(key, new Dat());
		d.getComments().clear();
		if(comments!=null)
			d.getComments().addAll(comments);
		map.put(key, d);
	}

	public Set<String> getKeys() {
		HashSet<String> a = Sets.newHashSet();
		for(String d : map.keySet())
			if(!d.contains("."))a.add(d);
		return a;
	}

	public Set<String> getKeys(String key) {
		return getKeys(key, false);
	}
	
	public Set<String> getKeys(String key, boolean subkeys) {
		HashSet<String> a = Sets.newHashSet();
		for(String d : map.keySet())
			if(d.startsWith(key) && !d.replaceFirst(d.split(key)[0], "").startsWith("."))
				if(subkeys)
					a.add(d.replaceFirst(key+"\\.", ""));
				else
				a.add((d.replaceFirst(key+"\\.", "")).split("\\.")[0]);
		return a;
	}
	
	public Set<String> keySet() {
		return map.keySet();
	}
	
	public int size() {
		return map.size();
	}
	
	public void set(String key, Object value) {
		if(value==null) {
			remove(key);
			return;
		}
		if(key.startsWith("#")||key.trim().isEmpty())return;
		Dat d = map.getOrDefault(key, new Dat());
		d.set(value);
		map.put(key, d);
	}
	
	public boolean exists(String key) {
		boolean a = false;
		for(String d : map.keySet())
			if(d.contains(key)) {
				String f = d.replaceFirst(key.replace(".", "\\."), "");
			if(f.startsWith(".")||f.trim().isEmpty()) {
				a=true;
				break;
			}}
		return a;
	}
	
	public Object get(String key) {
		return map.containsKey(key)?map.get(key).get():null;
	}
	
	public short getShort(String key) {
		try {
			return (short)get(key);
		} catch (Exception error) {
			return StringUtils.getShort(key);
	}}
	
	public byte getByte(String key) {
		try {
			return (byte)get(key);
		} catch (Exception error) {
			return StringUtils.getByte(key);
		}
	}
	
	public float getFloat(String key) {
		try {
			return (float)get(key);
		} catch (Exception error) {
			try {
				return StringUtils.getFloat(getString(key));
			} catch (Exception errorr) {
				return 0;
			}
		}
	}
	
	public long getLong(String key) {
		try {
			return (long)get(key);
		} catch (Exception error) {
			return StringUtils.getLong(key);
		}
	}
	
	public int getInt(String key) {
		try {
			return (int)get(key);
		} catch (Exception error) {
			return StringUtils.getInt(key);
		}
	}
	
	public double getDouble(String key) {
		try {
			return (double)get(key);
		} catch (Exception error) {
			try {
				return StringUtils.getDouble(getString(key));
			} catch (Exception errorr) {
				return 0;
			}
		}
	}
	
	public String getString(String key) {
		try {
			return (String)get(key);
		} catch (Exception error) {
			return null;
		}
	}
	
	public double[] getDoubleArray(String key) {
		try {
			return (double[])get(key);
		} catch (Exception error) {
			return new double[0];
		}
	}
	
	public byte[] getByteArray(String key) {
		try {
			return (byte[])get(key);
		} catch (Exception error) {
			return new byte[0];
		}
	}
	
	public int[] getIntArray(String key) {
		try {
			return (int[])get(key);
		} catch (Exception error) {
			return new int[0];
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String key) {
		try {
			return (List<T>)get(key);
		} catch (Exception error) {
			return Lists.newArrayList();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getStringList(String key) {
		try {
			return get(key) instanceof List<?> ? (List<String>)get(key):Lists.newArrayList();
		} catch (Exception error) {
			return Lists.newArrayList();
		}
	}
	
	public boolean getBoolean(String key) {
		try {
			return (boolean)get(key);
		} catch (Exception error) {
			try {
				return Boolean.parseBoolean(get(key).toString());
			} catch (Exception errorr) {
				return false;
			}
		}
	}
	
	public void remove(String key) {
		map.remove(key);
	}
	
	public String toString() {
		return toString(DataType.DATA);
	}
	
	public String toString(DataType type) {
		if(type==DataType.DATA) {
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				GZIPOutputStream zos = new GZIPOutputStream(bos);
				ObjectOutputStream ous = new ObjectOutputStream(zos);
				for(String key : map.keySet()) {
					try {
						ous.writeUTF(key);
						ous.writeObject(map.get(key).get());
						ous.writeObject(map.get(key).getComments());
					}catch(Exception er) {}
				}
				zos.finish();
				return Base64.getEncoder().encodeToString(bos.toByteArray());
			}catch(Exception e) {}
			return Base64.getEncoder().encodeToString(new byte[0]);
		}else {
			StringBuilder f = new StringBuilder();
			List<String> exists = Lists.newArrayList();
			if(type==DataType.SORTED_YAML)
				map=SortedMap.sortByKey(map);
			for(String entry : map.keySet()) {
				String sp = "", path="";
				for(String s : entry.split("\\.")) {
					path+=(path.equals("")?"":".")+s;
					if(!exists.contains(path)) {
						exists.add(path);
						if(map.containsKey(path))
						for(String c : map.get(path).c)
							f.append(sp+"# "+c+System.lineSeparator());
						Object get = get(path);
						if(get!=null) {
							if(get instanceof List) {
								f.append(sp+s+":"+System.lineSeparator());
								for(Object o : (List<?>)get) {
									f.append(sp+"- "+addQuetos(o, true)+System.lineSeparator());
								}
							}else {
								f.append(sp+s+": "+addQuetos(get, true)+System.lineSeparator());
							}
						}else
							f.append(sp+s+":"+System.lineSeparator());
					}
					sp+="  ";
				}
			}
			return f.toString();
		}
			
	}
	
    private static String removeQuetos(String value) {
    	try {
        if (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length()-1);
        }
    	}catch(Exception er) {}
        return value;
    }

    private static String addQuetos(Object value, boolean toJson) {
    	if(value instanceof Serializable)
            return "'" + value + "'";
    	if(toJson)return "[\""+value.getClass().getName()+"\","+g.toJson(value)+"]";
        return "\"" + value + "\"";
    }

	public void writeToFile(DataType yaml) {
		if(f!=null && yaml != null)
		writeToFile(f, yaml);
	}

	public void writeToFile(File f, DataType yaml) {
		Writer we=new Writer(f);
		we.append(toString(yaml));
		we.flush();
		we.close();
	}

	public void load(File file, boolean removeData) {
		if(removeData)
			map.clear();
		load(this, file);
	}
}